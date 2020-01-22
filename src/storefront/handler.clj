(ns storefront.handler
  (:require [bidi.bidi :as bidi]
            [catalog.categories :as categories]
            catalog.keypaths
            [catalog.facets :as facets]
            [catalog.product-details :as product-details]
            [catalog.products :as products]
            [catalog.skuers :as skuers]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as string]
            [clojure.xml :as xml]
            [comb.template :as template]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [lambdaisland.uri :as uri]
            [noir-exception.core :refer [wrap-exceptions wrap-internal-error]]
            [ring-logging.core :as ring-logging]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.defaults :refer :all]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.codec :as codec]
            [ring.util.response :as util.response]
            [spice.core :as spice]
            [spice.maps :as maps]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.skus :as skus]
            [storefront.assets :as assets]
            [storefront.backend-api :as api]
            [storefront.config :as config]
            [storefront.cookies :as cookies]
            [storefront.events :as events]
            [adventure.keypaths :as adventure-keypaths]
            [storefront.keypaths :as keypaths]
            [storefront.routes :as routes]
            [storefront.system.contentful :as contentful]
            [storefront.transitions :as transitions]
            [storefront.views :as views]))

(def root-domain-pages-to-preserve-paths-in-redirects
  #{"/mayvenn-made"})

(defn ^:private path-for
  "Like routes/path-for, but preserves query params."
  ([req navigation-event] (path-for req navigation-event nil))
  ([req navigation-event args] (routes/path-for navigation-event (merge {:query-params (:query-params req)} args))))

(defn ^:private str->int [s]
  (try
    (Integer/parseInt s)
    (catch NumberFormatException _
      nil)))

(defn render-static-page [template]
  (template/eval template {:url assets/path}))

(defn static-page [[navigate-kw content-kw & static-content-id]]
  (when (= [navigate-kw content-kw] events/navigate-content)
    {:id      static-content-id
     :content (->> static-content-id
                   (map name)
                   (string/join "-")
                   (format "public/content/%s.html")
                   io/resource
                   slurp
                   render-static-page)}))

(defn storefront-site-defaults
  [environment]
  (if (config/development? environment)
    site-defaults
    (-> secure-site-defaults
        (assoc :proxy true)
        (assoc-in [:security :hsts] false)
        (assoc-in [:params :multipart] false)
        (assoc-in [:static :resources] false))))

(defn parse-root-domain [server-name]
  (->> (string/split server-name #"\.")
       (take-last 2)
       (string/join ".")))

(defn cookie-root-domain [server-name]
  (str "." (parse-root-domain server-name)))

;; Until @weavejester merges a PR
;; https://github.com/ring-clojure/ring-codec/pull/18
(extend-protocol codec/FormEncodeable
  nil
  (form-encode* [x encoding] ""))

(defn query-string [{:keys [query-params query-string] :as req}]
  (cond
    (and (contains? req :query-params)
         (seq query-params))
    (try
      (str "?" (codec/form-encode query-params))
      (catch IllegalArgumentException e
        (throw
         (ex-info "Unable to encode query params"
                  {:query-params query-params
                   :query-string query-string}
                  e))))

    (contains? req :query-params)
    ""

    (seq query-string)
    (str "?" query-string)

    :else nil))

(defn parse-subdomains [server-name]
  (->> (string/split server-name #"\.")
       (drop-last 2)))

(defn ->html-resp [h]
  (util.response/content-type (util.response/response h) "text/html"))

(defn store-scheme-and-authority [store-slug environment {:keys [scheme server-name server-port] :as req}]
  (let [dev? (config/development? environment)]
    (str (if dev? (name scheme) "https") "://"
         store-slug "." (parse-root-domain server-name)
         (if dev? (str ":" server-port) ""))))

(defn store-url [store-slug environment {:keys [uri] :as req}]
  (str (store-scheme-and-authority store-slug environment req)
       uri
       (query-string req)))

(defn store-homepage [store-slug environment req]
  (store-url store-slug environment (assoc req :uri "/")))

(defn redirect-to-home
  ([environment req]
   (redirect-to-home environment req :moved-permanently))
  ([environment {:keys [subdomains] :as req} redirect-type]
   (util.response/redirect (store-homepage (first subdomains) environment req)
                           redirect-type)))

(defn redirect-to-product-details [environment {:keys [subdomains params] :as req}]
  (util.response/redirect (str "/products/" (:id-and-slug params) "?SKU=" (:sku params))
                          :moved-permanently))

(defn wrap-no-cache [f]
  (fn no-cache-handler [req]
    (when-let [res (f req)]
      (let [headers (:headers res)
            cc      (get headers "cache-control")
            pragma  (get headers "pragma")]
        (if (or cc pragma)
          res
          ;; The proper way to force no-caching for all browsers
          ;; https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cache-Control#Preventing_caching
          ;;
          ;; Cache-Control: (http/1.1)
          ;;  - no-store: means the request & response shouldn't be saved in the cache
          ;;  - no-cache: means the proxies/clients must make the request to the origin server
          ;;  - must-revalidate: means proxies & clients must validate expiration time period
          ;;
          ;; Pragma: (http/1.0)
          ;;  - no-cache: behaves like 'cache-control: no-cache'
          ;;
          ;; Expires:
          ;;  - a date for when this response is considered stale, an invalid
          ;;    date (like 0) indicates already expired.
          (update res :headers merge
                  {"cache-control" "no-store, must-revalidate"
                   "pragma"        "no-cache"
                   "expires"       "0"}))))))

(defn wrap-add-domains [h]
  (fn [req]
    (h (merge req {:subdomains (parse-subdomains (:server-name req))}))))

(defn wrap-remove-superfluous-www-redirect [h environment]
  (fn [{:as req :keys [subdomains]}]
    (if (= "www" (first subdomains))
      (util.response/redirect (store-url (second subdomains) environment req))
      (h req))))

(defn- storeback-offline-response [environment]
  (do
    (config/dev-print! environment "Could not connect to storeback")
    (->html-resp (views/error-page (config/development? environment) "Could not connect to storeback"))))

(defn wrap-stylist-not-found-redirect [h environment]
  (fn [{server-name                        :server-name
        {store-slug :store-slug :as store} :store
        subdomains                         :subdomains
        cookies                            :cookies
        :as                                req}]
    (cond
      (= store :storefront.backend-api/storeback-unavailable)
      (storeback-offline-response environment)

      store-slug
      (h req)

      :else
      (-> (util.response/redirect
           (store-url "store" environment (assoc-in req [:query-params "redirect"] (last subdomains))))
          (cookies/expire environment
                          "preferred-store-slug"
                          {:http-only true
                           :domain    (cookie-root-domain server-name)})))))

(defn wrap-redirect-affiliates [h environment]
  (fn [{{experience :experience
         stylist-id :stylist-id} :store
        [nav-event _] :nav-message
        :as         req}]
    (if (routes/should-redirect-affiliate-route? experience)
      (util.response/redirect
       (store-url "shop" environment (update req :query-params merge {"affiliate_stylist_id" stylist-id})))
      (h req))))

(defn wrap-known-subdomains-redirect [h environment]
  (fn [{:keys [subdomains] :as req}]
    (cond
      (#{[] ["ambassador"] ["www"] ["internal"] ["vistaprint"] ["classes"] ["stylist"] ["community"]} subdomains)
      (util.response/redirect (store-url "shop" environment req) 301)

      (#{["peakmill"]} subdomains)
      (util.response/redirect (store-url "shop" environment req) 302)

      :else
      (h req))))

(defn wrap-set-preferred-store
  [handler environment]
  (fn [{:keys [server-name store] :as req}]
    (when-let [resp (handler req)]
      (cookies/set resp
                   environment
                   "preferred-store-slug"
                   (:store-slug store)
                   {:http-only true
                    :max-age   (cookies/days 365)
                    :domain    (cookie-root-domain server-name)}))))

(defn ^:private assoc-in-req-state [req keypath value]
  (update req :state assoc-in keypath value))

(defn ^:private update-in-req-state [req keypath fn & args]
  (apply update-in req (concat [:state] keypath) fn args))

(defn ^:private get-in-req-state
  ([req keypath]
   (get-in-req-state req keypath nil))
  ([req keypath default-value]
   (get-in req (concat [:state] keypath) default-value)))

(defn wrap-set-initial-state [h environment]
  (fn [req]
    (let [nav-message        (:nav-message req)
          [nav-event params] nav-message]
      (h (-> req
             (assoc-in-req-state keypaths/scheme (name (:scheme req)))
             (assoc-in-req-state keypaths/navigation-message nav-message)
             (assoc-in-req-state keypaths/navigation-uri (:nav-uri req))
             (assoc-in-req-state keypaths/static (static-page nav-event))
             (assoc-in-req-state keypaths/environment environment)
             (update-in-req-state [] experiments/determine-features))))))

(defn wrap-affiliate-initial-login-landing-navigation-message [h]
  (fn [{{experience :experience} :store
        :as                      req}]
    (h (cond->
        req

         (= "affiliate" experience)
         (assoc-in-req-state keypaths/return-navigation-message [events/navigate-stylist-account-profile {}])))))

(defn copy-cms-to-data
  ([cms-data data] data)
  ([cms-data data keypath]
   (assoc-in data
             keypath
             (get-in cms-data keypath))))

(defn wrap-set-cms-cache
  [h contentful]
  (fn [req]
    (let [shop?                                 (= "shop" (get-in-req-state req keypaths/store-slug))
          aladdin?                              (= "aladdin" (get-in-req-state req keypaths/store-experience))
          [nav-event
           {album-keyword :album-keyword
            product-id    :catalog/product-id}] (:nav-message req)
          update-data                           (partial copy-cms-to-data @(:cache contentful))]
      (h (update-in-req-state req keypaths/cms merge
                              (update-data {} [:advertisedPromo])
                              (cond shop?
                                    (cond (= events/navigate-mayvenn-made nav-event)
                                          (-> {}
                                              (update-data [:mayvennMadePage]))

                                          (= events/navigate-home nav-event)
                                          (-> {}
                                              (update-data [:homepage])
                                              (update-data [:ugc-collection :free-install-mayvenn])
                                              contentful/derive-all-looks)

                                          (contains? #{events/navigate-shop-by-look events/navigate-shop-by-look-details} nav-event)
                                          (-> {}
                                              (update-data [:ugc-collection (if (= :look album-keyword)
                                                                              :aladdin-free-install
                                                                              album-keyword)])
                                              contentful/derive-all-looks)

                                          (= events/navigate-product-details nav-event)
                                          (-> {}
                                              (update-data [:ugc-collection (some->> (conj keypaths/v2-products product-id :legacy/named-search-slug)
                                                                                     (get-in-req-state req)
                                                                                     keyword)])
                                              contentful/derive-all-looks)

                                          (routes/sub-page? [nav-event] [events/navigate-info])
                                          (-> {}
                                              (update-data [:ugc-collection :free-install-mayvenn])
                                              contentful/derive-all-looks)

                                          :else nil)

                                    aladdin?
                                    (cond (= events/navigate-mayvenn-made nav-event)
                                          (-> {}
                                              (update-data [:mayvennMadePage]))

                                          (= events/navigate-home nav-event)
                                          (-> {}
                                              (update-data [:homepage])
                                              (update-data [:ugc-collection :sleek-and-straight])
                                              (update-data [:ugc-collection :waves-and-curly])
                                              (update-data [:ugc-collection :free-install-mayvenn])
                                              contentful/derive-all-looks)

                                          (contains? #{events/navigate-shop-by-look events/navigate-shop-by-look-details} nav-event)
                                          (-> {}
                                              (update-data [:ugc-collection (if (= :look album-keyword)
                                                                              :aladdin-free-install
                                                                              album-keyword)])
                                              contentful/derive-all-looks)

                                          (= events/navigate-product-details nav-event)
                                          (-> {}
                                              (update-data [:ugc-collection (some->> (conj keypaths/v2-products product-id :legacy/named-search-slug)
                                                                                     (get-in-req-state req)
                                                                                     keyword)])
                                              contentful/derive-all-looks)
                                          :else nil)

                                    :else
                                    (cond (= events/navigate-mayvenn-made nav-event)
                                          (-> {}
                                              (update-data [:mayvennMadePage]))

                                          (= events/navigate-home nav-event)
                                          (-> {}
                                              (update-data [:homepage]))

                                          (contains? #{events/navigate-shop-by-look events/navigate-shop-by-look-details} nav-event)
                                          (-> {}
                                              (update-data [:ugc-collection album-keyword]))

                                          (= events/navigate-product-details nav-event)
                                          (-> {}
                                              (update-data [:ugc-collection (some->> (conj keypaths/v2-products product-id :legacy/named-search-slug)
                                                                                     (get-in-req-state req)
                                                                                     keyword)]))
                                          :else nil)))))))

(defn wrap-set-welcome-url [h welcome-config]
  (fn [req]
    (h (assoc-in-req-state req keypaths/welcome-url
                           (str (:url welcome-config) "?utm_source=shop&utm_medium=referral&utm_campaign=ShoptoWelcome")))))

(defn wrap-fetch-store [h storeback-config]
  (fn [{:keys [subdomains] :as req}]
    (let [store (api/store storeback-config (last subdomains))]
      (h (-> req
             (merge {:store store})
             (assoc-in-req-state keypaths/store store))))))

(defn wrap-fetch-order [h storeback-config]
  (fn [{:as req :keys [nav-message]}]
    (let [params       (second nav-message)
          order-number (cookies/get req "number")
          order-token  (cookies/get-and-attempt-parsing-poorly-encoded req "token")]
      (h (cond-> req
           (and order-number order-token
                (not (get-in-req-state req keypaths/order)))
           (assoc-in-req-state keypaths/order (api/get-order storeback-config order-number order-token)))))))

(defn wrap-fetch-completed-order [h storeback-config]
  (fn [{:as req :keys [nav-message]}]
    (let [[nav-event params] nav-message
          order-number       (cookies/get req "completed-order-number")
          order-token        (cookies/get-and-attempt-parsing-poorly-encoded req "completed-order-token")]
      (h (cond-> req
           (and order-number
                order-token
                (or (= :adventure (second nav-event)) ;; for stylist matching
                    (= events/navigate-order-complete nav-event)) ;; for stylist-matched checkout complete
                (not (get-in-req-state req keypaths/completed-order)))
           (assoc-in-req-state keypaths/completed-order (api/get-order storeback-config order-number order-token)))))))

(defn wrap-fetch-servicing-stylist-for-order
  [h storeback-config]
  (fn [req]
    (let [{:keys [servicing-stylist-id]} (get-in-req-state req keypaths/order)]
      (h (cond-> req
           servicing-stylist-id
           (assoc-in-req-state adventure.keypaths/adventure-servicing-stylist
                               (api/get-servicing-stylist storeback-config
                                                          servicing-stylist-id)))))))

(defn wrap-fetch-servicing-stylist-for-completed-order
  [h storeback-config]
  (fn [req]
    (let [servicing-stylist-id (get-in-req-state req keypaths/completed-order-servicing-stylist-id)]
      (h (cond-> req
           (and servicing-stylist-id
                (= events/navigate-order-complete (-> req :nav-message first)))
           (assoc-in-req-state adventure.keypaths/adventure-servicing-stylist
                               (api/get-servicing-stylist storeback-config
                                                          servicing-stylist-id)))))))

(defn wrap-adventure-route-params [h]
  (fn [{:as req :keys [nav-message]}]
    (let [params        (second nav-message)
          album-keyword (keyword (:album-keyword params))
          look-id       (spice/parse-int (:look-id params))]
      (h (cond-> req
           album-keyword (assoc-in-req-state keypaths/selected-album-keyword album-keyword)
           look-id       (assoc-in-req-state keypaths/selected-look-id look-id))))))

;; TODO: Add joining ability to v2 product queries
(defn wrap-fetch-catalog [h storeback-config]
  (fn [req]
    (let [order                      (get-in-req-state req keypaths/order)
          skus-on-order              (mapv :sku (orders/product-items order))
          skus-we-have               (keys (get-in-req-state req keypaths/v2-skus))
          needed-skus                (set/difference (set skus-on-order) (set skus-we-have))
          {order-skus     :skus
           order-products :products} (when (seq needed-skus)
                                       (api/fetch-v2-products storeback-config {:selector/sku-ids needed-skus}))
          {pdp-skus     :skus
           pdp-products :products}   (when-let [product-id (-> req :nav-message second :catalog/product-id)]
                                       (api/fetch-v2-products storeback-config {:catalog/product-id product-id}))
          {:keys [facets]}           (when-not (get-in-req-state req keypaths/v2-facets)
                                       (api/fetch-v2-facets storeback-config))]
      (h (-> req
             (update-in-req-state keypaths/v2-products merge (products/index-products (concat order-products pdp-products)))
             (update-in-req-state keypaths/v2-skus merge (products/index-skus (concat order-skus pdp-skus)))
             (assoc-in-req-state keypaths/v2-facets (map #(update % :facet/slug keyword) facets))
             (assoc-in-req-state keypaths/categories categories/initial-categories))))))

(defn wrap-set-user [h]
  (fn [req]
    (h (-> req
           (assoc-in-req-state keypaths/user-id (str->int (cookies/get req "id")))
           (assoc-in-req-state keypaths/user-token (cookies/get req "user-token"))
           (assoc-in-req-state keypaths/user-store-slug (cookies/get req "store-slug"))
           (assoc-in-req-state keypaths/user-store-id (str->int (cookies/get req "store-id")))
           (assoc-in-req-state keypaths/user-stylist-experience (cookies/get req "stylist-experience"))
           (assoc-in-req-state keypaths/user-email (cookies/get-and-attempt-parsing-poorly-encoded req "email"))))))

(defn wrap-fetch-promotions
  [h storeback-config]
  (fn [req]
    (h (assoc-in-req-state req keypaths/promotions
                           (api/get-promotions
                            storeback-config
                            (or (first (get-in-req-state req keypaths/order-promotion-codes))
                                (get-in-req-state req keypaths/pending-promo-code)))))))

;;TODO Have all of these middleswarez perform event transitions, just like the frontend
(defn wrap-state [routes {:keys [storeback-config welcome-config contentful environment]}]
  (-> routes
      (wrap-set-cms-cache contentful)
      (wrap-fetch-promotions storeback-config)
      (wrap-fetch-catalog storeback-config)
      (wrap-set-user)
      (wrap-set-welcome-url welcome-config)
      wrap-affiliate-initial-login-landing-navigation-message
      (wrap-set-initial-state environment)))

(defn wrap-site-routes
  [routes {:keys [storeback-config environment]}]
  (-> routes
      (wrap-set-preferred-store environment)
      (wrap-redirect-affiliates environment)
      (wrap-stylist-not-found-redirect environment)
      (wrap-defaults (dissoc (storefront-site-defaults environment) :cookies))
      (wrap-remove-superfluous-www-redirect environment)
      (wrap-fetch-store storeback-config)
      (wrap-known-subdomains-redirect environment)))

(defn wrap-logging [handler logger]
  (-> handler
      ring-logging/wrap-request-timing
      (ring-logging/wrap-logging logger ring-logging/simple-inbound-config)
      ring-logging/wrap-trace-request))

(declare server-render-pages)
(defn html-response [render-ctx data]
  (let [prerender? (contains? server-render-pages (get-in data keypaths/navigation-event))]
    (-> ((if prerender? views/prerendered-page views/index) render-ctx data)
        ->html-resp)))

(defn generic-server-render [render-ctx data req params]
  (html-response render-ctx data))

(defn redirect-to-cart [query-params]
  (util.response/redirect (str "/cart?" (codec/form-encode query-params))))

(defn redirect-named-search
  [render-ctx data req {:keys [named-search-slug]}]
  (let [categories (get-in data keypaths/categories)]
    (when-let [category (categories/named-search->category named-search-slug categories)]
      (-> (path-for req events/navigate-category category)
          (util.response/redirect :moved-permanently)))))

(def legacy-product-slug->new-location
  {"deluxe-body-wave-hair"             [:category "5"]
   "deluxe-curly-hair"                 [:category "9"]
   "deluxe-deep-wave-hair"             [:category "8"]
   "deluxe-straight-hair"              [:category "2"]
   "ultra-body-wave-hair"              [:category "5"]
   "ultra-deep-wave-hair"              [:category "8"]
   "ultra-straight-hair"               [:category "2"]

   "brazilian-curly-lace-closure"      [:product "35"]
   "brazilian-deep-wave"               [:product "11"]
   "brazilian-deep-wave-silk-closure"  [:product "19"]
   "brazilian-loose-wave"              [:product "22"]
   "brazilian-loose-wave-lace-closure" [:product "33"]
   "brazilian-natural-straight"        [:product "9"]
   "brazilian-straight-lace-closure"   [:product "16"]
   "malaysian-body-wave-lace-closure"  [:product "3"]
   "peruvian-loose-wave"               [:product "26"]
   "peruvian-loose-wave-silk-closure"  [:product "36"]
   "peruvian-straight"                 [:product "37"]})

(def discontinued-categories
  #{"19" "20"})

(def discontinued-products
  #{"102" "103" "104" "105" "106" "107" "108" "109" "116" "117"})

(defn redirect-legacy-product-page
  [render-ctx data req {:keys [legacy/product-slug]}]
  (when-let [[type id] (legacy-product-slug->new-location product-slug)]
    (let [product    (first (:products (api/fetch-v2-products (:storeback-config render-ctx) {:catalog/product-id id})))
          categories (maps/index-by :catalog/category-id (get-in data keypaths/categories))
          path       (if (= :product type)
                       (path-for req events/navigate-product-details product)
                       (path-for req events/navigate-category (categories id)))]
      (util.response/redirect path :moved-permanently))))


(def dyed-virgin-category-id->virgin-category
  {"16" {:catalog/category-id "2"
         :page/slug           "virgin-straight"}
   "17" {:catalog/category-id "0"
         :page/slug           "virgin-closures"}
   "18" {:catalog/category-id "1"
         :page/slug           "virgin-frontals"}})

(defn render-category
  [render-ctx data req {:keys [catalog/category-id page/slug]}]
  (let [virgin-category (get dyed-virgin-category-id->virgin-category category-id)]
    (cond
      (contains? discontinued-categories category-id)
      (util.response/redirect (path-for req events/navigate-home) :moved-permanently)

      virgin-category
      (util.response/redirect (path-for req events/navigate-category virgin-category) 302)

      :else
      (let [categories (get-in data keypaths/categories)]
        (when-let [category (categories/id->category category-id categories)]
          (cond
            (not= slug (:page/slug category)) (-> (path-for req events/navigate-category category)
                                                  (util.response/redirect :moved-permanently))
            (:page/redirect? category)        (util.response/redirect (path-for req events/navigate-home) :moved-permanently)
            :else                             (->> (assoc-in data
                                                             keypaths/current-category-id
                                                             (:catalog/category-id category))
                                                   (html-response render-ctx))))))))

(defn determine-sku-id [data product route-sku-id]
  (let [valid-product-skus (product-details/get-valid-product-skus product (get-in data keypaths/v2-skus))
        valid-sku-ids      (set (map :catalog/sku-id valid-product-skus))]
    (or (when (seq route-sku-id) (valid-sku-ids route-sku-id)) ;; Find the sku that matches the one in the uri
        (:catalog/sku-id ;; Fallback to epitome
         (skus/determine-epitome
          (facets/color-order-map (get-in data keypaths/v2-facets))
          valid-product-skus)))))

(def dyed-virgin-product-id->virgin-product
  {"89"  {:catalog/product-id "12" :page/slug "indian-straight-bundles"}
   "90"  {:catalog/product-id "16" :page/slug "indian-straight-lace-closures"}
   "93"  {:catalog/product-id "50" :page/slug "indian-straight-lace-frontals"}
   "94"  {:catalog/product-id "9" :page/slug "brazilian-straight-bundles"}
   "95"  {:catalog/product-id "8" :page/slug "malaysian-body-wave-bundles"}
   "96"  {:catalog/product-id "22" :page/slug "brazilian-loose-wave-bundles"}
   "97"  {:catalog/product-id "30" :page/slug "brazilian-deep-wave-bundles"}
   "98"  {:catalog/product-id "4" :page/slug "brazilian-straight-lace-closures"}
   "99"  {:catalog/product-id "3" :page/slug "malaysian-body-wave-lace-closures"}
   "100" {:catalog/product-id "33" :page/slug "brazilian-loose-wave-lace-closures"}
   "101" {:catalog/product-id "11" :page/slug "brazilian-deep-wave-lace-closures"}})

(defn render-product-details [{:keys [environment] :as render-ctx}
                              data
                              {:keys [params] :as req}
                              {:keys [catalog/product-id
                                      page/slug]}]
  (let [virgin-product (get dyed-virgin-product-id->virgin-product product-id)]
    (cond
      (contains? discontinued-products product-id)
      (util.response/redirect (path-for req events/navigate-home) :moved-permanently)

      virgin-product
      (util.response/redirect (path-for req events/navigate-product-details (merge params virgin-product))
                              :moved-permanently)

      :else
      (when-let [product (get-in data (conj keypaths/v2-products product-id))]
        (let [sku-id         (determine-sku-id data product (:SKU params))
              sku            (get-in data (conj keypaths/v2-skus sku-id))
              canonical-slug (:page/slug product)
              redirect?      (and canonical-slug
                                  (or (not= slug canonical-slug)
                                      (and sku-id (not sku))))
              permitted?     (auth/permitted-product? data product)]
          (cond
            (not permitted?)
            (redirect-to-home environment req)

            redirect?
            (let [path (products/path-for-sku product-id canonical-slug sku-id)]
              (util.response/redirect path))

            :else
            (html-response render-ctx
                           (-> data
                               (assoc-in catalog.keypaths/detailed-product-selected-sku sku)
                               (assoc-in catalog.keypaths/detailed-product-selected-sku-id sku-id)
                               (assoc-in catalog.keypaths/detailed-product-id product-id)))))))))

;;TODO Move to wrap set catalog
;;TODO join queries!!!
(defn- assoc-category-route-data [data storeback-config params]
  (let [category                (categories/id->category (:catalog/category-id params)
                                                         (get-in data keypaths/categories))
        {:keys [skus products]} (api/fetch-v2-products storeback-config (spice.maps/map-values
                                                                         first
                                                                         (skuers/essentials category)))]
    (-> data
        (assoc-in catalog.keypaths/category-id (:catalog/category-id params))
        (update-in keypaths/v2-products merge (products/index-products products))
        (update-in keypaths/v2-skus merge (products/index-skus skus)))))

(defn- transition [app-state [event args]]
  (reduce (fn [app-state dispatch]
            (or (transitions/transition-state dispatch event args app-state)
                app-state))
          app-state
          (reductions conj [] event)))

(defn frontend-routes [{:keys [storeback-config environment client-version] :as ctx}]
  (fn [{:keys [state] :as req}]
    (let [nav-message        (get-in-req-state req keypaths/navigation-message)
          [nav-event params] nav-message]
      (when (not= nav-event events/navigate-not-found)
        (let [render-ctx (maps/auto-map storeback-config environment client-version)
              data       (cond-> state
                           (= events/navigate-category nav-event)
                           (assoc-category-route-data storeback-config params)

                           (#{events/navigate-shop-by-look-details events/navigate-shop-by-look} nav-event)
                           (transition nav-message)

                           (= events/navigate-stylist-dashboard-cash-out-success nav-event)
                           (assoc-in keypaths/stylist-cash-out-balance-transfer-id (:balance-transfer-id params))

                           (= events/navigate-stylist-dashboard-cash-out-pending nav-event)
                           (assoc-in keypaths/stylist-cash-out-status-id (:status-id params)))
              render (server-render-pages nav-event generic-server-render)]
          (render render-ctx data req params))))))

(def user-specific-disalloweds ["User-agent: *"
                                "Disallow: /account"
                                "Disallow: /checkout"
                                "Disallow: /orders"
                                "Disallow: /stylist"
                                "Disallow: /cart"
                                "Disallow: /m/"
                                "Disallow: /c/"
                                "Disallow: /admin"
                                "Disallow: /content"
                                "Disallow: /policy/privacy"
                                "Disallow: /policy/tos"])

(def server-render-pages
  {events/navigate-home                      generic-server-render
   events/navigate-category                  render-category
   events/navigate-legacy-named-search       redirect-named-search
   events/navigate-legacy-ugc-named-search   redirect-named-search
   events/navigate-legacy-product-page       redirect-legacy-product-page
   events/navigate-product-details           render-product-details
   events/navigate-content-help              generic-server-render
   events/navigate-content-about-us          generic-server-render
   events/navigate-content-privacy           generic-server-render
   events/navigate-content-tos               generic-server-render
   events/navigate-content-guarantee         generic-server-render
   events/navigate-content-ugc-usage-terms   generic-server-render
   events/navigate-content-voucher-terms     generic-server-render
   events/navigate-content-program-terms     generic-server-render
   events/navigate-store-gallery             generic-server-render
   events/navigate-checkout-processing       generic-server-render
   events/navigate-mayvenn-made              generic-server-render})

(defn robots [{:keys [subdomains]}]
  (string/join "\n" user-specific-disalloweds))

(defn sitemap [{:keys [storeback-config]} {:keys [subdomains] :as req}]
  (if (seq subdomains)
    (if-let [launched-products (->> (api/fetch-v2-products storeback-config {})
                                    :products
                                    (filter :catalog/launched-at)
                                    (remove :catalog/discontinued-at)
                                    (remove :stylist-exclusives/experience)
                                    (remove :stylist-exclusives/family))]
      (letfn [(url-xml-elem [[location priority]]
                {:tag :url :content (cond-> [{:tag :loc :content [(str location)]}]
                                      priority (conj {:tag :priority :content [(str priority)]}))})]

        (-> (xml/emit {:tag     :urlset
                       :attrs   {:xmlns "http://www.sitemaps.org/schemas/sitemap/0.9"}
                       :content (->> (into [["https://mayvenn.com"]
                                            [(str "https://" config/welcome-subdomain ".mayvenn.com")       "0.60"]
                                            ["https://shop.mayvenn.com"                                     "1.00"]
                                            ["https://shop.mayvenn.com/guarantee"                           "0.60"]
                                            ["https://shop.mayvenn.com/help"                                "0.60"]
                                            ["https://shop.mayvenn.com/about-us"                            "0.60"]
                                            ["https://shop.mayvenn.com/shop/look"                           "0.80"]
                                            ["https://shop.mayvenn.com/shop/straight-looks"                 "0.80"]
                                            ["https://shop.mayvenn.com/shop/wavy-curly-looks"               "0.80"]
                                            ["https://shop.mayvenn.com/shop/all-bundle-sets"                "0.80"]
                                            ["https://shop.mayvenn.com/shop/straight-bundle-sets"           "0.80"]
                                            ["https://shop.mayvenn.com/shop/wavy-curly-bundle-sets"         "0.80"]
                                            ["https://shop.mayvenn.com/how-it-works"                        "0.80"]
                                            ["https://shop.mayvenn.com/certified-stylists"                  "0.80"]]

                                           (concat
                                            (for [{:keys [catalog/category-id page/slug seo/sitemap]} categories/initial-categories
                                                  :when                                               sitemap]
                                              [(str "https://shop.mayvenn.com/categories/" category-id "-" slug) "0.80"])

                                            (for [{:keys [catalog/product-id page/slug]} launched-products]
                                              [(str "https://shop.mayvenn.com/products/" product-id "-" slug) "0.80"])))
                                     (mapv url-xml-elem))})
            with-out-str
            util.response/response
            (util.response/content-type "text/xml")))
      (-> (util.response/response "<error />")
          (util.response/content-type "text/xml")
          (util.response/status 502)))
    (-> (util.response/response "")
        (util.response/status 404))))

(defn paypal-routes [{:keys [storeback-config]}]
  (GET "/orders/:order-number/paypal/:order-token" [order-number order-token :as request]
    (let [order          (get-in-req-state request keypaths/order)]
      (cond
        (or (nil? order)
            (not= (:number order) order-number)
            (not= (:token order) order-token)
            (not (#{"cart" "submitted"} (:state order))))
        (util.response/redirect "/cart?error=bad-request")

        (= "cart" (:state order))
        (util.response/redirect "/checkout/processing")

        (= "submitted" (:state order))
        (util.response/redirect (str "/orders/" (:number order) "/complete"))))))

(defn quadpay-routes [ctx]
  (GET "/orders/:order-number/quadpay" [order-number :as request]
    (let [order       (get-in-req-state request keypaths/order)
          order-token (get (:query-params request) "order-token")]
      (cond
        (or (nil? order)
            (not= (:number order) order-number)
            (not= (:token order) order-token)
            (not (#{"cart" "submitted"} (:state order))))
        (util.response/redirect "/checkout/payment?error=quadpay-invalid-state")

        (= "cart" (:state order))
        (util.response/redirect "/checkout/processing")

        (= "submitted" (:state order))
        (util.response/redirect (str "/orders/" (:number order) "/complete"))))))

(defn static-routes [_]
  (fn [{:keys [uri] :as req}]
    ;; can't use (:nav-message req) because routes/static-api-routes are not
    ;; included in routes/app-routes
    (let [{nav-event :handler} (bidi/match-route routes/static-api-routes uri)]
      (some-> nav-event routes/bidi->edn static-page :content ->html-resp))))

(defn wrap-filter-params
  "Technically an invalid value, but query-params could generate this value
  which doesn't serialize to EDN correctly.
  "
  [h]
  (fn [req]
    (h (update req :query-params dissoc ""))))

(defn wrap-add-nav-message [h]
  (fn [{:keys [server-name subdomains uri query-params query-string] :as req}]
    (let [nav-message (routes/navigation-message-for uri query-params (first subdomains))]
      (h (assoc req
                :nav-uri (uri/map->URI {:host server-name :path uri :query query-string})
                :nav-message nav-message)))))

(defn login-and-redirect
  [{:keys [environment storeback-config] :as ctx}
   {:keys [subdomains query-params server-name store] :as req}]
  (let [{:strs [token user-id target]} query-params
        {:keys [user] :as response}    (api/one-time-login-in storeback-config user-id token (:stylist-id store))
        cookie-options                 {:max-age (cookies/days 30)
                                        :domain  (str (first subdomains) (cookie-root-domain server-name))}
        whitelisted-redirect-paths     #{"/" "/products/49-rings-kits"}
        dest-req                       (-> req
                                           (assoc :uri (or (whitelisted-redirect-paths target) "/"))
                                           (update :query-params dissoc "token" "user-id" "target"))]
    (if user
      (->  (util.response/redirect (store-url (first subdomains) environment dest-req))
           (cookies/set environment :email (:email user) cookie-options)
           (cookies/set environment :id (:id user) cookie-options)
           (cookies/set environment :store-slug (:store-slug user) cookie-options)
           (cookies/set environment :store-id (:store-id user) cookie-options)
           (cookies/set environment :stylist-experience (:stylist-experience user) cookie-options)
           (cookies/set environment :user-token (:token user) cookie-options))
      (util.response/redirect (store-homepage (first subdomains) environment dest-req)))))

(defn site-routes [ctx]
  (routes
   (GET "/one-time-login" req (login-and-redirect ctx req))
   (frontend-routes ctx)))

(defn shared-cart-routes [ctx]
  (GET "/create-cart-from/:shared-cart-id" req
    (let [cookie-options {:http-only false
                          :max-age   (cookies/days 28)}
          order          (api/create-order-from-shared-cart (:storeback-config ctx)
                                                            (get-in-req-state req keypaths/session-id)
                                                            (:shared-cart-id (:params req))
                                                            (get-in-req-state req keypaths/user-id)
                                                            (get-in-req-state req keypaths/user-token)
                                                            (get-in-req-state req keypaths/store-stylist-id))]
      (-> (util.response/redirect (str "/cart" (query-string req)))
          (cookies/set (:environment ctx) :number (:number order) cookie-options)
          (cookies/set (:environment ctx) :token (:token order) cookie-options)))))

(defn wrap-fetch-stylist-profile [h ctx]
  (fn [{:keys [subdomains uri query-params] :as req}]
    (let [[event {:keys [stylist-id store-slug]}] (routes/navigation-message-for uri query-params (first subdomains))]
      (if (#{events/navigate-adventure-stylist-gallery
             events/navigate-adventure-stylist-profile
             events/navigate-adventure-stylist-profile-post-purchase} event)
        (if-let [stylist (api/get-servicing-stylist (:storeback-config ctx) stylist-id)]
          (if (not= (:store-slug stylist) store-slug)
            (util.response/redirect ; Correct stylist slug
             (path-for req event {:stylist-id   (:stylist-id stylist)
                                  :store-slug   (:store-slug stylist)}))
            (h req)) ; No correction needed
          (-> req ; redirect to find your stylist
              (path-for events/navigate-adventure-find-your-stylist {:query-params {:error "stylist-not-found"}})
              util.response/redirect))
        (h req))))) ; not on the stylist profile or gallery page

(defn routes-with-orders [ctx]
  (-> (routes (paypal-routes ctx)
              (quadpay-routes ctx)
              (-> (routes (site-routes ctx)
                          (shared-cart-routes ctx))
                  (wrap-state ctx)
                  (wrap-site-routes ctx)))
      (wrap-fetch-stylist-profile ctx)
      (wrap-fetch-servicing-stylist-for-order (:storeback-config ctx))
      (wrap-fetch-order (:storeback-config ctx))
      (wrap-fetch-servicing-stylist-for-completed-order (:storeback-config ctx))
      (wrap-fetch-completed-order (:storeback-config ctx))
      (wrap-adventure-route-params)
      (wrap-cookies (storefront-site-defaults (:environment ctx)))))

(defn wrap-redirect-legacy-routes
  [h {:keys [environment]}]
  (fn [{:keys [subdomains] :as req}]
    (letfn [(freeinstall-redirects [uri]
              (condp (fn [substr s] (string/starts-with? s substr)) (str uri)
                "/adv/match-stylist"     "/adv/match-stylist"
                "/adv/find-your-stylist" "/adv/find-your-stylist"
                "/stylist/"              uri
                "/adv/stylist-results"   "/adv/stylist-results"
                "/adv/shop-hair"         "/categories/23-mayvenn-install"
                "/adv/how-shop-hair"     "/categories/23-mayvenn-install"
                "/adv/shop-a-la-carte"   "/categories/23-mayvenn-install"
                "/adv/shop/bundle-sets"  "/shop/all-bundle-sets"
                "/adv/shop/shop-by-look" "/shop/look"
                "/cart"                  "/cart"
                "/"))]
      (if (= "freeinstall" (first subdomains))
        (util.response/redirect (store-url "shop"
                                           environment
                                           (update req :uri freeinstall-redirects))
                                :moved-permanently)
        (h req)))))

(defn wrap-redirect-inconsistent-subdomain-bundle-sets-routes
  [h]
  (fn [{:keys [subdomains uri] :as req}]
    (if (= ["shop" "/shop/deals"] [(first subdomains) uri])
      (util.response/redirect "/shop/all-bundle-sets")
      (h req))))

(defn prepare-cms-query-params [query-params]
  (maps/map-values
   (fn [v] (mapv keyword
                 (if (and (coll? v)
                          (not (string? v)))
                   v
                   (vector v))))
   (maps/map-keys keyword query-params)))

(defn prepare-cms-data [cms-data slices ugc-collections]
  (if (or slices ugc-collections)
    (cond-> {}
      (seq slices)
      (merge (select-keys cms-data slices))

      (seq ugc-collections)
      (merge (-> (select-keys cms-data [:ugc-collection])
                 (update :ugc-collection select-keys ugc-collections)
                 contentful/derive-all-looks)))
    cms-data))

(def ^{:doc "A map of file extensions to mime-types that are incorrect or missing."}
  extra-mimetypes
  {"otf"   "font/otf"
   "sfnt"  "font/sfnt"
   "ttf"   "font/ttf"
   "woff"  "font/woff"
   "woff2" "font/woff2"})

(defn create-handler
  ([] (create-handler {}))
  ([{:keys [logger exception-handler environment contentful] :as ctx}]
   (-> (routes (GET "/healthcheck" [] "cool beans")
               (GET "/robots.txt" req (-> (robots req) util.response/response (util.response/content-type "text/plain")))
               (GET "/sitemap.xml" req (sitemap ctx req))
               (GET "/blog" req (util.response/redirect (store-url "shop" environment (assoc req :uri "/blog/"))))
               (GET "/blog/" req (util.response/redirect (store-url "shop" environment req)))
               (GET "/install" req (util.response/redirect (store-url "shop" environment (assoc req :uri "/"))))
               (GET "/adv/home" req (util.response/redirect (store-url "shop" environment (assoc req :uri "/")) :moved-permanently))
               (GET "/stylist/edit" [] (util.response/redirect "/stylist/account/profile" :moved-permanently))
               (GET "/stylist/account" [] (util.response/redirect "/stylist/account/profile" :moved-permanently))
               (GET "/stylist/commissions" [] (util.response/redirect "/stylist/earnings" :moved-permanently))
               (GET "/freeinstall-share" req (redirect-to-home environment req))
               (GET "/categories" req (redirect-to-home environment req))
               (GET "/categories/" req (redirect-to-home environment req))
               (GET "/products" req (redirect-to-home environment req))
               (GET "/products/" req (redirect-to-home environment req))
               (GET "/products/:id-and-slug/:sku" req (redirect-to-product-details environment req))
               (GET "/cms/*" {uri :uri}
                 (let [keypath (->> #"/" (clojure.string/split uri) (drop 2) (map keyword))]
                   (-> (contentful/read-cache contentful)
                       (get-in keypath)
                       ((partial assoc-in {} keypath))
                       json/generate-string
                       util.response/response
                       (util.response/content-type "application/json"))))
               (GET "/marketing-site" req
                 (contentful/marketing-site-redirect req))
               (-> (routes (static-routes ctx)
                           (routes-with-orders ctx)
                           (route/not-found views/not-found))
                   (wrap-resource "public")
                   (wrap-content-type {:mime-types extra-mimetypes})))
       (wrap-redirect-legacy-routes ctx)
       (wrap-redirect-inconsistent-subdomain-bundle-sets-routes)
       (wrap-add-nav-message)
       (wrap-add-domains)
       (wrap-logging logger)
       (wrap-filter-params)
       (wrap-params)
       (wrap-no-cache)
       (#(if (#{"development" "test"} environment)
           (wrap-exceptions %)
           (wrap-internal-error %
                                :log (comp (partial logger :error) exception-handler)
                                :error-response (do
                                                  (config/dev-print! environment "EXCEPTION")
                                                  (views/error-page (config/development? environment) "An exception occurred"))))))))
