(ns storefront.api
  (:require [clj-http.client :as http]))

(defn storeback-fetch [storeback-config path params]
  (http/get (str (:internal-endpoint storeback-config) path)
            (merge
             {:throw-exceptions false
              :socket-timeout 30000
              :conn-timeout 30000
              :as :json}
             params)))

(defn storeback-post [storeback-config path params]
  (http/post (str (:internal-endpoint storeback-config) path)
             (merge {:content-type :json
                     :throw-exceptions false
                     :socket-timeout 30000
                     :conn-timeout 30000
                     :as :json
                     :coerce :always}
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

(defn products-by-ids [storeback-config product-ids user-token]
  (let [response (storeback-fetch storeback-config "/products"
                                  {:query-params {:ids (sort (distinct product-ids))
                                                  :user-token user-token}})]
    (when (not-404 response)
      (:products (:body response)))))

(defn product [storeback-config product-slug user-token]
  (let [response (storeback-fetch storeback-config "/products"
                                  {:query-params {:slug product-slug
                                                  :user-token user-token}})]
    (when (not-404 response)
      (:body response))))

(defn verify-paypal-payment [storeback-config number order-token ip-addr {:strs [sid utm-params]}]
  (let [{:keys [status body]} (storeback-post storeback-config "/v2/place-order"
                                              {:form-params {:number number
                                                             :token order-token
                                                             :session-id sid
                                                             :utm-params utm-params}
                                               :socket-timeout 30000
                                               :conn-timeout 30000
                                               :headers {"X-Forwarded-For" ip-addr}})]
    (when-not (<= 200 status 299)
      (-> body :error-code (or "paypal-incomplete")))))
