(ns storefront.accessors.shipping
  (:require [storefront.accessors.line-items :as line-items]))

(defn longform-timeframe [rate-sku]
  (case rate-sku
    "WAITER-SHIPPING-1" "3-5 business days"
    "WAITER-SHIPPING-2" "1-2 business days (No Weekend & No P.O. Box)"
    "WAITER-SHIPPING-4" "1 business day (No Weekend & No P.O. Box)"
    "WAITER-SHIPPING-7" "2-4 days (Weekend Delivery Included)"
    nil))

(defn timeframe [rate-sku]
  (case rate-sku
    "WAITER-SHIPPING-1" "3-5 business days"
    "WAITER-SHIPPING-2" "1-2 business days"
    "WAITER-SHIPPING-4" "1 business day"
    "WAITER-SHIPPING-7" "2-4 days"
    nil))


(defn priority-shipping-experimental-longform-timeframe [rate-sku]
  (case rate-sku
    "WAITER-SHIPPING-1" "4-6 days (Weekend Delivery Included)"
    "WAITER-SHIPPING-2" "1-2 business days (No Weekend & No P.O. Box)"
    "WAITER-SHIPPING-4" "1 business day (No Weekend & No P.O. Box)"
    "WAITER-SHIPPING-7" "2-4 days (Weekend Delivery Included)"
    nil))

(defn priority-shipping-experimental-timeframe [rate-sku]
  (case rate-sku
    "WAITER-SHIPPING-1" "4-6 days"
    "WAITER-SHIPPING-2" "1-2 business days"
    "WAITER-SHIPPING-4" "1 business day"
    "WAITER-SHIPPING-7" "2-4 days"
    nil))

(defn shipping-details [show-priority-shipping-method? shipment]
  (let [shipping-line-item    (->> shipment
                                   :line-items
                                   (filter line-items/shipping-method?)
                                   first)
        longform-timeframe-fn (if show-priority-shipping-method?
                                priority-shipping-experimental-longform-timeframe
                                longform-timeframe)]
    {:state     (:state shipment)
     :timeframe (-> shipping-line-item :sku longform-timeframe-fn)
     :name      (-> shipping-line-item :product-name)}))
