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
      (->
       (storeback-fetch storeback-config "/store" {:store_slug store-slug})
       :body)
      (catch java.io.IOException e
        ::storeback-unavailable))))

(defn category [storeback-config taxon-slug user-token]
  (storeback-fetch storeback-config "/products" {:taxon-slug taxon-slug
                                                 :user-token user-token}))

(defn product [storeback-config product-slug user-token]
  (storeback-fetch storeback-config "/products" {:slug product-slug :user-token user-token}))
