(ns storefront.handler
  (:require [storefront.config :as config]
            [storefront.app-routes :refer [app-routes bidi->edn]]
            [storefront.events :as events]
            [storefront.fetch :as fetch]
            [clojure.string :as string]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [bidi.bidi :as bidi]
            [clj-http.client :as http]
            [cheshire.core :refer [generate-string]]
            [ring.middleware.defaults :refer :all]
            [ring.util.response :refer [redirect response status content-type header]]
            [noir-exception.core :refer [wrap-internal-error wrap-exceptions]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.codec :as codec]
            [ring-logging.core :as ring-logging]
            [storefront.prerender :refer [wrap-prerender]]
            [hiccup.page :as page]
            [hiccup.element :as element]
            [storefront.assets :refer [asset-path]])
  (:import [java.io ByteArrayInputStream]))

(defn storefront-site-defaults
  [env]
  (if (config/development? env)
    site-defaults
    (-> secure-site-defaults
        (assoc :proxy true)
        (assoc-in [:security :hsts] false)
        (assoc-in [:static :resources] false))))

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

(def mayvenn-logo-splash "<svg version=\"1.1\" id=\"Layer_1\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" x=\"0px\" y=\"0px\" viewBox=\"0 0 158 120.1\" style=\"enable-background:new 0 0 158 120.1;\" xml:space=\"preserve\"> <style type=\"text/css\"> .st0{fill-rule:evenodd;clip-rule:evenodd;stroke:#25211D;stroke-width:0;stroke-linecap:square;stroke-miterlimit:10;} .st1{fill-rule:evenodd;clip-rule:evenodd;fill:#232222;} .st2{fill-rule:evenodd;clip-rule:evenodd;fill:#CBCBCB;} .st3{fill-rule:evenodd;clip-rule:evenodd;fill:#FFFFFF;} .st4{fill-rule:evenodd;clip-rule:evenodd;fill:#232222;stroke:#25211D;stroke-width:0;stroke-linecap:square;stroke-miterlimit:10;} .st5{fill:#FFFFFF;} .st6{fill-rule:evenodd;clip-rule:evenodd;fill:#77C8BD;} .st7{fill:#232222;} .st8{fill-rule:evenodd;clip-rule:evenodd;fill:#EAEAEA;} .st9{fill-rule:evenodd;clip-rule:evenodd;fill:#FFFFFF;stroke:#E5E5E5;stroke-width:0;stroke-linecap:square;stroke-miterlimit:10;} .st10{fill:#6D6C6C;} .st11{fill:#00DDB6;} </style> <g> <path class=\"st7\" d=\"M0,120.1l2.3-16.4h0.3l6.7,13.5l6.6-13.5h0.3l2.4,16.4h-1.6l-1.6-11.7l-5.8,11.7H9.1l-5.9-11.8l-1.6,11.8H0z M145.1,120.1v-16.4h0.3l10.9,12.6v-12.6h1.6v16.4h-0.4l-10.8-12.4v12.4H145.1z M119.5,120.1v-16.4h0.4l10.9,12.6v-12.6h1.6v16.4 H132l-10.8-12.4v12.4H119.5z M97.4,103.7h9.4v1.6H99v5.1h7.7v1.6H99v6.4h7.7v1.6h-9.3V103.7z M73.5,103.7h1.8l5.4,12.7l5.5-12.7H88 l-7.1,16.4h-0.3L73.5,103.7z M52.9,103.7h1.9l4.2,6.8l4.1-6.8H65l-5.2,8.6v7.8h-1.6v-7.8L52.9,103.7z M37.7,103.7l7.7,16.4h-1.8 l-2.6-5.4H34l-2.6,5.4h-1.8l7.8-16.4H37.7z M37.5,107.1l-2.8,5.9h5.6L37.5,107.1z\"/> <path class=\"st6\" d=\"M56.7,57.1c-0.7-8,3.3-14.7,8.4-20c7.5-7.8,17.9-14.9,21.2-20.2C72.2,28.8,50.7,32.1,56.7,57.1L56.7,57.1z M91.1,0c9.7,6.2,9.2,15.9,2.6,24.7c-7.4,9.8-18.4,17.6-24.3,28.6c-6.9,13-7.1,34.3,14,33.9C66.7,87,66,68.3,74.5,54.2 C82.7,40.5,121,15.4,91.1,0L91.1,0z M93,6.5c3.8,21.7-51.8,32.5-29,72.3C50.6,40.5,104.7,27.1,93,6.5L93,6.5z\"/></g></svg>")

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

(defn index [{:keys [store]} storeback-config env]
  (page/html5
   [:head
    [:title "Shop | Mayvenn"]
    [:meta {:name "fragment" :content "!"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, maximum-scale=1.0"}]
    [:meta {:http-equiv "Content-type" :content "text/html;charset=UTF-8"}]

    [:link {:href (asset-path "/images/favicon.png") :rel "shortcut icon" :type "image/vnd.microsoft.icon"}]
    [:script.bugsnag-script-src {:type "text/javascript"
                                 :src "//d2wy8f7a9ursnm.cloudfront.net/bugsnag-2.min.js"
                                 :data-apikey "acbe770e8c0942f8bf97bd9c107483b1"}]
    [:script.bugsnag-script-src {:type "text/javascript"}
     (str "Bugsnag.releaseStage = \"" env "\";"
          "Bugsnag.notifyReleaseStages = ['acceptance', 'production'];")]
    [:script {:type "text/javascript"}
     (str "store = " (generate-string store) ";")]
    (page/include-css (asset-path "/css/all.css"))
    (page/include-css (asset-path "/css/app.css"))]
   [:body
    [:div#content
     [:div {:style "height:100vh;"}
      [:div {:style "margin:auto; width:50%; position: relative; top: 50%; transform: translateY(-50%);"} mayvenn-logo-splash
       [:div {:style (str "height: 2em;"
                          "margin-top: 2em;"
                          "background-image: url('/images/spinner.svg');"
                          "background-position: center center;"
                          "background-repeat: no-repeat;")}]]]]
    (element/javascript-tag (str "var environment=\"" env "\";"
                                 "var canonicalImage=\"" (asset-path "/images/home_image.jpg") "\";"
                                 "var apiUrl=\"" (:endpoint storeback-config) "\";"))
    [:script {:src (asset-path "/js/out/main.js")}]]))

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
        (-> (index req storeback-config environment)
            response
            (content-type "text/html"))))))

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
               (route/not-found "Not found"))
       (wrap-logging logger)
       (wrap-params)
       (#(if (config/development? environment)
           (wrap-exceptions %)
           (wrap-internal-error %
                                :log (comp (partial logger :error) exception-handler)
                                :error-response "{\"error\": \"something went wrong\"}"))))))
