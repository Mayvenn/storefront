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
             [api :as api]
             [routes :as routes]
             [config :as config]
             [cookies :as cookies]
             [events :as events]
             [keypaths :as keypaths]
             [assets :as assets]
             [views :as views]]
            [storefront.accessors
             [bundle-builder :as bundle-builder]
             [experiments :as experiments]
             [named-searches :as named-searches]]
            [comb.template :as template]
            [storefront.utils.maps :refer [key-by]]
            [clojure.string :as str]
            [clojure.xml :as xml]
            [storefront.accessors.categories :as categories]))

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
      (= store :storefront.api/storeback-unavailable)
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

      (#{[] ["www"] ["internal"]} subdomains)
      (util.response/redirect (store-homepage "shop" environment req))

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

(def server-render-pages
  #{events/navigate-home
    events/navigate-category
    events/navigate-named-search
    events/navigate-content-help
    events/navigate-content-about-us
    events/navigate-content-privacy
    events/navigate-content-tos
    events/navigate-content-guarantee
    events/navigate-content-ugc-usage-terms
    events/navigate-content-program-terms
    events/navigate-gallery
    events/navigate-leads-home})

(defn html-response [render-ctx data]
  (let [prerender? (server-render-pages (get-in data keypaths/navigation-event))]
    (-> ((if prerender? views/prerendered-page views/index) render-ctx data)
        ->html-resp)))

(defn redirect-to-cart [query-params]
  (util.response/redirect (str "/cart?" (codec/form-encode query-params))))

(defn redirect-product->canonical-url
  "Checks that the product exists, and redirects to its canonical url"
  [{:keys [storeback-config]} req {:keys [product-slug]}]
  (some-> (api/product storeback-config product-slug (cookies/get req "id") (cookies/get req "user-token"))
          ;; currently, always the category url... better logic would be to
          ;; util.response/redirect if we're not on the canonical url, though
          ;; that would require that the cljs code handle event/navigate-product
          :url-path
          (util.response/redirect :moved-permanently)))

(defn render-named-search
  "Checks that the named-search exists, and that customer has access to its products"
  [{:keys [storeback-config] :as render-ctx}
   data
   req
   {:keys [named-search-slug]}]
  (let [data (assoc-in data (conj keypaths/browse-named-search-query :slug) named-search-slug)]
    (when-let [named-search (named-searches/current-named-search data)]
      (let [{:keys [product-ids]} named-search
            user-token            (cookies/get req "user-token")
            user-id               (cookies/get req "id")]
        (if-let [products (seq (api/products-by-ids storeback-config product-ids user-id user-token))]
          (let [products-by-id (key-by :id products)]
            (html-response render-ctx (-> data
                                          (assoc-in keypaths/browse-variant-quantity 1)
                                          (assoc-in keypaths/products products-by-id)
                                          (assoc-in keypaths/bundle-builder (bundle-builder/initialize named-search products-by-id)))))
          (when-not (seq user-token)
            (util.response/redirect (str "/login?path=" (:uri req)))))))))

(defn render-category [{:keys [storeback-config] :as render-ctx}
                       data
                       req
                       {:keys [id slug] :as params}]
  (when-let [category (categories/id->category id (get-in data keypaths/categories))]
    (if (= slug (:slug category))
      (html-response render-ctx (-> data
                                    (assoc-in keypaths/current-category-id (:id category))))
      (util.response/redirect (routes/path-for events/navigate-category (select-keys category [:id :slug]))))))

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
              data       (as-> {} data
                           (assoc-in data keypaths/welcome-url
                                     (str (:endpoint leads-config) "?utm_source=shop&utm_medium=referral&utm_campaign=ShoptoWelcome"))
                           (assoc-in data keypaths/store store)
                           (assoc-in data keypaths/environment environment)
                           (experiments/determine-features data)
                           (assoc-in data keypaths/named-searches (api/named-searches storeback-config))
                           (assoc-in data keypaths/categories categories/initial-categories)
                           (assoc-in data keypaths/static (static-page nav-event))
                           (assoc-in data keypaths/navigation-message nav-message))]
          (condp = nav-event
            events/navigate-product      (redirect-product->canonical-url ctx req params)
            events/navigate-named-search (if (= "blonde" (:named-search-slug params))
                                           (util.response/redirect (store-homepage (:store_slug store) environment req)
                                                                   :moved-permanently)
                                           (render-named-search render-ctx data req params))
            events/navigate-category     (render-category render-ctx data req params)
            (html-response render-ctx data)))))))

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

(defn robots [{:keys [subdomains]}]
  (if (#{["shop"] ["www"] []} subdomains)
    (string/join "\n" private-disalloweds)
    (string/join "\n" (concat ["User-agent: googlebot"
                               "Disallow: /"
                               ""]
                              private-disalloweds))))

(defn sitemap [{:keys [storeback-config]}]
  (letfn [(url [[location priority]]
            {:tag :url :content (cond-> [{:tag :loc :content [(str location)]}]
                                  priority (conj {:tag :priority :content [(str priority)]}))})]
    (with-out-str (xml/emit {:tag     :urlset
                             :attrs   {:xmlns "http://www.sitemaps.org/schemas/sitemap/0.9"}
                             :content (mapv url [["https://mayvenn.com"]
                                                 ["https://welcome.mayvenn.com"                             "0.60"]
                                                 ["https://shop.mayvenn.com"                                "1.00"]
                                                 ["https://shop.mayvenn.com/guarantee"                      "0.60"]
                                                 ["https://shop.mayvenn.com/help"                           "0.60"]
                                                 ["https://shop.mayvenn.com/about-us"                       "0.60"]
                                                 ["https://shop.mayvenn.com/categories/hair/straight"       "0.80"]
                                                 ["https://shop.mayvenn.com/categories/hair/yaki-straight"  "0.80"]
                                                 ["https://shop.mayvenn.com/categories/hair/kinky-straight" "0.80"]
                                                 ["https://shop.mayvenn.com/categories/hair/body-wave"      "0.80"]
                                                 ["https://shop.mayvenn.com/categories/hair/loose-wave"     "0.80"]
                                                 ["https://shop.mayvenn.com/categories/hair/water-wave"     "0.80"]
                                                 ["https://shop.mayvenn.com/categories/hair/deep-wave"      "0.80"]
                                                 ["https://shop.mayvenn.com/categories/hair/curly"          "0.80"]
                                                 ["https://shop.mayvenn.com/categories/hair/closures"       "0.80"]
                                                 ["https://shop.mayvenn.com/categories/hair/frontals"       "0.80"]
                                                 ["https://shop.mayvenn.com/categories/hair/360-frontals"   "0.80"]
                                                 ["https://shop.mayvenn.com/shop/look"                      "0.80"]])}))))

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

(defn leads-routes [{:keys [storeback-config leads-config environment client-version] :as ctx}]
  (fn [{:keys [nav-message] :as request}]
    (when (not= (get nav-message 0) events/navigate-not-found)
      (let [render-ctx {:storeback-config storeback-config
                        :environment      environment
                        :client-version   client-version}
            data       (as-> {} data
                         (assoc-in data keypaths/leads-lead-tracking-id (cookies/get request "leads.tracking-id"))
                         (assoc-in data keypaths/leads-utm-source (cookies/get request "leads.utm-source"))
                         (assoc-in data keypaths/leads-utm-content (cookies/get request "leads.utm-content"))
                         (assoc-in data keypaths/leads-utm-campaign (cookies/get request "leads.utm-campaign"))
                         (assoc-in data keypaths/leads-utm-medium (cookies/get request "leads.utm-medium"))
                         (assoc-in data keypaths/leads-utm-term (cookies/get request "leads.utm-term"))
                         (assoc-in data keypaths/store-slug "welcome")
                         (assoc-in data keypaths/environment environment)
                         (assoc-in data keypaths/navigation-message nav-message))]
        (html-response render-ctx data)))))

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
    (let [tracking-id     (or (cookies/get req "leads.tracking-id")
                              (cookies/get req "tracking_id") ;; from old leads site
                              (java.util.UUID/randomUUID))
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
                                    (or (cookies/get req new-leads-key)
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

(defn create-handler
  ([] (create-handler {}))
  ([{:keys [logger exception-handler environment] :as ctx}]
   (-> (routes (GET "/healthcheck" [] "cool beans")
               (GET "/robots.txt" req (-> (robots req) util.response/response (util.response/content-type "text/plain")))
               (GET "/sitemap.xml" req (-> (sitemap ctx) util.response/response (util.response/content-type "text/xml")))
               (GET "/stylist/edit" [] (util.response/redirect "/stylist/account/profile" :moved-permanently))
               (GET "/stylist/account" [] (util.response/redirect "/stylist/account/profile" :moved-permanently))
               (GET "/categories" {:keys [subdomains] :as req}
                 (util.response/redirect (store-homepage (first subdomains) environment req)
                                         :moved-permanently))
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
