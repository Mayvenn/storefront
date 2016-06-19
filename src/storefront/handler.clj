(ns storefront.handler
  (:require [storefront.config :as config]
            [storefront.app-routes :refer [app-routes bidi->edn]]
            [storefront.events :as events]
            [storefront.api :as api]
            [storefront.views :as views]
            [storefront.keypaths :as keypaths]
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
            [ring.middleware.cookies :as cookies]
            [ring.util.codec :as codec]
            [ring-logging.core :as ring-logging]
            [storefront.prerender :refer [wrap-prerender]]))

(defn storefront-site-defaults
  [env]
  (if (config/development? env)
    site-defaults
    (-> secure-site-defaults
        (assoc :proxy true)
        (assoc-in [:security :hsts] false)
        (assoc-in [:static :resources] false))))

(defn parse-tld [server-name]
  (->> (string/split server-name #"\.")
       (take-last 2)
       (string/join ".")))

(defn query-string [req]
  (let [query-str (:query-string req)]
    (when (seq query-str)
      (str "?" query-str))))

(defn parse-subdomains [server-name]
  (->> (string/split server-name #"\.")
       (drop-last 2)))

(defn wrap-fetch-store [h storeback-config]
  (fn [{:keys [subdomains] :as req}]
    (h (merge req
              {:store (api/store storeback-config (last subdomains))}))))

(defn parse-domain [{:keys [server-name server-port]}]
  (str (parse-tld server-name) ":" server-port))

(defn wrap-stylist-not-found-redirect [h]
  (fn [{domain :domain
       subdomains :subdomains
       {store-slug :store_slug :as store} :store
       :as req}]
    (cond
      (= "www" (first subdomains))
      (redirect (str "http://" store-slug "." domain (query-string req)))

      (= store :storefront.api/storeback-unavailable)
      (h req)

      store-slug
      (h req)

      :else
      (redirect (str "http://store." domain (query-string req))))))

(defn wrap-known-subdomains-redirect [h]
  (fn [{:keys [subdomains domain] :as req}]
    (cond
      (= "vistaprint" (first subdomains))
      (redirect "http://www.vistaprint.com/vp/gateway.aspx?sr=no&s=6797900262")

      (#{[] ["www"]} subdomains)
      (redirect (str "http://welcome." domain "/hello" (query-string req)))

      :else
      (h req))))

(defn wrap-add-domains [h]
  (fn [req]
    (h (merge req {:subdomains (parse-subdomains (:server-name req))
                   :domain (parse-domain req)}))))

(defn prerender-original-request-url [development? req]
  (str (name (:scheme req)) "://shop."
       (parse-tld (:server-name req))
       ":" (if development? (:server-port req) 443) (:uri req)))

(defn wrap-logging [handler logger]
  (-> handler
      ring-logging/wrap-request-timing
      (ring-logging/wrap-logging logger ring-logging/simple-inbound-config)
      ring-logging/wrap-trace-request))

(defn wrap-site-routes
  [routes {:keys [storeback-config environment prerender-token]}]
  (-> routes
      (wrap-prerender (config/development? environment)
                      prerender-token
                      (partial prerender-original-request-url
                               (config/development? environment)))
      (wrap-defaults (storefront-site-defaults environment))
      (wrap-stylist-not-found-redirect)
      (wrap-fetch-store storeback-config)
      (wrap-known-subdomains-redirect)
      (wrap-resource "public")
      (wrap-content-type)))

(defn dumb-encoder
  "Our cookies have colons at the beginning of their names (e.g. ':token'). But
  the default cookie encoder is codec/form-encode, which escapes colons. It is
  only safe to use this encoder if you know the cookie names are URL safe"
  [map-entry]
  (let [[k v] (first map-entry)]
    (str k "=" v)))

(defn encode-cookies [resp]
  (cookies/cookies-response resp {:encoder dumb-encoder}))

(defn get-cookie [req name] (get-in req [:cookies name :value]))
(defn expire-cookie [resp environment name]
  (assoc-in resp [:cookies name] {:value   ""
                                  :max-age 0
                                  :secure  (not (config/development? environment))
                                  :path    "/"}))
(defn set-cookie [resp environment name value]
  (assoc-in resp [:cookies name] {:value   value
                                  :max-age (* 60 60 24 7 4)
                                  :secure  (not (config/development? environment))
                                  :path    "/"}))

(def server-render-pages
  #{events/navigate-home
    events/navigate-guarantee
    events/navigate-help
    events/navigate-categories})

(defn html-response [render-ctx data]
  (let [prerender? (server-render-pages (get-in data keypaths/navigation-event))]
    (-> ((if prerender? views/prerendered-page views/index) render-ctx data)
        response
        (content-type "text/html"))))

(defn redirect-to-cart [query-params]
  (redirect (str "/cart?" (codec/form-encode query-params))))

(defn redirect-product->canonical-url
  "Checks that the product exists, and redirects to its canonical url"
  [{:keys [storeback-config]} req {:keys [product-slug]}]
  (some-> (api/product storeback-config product-slug (get-cookie req "user-token"))
          ;; currently, always the category url... better logic would be to redirect if we're not on the canonical url, though that would require that the cljs code handle event/navigate-product
          :url-path
          redirect))

(defn render-category
  "Checks that the category exists"
  [{:keys [storeback-config] :as render-ctx} data req {:keys [taxon-slug]}]
  (when (api/category storeback-config taxon-slug (get-cookie req "user-token"))
    (html-response render-ctx data)))

(defn create-order-from-shared-cart [{:keys [storeback-config environment]}
                                     {:keys [store] :as req}
                                     {:keys [shared-cart-id]}]
  (let [{stylist-id :stylist_id store-slug :store_slug} store
        {:keys [number token error-code]} (api/create-order-from-cart storeback-config
                                                                      shared-cart-id
                                                                      (get-cookie req "id")
                                                                      (get-cookie req "user-token")
                                                                      stylist-id)]
    (if number
      (-> (redirect-to-cart {:utm_source "sharecart"
                             :utm_content store-slug
                             :message "shared-cart"})
          (set-cookie environment :number number)
          (set-cookie environment :token token)
          encode-cookies)
      (-> (redirect-to-cart {:error "share-cart-failed"})
          (expire-cookie environment :number)
          (expire-cookie environment :token)
          encode-cookies))))

(defn site-routes [{:keys [storeback-config environment] :as ctx}]
  (fn [{:keys [uri store] :as req}]
    (let [{nav-event :handler params :route-params} (bidi/match-route app-routes uri)
          nav-event (bidi->edn nav-event)
          render-ctx {:storeback-config storeback-config
                      :environment environment}
          data (-> {}
                   (assoc-in keypaths/store store)
                   (assoc-in keypaths/taxons (api/taxons storeback-config))
                   (assoc-in keypaths/navigation-message [nav-event params]))]
      (when nav-event
        (condp = nav-event
          events/navigate-product     (redirect-product->canonical-url ctx req params)
          events/navigate-category    (render-category render-ctx data req params)
          events/navigate-shared-cart (create-order-from-shared-cart ctx req params)
          (html-response render-ctx data))))))

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
  (routes
   (GET "/orders/:number/paypal/:order-token" [number order-token :as request]
        (if-let [error-code (api/verify-paypal-payment storeback-config number order-token
                                                       (let [headers (:headers request)]
                                                         (or (headers "x-forwarded-for")
                                                             (headers "remote-addr")
                                                             "localhost"))
                                                       (:query-params request))]
          (redirect (str "/cart?error=" error-code))
          (redirect (str "/orders/"
                         number
                         "/complete?"
                         (codec/form-encode {:paypal true
                                             :order-token order-token})))))))

(defn create-handler
  ([] (create-handler {}))
  ([{:keys [logger exception-handler environment] :as ctx}]
   (-> (routes (GET "/healthcheck" [] "cool beans")
               (GET "/robots.txt" req (content-type (response (robots req))
                                                    "text/plain"))
               (paypal-routes ctx)
               (wrap-site-routes (site-routes ctx) ctx)
               (route/not-found views/not-found))
       (wrap-add-domains)
       (wrap-logging logger)
       (wrap-params)
       (#(if (config/development? environment)
           (wrap-exceptions %)
           (wrap-internal-error %
                                :log (comp (partial logger :error) exception-handler)
                                :error-response "{\"error\": \"something went wrong\"}"))))))
