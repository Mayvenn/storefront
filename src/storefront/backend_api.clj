(ns storefront.backend-api
  (:require [spice.maps :as maps]
            [storefront.accessors.orders :as orders]
            [catalog.skuers :as skuers]
            [tugboat.core :as tugboat]))

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
                   (merge {:socket-timeout 37000 ;; Note that some apis trigger multiple requests
                           :conn-timeout   35000
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

(defn criteria->query-params [criteria]
  (->> criteria
       (map (fn [[k v]]
              [(if-let [ns (namespace k)]
                 (str ns "/" (name k))
                 (name k))
               (if (coll? v)
                 (vec v)
                 v)]))
       (into {})))

(defn fetch-v3-products [storeback-config criteria-or-id]
  (let [response (storeback-fetch storeback-config "/v3/products"
                                  {:query-params (criteria->query-params (if (map? criteria-or-id)
                                                                           criteria-or-id
                                                                           {:catalog/product-id criteria-or-id}))})]
    (when (not-404 response)
      (-> (:body response)
          (update :skus (fn [skus]
                          (into {}
                                (map (fn [[k v]]
                                       [(name k) (skuers/->skuer v)]))
                                skus)))
          (update :images (fn [images]
                            (into {}
                                  (map (fn [[k v]]
                                         [(name k) v]))
                                  images)))))))

(defn fetch-v2-skus [storeback-config criteria-or-id]
  (let [response (storeback-fetch storeback-config "/v2/skus"
                                  {:query-params (criteria->query-params criteria-or-id)})]
    (when (not-404 response)
      (:body response))))

(defn fetch-v2-facets [storeback-config]
  (let [response (storeback-fetch storeback-config "/v2/facets" {})]
    (when (not-404 response)
      (:body response))))

(defn get-order [storeback-config order-number order-token]
  (when (and order-number order-token)
    (let [response (storeback-fetch storeback-config (str "/v2/orders/" order-number)
                                    {:query-params {:token order-token}})]
      (when (not-404 response)
        (orders/TEMP-pretend-service-items-do-not-exist (:body response))))))

(defn get-servicing-stylist [storeback-config servicing-stylist-id]
  (when servicing-stylist-id
    (let [response (storeback-fetch storeback-config
                                    "/v1/stylist/matched-by-id"
                                    {:query-params {:stylist-id servicing-stylist-id}})]
      (when (not-404 response)
        (-> response :body :stylist)))))

;; todo clean up
(defn select-user-keys [user]
  (-> user
      maps/kebabify
      (select-keys [:email
                    :token
                    :store-slug
                    :id
                    :is-new-user
                    :store-id
                    :stylist-experience])))

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

(defn create-order-from-shared-cart [storeback-config session-id shared-cart-id user-id user-token stylist-id]
  (let [{:keys [status body] :as r} (storeback-post storeback-config
                                                    "/create-order-from-shared-cart"
                                                    {:form-params {:session-id     session-id
                                                                   :shared-cart-id shared-cart-id
                                                                   :user-id        user-id
                                                                   :user-token     user-token
                                                                   :stylist-id     stylist-id}})]
    (when (<= 200 status 299)
      body)))

(defn get-promotions [storeback-config promo-code]
  (let [{:keys [status body]} (storeback-fetch storeback-config
                                               "/promotions"
                                               {:query-params {:additional-promo-code promo-code}})]
    (when (<= 200 status 299)
      (:promotions body))))

(defn fetch-shared-cart [storeback-config shared-cart-id]
  (let [{:keys [status body]} (storeback-fetch storeback-config
                                               "/fetch-shared-cart"
                                               {:query-params {:shared-cart-id shared-cart-id}})]
    (when (<= 200 status 299)
      body)))
