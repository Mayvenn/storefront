;; TODO: rename me to api
(ns storefront.fetch
  (:require [clj-http.client :as http]))

(defn storeback-fetch [storeback-config path params]
  (http/get (str (:endpoint storeback-config) path)
            (merge
             {:throw-exceptions false
              :socket-timeout 10000
              :conn-timeout 10000
              :as :json}
             params)))

(defn storeback-post [storeback-config path params]
  (http/post (str (:endpoint storeback-config) path)
             (merge {:content-type :json
                     :throw-exceptions false
                     :socket-timeout 10000
                     :conn-timeout 10000
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

;; TODO Fetch the taxon not the products for the taxon (not currently possible in storeback)
(defn category [storeback-config taxon-slug user-token]
  (let [response (storeback-fetch storeback-config "/products"
                                  {:query-params {:taxon-slug taxon-slug
                                                  :user-token user-token}})]
    (when (not-404 response)
      (:products (:body response)))))

(defn product [storeback-config product-slug user-token]
  (let [response (storeback-fetch storeback-config "/products"
                                  {:query-params {:slug product-slug
                                                  :user-token user-token}})]
    (when (not-404 response)
      (:body response))))

(defn create-order-from-cart [storeback-config shared-cart-id user-id user-token stylist-id]
  (let [{:keys [body]} (storeback-post storeback-config "/create-order-from-shared-cart"
                                       {:form-params {:shared-cart-id shared-cart-id
                                                      :user-id        user-id
                                                      :user-token     user-token
                                                      :stylist-id     stylist-id}})]
    body))

(defn verify-paypal-payment [storeback-config number order-token ip-addr {:strs [sid]}]
  (let [{:keys [status body]} (storeback-post storeback-config "/v2/place-order"
                                              {:form-params {:number number
                                                             :token order-token
                                                             :session-id sid}
                                               :headers {"X-Forwarded-For" ip-addr}})]
    (when-not (<= 200 status 299)
      (-> body :error-code (or "paypal-incomplete")))))
