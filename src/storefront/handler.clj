(ns storefront.handler
  (:require [storefront.config :as config]
            [storefront.app-routes :refer [app-routes bidi->edn]]
            [storefront.events :as events]
            [storefront.api :as api]
            [storefront.views :as views]
            [storefront.keypaths :as keypaths]
            [storefront.cache :as cache]
            [storefront.cookies :as cookies]
            [storefront.utils.combinators :refer [key-by]]
            [storefront.accessors.taxons :as taxons]
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

(defn store-scheme-and-authority [store-slug environment {:keys [scheme server-name server-port] :as req}]
  (let [dev? (config/development? environment)]
    (str (if dev? (name scheme) "https") "://"
         store-slug "." (parse-root-domain server-name)
         ":" (if dev? server-port 443))))

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
      (h req)

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
      ;; TODO: when the removal of /hello has been deployed in leads, remove it here too
      (redirect (str (store-scheme-and-authority "welcome" environment req) "/hello" (query-string req)))

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
  [routes {:keys [storeback-config environment prerender-token]}]
  (-> routes
      (wrap-prerender (config/development? environment)
                      prerender-token
                      (partial store-url "shop" environment))
      (wrap-set-preferred-store environment)
      (wrap-preferred-store-redirect environment)
      (wrap-stylist-not-found-redirect environment)
      ;; Keep defaults higher up to avoid SSL certificate error. SSL redirect
      ;; must happen *after* www trimming since our SSL certificate doesn't
      ;; support multiple subdomains. Example incorrect behavior would be:
      ;;
      ;; - User goes to http://www.store.mayvenn.com
      ;; - defaults redirects to https://www.store.mayvenn.com
      ;; - browser sees SSL certificate as invalid and refuses to request page
      ;;
      ;; Because of this, we must always leave wrap-defaults above wrap-remove-superfluous-www.
      ;; TODO: needs a test
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
        response
        (content-type "text/html"))))

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
  "Checks that the category exists"
  [{:keys [storeback-config] :as render-ctx} data req {:keys [taxon-slug]}]
  (when-let [products (api/category storeback-config taxon-slug (cookies/get req "user-token"))]
    (html-response render-ctx (-> data
                                  (assoc-in keypaths/products (key-by :id products))
                                  (assoc-in keypaths/browse-taxon-query {:slug taxon-slug})
                                  (assoc-in keypaths/browse-variant-quantity 1)
                                  (assoc-in (conj keypaths/api-cache (taxons/cache-key taxon-slug)) {:products products})))))

(defn create-order-from-shared-cart [{:keys [storeback-config environment]}
                                     {:keys [store] :as req}
                                     {:keys [shared-cart-id]}]
  (let [{stylist-id :stylist_id store-slug :store_slug} store
        {:keys [number token error-code]} (api/create-order-from-cart storeback-config
                                                                      shared-cart-id
                                                                      (cookies/get req "id")
                                                                      (cookies/get req "user-token")
                                                                      stylist-id)]
    (if number
      (-> (redirect-to-cart {:utm_source "sharecart"
                             :utm_content store-slug
                             :message "shared-cart"})
          (cookies/set environment :number number)
          (cookies/set environment :token token)
          cookies/encode)
      (-> (redirect-to-cart {:error "share-cart-failed"})
          (cookies/expire environment :number)
          (cookies/expire environment :token)
          cookies/encode))))

(defn site-routes [{:keys [storeback-config environment] :as ctx}]
  (fn [{:keys [uri store] :as req}]
    (let [{nav-event :handler params :route-params} (bidi/match-route app-routes uri)]
      (when nav-event
        (let [nav-event (bidi->edn nav-event)
              render-ctx {:storeback-config storeback-config
                          :environment environment}
              data (-> {}
                       (assoc-in keypaths/store store)
                       (assoc-in keypaths/taxons (api/taxons storeback-config))
                       (assoc-in keypaths/navigation-message [nav-event params]))]
          (condp = nav-event
            events/navigate-product     (redirect-product->canonical-url ctx req params)
            events/navigate-category    (render-category render-ctx data req params)
            events/navigate-shared-cart (create-order-from-shared-cart ctx req params)
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
