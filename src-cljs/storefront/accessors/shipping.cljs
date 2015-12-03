(ns storefront.accessors.shipping
  (:require [storefront.hooks.experiments :as experiments]))

(defn timeframe [rate-sku]
  (case rate-sku
    "WAITER-SHIPPING-1" "3-5 business days"
    "WAITER-SHIPPING-2" "1-2 business days (No Weekends)"
    "WAITER-SHIPPING-4" "1 business day (No Weekends)"
    ""))
