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
             [experiments :as experiments]
             [named-searches :as named-searches]]
            [comb.template :as template]
            [spice.maps :refer [index-by]]
            [clojure.string :as str]
            [clojure.xml :as xml]
            [catalog.categories :as categories]
            [clj-time.core :as clj-time.core]
            [clojure.set :as set]
            [spice.core :as spice]
            [catalog.products :as products]))

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

(defn query-string [req]
  (let [query-str (:query-string req)]
    (when (seq query-str)
      (str "?" query-str))))

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

(defn wrap-add-domains [h]
  (fn [req]
    (h (merge req {:subdomains (parse-subdomains (:server-name req))}))))

(defn wrap-remove-superfluous-www-redirect [h environment]
  (fn [{subdomains :subdomains
        {store-slug :store_slug} :store
        :as req}]
    (if (= "www" (first subdomains))
      (util.response/redirect (store-url store-slug environment req))
      (h req))))

(defn wrap-stylist-not-found-redirect [h environment]
  (fn [{server-name                        :server-name
        {store-slug :store_slug :as store} :store
        :as                                req}]
    (cond
      (= store :storefront.backend-api/storeback-unavailable)
      (->html-resp views/error-page)

      store-slug
      (h req)

      :else
      (-> (util.response/redirect (store-url "store" environment req))
          (cookies/expire environment
                          "preferred-store-slug"
                          {:http-only true
                           :domain    (cookie-root-domain server-name)})))))

(defn wrap-known-subdomains-redirect [h environment]
  (fn [{:keys [subdomains server-name server-port] :as req}]
    (cond
      (= "classes" (first subdomains))
      (util.response/redirect "https://docs.google.com/a/mayvenn.com/forms/d/e/1FAIpQLSdpA5Kvl8hhI5TkPRGwWLyFcWLtUpRyQksrbA-cikQvTXekwQ/viewform")

      (= "vistaprint" (first subdomains))
      (util.response/redirect "http://www.vistaprint.com/vp/gateway.aspx?sr=no&s=6797900262")

      ;; Old stylist resource page has moved into the community, Aug-2017
      (= "stylist" (first subdomains))
      (util.response/redirect "https://community.mayvenn.com" 301)

      (#{[] ["www"] ["internal"]} subdomains)
      (util.response/redirect (store-homepage "shop" environment req) 301)

      :else
      (h req))))

(defn wrap-set-preferred-store [handler environment]
  (fn [{:keys [server-name store] :as req}]
    (when-let [resp (handler req)]
      (-> resp
          (cookies/set environment
                       "preferred-store-slug" (:store_slug store)
                       {:http-only true
                        :max-age   (cookies/days 365)
                        :domain    (cookie-root-domain server-name)})))))

(defn wrap-preferred-store-redirect [handler environment]
  (fn [{:keys [subdomains] :as req}]
    (if-let [preferred-store-slug (cookies/get req "preferred-store-slug")]
      (if (and (#{"store" "shop" "internal"} (last subdomains))
               (not (#{nil "" "store" "shop" "internal"} preferred-store-slug)))
        (util.response/redirect (store-url preferred-store-slug environment req))
        (handler req))
      (handler req))))

(defn wrap-fetch-store [h storeback-config]
  (fn [{:keys [subdomains] :as req}]
    (h (merge req
              {:store (api/store storeback-config (last subdomains))}))))

(defn wrap-logging [handler logger]
  (-> handler
      ring-logging/wrap-request-timing
      (ring-logging/wrap-logging logger ring-logging/simple-inbound-config)
      ring-logging/wrap-trace-request))

(defn wrap-site-routes
  [routes {:keys [storeback-config environment]}]
  (-> routes
      (wrap-set-preferred-store environment)
      (wrap-preferred-store-redirect environment)
      (wrap-stylist-not-found-redirect environment)
      (wrap-defaults (storefront-site-defaults environment))
      (wrap-remove-superfluous-www-redirect environment)
      (wrap-fetch-store storeback-config)
      (wrap-known-subdomains-redirect environment)
      (wrap-resource "public")
      (wrap-content-type)))

(declare server-render-pages)
(defn html-response [render-ctx data]
  (let [prerender? (contains? server-render-pages (get-in data keypaths/navigation-event))]
    (-> ((if prerender? views/prerendered-page views/index) render-ctx data)
        ->html-resp)))

(defn generic-server-render [render-ctx data req params]
  (html-response render-ctx data))

(defn redirect-to-cart [query-params]
  (util.response/redirect (str "/cart?" (codec/form-encode query-params))))

(defn redirect-product->canonical-url
  "Checks that the product exists, and redirects to its canonical url"
  [{:keys [storeback-config]} req {:keys [product-slug]}]
  (some-> (api/product storeback-config product-slug (cookies/get req "id") (cookies/get req "user-token"))
          ;; currently, always the category url... better logic would be to
          ;; util.response/redirect if we're not on the canonical url, though
          ;; that would require that the cljs code handle event/navigate-old-product
          :url-path
          (util.response/redirect :moved-permanently)))

(defn redirect-named-search
  [render-ctx data req {:keys [named-search-slug]}]
  (let [categories (get-in data keypaths/categories)]
    (when-let [category (categories/named-search->category named-search-slug categories)]
      (-> (routes/path-for events/navigate-category category)
          (util.response/redirect :moved-permanently)))))

(defn render-category
  [render-ctx data req {:keys [catalog/category-id page/slug]}]
  (let [categories (get-in data keypaths/categories)]
    (when-let [category (categories/id->category category-id categories)]
      (if (not= slug (:page/slug category))
        (-> (routes/path-for events/navigate-category category)
            (util.response/redirect :moved-permanently))
        (->> (assoc-in data
                       keypaths/current-category-id
                       (:catalog/category-id category))
             (html-response render-ctx))))))

(defn render-product-details [{:keys [storeback-config] :as render-ctx}
                              data
                              {:keys [params] :as req}
                              {:keys [catalog/product-id
                                      page/slug]}]
  (let [sku-id    (:SKU params)
        product   (products/->skuer-schema (get-in data (conj keypaths/sku-sets product-id)))
        sku       (get-in data (conj keypaths/skus sku-id))
        redirect? (or (not= slug (:page/slug product))
                      (and sku-id (not sku)))
        criteria  (select-keys sku
                               (concat (:selector/electives product)
                                       (:selector/essentials product)))]
    (when-let [{:keys [:catalog/product-id :page/slug]} product]
      (if redirect?
        (let [path (routes/path-for events/navigate-product-details
                                    (merge {:catalog/product-id product-id
                                            :page/slug          slug}
                                           (when sku
                                             {:query-params {:SKU (:sku sku)}})))]
          (util.response/redirect path))
        (html-response render-ctx
                       (-> data
                           (assoc-in catalog.keypaths/detailed-product-selected-sku sku)
                           (assoc-in catalog.keypaths/detailed-product-selected-sku-id sku-id)
                           (assoc-in catalog.keypaths/detailed-product-id product-id)
                           (assoc-in keypaths/bundle-builder-selections criteria)))))))

(defn- redirect-if-necessary [render-ctx data event]
  (if (not= (get-in data keypaths/navigation-event) event)
    (util.response/redirect (routes/path-for event))
    (html-response render-ctx data)))

(defn render-leads-page [render-ctx data req params]
  (let [step-id (get-in data leads.keypaths/lead-step-id)]
    (redirect-if-necessary render-ctx data
                           (cond
                             (not (get-in data leads.keypaths/lead-id))
                             events/navigate-leads-home

                             (not (get-in data leads.keypaths/lead-flow-id))
                             events/navigate-leads-resolve

                             (= "details" step-id)
                             events/navigate-leads-registration-details

                             (= "thank-you" step-id)
                             events/navigate-leads-registration-resolve

                             :else
                             (throw (ex-info "Unknown step-id" {:step-id step-id}))))))

(defn render-static-page [template]
  (template/eval template {:url assets/path}))

(defn static-page [[navigate-kw content-kw & static-content-id]]
  (when (= [navigate-kw content-kw] events/navigate-content)
    {:id      static-content-id
     :content (->> static-content-id
                   (map name)
                   (str/join "-")
                   (format "public/content/%s.html")
                   io/resource
                   slurp
                   render-static-page)}))

(defn site-routes [{:keys [storeback-config leads-config environment client-version] :as ctx}]
  (fn [{:keys [store nav-message] :as req}]
    (let [[nav-event params] nav-message]
      (when (not= nav-event events/navigate-not-found)
        (let [render-ctx {:storeback-config storeback-config
                          :environment      environment
                          :client-version   client-version}
              data       (cond-> {}
                           true
                           ((fn [data]
                              (-> data
                                  (assoc-in keypaths/welcome-url
                                            (str (:endpoint leads-config) "?utm_source=shop&utm_medium=referral&utm_campaign=ShoptoWelcome"))
                                  (assoc-in keypaths/store store)
                                  (assoc-in keypaths/environment environment)
                                  experiments/determine-features
                                  (assoc-in keypaths/named-searches (api/named-searches storeback-config))
                                  (assoc-in keypaths/categories categories/initial-categories)
                                  (assoc-in keypaths/static (static-page nav-event))
                                  (assoc-in keypaths/navigation-message nav-message))))

                           (= events/navigate-category nav-event)
                           ((fn [data]
                              (let [category         (categories/id->category (:catalog/category-id params)
                                                                              categories/initial-categories)
                                    {:keys [skus sku-sets]
                                     :as   response} (api/fetch-sku-sets storeback-config (spice.maps/map-values
                                                                                           first
                                                                                           (:criteria category)))
                                    sku-sets         (map (fn [sku-set]
                                                            (update sku-set :criteria/selectors (partial mapv keyword)))
                                                          sku-sets)
                                    {:keys [facets]} (api/fetch-facets storeback-config)
                                    data             (-> data
                                                         (assoc-in keypaths/facets (map #(update % :facet/slug keyword) facets))
                                                         (update-in keypaths/sku-sets merge (index-by :sku-set/id sku-sets))
                                                         (update-in keypaths/skus merge (index-by :sku skus)))]
                                (assoc-in data
                                          keypaths/category-filters-for-browse
                                          (categories/make-category-filters data response)))))

                           (= events/navigate-product-details nav-event)
                           ((fn [data]
                              (let [{:keys [skus sku-sets]} (api/fetch-sku-sets storeback-config (:catalog/product-id params))
                                    sku-sets                (map products/->skuer-schema sku-sets)
                                    {:keys [facets]}        (api/fetch-facets storeback-config)]
                                (-> data
                                    (assoc-in keypaths/facets (map #(update % :facet/slug keyword) facets))
                                    (update-in keypaths/sku-sets merge (index-by :catalog/product-id sku-sets))
                                    (update-in keypaths/skus merge (products/normalize-skus skus)))))))
              render (server-render-pages nav-event generic-server-render)]
          (render render-ctx data req params))))))

(def private-disalloweds ["User-agent: *"
                          "Disallow: /account"
                          "Disallow: /checkout"
                          "Disallow: /orders"
                          "Disallow: /stylist"
                          "Disallow: /cart"
                          "Disallow: /m/"
                          "Disallow: /c/"
                          "Disallow: /admin"
                          "Disallow: /content"])

(def server-render-pages
  {events/navigate-home                       generic-server-render
   events/navigate-category                   render-category
   events/navigate-legacy-named-search        redirect-named-search
   events/navigate-product-details            render-product-details
   events/navigate-content-help               generic-server-render
   events/navigate-content-about-us           generic-server-render
   events/navigate-content-privacy            generic-server-render
   events/navigate-content-tos                generic-server-render
   events/navigate-content-guarantee          generic-server-render
   events/navigate-content-ugc-usage-terms    generic-server-render
   events/navigate-content-program-terms      generic-server-render
   events/navigate-gallery                    generic-server-render
   events/navigate-leads-home                 render-leads-page
   events/navigate-leads-registration-details render-leads-page
   events/navigate-leads-registration-resolve render-leads-page
   events/navigate-leads-resolve              render-leads-page})

(defn robots [{:keys [subdomains]}]
  (if (#{["shop"] ["www"] []} subdomains)
    (string/join "\n" private-disalloweds)
    (string/join "\n" (concat ["User-agent: googlebot"
                               "Disallow: /"
                               ""]
                              private-disalloweds))))

(defn sitemap [{:keys [storeback-config]}]
  (let [{:keys [sku-sets]} (api/fetch-sku-sets storeback-config {})]
    (if sku-sets
      (letfn [(url [[location priority]]
                {:tag :url :content (cond-> [{:tag :loc :content [(str location)]}]
                                      priority (conj {:tag :priority :content [(str priority)]}))})]
        (util.response/response
         (with-out-str (xml/emit {:tag     :urlset
                                  :attrs   {:xmlns "http://www.sitemaps.org/schemas/sitemap/0.9"}
                                  :content (->> (into [["https://mayvenn.com"]
                                                       ["https://welcome.mayvenn.com"                                  "0.60"]
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
                                                       ["https://shop.mayvenn.com/shop/look"                           "0.80"]]
                                                      (for [{:keys [sku-set/id sku-set/slug]} sku-sets]
                                                        [(str "https://shop.mayvenn.com/products/" id "-" slug) "0.80"]))
                                                (mapv url))}))))
      (-> (util.response/response "<error />")
          (util.response/status 502)))))

(defn paypal-routes [{:keys [storeback-config]}]
  (wrap-cookies
   (GET "/orders/:number/paypal/:order-token" [number order-token :as request]
     (if-let [error-code (api/verify-paypal-payment storeback-config number order-token
                                                    (let [headers (:headers request)]
                                                      (or (headers "x-forwarded-for")
                                                          (headers "remote-addr")
                                                          "localhost"))
                                                    (assoc (:query-params request)
                                                           "utm-params"
                                                           {"utm-source"   (cookies/get request "utm-source")
                                                            "utm-campaign" (cookies/get request "utm-campaign")
                                                            "utm-term"     (cookies/get request "utm-term")
                                                            "utm-content"  (cookies/get request "utm-content")
                                                            "utm-medium"   (cookies/get request "utm-medium")}))]
       (util.response/redirect (str "/cart?error=" error-code))
       (util.response/redirect (str "/orders/"
                                    number
                                    "/complete?"
                                    (codec/form-encode {:paypal      true
                                                        :order-token order-token})))))))

(defn logo-routes [{:keys [dc-logo-config]}]
  (wrap-cookies
   (GET "/logo.:ext{htm|gif}" req
     (let [s (or (cookies/get req "session-id")
                 "missing-session-id")]
       (util.response/redirect (str (:endpoint dc-logo-config) "&s=" s))))))

(defn static-routes [_]
  (fn [{:keys [uri] :as req}]
    ;; can't use (:nav-message req) because routes/static-api-routes are not
    ;; included in routes/app-routes
    (let [{nav-event :handler} (bidi/match-route routes/static-api-routes uri)]
      (some-> nav-event routes/bidi->edn static-page :content ->html-resp))))

(defn eastern-offset []
  (- (-> (clj-time.core/time-zone-for-id "America/New_York")
         (.getOffset (clj-time.core/now))
         (/ 3600000))))

(defn leads-routes [{:keys [storeback-config leads-config environment client-version] :as ctx}]
  (fn [{:keys [nav-message] :as request}]
    (when (not= (get nav-message 0) events/navigate-not-found)
      (let [render-ctx           {:storeback-config storeback-config
                                  :environment      environment
                                  :client-version   client-version}
            [nav-event nav-args] nav-message
            data                 (-> {}
                                     (assoc-in leads.keypaths/lead-tracking-id (cookies/get request "leads.tracking-id"))
                                     (assoc-in leads.keypaths/lead-utm-source (cookies/get request "leads.utm-source"))
                                     (assoc-in leads.keypaths/lead-utm-content (cookies/get request "leads.utm-content"))
                                     (assoc-in leads.keypaths/lead-utm-campaign (cookies/get request "leads.utm-campaign"))
                                     (assoc-in leads.keypaths/lead-utm-medium (cookies/get request "leads.utm-medium"))
                                     (assoc-in leads.keypaths/lead-utm-term (cookies/get request "leads.utm-term"))
                                     (assoc-in keypaths/store-slug "welcome")
                                     (assoc-in keypaths/environment environment)
                                     (assoc-in keypaths/navigation-message nav-message)
                                     (assoc-in leads.keypaths/eastern-offset (eastern-offset))
                                     (assoc-in leads.keypaths/tz-abbreviation "EST")
                                     (assoc-in leads.keypaths/call-slot-options [["Best time to call*" ""]])
                                     ((fn [data]
                                        (let [cookies     (get request :cookies)
                                              lead-id     (get-in cookies ["lead-id" :value])
                                              remote-lead (api/lookup-lead storeback-config lead-id)]
                                          (-> data
                                              (assoc-in leads.keypaths/remote-lead remote-lead)
                                              (update-in leads.keypaths/lead merge remote-lead))))))]
        ((server-render-pages nav-event) render-ctx data request nav-args)))))

(defn wrap-welcome-is-for-leads [h]
  (fn [{:keys [subdomains nav-message query-params] :as req}]
    (let [on-leads-page?        (routes/sub-page? nav-message [events/navigate-leads])
          on-home-page?         (= events/navigate-home (get nav-message 0))
          on-welcome-subdomain? (= "welcome" (first subdomains))
          not-found             #(-> views/not-found
                                     ->html-resp
                                     (util.response/status 404))]
      (if on-welcome-subdomain?
        (cond
          on-leads-page? (h req)
          on-home-page?  (util.response/redirect (routes/path-for events/navigate-leads-home {:query-params query-params})
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

(defn wrap-migrate-lead-utm-params-cookies [h {:keys [environment]}]
  (fn [{:keys [server-name] :as req}]
    (let [leads-utm-params        {"utm_source"   "leads.utm-source"
                                   "utm_medium"   "leads.utm-medium"
                                   "utm_campaign" "leads.utm-campaign"
                                   "utm_content"  "leads.utm-content"
                                   "utm_term"     "leads.utm-term"}
          value-from-original-req (fn [old-leads-key new-leads-key]
                                    (or (-> req :query-params (get old-leads-key))
                                        (cookies/get req new-leads-key)
                                        (cookies/get req old-leads-key)))
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
      (wrap-migrate-lead-utm-params-cookies ctx)
      (wrap-migrate-lead-tracking-id-cookie ctx)
      (wrap-defaults (storefront-site-defaults environment))
      (wrap-welcome-is-for-leads)
      (wrap-resource "public")
      (wrap-content-type)))

(defn wrap-add-nav-message [h]
  (fn [{:keys [uri query-params] :as req}]
    (h (assoc req :nav-message (routes/navigation-message-for uri query-params)))))

(defn redirect-to-home [environment {:keys [subdomains] :as req}]
  (util.response/redirect (store-homepage (first subdomains) environment req)
                          :moved-permanently))

(defn redirect-to-product-details [environment {:keys [subdomains params] :as req}]
  (util.response/redirect (str "/products/" (:id-and-slug params) "?SKU=" (:sku params))
                          :moved-permanently))

(defn create-handler
  ([] (create-handler {}))
  ([{:keys [logger exception-handler environment] :as ctx}]
   (-> (routes (GET "/healthcheck" [] "cool beans")
               (GET "/robots.txt" req (-> (robots req) util.response/response (util.response/content-type "text/plain")))
               (GET "/sitemap.xml" req (-> (sitemap ctx) (util.response/content-type "text/xml")))
               (GET "/stylist/edit" [] (util.response/redirect "/stylist/account/profile" :moved-permanently))
               (GET "/stylist/account" [] (util.response/redirect "/stylist/account/profile" :moved-permanently))
               (GET "/categories" req (redirect-to-home environment req))
               (GET "/categories/" req (redirect-to-home environment req))
               (GET "/products" req (redirect-to-home environment req))
               (GET "/products/" req (redirect-to-home environment req))
               (GET "/products/:id-and-slug/:sku" req (redirect-to-product-details environment req))
               (logo-routes ctx)
               (static-routes ctx)
               (paypal-routes ctx)
               (wrap-leads-routes (leads-routes ctx) ctx)
               (wrap-site-routes (site-routes ctx) ctx)
               (route/not-found views/not-found))
       (wrap-add-nav-message)
       (wrap-add-domains)
       (wrap-logging logger)
       (wrap-params)
       (#(if (#{"development" "test"} environment)
           (wrap-exceptions %)
           (wrap-internal-error %
                                :log (comp (partial logger :error) exception-handler)
                                :error-response views/error-page))))))
