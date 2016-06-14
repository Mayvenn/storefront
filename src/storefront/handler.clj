(ns storefront.handler
  (:require [storefront.config :as config]
            [storefront.app-routes :refer [app-routes bidi->edn]]
            [storefront.events :as events]
            [storefront.fetch :as fetch]
            [storefront.views :as views]
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
              {:store (fetch/store storeback-config (last subdomains))}))))

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

      ;; TODO: render an unavailable page
      (= store :storefront.fetch/storeback-unavailable)
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

(defn respond-with-index [req storeback-config environment]
  (-> (views/index req storeback-config environment)
      response
      (content-type "text/html")))

(defn dumb-encoder
  "Our cookies have colons at the beginning of their names (e.g. ':token'). But
  the default cookie encoder is codec/form-encode, which escapes colons. It is
  only safe to use this encoder if you know the cookie names are URL safe"
  [map-entry]
  (let [[k v] (first map-entry)]
    (str k "=" v)))

(defn site-routes
  [{:keys [storeback-config environment]}]
  (fn [{:keys [uri] :as req}]
    (let [{nav-event :handler params :route-params} (bidi/match-route app-routes uri)
          token                                     (get-in req [:cookies "user-token" :value])
          user-id                                   (get-in req [:cookies "id" :value])
          store-id                                  (get-in req [:store :stylist_id])
          store-slug                                (get-in req [:store :store_slug])]
      (when nav-event
        (condp = (bidi->edn nav-event)
          events/navigate-product     (some-> (fetch/product storeback-config (:product-slug params) token)
                                              ;; currently, always the category url... better logic would be to redirect if we're not on the canonical url, though that would require that the cljs code handle event/navigate-product
                                              :url-path
                                              redirect)
          events/navigate-category    (when (fetch/category storeback-config (:taxon-slug params) token)
                                        (respond-with-index req storeback-config environment))
          events/navigate-shared-cart (when-let [order (fetch/create-order-from-cart storeback-config (:shared-cart-id params) user-id token store-id)]
                                        (let [cookie-config {:secure  (not (config/development? environment))
                                                             :path    "/"
                                                             :max-age (* 60 60 24 7 4)}]
                                          (-> (redirect (str "/cart?" (codec/form-encode {:utm_source "sharecart"
                                                                                          :utm_medium store-slug})))
                                              (assoc :cookies {:number (merge cookie-config {:value (:number order)})
                                                               :token  (merge cookie-config {:value (:token order)})})
                                              (cookies/cookies-response {:encoder dumb-encoder}))))
          (respond-with-index req storeback-config environment))))))

(defn robots [{:keys [subdomains]}]
  (if (#{["shop"] ["www"] []} subdomains)
    (string/join "\n" ["User-agent: *"
                       "Disallow: /account"
                       "Disallow: /checkout"
                       "Disallow: /orders"
                       "Disallow: /stylist"
                       "Disallow: /cart"
                       "Disallow: /m/"
                       "Disallow: /admin"])
    (string/join "\n" ["User-agent: googlebot"
                       "Disallow: /"])))

;; TODO: move me to fetch
(defn verify-paypal-payment [storeback-config number order-token ip-addr {:strs [sid]}]
  (let [response (http/post (str (:endpoint storeback-config) "/v2/place-order")
                            {:form-params {:number number
                                           :token order-token
                                           :session-id sid}
                             :headers {"X-Forwarded-For" ip-addr}
                             :content-type :json
                             :throw-exceptions false
                             :socket-timeout 10000
                             :conn-timeout 10000
                             :as :json
                             :coerce :always})]
    (when-not (<= 200 (:status response) 299)
      (-> response :body :error-code (or "paypal-incomplete")))))

(defn paypal-routes [{:keys [storeback-config]}]
  (routes
   (GET "/orders/:number/paypal/:order-token" [number order-token :as request]
        (if-let [error-code (verify-paypal-payment storeback-config number order-token
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
