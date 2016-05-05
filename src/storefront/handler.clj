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
  (fn [req]
    (let [subdomains (parse-subdomains (:server-name req))
          store (fetch/store storeback-config (last subdomains))]
      (h (merge req
                {:store store
                 :subdomains subdomains})))))

(defn wrap-redirect [h]
  (fn [req]
    (let [subdomains (:subdomains req)
          subdomain (first subdomains)
          domain (str (parse-tld (:server-name req)) ":"
                      (:server-port req))
          store (:store req)
          store-slug (:store_slug store)]
      (cond
        (= "vistaprint" subdomain)
        (redirect "http://www.vistaprint.com/vp/gateway.aspx?sr=no&s=6797900262")

        (#{[] ["www"]} subdomains)
        (redirect (str "http://welcome." domain "/hello" (query-string req)))

        (= "www" subdomain)
        (redirect (str "http://" store-slug "." domain (query-string req)))

        (= store ::storeback-unavailable)
        (h req)

        store-slug
        (h req)

        :else
        (redirect (str "http://store." domain (query-string req)))))))

(defn request-scheme [req]
  (if-let [forwarded-proto (get-in req [:headers "x-forwarded-proto"])]
    (keyword forwarded-proto)
    (:scheme req)))

(defn prerender-original-request-url [development? req]
  (str (name (request-scheme req)) "://shop."
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
      (wrap-redirect)
      (wrap-fetch-store storeback-config)
      (wrap-resource "public")
      (wrap-content-type)))

(def not-404 (comp (partial not= 404) :status))
(defn resource-exists [storeback-config nav-event params req]
  (let [token (get-in req [:cookies "token" :value])]
    (condp = nav-event
      events/navigate-category (not-404 (fetch/category storeback-config (:taxon-slug params) token))
      events/navigate-product (not-404 (fetch/product storeback-config (:product-slug params) token))
      true)))

(defn site-routes
  [{:keys [storeback-config environment]}]
  (fn [{:keys [uri] :as req}]
    (let [{nav-event :handler params :route-params} (bidi/match-route app-routes uri)]
      (when (and nav-event
                 (resource-exists storeback-config (bidi->edn nav-event) params req))
        (-> (views/index req storeback-config environment)
            response
            (content-type "text/html"))))))

(defn robots [req]
  (let [subdomains (parse-subdomains (:server-name req))]
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
                         "Disallow: /"]))))

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
       (wrap-logging logger)
       (wrap-params)
       (#(if (config/development? environment)
           (wrap-exceptions %)
           (wrap-internal-error %
                                :log (comp (partial logger :error) exception-handler)
                                :error-response "{\"error\": \"something went wrong\"}"))))))
