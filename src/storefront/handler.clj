(ns storefront.handler
  (:require [api.catalog :refer [?a-la-carte ?discountable]]
            [bidi.bidi :as bidi]
            [catalog.categories :as categories]
            catalog.keypaths
            [catalog.facets :as facets]
            [catalog.products :as products]
            [catalog.skuers :as skuers]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as string]
            [clojure.xml :as xml]
            [clojure.walk :as walk]
            [comb.template :as template]
            [compojure.core :refer [routes GET POST]]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [lambdaisland.uri :as uri]
            [markdown.core :as markdown]
            [noir-exception.core :refer [wrap-exceptions wrap-internal-error]]
            [ring-logging.core :as ring-logging]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.defaults :refer [site-defaults secure-site-defaults wrap-defaults]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.codec :as codec]
            [ring.util.response :as util.response]
            [ring.util.request :as util.request]
            [spice.maps :as maps]
            [spice.selector :as selector]
            spice.core
            storefront.ugc
            stylist-directory.keypaths
            [storefront.safe-hiccup :refer [html5]]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.categories :as accessors.categories]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.skus :as skus]
            [storefront.accessors.products :as accessors.products]
            [storefront.assets :as assets]
            [storefront.backend-api :as api]
            [storefront.config :as config]
            [storefront.cookies :as cookies]
            [storefront.events :as events]
            [storefront.feature-flags :as feature-flags]
            [adventure.keypaths :as adventure-keypaths]
            [storefront.keypaths :as keypaths]
            [storefront.routes :as routes]
            [storefront.system.contentful :as contentful]
            [storefront.system.contentful.static-page :as static-pages]
            [storefront.transitions :as transitions]
            [storefront.views :as views]
            storefront.accessors.contentful
            [storefront.accessors.sites :as sites]
            catalog.look-details-v202105
            [api.stylist :as stylist]))

(defn- transition [app-state [event args]]
  (reduce (fn [app-state dispatch]
            (or (transitions/transition-state dispatch event args app-state)
                app-state))
          app-state
          (reductions conj [] event)))

(defn ^:private path-for
  "Like routes/path-for, but preserves query params."
  ([req navigation-event] (path-for req navigation-event nil))
  ([req navigation-event args] (routes/path-for navigation-event (merge {:query-params (:query-params req)} args))))

(defn ^:private str->int [s]
  (try
    (Integer/parseInt s)
    (catch NumberFormatException _
      nil)))

(defn static-page [[navigate-kw content-kw & static-content-id] static-pages-repo req]
  ;; TODO(jeff): use an env var for preview parameter value & use constant-time= instead of =
  (if-let [src (static-pages/content-for static-pages-repo (:uri req) (= "N60sUd" (get (:query-params req) "preview")))]
    {:id      static-content-id
     :content src}
    (when (= [navigate-kw content-kw] events/navigate-content)
      (when-let [local-file (->> static-content-id
                                 (map name)
                                 (string/join "-")
                                 (format "public/content/%s.html")
                                 io/resource)]
        {:id      static-content-id
         :content (-> local-file slurp (template/eval {:uri assets/path}))}))))

(defn storefront-site-defaults
  [environment]
  (if (config/development? environment)
    site-defaults
    (-> secure-site-defaults
        (assoc :proxy true)
        (assoc-in [:security :hsts] false)
        (assoc-in [:security :anti-forgery] false)
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

(defn store-scheme-and-authority [store-slug environment {:keys [scheme server-name server-port] :as _req}]
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

(defn redirect-to-product-details [_environment {:keys [params] :as _req}]
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
                  {"cache-control"           "no-store, must-revalidate"
                   "pragma"                  "no-cache"
                   "expires"                 "0"}))))))

(defn wrap-set-cache-header
  [f cache-header-val]
  (fn set-cache-header [req]
    (when-let [res (f req)]
      (update res :headers merge
              {"cache-control" cache-header-val}))))

(defn wrap-add-domains [h]
  (fn [req]
    (h (merge req {:subdomains (parse-subdomains (:server-name req))}))))

(defn wrap-remove-superfluous-www-redirect [h environment]
  (fn [{:as req :keys [subdomains]}]
    (if (= "www" (first subdomains))
      (util.response/redirect (store-url (second subdomains) environment req))
      (h req))))

(defn- storeback-offline-response [environment]
  (config/dev-print! environment "Could not connect to storeback")
  (->html-resp (views/error-page (config/development? environment) "Could not connect to storeback")))

(defn wrap-stylist-not-found-redirect [h environment]
  (fn [{server-name                        :server-name
        {store-slug :store-slug :as store} :store
        subdomains                         :subdomains
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
        :as         req}]
    (if (routes/should-redirect-affiliate-route? experience)
      (util.response/redirect
       (store-url "shop" environment (update req :query-params merge {"affiliate_stylist_id" stylist-id})))
      (h req))))

(defn wrap-known-subdomains-redirect [h environment]
  (fn [{:keys [subdomains] :as req}]
    (cond
      (#{[] ["ambassador"] ["www"] ["internal"] ["vistaprint"] ["classes"] ["stylist"] ["community"] ["info"] ["looks"]} subdomains)
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

(defn wrap-set-initial-state [h environment static-pages-repo]
  (fn [req]
    (let [nav-message        (:nav-message req)
          [nav-event _params] nav-message
          nav-uri            (-> (into {} (:nav-uri req))
                                 (assoc :protocol (-> req :scheme name))
                                 (assoc :query (-> req :query-params)))]
      (h (-> req
             (assoc-in-req-state keypaths/scheme (name (:scheme req)))
             (assoc-in-req-state keypaths/navigation-message nav-message)
             (assoc-in-req-state keypaths/navigation-uri nav-uri)
             (assoc-in-req-state keypaths/static (static-page nav-event static-pages-repo req))
             (assoc-in-req-state keypaths/environment environment)
             (update-in-req-state [] experiments/determine-features))))))

(defn wrap-affiliate-initial-login-landing-navigation-message [h]
  (fn [{{experience :experience} :store
        :as                      req}]
    (h (cond->
        req

         (= "affiliate" experience)
         (assoc-in-req-state keypaths/return-navigation-message [events/navigate-stylist-account-profile {}])))))

(defn is-link? [node]
  (-> node :sys :type (= "Link")))

(defn retrieve-link [nodes-db link-node]
  (->> link-node :sys :id keyword (get nodes-db) contentful/extract-fields))

(defn resolve-cms-node
  ([nodes-db node]
   (resolve-cms-node nodes-db #{} node))
  ([nodes-db content-ids-in-ancestors node]
   (let [link-content-id (when (is-link? node) (-> node :sys :id keyword))
         resolved-node   (if (and link-content-id
                                  (->> content-ids-in-ancestors (some #(= % link-content-id)) not))
                           (retrieve-link nodes-db node)
                           node)]
     (cond (map? resolved-node) (into {}
                                      (map (fn [[key subnode]]
                                             [key (resolve-cms-node nodes-db (cond-> content-ids-in-ancestors
                                                                                 link-content-id (conj link-content-id)) subnode)]))
                                      resolved-node)

           (coll? resolved-node) (map (partial resolve-cms-node nodes-db content-ids-in-ancestors) resolved-node)
           (nil? resolved-node)  node
           :else                 resolved-node))))

(def content-type->id-path
  {:landingPageV2       [:slug]
   :homepage            [:experience]
   :retailLocation      [:slug]
   :templateSlot        [:slug]
   :phoneConsultCta     [:slug]
   :copyUrlSlug         [:slug]})


(defn find-cms-nodes
  ([nodes-db content-type]
   (find-cms-nodes nodes-db content-type nil))
  ([nodes-db content-type id]
   (let [content-type-id-path (into [:fields] (content-type content-type->id-path))]
     (->> nodes-db
          vals
          (filter #(and (= content-type (-> % :sys :contentType :sys :id keyword))
                        (or (nil? id)
                            (= id (keyword (get-in % content-type-id-path))))))))))

(defn assemble-cms-node
  ([nodes-db content-type]
   (assemble-cms-node nodes-db content-type nil))
  ([nodes-db content-type id]
   (some->> (find-cms-nodes nodes-db content-type id)
            (map contentful/extract-fields)
            (resolve-cms-node nodes-db))))

(defn ^:private copy-cms-to-data
  ([cms-data data keypath]
   (assoc-in data
             keypath
             (get-in cms-data keypath))))

(defn include-content [data normalized-cms-cache key]
  (assoc-in data [key]
            (some->>
             (assemble-cms-node normalized-cms-cache key)
             (maps/index-by (first (get content-type->id-path key)))
             (walk/keywordize-keys))))

(defn wrap-set-cms-cache
  [h contentful]
  (fn [req]
    (let [shop?                (or (= "shop" (get-in-req-state req keypaths/store-slug))
                                   (= "retail-location" (get-in-req-state req keypaths/store-experience)))
          [nav-event {album-keyword     :album-keyword
                      product-id        :catalog/product-id
                      landing-page-slug :landing-page-slug
                      :as               nav-args}]
          (:nav-message req)
          cms-cache            @(:cache contentful)
          normalized-cms-cache @(:normalized-cache contentful)
          update-data          (partial copy-cms-to-data cms-cache)]
      (h (update-in-req-state req keypaths/cms
                              maps/deep-merge
                              (update-data {} [:advertisedPromo])
                              ;; (update-data {} [:phoneConsultCta])
                              {:phoneConsultCta (first (assemble-cms-node normalized-cms-cache :phoneConsultCta))}
                              {:copyUrlSlug (maps/index-by :slug  (assemble-cms-node normalized-cms-cache :copyUrlSlug :banner-omni))}
                              {:emailModal (into {} (for [modal (filter (comp (partial = "emailModal") :id :sys :contentType :sys) (vals normalized-cms-cache))]
                                                      [(-> modal :sys :id) (->> modal
                                                                                contentful/extract-fields
                                                                                (resolve-cms-node normalized-cms-cache))]))}

                              (cond (= events/navigate-home nav-event)
                                    (-> {:homepage {:unified (-> normalized-cms-cache (assemble-cms-node :homepage :unified) first)
                                                    :omni    (-> normalized-cms-cache (assemble-cms-node :homepage :omni) first)}}
                                        (update-data [:ugc-collection :homepage-looks-spotlight])
                                        contentful/derive-all-looks)

                                    (= events/navigate-category nav-event)
                                    (when-let [category-faq (->> (accessors.categories/id->category (:catalog/category-id nav-args)
                                                                                                    categories/initial-categories)
                                                                 :contentful/faq-id)]
                                      (update-data {} [:faq category-faq]))

                                    (contains? #{events/navigate-shop-by-look events/navigate-shop-by-look-details} nav-event)
                                    (-> {}
                                        (update-data (let [override-keyword (when (and shop? (= :look album-keyword))
                                                                              :aladdin-free-install)]
                                                       [:ugc-collection (or override-keyword album-keyword)]))
                                        contentful/derive-all-looks)

                                    (= events/navigate-product-details nav-event)
                                    (let [product       (get-in-req-state req (conj keypaths/v2-products product-id))
                                          pdp-faq-id    (accessors.products/product->faq-id product)
                                          album-keyword (storefront.ugc/product->album-keyword shop? product)]
                                      (cond-> {}
                                        pdp-faq-id
                                        (update-data [:faq pdp-faq-id])

                                        album-keyword
                                        (-> (update-data [:ugc-collection (keyword album-keyword)])
                                            contentful/derive-all-looks)

                                        :always
                                        (include-content normalized-cms-cache :templateSlot)))

                                    (= events/navigate-landing-page nav-event)
                                    (-> {}
                                        (update-data [:ugc-collection :all-looks])
                                        (assoc-in [:landingPageV2]
                                                  (some->> landing-page-slug
                                                           keyword
                                                           (assemble-cms-node normalized-cms-cache :landingPageV2)
                                                           (maps/index-by (comp keyword :slug)))))

                                    (#{events/navigate-retail-walmart
                                       events/navigate-retail-walmart-grand-prairie
                                       events/navigate-retail-walmart-katy
                                       events/navigate-retail-walmart-mansfield
                                       events/navigate-retail-walmart-dallas
                                       events/navigate-retail-walmart-houston}  nav-event)
                                    (-> {:retailLocation (maps/index-by (comp keyword :slug)
                                                                        (assemble-cms-node normalized-cms-cache :retailLocation))})
                                    :else nil))))))

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
  (fn [req]
    (let [order-number (cookies/get req "number")
          order-token  (cookies/get-and-attempt-parsing-poorly-encoded req "token")]
      (h (cond-> req
           (and order-number order-token
                (not (get-in-req-state req keypaths/order)))
           (assoc-in-req-state keypaths/order (api/get-order storeback-config order-number order-token)))))))

(defn wrap-fetch-completed-order [h storeback-config]
  (fn [{:as req :keys [nav-message]}]
    (let [[nav-event _params] nav-message
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
           (assoc-in-req-state (conj keypaths/models-stylists servicing-stylist-id)
                               (stylist/stylist<- {}
                                                  (api/get-servicing-stylist storeback-config
                                                                             servicing-stylist-id))))))))

(defn wrap-fetch-servicing-stylist-for-completed-order
  [h storeback-config]
  (fn [req]
    (let [servicing-stylist-id (get-in-req-state req keypaths/completed-order-servicing-stylist-id)]
      (h (cond-> req
           (and servicing-stylist-id
                (= events/navigate-order-complete (-> req :nav-message first)))
           (assoc-in-req-state (conj keypaths/models-stylists servicing-stylist-id)
                               (stylist/stylist<- {}
                                                  (api/get-servicing-stylist storeback-config
                                                                             servicing-stylist-id))))))))

(defn wrap-fetch-catalog [h storeback-config]
  (fn [req]
    (let [order                    (get-in-req-state req keypaths/order)
          skus-on-order            (mapv :sku (orders/product-and-service-items order))
          skus-we-have             (keys (get-in-req-state req keypaths/v2-skus))
          needed-skus              (set/difference (set skus-on-order) (set skus-we-have))
          {order-skus     :skus
           order-products :products
           order-images   :images} (when (seq needed-skus) ;; NOTE: does not return addon services because they do not have associated products
                                     (api/fetch-v3-products storeback-config {:selector/sku-ids needed-skus}))
          ;; TODO: clean this up
          {addon-skus :skus}       (api/fetch-v2-skus storeback-config ;; NOTE: get all addon services, except for wig construction
                                                      {:catalog/department "service"
                                                       :service/category   ["preparation" "customization"]
                                                       :service/type       "addon"})
          {pdp-skus     :skus
           pdp-products :products
           pdp-images   :images}   (when-let [product-id (-> req :nav-message second :catalog/product-id)]
                                     (api/fetch-v3-products storeback-config {:catalog/product-id product-id}))
          pdp-product              (first pdp-products)
          related-addon-skus       (when (some #{"base"} (:service/type pdp-product))
                                     (selector/match-all {:selector/strict? true}
                                                         {:hair/family (->> pdp-product
                                                                            :selector/sku-ids
                                                                            (filter #(re-find #"000" %)) ; GROT(SRV) old "bases"
                                                                            first
                                                                            (get pdp-skus)
                                                                            :hair/family
                                                                            set)}
                                                         addon-skus))
          {:keys [facets]}         (when-not (get-in-req-state req keypaths/v2-facets)
                                     (api/fetch-v2-facets storeback-config))]
      (h (-> req
             (update-in-req-state keypaths/v2-products merge (products/index-products (concat order-products pdp-products)))
             (update-in-req-state keypaths/v2-skus merge order-skus pdp-skus (products/index-skus addon-skus))
             (update-in-req-state keypaths/v2-images merge order-images pdp-images)
             (assoc-in-req-state keypaths/v2-facets (map #(update % :facet/slug keyword) facets))
             (assoc-in-req-state catalog.keypaths/detailed-product-related-addons related-addon-skus)
             (assoc-in-req-state keypaths/categories categories/initial-categories))))))

;; GROT: when integration with Bakester's notion of the account profile happens, swap this out.
;; NOTE: this data structure is speculative. I encourage you to throw it out if it doesn't work for you.
(defn ^:private acct-profile-scaffold
  [req]
  {:landfalls
   [{:utm-params   (->> req
                      :query-params
                      (filter (fn [[k _v]] (re-matches #"utm_.*|_utmt\d*" k)))
                      (into {}))
     :query-params (:query-params req)
     :path         (-> req
                      :uri)}]})

(defn wrap-set-user [h]
  (fn [req]
    (h (-> req
           (assoc-in-req-state keypaths/user-id (str->int (cookies/get req "id")))
           (assoc-in-req-state keypaths/user-token (cookies/get req "user-token"))
           (assoc-in-req-state keypaths/user-store-slug (cookies/get req "store-slug"))
           (assoc-in-req-state keypaths/user-store-id (str->int (cookies/get req "store-id")))
           (assoc-in-req-state keypaths/user-stylist-experience (cookies/get req "stylist-experience"))
           (assoc-in-req-state keypaths/user-email (cookies/get-and-attempt-parsing-poorly-encoded req "email"))
           (assoc-in-req-state keypaths/user-verified-at (cookies/get req "verified-at"))
           (assoc-in-req-state keypaths/account-profile (acct-profile-scaffold req))))))

(defn wrap-fetch-promotions
  [h storeback-config]
  (fn [req]
    (h (assoc-in-req-state req keypaths/promotions
                           (api/get-promotions
                            storeback-config
                            (or (first (get-in-req-state req keypaths/order-promotion-codes))
                                (get-in-req-state req keypaths/pending-promo-code)))))))

(defn wrap-add-feature-flags [h ld]
  (fn [req]
    (as-> {} _
      (assoc _ :stylist-results-test (feature-flags/retrieve-flag ld "stylist-results-test" :bool false))
      (assoc _ :first-pageview-email-capture (feature-flags/retrieve-flag ld "first-pageview-email-capture" :string "off"))
      (assoc _ :2022-new-products-hd-lace (feature-flags/retrieve-flag ld "2022-new-products-hd-lace" :bool false))
      (assoc _ :contentful-driven-email-capture-modal (feature-flags/retrieve-flag ld "contentful-driven-email-capture-modal" :bool false))
      (assoc _ :show-guaranteed-shipping (feature-flags/retrieve-flag ld "show-guaranteed-shipping" :bool true))
      (assoc _ :remove-free-install (feature-flags/retrieve-flag ld "remove-free-install" :bool true))
      (assoc _ :migrate-to-rich-text (feature-flags/retrieve-flag ld "migrate-to-rich-text" :bool false))
      (assoc _ :new-homepage-2022-09 (feature-flags/retrieve-flag ld "new-homepage-2022-09" :bool true))
      (assoc _ :carousel-redesign (feature-flags/retrieve-flag ld "carousel-redesign" :bool false))
      (assoc _ :pdp-faq-in-accordion (feature-flags/retrieve-flag ld "pdp-faq-in-accordion" :bool false))
      (assoc _ :footer-v22 (feature-flags/retrieve-flag ld "footer-v22" :bool false))
      (assoc _ :pdp-content-slots (feature-flags/retrieve-flag ld "pdp-content-slots" :bool false))
      (assoc _ :retail-stores-more-info (feature-flags/retrieve-flag ld "retail-stores-more-info" :bool false))
      (assoc _ :footer-email-capture (feature-flags/retrieve-flag ld "footer-email-capture" :bool false))
      (assoc _ :hdyhau-post-purchase (feature-flags/retrieve-flag ld "hdyhau-post-purchase" :bool false))
      (assoc _ :experience-omni (feature-flags/retrieve-flag ld "experience-omni" :bool false))
      (assoc _ :ww-upcp (feature-flags/retrieve-flag ld "ww-upcp" :bool false))
      (assoc _ :homepage-cms-update (feature-flags/retrieve-flag ld "homepage-cms-update" :bool false))
      (assoc _ :good-to-know (feature-flags/retrieve-flag ld "good-to-know" :bool false))
      (assoc _ :plp-header (feature-flags/retrieve-flag ld "plp-header" :bool false))
      (assoc _ :show-shipping-delay (feature-flags/retrieve-flag ld "show-shipping-delay" :bool false))
      (assoc _ :show-date-specified-shipping-delay (feature-flags/retrieve-flag ld "show-date-specified-shipping-delay" :bool false)) ;; Not currently implemented
      (assoc _ :hide-old-classic-homepage (feature-flags/retrieve-flag ld "hide-old-classic-homepage" :bool false))
      (assoc _ :hide-old-static-homepage-content (feature-flags/retrieve-flag ld "hide-old-static-homepage-content" :bool false))
      (assoc _ :yotpo-reviews-by-variant-id? (feature-flags/retrieve-flag ld "yotpo-reviews-by-variant-id" :bool false))
      (assoc _ :natural-page-view (feature-flags/retrieve-flag ld "natural-page-view" :bool false))
      (assoc-in-req-state req keypaths/features _)
      (h _))))


;;TODO Have all of these middleswarez perform event transitions, just like the frontend
(defn wrap-state [routes {:keys [storeback-config welcome-config contentful static-pages-repo launchdarkly environment]}]
  (-> routes
      (wrap-add-feature-flags launchdarkly)
      (wrap-set-cms-cache contentful)
      (wrap-fetch-promotions storeback-config)
      (wrap-fetch-catalog storeback-config)
      (wrap-set-user)
      (wrap-set-welcome-url welcome-config)
      wrap-affiliate-initial-login-landing-navigation-message
      (wrap-set-initial-state environment static-pages-repo)))

(defn wrap-redirect-aladdin
  [h environment]
  (fn [{[nav-event] :nav-message :as req}]
    (let [{:keys [experience
                  stylist-id
                  store-slug]} (get-in-req-state req keypaths/store)]
      (if (and (= "aladdin" experience)
               (not= events/navigate-mayvenn-stylist-pay
                     nav-event))
        (util.response/redirect
         (store-url "shop" environment
                    (cond-> req
                      (= events/navigate-home nav-event)
                      (assoc :uri (path-for req events/navigate-adventure-stylist-profile
                                            {:stylist-id stylist-id
                                             :store-slug store-slug})))) 301)
        (h req)))))

(defn wrap-redirect-shop-only-routes
  [h environment]
  (fn [req]
    (if (and (not (or (= "shop" (get-in-req-state req keypaths/store-slug))
                      (= "retail-location" (get-in-req-state req keypaths/store-experience))))
             (some #(re-find % (:uri req))
                   #{#"^\/adv\/.*" #"^\/stylist\/.*"}))
      (redirect-to-home environment req :found)
      (h req))))

(defn wrap-redirect-free-install-sunset
  [h environment]
  (fn [req]
    (if (some #(re-find % (:uri req))
              #{#"^\/about-mayvenn-install$"
                #"^\/about-our-hair$"
                #"^\/certified-stylists$"})
      (redirect-to-home environment req :found)
      (h req))))

(defn wrap-site-routes
  [routes {:keys [storeback-config environment]}]
  (-> routes
      (wrap-set-preferred-store environment)
      (wrap-redirect-affiliates environment)
      (wrap-redirect-aladdin environment)
      (wrap-redirect-free-install-sunset environment)
      (wrap-redirect-shop-only-routes environment)
      (wrap-stylist-not-found-redirect environment)
      (wrap-defaults (dissoc (storefront-site-defaults environment) :cookies))
      (wrap-remove-superfluous-www-redirect environment)
      (wrap-fetch-store storeback-config)
      (wrap-known-subdomains-redirect environment)))

(defn wrap-logging [handler logger]
  (-> handler
      ring-logging/wrap-request-timing
      (ring-logging/wrap-logging logger
                                 ring-logging/structured-inbound-config)
      ring-logging/wrap-trace-request))

(declare server-render-pages)
(defn html-response [render-ctx data]
  (let [prerender? (contains? server-render-pages (get-in data keypaths/navigation-event))]
    (-> ((if prerender? views/prerendered-page views/index) render-ctx data)
        ->html-resp)))

(defn render-look-details
  [{:keys [storeback-config] :as render-ctx} data req {:keys [look-id album-keyword]}]
  (let [kw-look-id                 (keyword look-id)
        {:keys
         [shared-cart
          skus
          products
          shared-cart-creator]} (some->> (get-in data (conj keypaths/cms-ugc-collection-all-looks kw-look-id))
                                         storefront.accessors.contentful/shared-cart-id
                                         (api/fetch-shared-cart storeback-config))
        shop?                   (sites/determine-site data)
        override-keyword        (when (and shop? (= :look album-keyword))
                                  :aladdin-free-install)]
    (if (nil? shared-cart)
      (util.response/redirect (path-for req events/navigate-shop-by-look {:album-keyword album-keyword}))
      (html-response render-ctx
                     (-> data
                         (assoc-in keypaths/selected-album-keyword (or override-keyword album-keyword))
                         (assoc-in keypaths/selected-look-id kw-look-id)
                         (assoc-in keypaths/shared-cart-current shared-cart)
                         (assoc-in keypaths/shared-cart-creator shared-cart-creator)
                         (update-in keypaths/v2-skus merge (products/index-skus skus))
                         (update-in keypaths/v2-products merge (products/index-products products)))))))

(defn generic-server-render [render-ctx data _req _params]
  (html-response render-ctx data))

(defn redirect-to-cart [query-params]
  (util.response/redirect (str "/cart?" (codec/form-encode query-params))))

(defn redirect-named-search
  [_render-ctx data req {:keys [named-search-slug]}]
  (let [categories (get-in data keypaths/categories)]
    (when-let [category (accessors.categories/named-search->category named-search-slug categories)]
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

(defn redirect-legacy-product-page
  [render-ctx data req {:keys [legacy/product-slug]}]
  (when-let [[type id] (legacy-product-slug->new-location product-slug)]
    (let [product    (first (:products (api/fetch-v3-products (:storeback-config render-ctx) {:_keys "products" :catalog/product-id id})))
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

(def service-category-ids
  #{"30" "31" "35" "36" "37"})

(defn service-category-ids->redirect [category-id]
  (when (contains? service-category-ids category-id)
    [events/navigate-adventure-find-your-stylist]))

(defn render-category
  [{:keys [environment] :as render-ctx} data req {:keys [catalog/category-id page/slug]}]
  (let [virgin-category (get dyed-virgin-category-id->virgin-category category-id)
        temporary-service-redirect  (service-category-ids->redirect category-id)]
    (cond
      (contains? discontinued-categories category-id)
      (util.response/redirect (path-for req events/navigate-home) :moved-permanently)

      virgin-category
      (util.response/redirect (path-for req events/navigate-category virgin-category) :moved-permanently)

      temporary-service-redirect
      (let [new-path (apply path-for req temporary-service-redirect)]
        (util.response/redirect (store-url "shop" environment (assoc req :uri new-path)) 302))

      :else
      (let [categories (get-in data keypaths/categories)]
        (when-let [category (accessors.categories/id->category category-id categories)]
          (cond
            (not= slug (:page/slug category)) (-> (path-for req events/navigate-category category)
                                                  (util.response/redirect :moved-permanently))
            (:page/redirect? category)        (util.response/redirect (path-for req events/navigate-home) :moved-permanently)

            :else                             (->> (assoc-in data
                                                             keypaths/current-category-id
                                                             (:catalog/category-id category))
                                                   (html-response render-ctx))))))))

(defn determine-sku-id [data product route-sku-id]
  (let [valid-product-skus (products/extract-product-skus data product)
        valid-sku-ids      (set (map :catalog/sku-id valid-product-skus))]
    (or (when (seq route-sku-id) (valid-sku-ids route-sku-id)) ;; Find the sku that matches the one in the uri
        (:catalog/sku-id ;; Fallback to epitome
         (skus/determine-epitome
          (facets/color-order-map (get-in data keypaths/v2-facets))
          valid-product-skus)))))

(def discontinued-product-id->redirect
  {"59"  [events/navigate-category        {:catalog/category-id "1" :page/slug "virgin-frontals"}]
   "89"  [events/navigate-product-details {:catalog/product-id "12" :page/slug "indian-straight-bundles"}]
   "90"  [events/navigate-product-details {:catalog/product-id "16" :page/slug "indian-straight-lace-closures"}]
   "93"  [events/navigate-product-details {:catalog/product-id "50" :page/slug "indian-straight-lace-frontals"}]
   "94"  [events/navigate-product-details {:catalog/product-id "9" :page/slug "brazilian-straight-bundles"}]
   "95"  [events/navigate-product-details {:catalog/product-id "8" :page/slug "malaysian-body-wave-bundles"}]
   "96"  [events/navigate-product-details {:catalog/product-id "22" :page/slug "brazilian-loose-wave-bundles"}]
   "97"  [events/navigate-product-details {:catalog/product-id "30" :page/slug "brazilian-deep-wave-bundles"}]
   "98"  [events/navigate-product-details {:catalog/product-id "4" :page/slug "brazilian-straight-lace-closures"}]
   "99"  [events/navigate-product-details {:catalog/product-id "3" :page/slug "malaysian-body-wave-lace-closures"}]
   "100" [events/navigate-product-details {:catalog/product-id "33" :page/slug "brazilian-loose-wave-lace-closures"}]
   "101" [events/navigate-product-details {:catalog/product-id "11" :page/slug "brazilian-deep-wave-lace-closures"}]
   "102" [events/navigate-home            {}]
   "103" [events/navigate-home            {}]
   "104" [events/navigate-home            {}]
   "105" [events/navigate-home            {}]
   "106" [events/navigate-home            {}]
   "107" [events/navigate-home            {}]
   "108" [events/navigate-home            {}]
   "109" [events/navigate-home            {}]
   "116" [events/navigate-home            {}]
   "117" [events/navigate-home            {}]})

(def service-product-ids
  #{"131" "132" "135" "136" "219" "220" "221" "222" "223" "224" "225" "226"
    "227" "228" "229" "230" "231" "232"})

(defn service-product-id->redirect [catalog-product-id]
  (when (contains? service-product-ids catalog-product-id)
    [events/navigate-adventure-find-your-stylist {}]))

(defn render-product-details [{:keys [environment] :as render-ctx}
                              data
                              {:keys [params] :as req}
                              {:keys [catalog/product-id
                                      page/slug]}]
  (let [permanent-redirect (get discontinued-product-id->redirect product-id)
        temporary-service-redirect (service-product-id->redirect product-id)]
    (cond
      permanent-redirect
      (let [[redirect-nav-event redirect-nav-params] permanent-redirect]
           (util.response/redirect (path-for req redirect-nav-event (merge params redirect-nav-params))
                                   :moved-permanently))

      temporary-service-redirect
      (let [new-path (apply path-for req temporary-service-redirect)]
        (util.response/redirect (store-url "shop" environment (assoc req :uri new-path))
                                302))

      :else
      (when-let [product (get-in data (conj keypaths/v2-products product-id))]
        (let [sku-id               (determine-sku-id data product (:SKU params))
              sku                  (get-in data (conj keypaths/v2-skus sku-id))
              canonical-slug       (:page/slug product)
              redirect-to-fix-url? (and canonical-slug
                                        (or (not= slug canonical-slug)
                                            (and sku-id (not sku))))
              permitted?           (auth/permitted-product? data product)]
          (cond
            (not permitted?)
            (redirect-to-home environment req)

            redirect-to-fix-url?
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
  (let [category                       (accessors.categories/id->category (:catalog/category-id params)
                                                                          (get-in data keypaths/categories))
        {:keys [skus products images]} (api/fetch-v3-products storeback-config (maps/map-values vec (skuers/essentials< category)))]
    (-> data
        (assoc-in catalog.keypaths/category-id (:catalog/category-id params))
        (update-in keypaths/v2-images merge images)
        (update-in keypaths/v2-products merge (products/index-products products))
        (update-in keypaths/v2-skus merge skus))))

(defn frontend-routes [{:keys [storeback-config environment client-version wirewheel-config] :as _ctx}]
  (fn [{:keys [state] :as req}]
    (let [nav-message        (get-in-req-state req keypaths/navigation-message)
          [nav-event params] nav-message]
      (when (not= nav-event events/navigate-not-found)
        (let [render-ctx (maps/auto-map storeback-config environment client-version wirewheel-config)
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

(def robots-content ["User-agent: *"
                     "Disallow: /account"
                     "Disallow: /checkout"
                     "Disallow: /orders"
                     "Disallow: /cart"
                     "Disallow: /m/"
                     "Disallow: /c/"
                     "Disallow: /admin"
                     "Disallow: /content"
                     "Disallow: /policy/privacy"
                     "Disallow: /policy/tos"
                     "Disallow: /your-looks/"
                     "Disallow: /lp/bundles-paid"
                     "Disallow: /lp/clip-ins-paid"
                     "Disallow: /lp/wigs-paid"
                     "Sitemap: https://shop.mayvenn.com/sitemap.xml"])

(def server-render-pages
  {events/navigate-home                               generic-server-render
   events/navigate-landing-page                       generic-server-render
   events/navigate-category                           render-category
   events/navigate-legacy-named-search                redirect-named-search
   events/navigate-legacy-ugc-named-search            redirect-named-search
   events/navigate-legacy-product-page                redirect-legacy-product-page
   events/navigate-product-details                    render-product-details
   events/navigate-content-help                       generic-server-render
   events/navigate-content-about-us                   generic-server-render
   events/navigate-content-privacy                    generic-server-render
   events/navigate-content-privacyv2                  generic-server-render
   events/navigate-content-privacyv1                  generic-server-render
   events/navigate-content-sms                        generic-server-render
   events/navigate-content-tos                        generic-server-render
   events/navigate-content-guarantee                  generic-server-render
   events/navigate-content-ugc-usage-terms            generic-server-render
   events/navigate-content-voucher-terms              generic-server-render
   events/navigate-content-program-terms              generic-server-render
   events/navigate-content-return-and-exchange-policy generic-server-render
   events/navigate-guide-clipin-extensions            generic-server-render
   events/navigate-store-gallery                      generic-server-render
   events/navigate-checkout-processing                generic-server-render
   events/navigate-adventure-stylist-profile          generic-server-render
   events/navigate-adventure-stylist-profile-reviews  generic-server-render
   events/navigate-adventure-stylist-gallery          generic-server-render
   events/navigate-shop-by-look                       generic-server-render
   events/navigate-shop-by-look-details               render-look-details})

(defn robots [_]
  (string/join "\n" robots-content))

(defn canonical-category-sitemap
  "As per SEO needs, we're only listing the following categories:
   - category pages without any filters
   - category pages with 1 filter applied
  But only if that category page is self-canonical."
  [categories products]
  (concat
   (for [category categories
         :let     [category-id (:catalog/category-id category)
                   slug (:page/slug category)]]
     [(str "https://shop.mayvenn.com/categories/" category-id "-" slug) "0.80"])
   (for [category                categories
         :let                    [category-id (:catalog/category-id category)
                                  slug (:page/slug category)
                                  electives (skuers/electives< category)
                                  essentials (skuers/essentials< category)
                                  category-products (selector/match-all {:selector/strict? true}
                                                                        (merge
                                                                         electives
                                                                         essentials)
                                                                        products)
                                  facet->values (maps/map-values set (apply merge-with into (map #(skuers/electives< category %)
                                                                                                 category-products)))]
         [facet-id facet-values] facet->values
         facet-value             facet-values
         :let                    [query-params (accessors.categories/category-selections->query-params {facet-id [facet-value]})
                                  canonical-category-id (:category-id (accessors.categories/canonical-category-data
                                                                       categories
                                                                       (accessors.categories/id->category category-id categories)
                                                                       {:query (codec/form-encode query-params)}))]
         :when (= category-id canonical-category-id)]
     [(str "https://shop.mayvenn.com" (routes/path-for events/navigate-category
                                                       {:catalog/category-id category-id
                                                        :page/slug           slug
                                                        :query-params        query-params}))
      "0.80"])))

(defn sitemap-pages [{:keys [storeback-config sitemap-cache contentful]} {:keys [subdomains] :as _req}]
  (if (seq subdomains)
    (if-let [hit (not-empty @(:atom sitemap-cache))]
      hit
      (if-let [launched-products (->> (api/fetch-v3-products storeback-config {:_keys "products"})
                                      :products
                                      (filter :catalog/launched-at)
                                      (remove :catalog/discontinued-at)
                                      (remove :stylist-exclusives/experience)
                                      (remove :stylist-exclusives/family)
                                      (remove :service/type))]
        (let [initial-categories (filter :seo/sitemap categories/initial-categories)
              landing-pages      (-> contentful contentful/read-cache :landingPage vals)]
          (letfn [(url-xml-elem [[location priority]]
                    {:tag :url :content (cond-> [{:tag :loc :content [(str location)]}]
                                          priority (conj {:tag :priority :content [(str priority)]}))})]

            (-> (xml/emit {:tag     :urlset
                           :attrs   {:xmlns "http://www.sitemaps.org/schemas/sitemap/0.9"}
                           :content (->> (into [[(str "https://" config/welcome-subdomain ".mayvenn.com/") "0.60"]
                                                [(str "https://" config/jobs-subdomain ".mayvenn.com/")    "0.60"]
                                                [(str "https://" config/help-subdomain ".mayvenn.com/")    "0.60"]
                                                ["https://shop.mayvenn.com/"                               "1.00"]
                                                ["https://shop.mayvenn.com/guarantee"                      "0.60"]
                                                ["https://shop.mayvenn.com/help"                           "0.60"]
                                                ["https://shop.mayvenn.com/about-us"                       "0.60"]
                                                ["https://shop.mayvenn.com/return-and-exchange-policy"     "0.60"]
                                                ["https://shop.mayvenn.com/our-hair"                       "0.60"]
                                                ["https://shop.mayvenn.com/info/wig-care-guide"            "0.60"]
                                                ["https://shop.mayvenn.com/info/wig-installation-guide"    "0.60"]
                                                ["https://shop.mayvenn.com/info/wig-styling-guide"         "0.60"]
                                                ["https://shop.mayvenn.com/info/wig-buying-guide-hub"      "0.60"]
                                                ["https://shop.mayvenn.com/info/wig-hair-guide"            "0.60"]
                                                ["https://shop.mayvenn.com/info/wigs-101-guide"            "0.60"]
                                                ["https://shop.mayvenn.com/login"                          "0.60"]
                                                ["https://shop.mayvenn.com/signup"                         "0.60"]
                                                ["https://shop.mayvenn.com/shop/look"                      "0.80"]
                                                ["https://shop.mayvenn.com/shop/straight-looks"            "0.80"]
                                                ["https://shop.mayvenn.com/shop/wavy-curly-looks"          "0.80"]]

                                               (concat
                                                (canonical-category-sitemap initial-categories launched-products)
                                                (for [{:keys [catalog/product-id page/slug]} launched-products]
                                                  [(str "https://shop.mayvenn.com/products/" product-id "-" slug) "0.80"])
                                                (for [{:keys [slug is-in-sitemap sitemap-priority]} landing-pages
                                                      :when                                         is-in-sitemap]
                                                  [(str "https://shop.mayvenn.com/lp/" slug) sitemap-priority])))
                                         (mapv url-xml-elem))})
                with-out-str
                util.response/response
                (util.response/content-type "text/xml")
                ((partial reset! (:atom sitemap-cache))))))
        (-> (util.response/response "<error />")
            (util.response/content-type "text/xml")
            (util.response/status 502))))
    (-> (util.response/response "")
        (util.response/status 404))))

(defn sitemap-index [req]
  (if (seq (:subdomains req))
    (-> (xml/emit {:tag     :sitemapindex
                   :attrs   {:xmlns "http://www.sitemaps.org/schemas/sitemap/0.9"}
                   :content [{:tag     :sitemap
                              :content [{:tag     :loc
                                         :content ["https://shop.mayvenn.com/sitemap-pages.xml"]}]}
                             {:tag     :sitemap
                              :content [{:tag     :loc
                                         :content ["https://shop.mayvenn.com/blog/sitemap-posts.xml"]}]}]})
        with-out-str
        util.response/response
        (util.response/content-type "text/xml"))
    (-> (util.response/response "")
        (util.response/status 404))))

(defn paypal-routes [_ctx]
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

(defn quadpay-routes
  "NOTE: Quadpay's successful charges are known to storefront only through the
  client-side pinging so that we can track the placed order event."
  [_ctx]
  (GET "/orders/:order-number/quadpay" [order-number :as request]
    (let [order-from-cookie (get-in-req-state request keypaths/order)
          order-token       (get (:query-params request) "order-token")]
      (if (and order-from-cookie
               (or (not= (:number order-from-cookie) order-number)
                   (not= (:token order-from-cookie) order-token)))
        (util.response/redirect "/checkout/payment?error=quadpay-invalid-state")
        (util.response/redirect "/checkout/processing")))))

(defn static-routes [{:keys [static-pages-repo]}]
  (fn [{:keys [uri] :as req}]
    ;; can't use (:nav-message req) because routes/static-api-routes are not
    ;; included in routes/app-routes
    (let [{nav-event :handler} (bidi/match-route routes/static-api-routes uri)]
      (some-> nav-event routes/bidi->edn (static-page static-pages-repo req) :content ->html-resp))))

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

(defn wrap-fetch-stylist-profile [h {:keys [storeback-config]}]
  (fn [{:keys [subdomains uri query-params] :as req}]
    (let [[event {:keys [stylist-id store-slug]}] (routes/navigation-message-for uri query-params (first subdomains))]
      (if (#{events/navigate-adventure-stylist-gallery
             events/navigate-adventure-stylist-profile
             events/navigate-adventure-stylist-profile-reviews} event)
        (if-let [stylist (api/get-servicing-stylist storeback-config stylist-id)]
          (if (not= (:store-slug stylist) store-slug)
            ;; Correct stylist slug
            (util.response/redirect
             (path-for req event {:stylist-id (:stylist-id stylist)
                                  :store-slug (:store-slug stylist)}))
            ;; No correction needed
            (let [{service-skus     :skus
                   service-products :products
                   service-images   :images} (api/fetch-v3-products storeback-config
                                                                    (merge-with clojure.set/union
                                                                                ?discountable
                                                                                ?a-la-carte))]
              (h (-> req
                     (assoc-in-req-state adventure.keypaths/stylist-profile-id (:stylist-id stylist))
                     (assoc-in-req-state (conj stylist-directory.keypaths/stylists (:stylist-id stylist)) stylist) ;; Original diva format, get rid of soon
                     (assoc-in-req-state (conj keypaths/models-stylists
                                               (:stylist-id stylist)) (stylist/stylist<- {} stylist))
                     (assoc-in-req-state keypaths/v2-products
                                         (products/index-products service-products))
                     (assoc-in-req-state keypaths/v2-skus service-skus)
                     (assoc-in-req-state keypaths/v2-images service-images)))))
          (-> req ; redirect to find your stylist
              (path-for events/navigate-adventure-find-your-stylist {:query-params {:error "stylist-not-found"}})
              util.response/redirect))
        (h req))))) ; not on the stylist profile or gallery page

(defn routes-with-orders [ctx]
  (-> (routes (paypal-routes ctx)
              (quadpay-routes ctx)
              (-> (routes (frontend-routes ctx)
                          (shared-cart-routes ctx))
                  (wrap-state ctx)
                  (wrap-site-routes ctx)))
      (wrap-fetch-stylist-profile ctx)
      (wrap-fetch-servicing-stylist-for-order (:storeback-config ctx))
      (wrap-fetch-order (:storeback-config ctx))
      (wrap-fetch-servicing-stylist-for-completed-order (:storeback-config ctx))
      (wrap-fetch-completed-order (:storeback-config ctx))
      (wrap-cookies (storefront-site-defaults (:environment ctx)))))

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
  ([{:keys [logger exception-handler environment contentful wirewheel-config] :as ctx}]
   (-> (routes (GET "/healthcheck" [] "cool beans")
               (GET "/robots.txt" req (-> (robots req) util.response/response (util.response/content-type "text/plain")))
               (GET "/sitemap.xml" req (sitemap-index req))
               (GET "/sitemap-pages.xml" req (sitemap-pages ctx req))
               (GET "/googlee2783b8646cb0bdd.html" _req
                 (-> "google-site-verification: googlee2783b8646cb0bdd.html"
                     (util.response/response) (util.response/content-type "text/html")))
               (GET "/care-guide" req (util.response/redirect "https://ucarecdn.com/7689dd8f-e9d7-4e43-af34-1938b4fc9439/" 302))
               (GET "/blog" req (util.response/redirect (store-url "shop" environment (assoc req :uri "/blog/"))))
               (GET "/blog/" req (util.response/redirect (store-url "shop" environment req)))
               (GET "/adv/home" req (util.response/redirect (store-url "shop" environment (assoc req :uri "/")) :moved-permanently))
               (GET "/stylist/edit" [] (util.response/redirect "/stylist/account/profile" :moved-permanently))
               (GET "/stylist/account" [] (util.response/redirect "/stylist/account/profile" :moved-permanently))
               (GET "/stylist/commissions" [] (util.response/redirect "/stylist/earnings" :moved-permanently))
               (GET "/shop/deals" req (redirect-to-home environment req))
               (GET "/categories" req (redirect-to-home environment req))
               (GET "/categories/" req (redirect-to-home environment req))
               (GET "/products" req (redirect-to-home environment req))
               (GET "/products/" req (redirect-to-home environment req))
               (GET "/products/:id-and-slug/:sku" req (redirect-to-product-details environment req))
               (GET "/how-it-works" _req (wrap-set-cache-header (partial redirect-to-home environment) "max-age=604800"))
               (GET "/mayvenn-made" req (util.response/redirect
                                         (store-url "shop" environment
                                                    (assoc req :uri "/"))))
               (GET "/share" req (redirect-to-home environment req :found))
               (GET "/account/referrals" req (redirect-to-home environment req :found))
               (GET "/stylist/referrals" req (redirect-to-home environment req :found))
               (GET "/adv/match-stylist" req (util.response/redirect (store-url "shop" environment (assoc req :uri "/adv/find-your-stylist")) :moved-permanently))
               (GET "/info/mayvenn-hair-care-guide" req (util.response/redirect "https://ucarecdn.com/dab8e356-8df2-47b8-904e-955ed1389b81/" :moved-permanently))
               (GET "/cms/*" {uri :uri} ;; GROT: Deprecated in favor of CMS2 below
                    (let [keypath (->> #"/" (clojure.string/split uri) (drop 2) (map keyword))]
                      (-> (get-in (contentful/read-cache contentful) keypath)
                          ((partial assoc-in {} keypath))
                          json/generate-string
                          util.response/response
                          (util.response/content-type "application/json"))))
               (GET "/cms2/*" {uri :uri}
                    (let [cms-cache         (contentful/read-normalized-cache contentful)
                          [content-type id] (->> (clojure.string/split uri #"/") (drop 2) (map keyword))]
                      (some-> {content-type (->> (assemble-cms-node cms-cache content-type id)
                                                 (maps/index-by #(get-in % (content-type content-type->id-path))))}
                              json/generate-string
                              util.response/response
                              (util.response/content-type "application/json"))))
               (POST "/contentful/webhook" req
                     (if (and (#{"development" "test" "acceptance"}  environment)
                              (spice.core/constant-time= (:webhook-secret contentful)
                                                         (-> req :headers (get "secret"))))
                       (do
                         (->> req
                              util.request/body-string
                              json/parse-string
                              walk/keywordize-keys
                              (contentful/upsert-into-normalized-cache contentful))
                         (util.response/response "OK"))
                       (util.response/status nil 403)))
               (-> (routes (static-routes ctx)
                           (routes-with-orders ctx)
                           (route/not-found views/not-found))
                   (wrap-resource "public")
                   (wrap-content-type {:mime-types extra-mimetypes})))
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
