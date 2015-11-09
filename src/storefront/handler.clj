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
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring-logging.core :refer [make-logger-middleware]]
            [storefront.prerender :refer [wrap-prerender]]
            [hiccup.page :as page]
            [hiccup.element :as element]
            [storefront.assets :refer [asset-path]])
  (:import [java.util Date Locale TimeZone Calendar GregorianCalendar]
           [java.text SimpleDateFormat]
           [java.io ByteArrayInputStream]))

(defn storefront-site-defaults
  [env]
  (if (config/development? env)
    site-defaults
    (-> secure-site-defaults
        (assoc :proxy true)
        (assoc-in [:security :hsts] false)
        (assoc-in [:static :resources] false))))

(defn fetch-store [storeback-config store-slug]
  (when (seq store-slug)
    (try
      (->
       (http/get (str (:endpoint storeback-config) "/store")
                 {:query-params {:store_slug store-slug}
                  :throw-exceptions false
                  :socket-timeout 10000
                  :conn-timeout 10000
                  :as :json})
       :body)
      (catch java.io.IOException e
        ::storeback-unavailable))))

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

(defn wrap-redirect [h storeback-config]
  (fn [req]
    (let [subdomains (parse-subdomains (:server-name req))
          subdomain (first subdomains)
          domain (str (parse-tld (:server-name req)) ":"
                      (:server-port req))
          store (fetch-store storeback-config (last subdomains))]
      (cond
        (= "vistaprint" subdomain)
        (redirect "http://www.vistaprint.com/vp/gateway.aspx?sr=no&s=6797900262")

        (#{[] ["www"]} subdomains)
        (redirect (str "http://welcome." domain "/hello" (query-string req)))

        (= "www" subdomain)
        (redirect (str "http://" (:store_slug store) "." domain (query-string req)))

        (= store ::storeback-unavailable) (h req)
        (:store_slug store) (h req)
        :else (redirect (str "http://store." domain (query-string req)))))))

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

(defn index [storeback-config env]
  (page/html5
   [:head
    [:meta {:name "p:domain_verify" :content "40c4b6d92049896f0171e23aecd881df"}]
    [:meta {:name "fragment" :content "!"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, maximum-scale=1.0"}]

    [:link {:href (asset-path "/images/favicon.png") :rel "shortcut icon" :type "image/vnd.microsoft.icon"}]
    [:script.honeybadger-script-src {:type "text/javascript"
                                     :src "//js.honeybadger.io/v0.3/honeybadger.min.js"}]
    [:script.honeybadger-script-src {:type "text/javascript"}
     (str "Honeybadger.configure({api_key: 'b0a4a070', environment: '" env "'});")]
    (page/include-css (asset-path "/css/all.css"))]
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

(defn- ^SimpleDateFormat make-http-format
  "Formats or parses dates into HTTP date format (RFC 822/1123).
  From ring"
  []
  ;; SimpleDateFormat is not threadsafe, so return a new instance each time
  (doto (SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:ss ZZZ" Locale/US)
    (.setTimeZone (TimeZone/getTimeZone "UTC"))))

(defn years-from-now []
  (.getTime (doto (GregorianCalendar.)
              (.add Calendar/YEAR 10))))

(defn wrap-cdn [f]
  (fn [req]
    (let [resp (f req)]
      (if (.startsWith (:uri req) "/cdn/")
        (-> resp
            (header "Content-Encoding" "gzip")
            (header "Access-Control-Allow-Origin" "*")
            (header "Access-Control-Allow-Methods" "GET")
            (header "Cache-Control" (str "max-age=" (* 10 365 24 60 60)))
            (header "Expires" (.format (make-http-format) (years-from-now))))
        resp))))

(defn request-scheme [req]
  (if-let [forwarded-proto (get-in req [:headers "x-forwarded-proto"])]
    (keyword forwarded-proto)
    (:scheme req)))

(defn prerender-original-request-url [development? req]
  (str (name (request-scheme req)) "://shop."
       (parse-tld (:server-name req))
       ":" (if development? (:server-port req) 443) (:uri req)))

(defn site-routes
  [logger storeback-config environment prerender-token]
  (->
   (routes
    (GET "*" req (->
                  (index storeback-config environment)
                  response
                  (content-type "text/html"))))
   (wrap-prerender (config/development? environment)
                   prerender-token
                   (partial prerender-original-request-url
                            (config/development? environment)))
   (make-logger-middleware logger)
   (wrap-defaults (storefront-site-defaults environment))
   (wrap-redirect storeback-config)
   (wrap-resource "public")
   (wrap-content-type)
   (wrap-cdn)))

(defn proxy-spree-images [env]
  (GET "/spree/*" {params :params :as req}
    (when (config/development? env)
      (let [response (http/get (str "http://localhost:3000/spree/" (:* params))
                               {:throw-exceptions false
                                :query-params params
                                :as :byte-array})]
        {:status (:status response)
         :headers (select-keys (:headers response) ["Content-Type"])
         :body (java.io.ByteArrayInputStream. (:body response))}))))

(defn create-handler
  ([] (create-handler {}))
  ([{:keys [logger exception-handler storeback-config environment prerender-token]}]
   (-> (routes (GET "/healthcheck" [] "cool beans")
               (GET "/robots.txt" req (content-type (response (robots req))
                                                    "text/plain"))
               (proxy-spree-images environment)
               (site-routes logger storeback-config environment prerender-token)
               (route/not-found "Not found"))
       (#(if (config/development? environment)
           (wrap-exceptions %)
           (wrap-internal-error %
                                :log (comp (partial logger :error) exception-handler)
                                :error-response "{\"error\": \"something went wrong\"}"))))))
