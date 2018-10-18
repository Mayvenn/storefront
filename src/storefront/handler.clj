(ns storefront.handler
  (:require [bidi.bidi :as bidi]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [compojure
             [core :refer :all]
             [route :as route]]
            [noir-exception.core :refer [wrap-exceptions wrap-internal-error]]
            [ring-logging.core :as ring-logging]
            [ring.middleware
             [content-type :refer [wrap-content-type]]
             [cookies :refer [wrap-cookies]]
             [defaults :refer :all]
             [params :refer [wrap-params]]
             [resource :refer [wrap-resource]]]
            [ring.util
             [codec :as codec]
             [response :as util.response]]
            [storefront
             [backend-api :as api]
             [routes :as routes]
             [config :as config]
             [cookies :as cookies]
             [events :as events]
             [keypaths :as keypaths]
             [assets :as assets]
             [views :as views]]
            [leads.keypaths]
            [storefront.accessors
             [experiments :as experiments]]
            [catalog.product-details :as product-details]
            [comb.template :as template]
            [spice.maps :refer [index-by auto-map]]
            [clojure.xml :as xml]
            [catalog.categories :as categories]
            [clj-time.core :as clj-time.core]
            [catalog.products :as products]
            [storefront.accessors.auth :as auth]
            [clojure.set :as set]
            [spice.core :as spice]
            [catalog.skuers :as skuers]
            [storefront.accessors.orders :as orders]
            [lambdaisland.uri :as uri]
            [spice.maps :as maps]
            [storefront.transitions :as transitions]
            [storefront.system.contentful :as contentful]))

(def root-domain-pages-to-preserve-paths-in-redirects
  #{"/mayvenn-made"})

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

(defn wrap-add-domains [h]
  (fn [req]
    (h (merge req {:subdomains (parse-subdomains (:server-name req))}))))

(defn wrap-remove-superfluous-www-redirect [h environment]
  (fn [{:as req :keys [subdomains]}]
    (if (= "www" (first subdomains))
      (util.response/redirect (store-url (second subdomains) environment req))
      (h req))))

(defn wrap-stylist-not-found-redirect [h environment]
  (fn [{server-name                        :server-name
        {store-slug :store-slug :as store} :store
        :as                                req}]
    (cond
      (= store :storefront.backend-api/storeback-unavailable)
      (do
        (config/dev-print! environment "Could not connect to storeback")
        (->html-resp (views/error-page (config/development? environment) "Could not connect to storeback")))

      store-slug
      (h req)

      :else
      (-> (util.response/redirect (store-url "store" environment req))
          (cookies/expire environment
                          "preferred-store-slug"
                          {:http-only true
                           :domain    (cookie-root-domain server-name)})))))

(defn wrap-known-subdomains-redirect [h environment]
  (fn [{:keys [subdomains] :as req}]
    (cond
      (= "classes" (first subdomains))
      (util.response/redirect "https://docs.google.com/a/mayvenn.com/forms/d/e/1FAIpQLSdpA5Kvl8hhI5TkPRGwWLyFcWLtUpRyQksrbA-cikQvTXekwQ/viewform")

      (= "vistaprint" (first subdomains))
      (util.response/redirect "http://www.vistaprint.com/vp/gateway.aspx?sr=no&s=6797900262")

      ;; Old stylist resource page has moved into the community, Aug-2017
      (= "stylist" (first subdomains))
      (util.response/redirect "https://community.mayvenn.com" 301)

      (and (#{[] ["www"] ["internal"]} subdomains)
           (root-domain-pages-to-preserve-paths-in-redirects (:uri req)))
      (util.response/redirect (store-url "shop" environment req) 301)

      (#{[] ["www"] ["internal"]} subdomains)
      (util.response/redirect (store-homepage "shop" environment req) 301)

      :else
      (h req))))

(defn wrap-set-preferred-store [handler environment]
  (fn [{:keys [server-name store] :as req}]
    (when-let [resp (handler req)]
      (-> resp
          (cookies/set environment
                       "preferred-store-slug" (:store-slug store)
                       {:http-only true
                        :max-age   (cookies/days 365)
                        :domain    (cookie-root-domain server-name)})))))

(defn wrap-preferred-store-redirect [handler environment]
  (fn [{:keys [subdomains] :as req}]
    (if-let [preferred-store-slug (cookies/get req "preferred-store-slug")]
      (let [last-subdomain               (last subdomains)
            loading-mayvenn-owned-store? (#{"store" "shop" "internal"} last-subdomain)
            have-a-preferred-store?      (not (contains? #{nil "" "store" "shop" "internal"} preferred-store-slug))
            query-params                 {:redirect last-subdomain}]
        (if (and loading-mayvenn-owned-store? have-a-preferred-store?)
          (util.response/redirect (store-url preferred-store-slug environment (assoc req :query-params query-params)))
          (handler req)))
      (handler req))))

(defn ^:private assoc-in-req-state [req keypath value]
  (update req :state assoc-in keypath value))

(defn ^:private update-in-req-state [req keypath fn & args]
  (apply update-in req (concat [:state] keypath) fn args))

(defn ^:private get-in-req-state
  ([req keypath]
   (get-in-req-state req keypath nil))
  ([req keypath default-value]
   (get-in req (concat [:state] keypath) default-value)))

(defn wrap-set-initial-state [h environment leads-config]
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

(defn wrap-set-cms-cache [h contentful]
  (fn [req]
    (h (assoc-in-req-state req keypaths/cms @(:cache contentful)))))

(defn wrap-set-welcome-url [h leads-config]
  (fn [req]
    (h (assoc-in-req-state req keypaths/welcome-url
                           (str (:endpoint leads-config) "?utm_source=shop&utm_medium=referral&utm_campaign=ShoptoWelcome")))))


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

(defn wrap-fetch-catalog [h storeback-config]
  (fn [req]
    (let [order                   (get-in-req-state req keypaths/order)
          skus-on-order           (mapv :sku (orders/product-items order))
          skus-we-have            (keys (get-in-req-state req keypaths/v2-skus))
          needed-skus             (set/difference (set skus-on-order) (set skus-we-have))
          {:keys [skus products]} (when (seq needed-skus)
                                    (api/fetch-v2-products storeback-config {:selector/sku-ids needed-skus}))
          {:keys [facets]}        (when-not (get-in-req-state req keypaths/v2-facets)
                                    (api/fetch-v2-facets storeback-config))]
      (h (-> req
           (update-in-req-state keypaths/v2-products merge (products/index-products products))
           (update-in-req-state keypaths/v2-skus merge (products/index-skus skus))
           (assoc-in-req-state keypaths/v2-facets (map #(update % :facet/slug keyword) facets))
           (assoc-in-req-state keypaths/categories categories/initial-categories))))))

(defn wrap-set-user [h]
  (fn [req]
    (h (-> req
           (assoc-in-req-state keypaths/user-id (str->int (cookies/get req "id")))
           (assoc-in-req-state keypaths/user-token (cookies/get req "user-token"))
           (assoc-in-req-state keypaths/user-store-slug (cookies/get req "store-slug"))
           (assoc-in-req-state keypaths/user-email (cookies/get-and-attempt-parsing-poorly-encoded req "email"))))))

;;TODO Have all of these middleswarez perform event transitions, just like the frontend
(defn wrap-state [routes {:keys [storeback-config leads-config contentful environment]}]
  (-> routes
      (wrap-fetch-catalog storeback-config)
      (wrap-set-user)
      (wrap-set-welcome-url leads-config)
      (wrap-set-cms-cache contentful)
      (wrap-set-initial-state environment leads-config)))

(defn wrap-site-routes
  [routes {:keys [storeback-config leads-config contentful environment]}]
  (-> routes
      (wrap-set-preferred-store environment)
      (wrap-preferred-store-redirect environment)
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
      (-> (routes/path-for events/navigate-category category)
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
          categories (index-by :catalog/category-id (get-in data keypaths/categories))
          path       (if (= :product type)
                       (routes/path-for events/navigate-product-details product)
                       (routes/path-for events/navigate-category (categories id)))]
      (util.response/redirect path :moved-permanently))))

(defn render-category
  [render-ctx data req {:keys [catalog/category-id page/slug]}]
  (if (contains? discontinued-categories category-id)
    (util.response/redirect (routes/path-for events/navigate-home) :moved-permanently)
    (let [categories (get-in data keypaths/categories)]
      (when-let [category (categories/id->category category-id categories)]
        (if (not= slug (:page/slug category))
          (-> (routes/path-for events/navigate-category category)
              (util.response/redirect :moved-permanently))
          (->> (assoc-in data
                         keypaths/current-category-id
                         (:catalog/category-id category))
               (html-response render-ctx)))))))

(defn render-product-details [{:keys [environment] :as render-ctx}
                              data
                              {:keys [params] :as req}
                              {:keys [catalog/product-id
                                      page/slug]}]
  (if (contains? discontinued-products product-id)
    (util.response/redirect (routes/path-for events/navigate-home) :moved-permanently)
    (when-let [product (get-in data (conj keypaths/v2-products product-id))]
      (let [sku-id         (product-details/determine-sku-id data product (:SKU params))
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
                             (assoc-in catalog.keypaths/detailed-product-id product-id))))))))

(defn- redirect-if-necessary [render-ctx data event]
  (if (not= (get-in data keypaths/navigation-event) event)
    (util.response/redirect (routes/path-for event))
    (html-response render-ctx data)))

(defn render-leads-page
  [render-ctx data req params]
  (let [lead                    (get-in data leads.keypaths/lead)
        nav-event               (get-in data keypaths/navigation-event)
        home                    events/navigate-leads-home
        thank-you               events/navigate-leads-resolve
        a1-applied-thank-you    events/navigate-leads-a1-applied-thank-you
        a1-applied-self-reg     events/navigate-leads-a1-applied-self-reg
        a1-registered-thank-you events/navigate-leads-a1-registered-thank-you
        in-flow?                (partial (fnil = "original")
                                         (:flow-id lead))
        on-step?                (partial (fnil = "initial")
                                         (:step-id lead))
        nav-event?              (partial = nav-event)
        flow-step?              (fn [flow step]
                                  (and (in-flow? flow)
                                       (on-step? step)))]
    (redirect-if-necessary render-ctx data
                           (cond
                             (-> lead :id empty?)              home
                             (flow-step? "a1" "registered")    a1-registered-thank-you
                             (and (nav-event? a1-applied-self-reg)
                                  (flow-step? "a1" "applied")) nav-event
                             (flow-step? "a1" "applied")       a1-applied-thank-you
                             (flow-step? "original" "initial") thank-you
                             :else                             nav-event))))

;;TODO Move to wrap set catalog
;;TODO join queries!!!
(defn- assoc-category-route-data [data storeback-config params]
  (let [category                (categories/id->category (:catalog/category-id params)
                                                         (get-in data keypaths/categories))
        {:keys [skus products]} (api/fetch-v2-products storeback-config (spice.maps/map-values
                                                                         first
                                                                         (skuers/essentials category)))
        {:keys [facets]}        (api/fetch-v2-facets storeback-config)]
    (-> data
        (assoc-in catalog.keypaths/category-id (:catalog/category-id params))
        (update-in keypaths/v2-products merge (products/index-products products))
        (update-in keypaths/v2-skus merge (products/index-skus skus)))))

;;TODO Move to wrap set catalog
;;TODO join queries!!!
(defn- assoc-product-details-route-data [data storeback-config params]
  (let [{:keys [skus products]} (api/fetch-v2-products storeback-config (:catalog/product-id params))
        {:keys [facets]}        (api/fetch-v2-facets storeback-config)]
    (-> data
        (update-in keypaths/v2-products merge (products/index-products products))
        (update-in keypaths/v2-skus merge (products/index-skus skus)))))

(defn- transition [app-state [event args]]
  (reduce (fn [app-state dispatch]
            (or (transitions/transition-state dispatch event args app-state)
                app-state))
          app-state
          (reductions conj [] event)))

(defn frontend-routes [{:keys [contentful storeback-config leads-config environment client-version] :as ctx}]
  (fn [{:keys [state] :as req}]
    (let [nav-message        (get-in-req-state req keypaths/navigation-message)
          [nav-event params] nav-message]
      (when (not= nav-event events/navigate-not-found)
        (let [render-ctx (auto-map storeback-config environment client-version)
              data       (cond-> state
                           (= events/navigate-category nav-event)
                           (assoc-category-route-data storeback-config params)

                           (= events/navigate-product-details nav-event)
                           (assoc-product-details-route-data storeback-config params)

                           (#{events/navigate-shop-by-look-details events/navigate-shop-by-look} nav-event)
                           (transition nav-message)

                           (= events/navigate-stylist-dashboard-cash-out-success nav-event)
                           (assoc-in keypaths/stylist-cash-out-balance-transfer-id (:balance-transfer-id params))

                           (= events/navigate-stylist-dashboard-cash-out-pending nav-event)
                           (assoc-in keypaths/stylist-cash-out-status-id (:status-id params)))
              render (server-render-pages nav-event generic-server-render)]
          (render render-ctx data req params))))))

(def leads-disalloweds ["User-agent: *"
                        "Disallow: /stylists/thank-you"
                        "Disallow: /stylists/flows/"])

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
  {events/navigate-home                          generic-server-render
   events/navigate-category                      render-category
   events/navigate-legacy-named-search           redirect-named-search
   events/navigate-legacy-ugc-named-search       redirect-named-search
   events/navigate-legacy-product-page           redirect-legacy-product-page
   events/navigate-product-details               render-product-details
   events/navigate-content-help                  generic-server-render
   events/navigate-content-about-us              generic-server-render
   events/navigate-content-privacy               generic-server-render
   events/navigate-content-tos                   generic-server-render
   events/navigate-content-guarantee             generic-server-render
   events/navigate-content-ugc-usage-terms       generic-server-render
   events/navigate-content-program-terms         generic-server-render
   events/navigate-gallery                       generic-server-render
   events/navigate-leads-home                    render-leads-page
   events/navigate-leads-a1-applied-self-reg     render-leads-page
   events/navigate-leads-a1-applied-thank-you    render-leads-page
   events/navigate-leads-a1-registered-thank-you render-leads-page
   events/navigate-leads-resolve                 render-leads-page
   events/navigate-install-home                  generic-server-render
   events/navigate-checkout-processing           generic-server-render
   events/navigate-mayvenn-made                  generic-server-render})

(defn robots [{:keys [subdomains]}]
  (cond
    (= [config/welcome-subdomain] subdomains) (string/join "\n" leads-disalloweds)
    :else (string/join "\n" user-specific-disalloweds)))

(defn sitemap [{:keys [storeback-config]} {:keys [subdomains] :as req}]
  (if (and (seq subdomains)
           (not= config/welcome-subdomain (first subdomains)))
    (if-let [launched-products (->> (api/fetch-v2-products storeback-config {})
                                    :products
                                    (filter :catalog/launched-at))]
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
                                            ["https://shop.mayvenn.com/categories/0-closures"               "0.80"]
                                            ["https://shop.mayvenn.com/categories/1-frontals"               "0.80"]
                                            ["https://shop.mayvenn.com/categories/2-straight"               "0.80"]
                                            ["https://shop.mayvenn.com/categories/3-yaki-straight"          "0.80"]
                                            ["https://shop.mayvenn.com/categories/4-kinky-straight"         "0.80"]
                                            ["https://shop.mayvenn.com/categories/5-body-wave"              "0.80"]
                                            ["https://shop.mayvenn.com/categories/6-loose-wave"             "0.80"]
                                            ["https://shop.mayvenn.com/categories/7-water-wave"             "0.80"]
                                            ["https://shop.mayvenn.com/categories/8-deep-wave"              "0.80"]
                                            ["https://shop.mayvenn.com/categories/9-curly"                  "0.80"]
                                            ["https://shop.mayvenn.com/categories/10-360-frontals"          "0.80"]
                                            ["https://shop.mayvenn.com/categories/12-closures-and-frontals" "0.80"]
                                            ["https://shop.mayvenn.com/categories/13-wigs"                  "0.80"]
                                            ["https://shop.mayvenn.com/categories/16-dyed-virgin-hair"      "0.80"]
                                            ["https://shop.mayvenn.com/categories/21-seamless-clip-ins"     "0.80"]
                                            ["https://shop.mayvenn.com/shop/look"                           "0.80"]]
                                           (for [{:keys [catalog/product-id page/slug]} launched-products]
                                             [(str "https://shop.mayvenn.com/products/" product-id "-" slug) "0.80"]))
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

(defn wrap-set-affirm-checkout-token [h]
  (fn [{:as req :keys [params]}]
    (h (-> req
           (assoc-in-req-state keypaths/affirm-checkout-token (get params "checkout_token"))))))

(defn affirm-routes [{:keys [logger storeback-config environment]}]
  (POST "/orders/:order-number/affirm/:order-token" [order-number order-token :as request]
        (let [order          (get-in-req-state request keypaths/order)
              checkout-token (get-in-req-state request keypaths/affirm-checkout-token)]
          (cond
            (or (nil? order)
                (nil? checkout-token)
                (not= (:number order) order-number)
                (not= (:token order) order-token)
                (not (#{"cart" "submitted"} (:state order))))
            (util.response/redirect "/checkout/payment?error=affirm-invalid-state")

            (= "cart" (:state order))
            (-> (util.response/redirect "/checkout/processing")
                (cookies/set environment
                             "affirm-token"
                             checkout-token
                             {:http-only false
                              :max-age   (cookies/minutes 5)}))

            (= "submitted" (:state order))
            (util.response/redirect (str "/orders/" (:number order) "/complete"))))))

(defn static-routes [_]
  (fn [{:keys [uri] :as req}]
    ;; can't use (:nav-message req) because routes/static-api-routes are not
    ;; included in routes/app-routes
    (let [{nav-event :handler} (bidi/match-route routes/static-api-routes uri)]
      (some-> nav-event routes/bidi->edn static-page :content ->html-resp))))

(defn install-routes [{:keys [storeback-config environment client-version] :as ctx}]
  (fn [{:keys [nav-message] :as request}]
    (when (not= (get nav-message 0) events/navigate-not-found)
      (let [render-ctx           (auto-map storeback-config environment client-version)
            [nav-event nav-args] nav-message
            data                 (-> {}
                                     (assoc-in keypaths/store-slug config/install-subdomain)
                                     (assoc-in keypaths/environment environment)
                                     (assoc-in keypaths/navigation-message nav-message))]
        ((server-render-pages nav-event generic-server-render) render-ctx data request nav-args)))))

(defn wrap-freeinstall-is-for-install
  "Handle only requests for freeinstall

   Verify that the routed pages exist, and redirect root to a subpage."
  [h environment]
  (fn [{:keys [subdomains nav-message query-params] :as req}]
    (let [on-install-page?          (routes/sub-page? nav-message [events/navigate-install])
          on-root-path?             (= events/navigate-home (get nav-message 0))
          on-freeinstall-subdomain? (= config/install-subdomain (last subdomains))
          is-www-prefixed?          (= ["www" config/install-subdomain]
                                       (map (comp string/lower-case str) subdomains))
          not-found                 #(-> views/not-found
                                         ->html-resp
                                         (util.response/status 404))]
      (cond
        (and (not on-freeinstall-subdomain?) on-install-page?) (not-found)
        (not on-freeinstall-subdomain?)                        nil ;; defer handling elsewhere for non-freeinstall domains
        is-www-prefixed?                                       (util.response/redirect (store-url "freeinstall" environment req))
        on-install-page?                                       (h req)
        on-root-path?                                          (util.response/redirect (routes/path-for events/navigate-install-home
                                                                                                        {:query-params query-params})
                                                                                       :moved-permanently)
        :else                                                  (not-found)))))

(defn leads-routes [{:keys [storeback-config environment client-version] :as ctx}]
  (fn [{:keys [nav-message] :as request}]
    (when (not= (get nav-message 0) events/navigate-not-found)
      (let [render-ctx           (auto-map storeback-config environment client-version)
            [nav-event nav-args] nav-message
            data                 (-> {}
                                     (assoc-in leads.keypaths/lead-tracking-id (cookies/get request "leads.tracking-id"))
                                     (assoc-in leads.keypaths/lead-utm-source (cookies/get request "leads.utm-source"))
                                     (assoc-in leads.keypaths/lead-utm-content (cookies/get request "leads.utm-content"))
                                     (assoc-in leads.keypaths/lead-utm-campaign (cookies/get request "leads.utm-campaign"))
                                     (assoc-in leads.keypaths/lead-utm-medium (cookies/get request "leads.utm-medium"))
                                     (assoc-in leads.keypaths/lead-utm-term (cookies/get request "leads.utm-term"))
                                     (assoc-in keypaths/store-slug config/welcome-subdomain)
                                     (assoc-in keypaths/environment environment)
                                     (assoc-in keypaths/navigation-message nav-message)
                                     (assoc-in leads.keypaths/lead-flow-id (-> nav-args :query-params :flow))
                                     ((fn [data]
                                        (let [lead-id     (cookies/get request "lead-id")
                                              remote-lead (when (seq lead-id)
                                                            (api/lookup-lead storeback-config lead-id))]
                                          (-> data
                                              (assoc-in leads.keypaths/remote-lead remote-lead)
                                              (update-in leads.keypaths/lead merge remote-lead))))))]
        ((server-render-pages nav-event generic-server-render) render-ctx data request nav-args)))))

(defn wrap-welcome-is-for-leads
  [h]
  (fn [{:keys [subdomains nav-message query-params] :as req}]
    (let [on-leads-page?        (routes/sub-page? nav-message [events/navigate-leads])
          on-home-page?         (= events/navigate-home (get nav-message 0))
          on-welcome-subdomain? (= config/welcome-subdomain (first subdomains))
          not-found             #(-> views/not-found
                                     ->html-resp
                                     (util.response/status 404))]
      (if on-welcome-subdomain?
        (cond
          on-leads-page? (h req)
          on-home-page?  (util.response/redirect (routes/path-for events/navigate-leads-home
                                                                  {:query-params query-params})
                                                 :moved-permanently)
          :else          (not-found))
        (when on-leads-page? (not-found))))))

(defn wrap-migrate-lead-tracking-id-cookie [h {:keys [environment]}]
  (fn [{:keys [server-name] :as req}]
    (let [tracking-id     (str (or (cookies/get req "leads.tracking-id")
                                   (cookies/get req "tracking_id") ;; from old leads site
                                   (java.util.UUID/randomUUID)))
          set-tracking-id (fn [req-or-resp]
                            (cookies/set req-or-resp
                                         environment
                                         "leads.tracking-id"
                                         tracking-id
                                         {:http-only false
                                          :max-age   (cookies/days 365)
                                          :domain    (cookie-root-domain server-name)}))]
      (-> req
          set-tracking-id ; set it on the request, so it's available to leads-routes as (cookies/get req "leads.tracking-id")
          h
          set-tracking-id ; set it on the response, so it's saved in the client
          (cookies/expire environment "tracking_id")))))

(defn wrap-lead-id-from-query-params [h {:keys [environment]}]
  (fn [{:keys [server-name] :as req}]
    (let [lead-id         (str (or (-> req :query-params (get "lead_id"))
                                   (cookies/get req "lead-id")))
          set-lead-id (fn [req-or-resp]
                            (cookies/set req-or-resp
                                         environment
                                         "lead-id"
                                         lead-id
                                         {:http-only false
                                          :max-age   (cookies/days 365)
                                          :domain    (cookie-root-domain server-name)}))]
      (-> req
          set-lead-id ; set it on the request, so it's available to leads-routes as (cookies/get req "lead-id")
          h
          set-lead-id)))) ; set it on the response, so it's saved in the client

(defn wrap-migrate-lead-utm-params-cookies [h {:keys [environment]}]
  (fn [{:keys [server-name] :as req}]
    (let [leads-utm-params        {"utm_source"   "leads.utm-source"
                                   "utm_medium"   "leads.utm-medium"
                                   "utm_campaign" "leads.utm-campaign"
                                   "utm_content"  "leads.utm-content"
                                   "utm_term"     "leads.utm-term"}
          value-from-original-req (fn [old-leads-key new-leads-key]
                                    (or (-> req :query-params (get old-leads-key))
                                        (cookies/get-and-attempt-parsing-poorly-encoded req new-leads-key)
                                        (cookies/get-and-attempt-parsing-poorly-encoded req old-leads-key)))
          migrate-utm             (fn [req-or-resp [old-leads-key new-leads-key]]
                                    (let [utm-value (value-from-original-req old-leads-key new-leads-key)]
                                      (cond-> req-or-resp
                                        utm-value             (cookies/set environment
                                                                           new-leads-key
                                                                           utm-value
                                                                           {:http-only false
                                                                            :max-age   (cookies/days 30)
                                                                            :domain    (cookie-root-domain server-name)})
                                        utm-value
                                        (cookies/expire environment old-leads-key))))
          migrate-utms            (fn [req]
                                    (reduce migrate-utm req leads-utm-params))]
      (-> req
          migrate-utms
          h
          migrate-utms))))

(defn wrap-leads-routes [h {:keys [environment] :as ctx}]
  (-> h
      ;; TODO: leads' version of utm param cookies stick around for 1 year, and are server-side only.
      ;; It would be nice to use storefront's server- and client-side copy of the UTM cookies, but they only stick for 1 month.
      ;; If we need to keep both, the leads' version must be converted to :http-only false
      (wrap-lead-id-from-query-params ctx)
      (wrap-migrate-lead-utm-params-cookies ctx)
      (wrap-migrate-lead-tracking-id-cookie ctx)
      (wrap-defaults (storefront-site-defaults environment))
      (wrap-welcome-is-for-leads)))

(defn wrap-add-nav-message [h]
  (fn [{:keys [server-name uri query-params query-string] :as req}]
    (h (assoc req
              :nav-uri (uri/map->URI {:host server-name :path uri :query query-string})
              :nav-message (routes/navigation-message-for uri query-params)))))

(defn login-and-redirect [{:keys [environment storeback-config] :as ctx}
                          {:keys [subdomains query-params server-name store] :as req}]
  (let [{:strs [token user-id target]}     query-params
        {:keys [user] :as response} (api/one-time-login-in storeback-config user-id token (:stylist-id store))
        cookie-options              {:max-age   (cookies/days 30)
                                     :domain    (str (first subdomains) (cookie-root-domain server-name))}
        whitelisted-redirect-paths #{"/" "/products/49-rings-kits"}
        dest-req (-> req
                     (assoc :uri (or (whitelisted-redirect-paths target) "/"))
                     (update :query-params dissoc "token" "user-id" "target"))]
    (if user
      (->  (util.response/redirect (store-url (first subdomains) environment dest-req))
           (cookies/set environment :email (:email user) cookie-options)
           (cookies/set environment :id (:id user) cookie-options)
           (cookies/set environment :store-slug (:store-slug user) cookie-options)
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

(defn routes-with-orders [ctx]
  (-> (routes (-> (affirm-routes ctx)
                  wrap-set-affirm-checkout-token)
              (paypal-routes ctx)
              (-> (routes (site-routes ctx)
                          (shared-cart-routes ctx))
                  (wrap-state ctx)
                  (wrap-site-routes ctx)))
      (wrap-fetch-order (:storeback-config ctx))
      (wrap-cookies (storefront-site-defaults (:environment ctx)))))

(defn create-handler
  ([] (create-handler {}))
  ([{:keys [logger exception-handler environment storeback-config] :as ctx}]
   (-> (routes (GET "/healthcheck" [] "cool beans")
               (GET "/robots.txt" req (-> (robots req) util.response/response (util.response/content-type "text/plain")))
               (GET "/sitemap.xml" req (sitemap ctx req))
               (GET "/stylist/edit" [] (util.response/redirect "/stylist/account/profile" :moved-permanently))
               (GET "/stylist/account" [] (util.response/redirect "/stylist/account/profile" :moved-permanently))
               (GET "/stylist/commissions" [] (util.response/redirect "/stylist/earnings" :moved-permanently))
               (GET "/categories" req (redirect-to-home environment req))
               (GET "/categories/" req (redirect-to-home environment req))
               (GET "/products" req (redirect-to-home environment req))
               (GET "/products/" req (redirect-to-home environment req))
               (GET "/products/:id-and-slug/:sku" req (redirect-to-product-details environment req))
               (GET "/cms" req (-> ctx :contentful contentful/read-cache cheshire.core/generate-string util.response/response))
               (-> (routes (static-routes ctx)
                           (wrap-leads-routes (leads-routes ctx) ctx)
                           (-> (install-routes ctx)
                               (wrap-defaults (storefront-site-defaults environment))
                               (wrap-freeinstall-is-for-install environment))
                           (routes-with-orders ctx)
                           (route/not-found views/not-found))
                   (wrap-resource "public")
                   (wrap-content-type)))
       (wrap-add-nav-message)
       (wrap-add-domains)
       (wrap-logging logger)
       (wrap-params)
       (#(if (#{"development" "test"} environment)
           (wrap-exceptions %)
           (wrap-internal-error %
                                :log (comp (partial logger :error) exception-handler)
                                :error-response (do
                                                  (config/dev-print! environment "EXCEPTION")
                                                  (views/error-page (config/development? environment) "An exception occurred"))))))))
