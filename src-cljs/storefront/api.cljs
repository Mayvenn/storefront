(ns storefront.api
  (:require [ajax.core :refer [GET POST json-response-format]]
            [cljs.core.async :refer [put! take! chan]]
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

(defn select-sign-in-keys [args]
  (select-keys args [:email :token :store_slug]))

(defn sign-in [events-ch email password]
  (api-req
   POST
   "/login"
   {:email email
    :password password}
   #(put! events-ch [events/api-success-sign-in (select-sign-in-keys %)])))

(defn sign-up [events-ch email password password-confirmation]
  (api-req
   POST
   "/signup"
   {:email email
    :password password
    :password_confirmation password-confirmation}
   #(put! events-ch [events/api-success-sign-up (select-sign-in-keys %)])))

(defn forgot-password [events-ch email]
  (api-req
   POST
   "/forgot_password"
   {:email email}
   #(put! events-ch [events/api-success-forgot-password])))

(defn reset-password [events-ch password password-confirmation reset-token]
  (api-req
   POST
   "/reset_password"
   {:password password
    :password_confirmation password-confirmation
    :reset_password_token reset-token}
   #(put! events-ch [events/api-success-reset-password (select-sign-in-keys %)])))

(defn get-stylist-commissions [events-ch user-token]
  (api-req
   GET
   "/stylist/commissions"
   {:user-token user-token}
   #(put! events-ch [events/api-success-stylist-commissions %])))

(defn create-order [events-ch user-token]
  (api-req
   POST
   "/orders"
   (if user-token {:token user-token} {})
   #(put! events-ch [events/api-success-create-order (select-keys % [:number :token])])))

(defn react [src-ch dest-ch f]
  (let [response-ch (chan)]
    (take! src-ch
           #(do
              (when (first %) (put! dest-ch %))
              (put! response-ch %)
              (f %)))
    response-ch))

(defn create-order-if-necessary [order-id order-token user-token]
  (let [create-order-ch (chan)]
    (if (and order-token order-id)
      (put! create-order-ch [nil {:number order-id :token order-token}])
      (create-order create-order-ch user-token))
    create-order-ch))

(defn add-to-bag [events-ch variant-id variant-quantity order-token order-id]
  (-> (create-order-if-necessary order-id order-token nil)
      (react events-ch
             (fn [[_ {order-number :number order-token :token}]]
               (println _)
               (api-req
                POST
                "/line-items"
                {:token order-token
                 :order_number order-number
                 :variant_id variant-id
                 :variant_quantity variant-quantity}
                #(put! events-ch [events/api-success-add-to-bag %]))))
      (react events-ch
             (fn [[_ {order-number :number order-token :token}]]
               (api-req
                GET
                "/orders"
                {:id order-number
                 :token order-token}
                #(put! events-ch [events/api-success-fetch-order %]))))))
