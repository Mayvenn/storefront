(ns storefront.accessors.shipping
  (:require [storefront.accessors.line-items :as line-items]))

(defn longform-timeframe [rate-sku]
  (case rate-sku
    "WAITER-SHIPPING-1" "4-6 days (Weekends Included)"
    "WAITER-SHIPPING-2" "1-2 business days (No Weekend & No P.O. Box)"
    "WAITER-SHIPPING-4" "1 business day (No Weekend & No P.O. Box)"
    "WAITER-SHIPPING-7" "2-4 days (Weekends Included)"
    nil))

(defn timeframe [rate-sku]
  (case rate-sku
    "WAITER-SHIPPING-1" "4-6 day shipping"
    "WAITER-SHIPPING-2" "1-2 business days"
    "WAITER-SHIPPING-4" "1 business day"
    "WAITER-SHIPPING-7" "2-4 days"
    nil))

(defn shipping-details [shipment]
  (let [shipping-line-item (->> shipment
                                :line-items
                                (filter line-items/shipping-method?)
                                first)]
    {:state     (:state shipment)
     :timeframe (-> shipping-line-item :sku longform-timeframe)
     :name      (-> shipping-line-item :product-name)}))
