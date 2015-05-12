(ns storefront.api
  (:require [ajax.core :refer [GET POST json-response-format]]
            [cljs.core.async :refer [put!]]
            [storefront.events :as events]
            [storefront.taxons :refer [taxon-name-from]]))

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

(defn get-products [events-ch taxon-path]
  (api-req
   GET
   "/products"
   {:taxon_name (taxon-name-from taxon-path)}
   #(put! events-ch [events/api-success-products (merge (select-keys % [:products])
                                                        {:taxon-path taxon-path})])))

(defn get-product [events-ch product-path]
  (api-req
   GET
   (str "/products")
   {:slug product-path}
   #(put! events-ch [events/api-success-product {:product-path product-path
                                                 :product %}])))

(defn sign-in [events-ch email password]
  (api-req
   POST
   "/login"
   {:email email
    :password password}
   #(put! events-ch [events/api-success-sign-in (select-keys % [:email :token :store_slug])])))

(defn sign-up [events-ch email password password-confirmation]
  (api-req
   POST
   "/signup"
   {:email email
    :password password
    :password_confirmation password-confirmation}
   #(put! events-ch [events/api-success-sign-up (select-keys % [:email :token :store_slug])])))

(defn forgot-password [events-ch email]
  (api-req
   POST
   "/forgot_password"
   {:email email}
   #(put! events-ch [events/api-success-forgot-password])))
