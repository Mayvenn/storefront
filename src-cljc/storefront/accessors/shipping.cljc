(ns storefront.accessors.shipping
  (:require [storefront.accessors.line-items :as line-items]
            [api.catalog :refer [select]]))


(defn longform-timeframe [rate-sku drop-shipping?]
  (case rate-sku
    "WAITER-SHIPPING-1" (if drop-shipping?
                          "7-10 days (Weekend Delivery Included)"
                          "4-6 days (Weekend Delivery Included)")
    "WAITER-SHIPPING-7" "2-4 days (Weekend Delivery Included)"
    "WAITER-SHIPPING-2" "1-2 business days (No Weekend & No P.O. Box)"
    "WAITER-SHIPPING-4" "1 business day (No Weekend & No P.O. Box)"
    "WAITER-SHIPPING-8" "In Store Pick-Up"
    nil))

(defn names-with-time-range [rate-sku-id drop-shipping?]
  (case rate-sku-id
    "WAITER-SHIPPING-1" (if drop-shipping?
                          "Free 7-10 Days Standard Shipping"
                          "Free 4-6 Days Standard Shipping")
    "WAITER-SHIPPING-7" "Priority 2-4 Days Shipping"
    "WAITER-SHIPPING-2" "Express 1-2 Days Shipping"
    "WAITER-SHIPPING-4" "Rush Shipping"
    "WAITER-SHIPPING-8" "In Store Pick-Up"
    nil))

(defn shipping-note [rate-sku-id]
  (case rate-sku-id
    "WAITER-SHIPPING-2" "(No P.O. Box)"
    "WAITER-SHIPPING-4" "(No P.O. Box)"
    nil) )

(defn timeframe [rate-sku drop-shipping?]
  (case rate-sku
    "WAITER-SHIPPING-1" (if drop-shipping?
                          "7-10 days"
                          "4-6 days")
    "WAITER-SHIPPING-7" "2-4 days"
    "WAITER-SHIPPING-2" "1-2 business days"
    "WAITER-SHIPPING-4" "1 business day"
    "WAITER-SHIPPING-8" "In Store Pick-Up"
    nil))

(defn shipping-details [shipment]
  (let [shipping-line-item    (->> shipment
                                   :line-items
                                   (filter line-items/shipping-method?)
                                   first)
        longform-timeframe-fn longform-timeframe
        drop-shipping? (->> (map :variant-attrs (:line-items shipment))
                            (select {:warehouse/slug #{"factory-cn"}})
                            boolean)]
    (if shipping-line-item
      {:state     (:state shipment)
       :timeframe (-> shipping-line-item :sku (longform-timeframe-fn drop-shipping?))
       :name      (-> shipping-line-item :product-name)}
      {:state     "Shipped"
       :name      "In Store Pickup"})))
