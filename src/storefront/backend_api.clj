(ns storefront.backend-api
  (:require [tugboat.core :as tugboat]
            [spice.maps :as maps]
            [storefront.config :as config]
            [spice.date :as date]))

(defn storeback-fetch [storeback-config path params]
  (tugboat/request {:endpoint (:internal-endpoint storeback-config)}
                   :get path
                   (merge
                    {:socket-timeout 30000
                     :conn-timeout   30000
                     :as             :json}
                    params)))

(def place-order-timeout 50000)

(defn storeback-post [storeback-config path params]
  (tugboat/request {:endpoint (:internal-endpoint storeback-config)}
                   :post path
                   (merge {:socket-timeout 30000 ;; Note that some apis trigger multiple requests
                           :conn-timeout   30000
                           :content-type   :json
                           :as             :json
                           :coerce         :always}
                          params)))

(defn store [storeback-config store-slug]
  (when (seq store-slug)
    (try
      (let [resp (storeback-fetch storeback-config "/store"
                                  {:query-params {:store_slug store-slug}})]
        (if (#{500} (:status resp))
          ::storeback-unavailable
          (maps/kebabify (:body resp))))
      (catch java.io.IOException e
        ::storeback-unavailable))))

(def not-404 (comp (partial not= 404) :status))

(defn verify-paypal-payment [storeback-config number order-token ip-addr {:strs [sid utm-params]}]
  (let [{:keys [status body]} (storeback-post storeback-config "/v2/paypal/place-order"
                                              {:form-params {:number number
                                                             :token order-token
                                                             :session-id sid
                                                             :utm-params utm-params}
                                               :socket-timeout place-order-timeout
                                               :conn-timeout   place-order-timeout
                                               :headers {"X-Forwarded-For" ip-addr}})]
    (when-not (<= 200 status 299)
      (-> body :error-code (or "paypal-incomplete")))))

(defn verify-affirm-payment [storeback-config number order-token checkout-token ip-addr {:strs [session-id utm-params]}]
  (let [{:keys [status body] :as r} (storeback-post storeback-config "/v2/affirm/place-order"
                                                    {:form-params    {:number         number
                                                                      :token          order-token
                                                                      :checkout-token checkout-token
                                                                      :session-id     session-id
                                                                      :utm-params     utm-params}
                                                     :socket-timeout place-order-timeout
                                                     :conn-timeout   place-order-timeout
                                                     :headers        {"X-Forwarded-For" ip-addr}})]
    (when-not (<= 200 status 299)
      (let [first-error-code (->> body :errors (some identity) :error-code)]
        (or first-error-code "affirm-incomplete")))))

(defn criteria->query-params [criteria]
  (->> criteria
       (map (fn [[k v]]
              [(if-let [ns (namespace k)]
                 (str ns "/" (name k))
                 (name k))
               v]))
       (into {})))

(defn fetch-v2-products [storeback-config criteria-or-id]
  (let [response (storeback-fetch storeback-config "/v2/products"
                                  {:query-params (if (map? criteria-or-id)
                                                   (criteria->query-params criteria-or-id)
                                                   {:catalog/product-id criteria-or-id})})]
    (when (not-404 response)
      (:body response))))

(defn fetch-v2-skus [storeback-config criteria-or-id]
  (let [response (storeback-fetch storeback-config "/v2/skus"
                                  {:query-params (criteria->query-params criteria-or-id)})]
    (when (not-404 response)
      (:body response))))

(defn fetch-v2-facets [storeback-config]
  (let [response (storeback-fetch storeback-config "/v2/facets" {})]
    (when (not-404 response)
      (:body response))))

(defn lookup-lead [storeback-config lead-id]
  (when lead-id
    (let [response (storeback-fetch storeback-config (str "/leads/" lead-id) {})]
      (when (not-404 response)
        (:lead (:body response))))))

(defn get-order [storeback-config order-number order-token]
  (when (and order-number order-token)
    (let [response (storeback-fetch storeback-config (str "/v2/orders/" order-number)
                                    {:query-params {:token order-token}})]
      (when (not-404 response)
        (:body response)))))


;; todo clean up
(defn select-user-keys [user]
  (-> user
      maps/kebabify
      (select-keys [:email :token :store-slug :id :is-new-user])))

(defn select-auth-keys [args]
  (-> args
      (update :user select-user-keys)))

(defn one-time-login-in [storeback-config user-id token stylist-id]
  (when (and user-id token)
    (let [{:keys [body status] :as response} (storeback-post storeback-config "/v2/one-time-login"
                                                             {:form-params {:user-id    user-id
                                                                            :user-token token
                                                                            :stylist-id stylist-id}})]
      (when (<= 200 status 299)
        (-> body
            select-auth-keys
            (assoc :flow "one-time-login"))))))

(defn contentful-request [{:keys [endpoint space-id api-key] :as contentful}]
  (tugboat/request {:endpoint endpoint }
                   :get (str "/spaces/" space-id "/entries")
                   (merge
                    {:socket-timeout 30000
                     :conn-timeout   30000
                     :as             :json
                     :query-params   {:access_token api-key}})) )

(def entry->content-type-id (comp :id :sys :contentType :sys))

(defn entry->fields [entry]
  (let [fields (:fields entry)]
    [(entry->content-type-id entry)
     (merge fields
            {:updated-at (-> entry :sys :updatedAt)
             :entry-id   (-> entry :sys :id)})]))

;; Rule is to use the most recently updated 'published' entry of a specific type
(defn tx-contentful-body [body]
  (let [contentful-key->data (->> (:items body)
                                  (sort-by (comp :updatedAt :sys))
                                  reverse
                                  (partition-by entry->content-type-id)
                                  (map first)
                                  (map entry->fields)
                                  (into {}))
        homepage-hero-data   (get contentful-key->data "homepage")]
    {:hero {:desktop-uuid (:heroImageDesktopUuid homepage-hero-data)
            :mobile-uuid  (:heroImageMobileUuid homepage-hero-data)
            :file-name    (:heroImageFileName homepage-hero-data)
            :alt-text     (:heroImageAltText homepage-hero-data)}}))

(defonce contentful-cache
  (atom {:transformed-response nil
         :timestamp            (date/now)}))

(defn contentful-fetch [{:keys [cache] :as contentful}]
  (let [current-time (date/now)
        expired?     (date/after? current-time
                                  (date/add-delta (:timestamp @cache)
                                                  {:minutes 5}))]
    (when expired?
      (let [{:keys [status body]} (contentful-request contentful)]
        (when (<= 200 status 299) ;; What if this fails?
          (reset! cache {:transformed-response (tx-contentful-body body)
                         :timestamp            current-time}))))
    @cache))

