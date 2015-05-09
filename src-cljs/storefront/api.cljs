(ns storefront.api
  (:require [ajax.core :refer [GET POST json-response-format]]
            [cljs.core.async :refer [put!]]
            [storefront.events :as events]))

(def base-url "http://localhost:3005")

(defn api-req [method path params success-handler]
  (method (str base-url path)
          {:handler success-handler
           :headers {"Accepts" "application/json"}
           :format :json
           :params params
           :response-format (json-response-format {:keywords? true})}))

(defn get-taxons [events-ch]
  (api-req
   GET
   "/product-nav-taxonomy"
   {}
   #(put! events-ch [events/api-success-taxons (select-keys % [:taxons])])))

(defn get-store [events-ch store-slug]
  (api-req
   GET
   "/stylist"
   {:store_slug store-slug}
   #(put! events-ch [events/api-success-store %])))

(defn get-products [events-ch taxon-id]
  (api-req
   GET
   "/products"
   {:taxon_id taxon-id}
   #(put! events-ch [events/api-success-products (merge (select-keys % [:products])
                                                        {:taxon-id taxon-id})])))

(defn sign-in [events-ch email password]
  (api-req
   POST
   "/login"
   {:email email
    :password password}
   #(put! events-ch [events/api-success-sign-in (select-keys % [:email :token :store_slug])])))
