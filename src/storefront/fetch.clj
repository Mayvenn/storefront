(ns storefront.fetch
  (:require [clj-http.client :as http]))

(defn storeback-fetch [storeback-config path params]
  (http/get (str (:endpoint storeback-config) path)
            {:query-params params
             :throw-exceptions false
             :socket-timeout 10000
             :conn-timeout 10000
             :as :json}))

(defn store [storeback-config store-slug]
  (when (seq store-slug)
    (try
      (let [resp (storeback-fetch storeback-config "/store" {:store_slug store-slug})]
        (if (#{500} (:status resp))
          ::storeback-unavailable
          (:body resp)))
      (catch java.io.IOException e
        ::storeback-unavailable))))

(def not-404 (comp (partial not= 404) :status))

;; TODO Fetch the taxon not the products for the taxon (not currently possible in storeback)
(defn category [storeback-config taxon-slug user-token]
  (let [response (storeback-fetch storeback-config "/products" {:taxon-slug taxon-slug
                                                                :user-token user-token})]
    (when (not-404 response)
      (:products (:body response)))))

(defn product [storeback-config product-slug user-token]
  (let [response (storeback-fetch storeback-config "/products" {:slug product-slug
                                                                :user-token user-token})]
    (when (not-404 response)
      (:body response))))
