(ns storefront.accessors.shipping
  (:require [storefront.accessors.line-items :as line-items]))


(defn longform-timeframe [rate-sku]
  (case rate-sku
    "WAITER-SHIPPING-1" "4-6 days (Weekend Delivery Included)"
    "WAITER-SHIPPING-7" "2-4 days (Weekend Delivery Included)"
    "WAITER-SHIPPING-2" "1-2 business days (No Weekend & No P.O. Box)"
    "WAITER-SHIPPING-4" "1 business day (No Weekend & No P.O. Box)"
    nil))

(defn names-with-time-range [rate-sku-id]
  (case rate-sku-id
    "WAITER-SHIPPING-1" "Free 4-6 Days Standard Shipping"
    "WAITER-SHIPPING-7" "Priority 2-4 Days Shipping"
    "WAITER-SHIPPING-2" "Express 1-2 Days Shipping"
    "WAITER-SHIPPING-4" "Overnight Shipping"
    nil))

(defn shipping-note [rate-sku-id]
  (case rate-sku-id
    "WAITER-SHIPPING-2" "(No P.O. Box)"
    "WAITER-SHIPPING-4" "(No P.O. Box)"
    nil) )

(defn timeframe [rate-sku]
  (case rate-sku
    "WAITER-SHIPPING-1" "4-6 days"
    "WAITER-SHIPPING-7" "2-4 days"
    "WAITER-SHIPPING-2" "1-2 business days"
    "WAITER-SHIPPING-4" "1 business day"
    nil))

(defn shipping-details [shipment]
  (let [shipping-line-item    (->> shipment
                                   :line-items
                                   (filter line-items/shipping-method?)
                                   first)
        longform-timeframe-fn longform-timeframe]
    {:state     (:state shipment)
     :timeframe (-> shipping-line-item :sku longform-timeframe-fn)
     :name      (-> shipping-line-item :product-name)}))
