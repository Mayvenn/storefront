(ns storefront.backend-api
  (:require [tugboat.core :as tugboat]))

(defn storeback-fetch [storeback-config path params]
  (tugboat/request {:endpoint (:internal-endpoint storeback-config)}
                   :get path
                   (merge
                    {:socket-timeout 30000
                     :conn-timeout   30000
                     :as             :json}
                    params)))

(defn storeback-post [storeback-config path params]
  (tugboat/request {:endpoint (:internal-endpoint storeback-config)}
                   :post path
                   (merge {:socket-timeout 30000
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
          (:body resp)))
      (catch java.io.IOException e
        ::storeback-unavailable))))

(def not-404 (comp (partial not= 404) :status))

(defn named-searches [storeback-config]
  (let [response (storeback-fetch storeback-config "/named-searches" {})]
    (when (not-404 response)
      (:searches (:body response)))))

(defn products-by-ids [storeback-config product-ids user-id user-token]
  (let [response (storeback-fetch storeback-config "/products"
                                  {:query-params {:ids (sort (distinct product-ids))
                                                  :user-id user-id
                                                  :user-token user-token}})]
    (when (not-404 response)
      (:products (:body response)))))

(defn product [storeback-config product-slug user-id user-token]
  (let [response (storeback-fetch storeback-config "/products"
                                  {:query-params {:slug product-slug
                                                  :user-id user-id
                                                  :user-token user-token}})]
    (when (not-404 response)
      (:body response))))

(defn verify-paypal-payment [storeback-config number order-token ip-addr {:strs [sid utm-params]}]
  (let [{:keys [status body]} (storeback-post storeback-config "/v2/place-order"
                                              {:form-params {:number number
                                                             :token order-token
                                                             :session-id sid
                                                             :utm-params utm-params}
                                               :headers {"X-Forwarded-For" ip-addr}})]
    (when-not (<= 200 status 299)
      (-> body :error-code (or "paypal-incomplete")))))

(defn fetch-facets [cache]
  {})

(defn search-sku-sets [criteria success-handler]
  {})

(defn get-sku-set [sku-set-id] {})
