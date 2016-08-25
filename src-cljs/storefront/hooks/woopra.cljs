(ns storefront.hooks.woopra
  (:require [ajax.core :refer [GET POST PUT DELETE json-response-format]]
            [storefront.config :as config]
            [storefront.utils.combinators :refer [filter-nil]]))

(defn- name-capitalization [s]
  (when s
    (if (< (count s) 2)
      (.toUpperCase s)
      (str (.toUpperCase (subs s 0 1))
           (subs s 1)))))

(defn- order->customer [order]
  {:first_name (name-capitalization
                (or (-> order :billing-address :first-name)
                    (-> order :shipping-address :first-name)))
   :last_name  (name-capitalization
                (or (-> order :billing-address :last-name)
                    (-> order :shipping-address :last-name)))})

(defn $ [value]
  (.toFixed (float value) 2))

(defn- order->user-event-data [order]
  {:ce_user_id    (some-> order :user :id)
   :ce_user_email (some-> order :user :email)})

(defn- order->visitor-data [order]
  (let [customer (order->customer order)]
    {:cv_id         (some-> order :user :id)

     :cv_email      (some-> customer :user :email)
     :cv_first_name (-> customer :first_name)
     :cv_last_name  (-> customer :last_name)

     :cv_bill_address_zipcode (some-> order :billing-address :zipcode)
     :cv_bill_address_city    (some-> order :billing-address :city)
     :cv_bill_address_state   (some-> order :billing-address :state)

     :cv_ship_address_zipcode (some-> order :shipping-address :zipcode)
     :cv_ship_address_city    (some-> order :shipping-address :city)
     :cv_ship_address_state   (some-> order :shipping-address :state)}))

(defn track-event [event-name {:keys [variant session-id quantity order] :as args}]
  (GET
   "https://www.woopra.com/track/ce"
   {:params (filter-nil (merge {:cookie                  session-id
                                :event                   event-name
                                :timestamp               (.getTime (js/Date.))
                                :host                    config/woopra-host

                                :ce_order_number         (:number order)
                                :ce_order_total          (-> order :total $)
                                :ce_sku                  (:sku variant)
                                :ce_quantity             quantity
                                :ce_item_price           (:price variant)
                                :ce_line_item_subtotal   ($ (* (:price variant) quantity))
                                :ce_store_id             (-> order :stylist-ids last)}
                               (order->user-event-data order)
                               (order->visitor-data order)))}))
