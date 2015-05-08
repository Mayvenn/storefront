(ns storefront.api
  (:require [ajax.core :refer [GET json-response-format]]
            [cljs.core.async :refer [put!]]
            [storefront.events :as events]))

(def base-url "http://localhost:3005")

(defn api-req [path params success-handler]
  (GET (str base-url path)
      {:handler success-handler
       :headers {"Accepts" "application/json"
                 "Content-Type" "application/json"}
       :params params
       :response-format (json-response-format {:keywords? true})}))

(defn get-taxons [events-ch]
  (api-req
   "/product-nav-taxonomy"
   {}
   #(put! events-ch [events/api-success-taxons (select-keys % [:taxons])])))

(defn get-store [events-ch store-slug]
  (api-req
   "/stylist"
   {:store_slug store-slug}
   #(put! events-ch [events/api-success-store %])))

(defn get-products [events-ch taxon-id]
  (api-req
   "/products"
   {:taxon_id taxon-id}
   #(put! events-ch [events/api-success-products (merge (select-keys % [:products])
                                                        {:taxon-id taxon-id})])))
