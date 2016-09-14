(ns storefront.handler
  (:require [storefront.config :as config]
            [storefront.app-routes :refer [app-routes bidi->edn]]
            [storefront.events :as events]
            [storefront.api :as api]
            [storefront.views :as views]
            [storefront.keypaths :as keypaths]
            [storefront.cache :as cache]
            [storefront.cookies :as cookies]
            [storefront.utils.maps :refer [key-by]]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.accessors.products :as products]
            [storefront.accessors.bundle-builder :as bundle-builder]
            [clojure.string :as string]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [bidi.bidi :as bidi]
            [clj-http.client :as http]
            [ring.middleware.defaults :refer :all]
            [ring.util.response :refer [redirect response status content-type header]]
            [noir-exception.core :refer [wrap-internal-error wrap-exceptions]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.util.codec :as codec]
            [ring-logging.core :as ring-logging]))

(defn storefront-site-defaults
  [env]
  (if (config/development? env)
    site-defaults
    (-> secure-site-defaults
        (assoc :proxy true)
        (assoc-in [:security :hsts] false)
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
  (content-type (response h) "text/html"))

(defn store-scheme-and-authority [store-slug environment {:keys [scheme server-name server-port] :as req}]
  (let [dev? (config/development? environment)]
    (str (if dev? (name scheme) "https") "://"
         store-slug "." (parse-root-domain server-name)
         (if dev? (str ":" server-port) ""))))

(defn store-url [store-slug environment {:keys [uri] :as req}]
  (str (store-scheme-and-authority store-slug environment req)
       uri
       (query-string req)))

(defn wrap-add-domains [h]
  (fn [req]
    (h (merge req {:subdomains (parse-subdomains (:server-name req))}))))

(defn wrap-remove-superfluous-www-redirect [h environment]
  (fn [{subdomains :subdomains
        {store-slug :store_slug} :store
        :as req}]
    (if (= "www" (first subdomains))
      (redirect (store-url store-slug environment req))
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
      (-> (redirect (store-url "store" environment req))
          (cookies/expire environment
                          "preferred-store-slug"
                          {:http-only true
                           :domain    (cookie-root-domain server-name)})))))

(defn wrap-known-subdomains-redirect [h environment]
  (fn [{:keys [subdomains server-name server-port] :as req}]
    (cond
      (= "vistaprint" (first subdomains))
      (redirect "http://www.vistaprint.com/vp/gateway.aspx?sr=no&s=6797900262")

      (#{[] ["www"]} subdomains)
      (redirect (str (store-scheme-and-authority "shop" environment req) "/" (query-string req)))

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
      (if (and (#{"store" "shop"} (last subdomains))
               (not (#{nil "" "store" "shop"} preferred-store-slug)))
        (redirect (store-url preferred-store-slug environment req))
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
    events/navigate-categories
    events/navigate-category
    events/navigate-help
    events/navigate-guarantee})

(defn html-response [render-ctx data]
  (let [prerender? (server-render-pages (get-in data keypaths/navigation-event))]
    (-> ((if prerender? views/prerendered-page views/index) render-ctx data)
        ->html-resp)))

(defn redirect-to-cart [query-params]
  (redirect (str "/cart?" (codec/form-encode query-params))))

(defn redirect-product->canonical-url
  "Checks that the product exists, and redirects to its canonical url"
  [{:keys [storeback-config]} req {:keys [product-slug]}]
  (some-> (api/product storeback-config product-slug (cookies/get req "user-token"))
          ;; currently, always the category url... better logic would be to redirect if we're not on the canonical url, though that would require that the cljs code handle event/navigate-product
          :url-path
          redirect))

(defn render-category
  "Checks that the category exists, and that customer has access to its products"
  [{:keys [storeback-config] :as render-ctx}
   {:keys [named-searches] :as data}
   req
   {:keys [named-search-slug]}]
  (let [data (assoc-in data (conj keypaths/browse-named-search-query :slug) named-search-slug)]
    (when-let [named-search (named-searches/current-named-search data)]
      (let [{:keys [product-ids]} named-search
            user-token            (cookies/get req "user-token")]
        (when-let [products (seq (api/products-by-ids storeback-config product-ids user-token))]
          (let [products-by-id (key-by :id products)]
            (html-response render-ctx (-> data
                                          (assoc-in keypaths/browse-variant-quantity 1)
                                          (assoc-in keypaths/products products-by-id)
                                          (assoc-in keypaths/bundle-builder (bundle-builder/initialize named-search products-by-id (experiments/kinky-straight? data)))))))))))

(defn site-routes [{:keys [storeback-config leads-config environment] :as ctx}]
  (fn [{:keys [uri store] :as req}]
    (let [{nav-event :handler params :route-params} (bidi/match-route app-routes uri)]
      (when nav-event
        (let [nav-event  (bidi->edn nav-event)
              render-ctx {:storeback-config storeback-config
                          :environment      environment}
              data       (as-> {} data
                           (assoc-in data keypaths/welcome-url
                                     (str (:endpoint leads-config) "?utm_source=shop&utm_medium=referral&utm_campaign=ShoptoWelcome"))
                           (assoc-in data keypaths/store store)
                           (experiments/determine-features data)
                           (assoc-in data keypaths/named-searches (api/named-searches storeback-config))
                           (assoc-in data keypaths/navigation-message [nav-event params]))]
          (condp = nav-event
            events/navigate-product  (redirect-product->canonical-url ctx req params)
            events/navigate-category (if (or (= "blonde" (:named-search-slug params))
                                             (and (= "kinky-straight" (:named-search-slug params))
                                                  (not (experiments/kinky-straight? data))))
                                       (redirect (store-url (:store_slug store) environment (assoc req :uri "/categories")))
                                       (render-category render-ctx data req params))
            (html-response render-ctx data)))))))

(def private-disalloweds ["User-agent: *"
                          "Disallow: /account"
                          "Disallow: /checkout"
                          "Disallow: /orders"
                          "Disallow: /stylist"
                          "Disallow: /cart"
                          "Disallow: /m/"
                          "Disallow: /c/"
                          "Disallow: /admin"])

(defn robots [{:keys [subdomains]}]
  (if (#{["shop"] ["www"] []} subdomains)
    (string/join "\n" private-disalloweds)
    (string/join "\n" (concat ["User-agent: googlebot"
                               "Disallow: /"
                               ""]
                              private-disalloweds))))

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
       (redirect (str "/cart?error=" error-code))
       (redirect (str "/orders/"
                      number
                      "/complete?"
                      (codec/form-encode {:paypal      true
                                          :order-token order-token})))))))

(defn create-handler
  ([] (create-handler {}))
  ([{:keys [logger exception-handler environment] :as ctx}]
   (-> (routes (GET "/healthcheck" [] "cool beans")
               (GET "/robots.txt" req (content-type (response (robots req))
                                                    "text/plain"))
               (GET "/stylist/edit" [] (redirect "/stylist/account/profile"))
               (GET "/stylist/account" [] (redirect "/stylist/account/profile"))
               (paypal-routes ctx)
               (wrap-site-routes (site-routes ctx) ctx)
               (route/not-found views/not-found))
       (wrap-add-domains)
       (wrap-logging logger)
       (wrap-params)
       (#(if (#{"development" "test"} environment)
           (wrap-exceptions %)
           (wrap-internal-error %
                                :log (comp (partial logger :error) exception-handler)
                                :error-response views/error-page))))))
