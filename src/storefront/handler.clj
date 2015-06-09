(ns storefront.handler
  (:require [storefront.config :as config]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [clj-http.client :as http]
            [ring.middleware.defaults :refer :all]
            [ring.util.response :refer [redirect response status content-type header]]
            [noir-exception.core :refer [wrap-internal-error wrap-exceptions]]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [ring-logging.core :refer [make-logger-middleware]]
            [hiccup.page :as page]
            [hiccup.element :as element]))

(defn storefront-site-defaults
  [env]
  (if (config/development? env)
    site-defaults
    (assoc secure-site-defaults :proxy true)))

(defn fetch-store [storeback-config store-slug]
  (when (seq store-slug)
    (->
     (http/get (str (:endpoint storeback-config) "/store")
               {:query-params {:store_slug store-slug}
                :throw-exceptions false
                :socket-timeout 10000
                :conn-timeout 10000
                :as :json})
     :body)))

(defn parse-subdomains [server-name]
  (->> (string/split server-name #"\.")
       (drop-last 2)))

(defn parse-tld [server-name]
  (->> (string/split server-name #"\.")
       (take-last 2)
       (string/join ".")))

(defn query-string [req]
  (let [query-str (:query-string req)]
    (when (seq query-str)
      (str "?" query-str))))

(defn wrap-redirect [h storeback-config]
  (fn [req]
    (let [subdomains (parse-subdomains (:server-name req))
          domain (str (parse-tld (:server-name req)) ":"
                      (:server-port req)
                      (query-string req))
          store (fetch-store storeback-config (last subdomains))]
      (cond
        (#{[] ["www"]} subdomains) (redirect (str "http://welcome." domain))
        (= "www" (first subdomains)) (redirect (str "http://" (:store_slug store) "." domain))
        (:store_slug store) (h req)
        :else (redirect (str "http://store." domain))))))

(defn index [storeback-config env]
  (page/html5
   [:head
    (page/include-css "/css/all.css")]
   [:body
    [:div#content]
    (element/javascript-tag (str "var environment=\"" env "\";"))
    [:script {:src "/js/out/main.js"}]]))

(defn site-routes
  [logger storeback-config environment]
  (->
   (routes
    (GET "/healthcheck" [] "cool beans")
    (route/resources "/")
    (GET "*" [] (content-type (response (index storeback-config environment)) "text/html")))
   (wrap-redirect storeback-config)
   (make-logger-middleware logger)
   (wrap-defaults (storefront-site-defaults environment))))

(defn create-handler
  ([] (create-handler {}))
  ([{:keys [logger exception-handler storeback-config environment]}]
   (-> (routes (site-routes logger storeback-config environment)
               (route/not-found "Not found"))
       (#(if (config/development? environment)
           (wrap-exceptions %)
           (wrap-internal-error %
                                :log (comp (partial logger :error) exception-handler)
                                :error-response "{\"error\": \"something went wrong\"}"))))))
