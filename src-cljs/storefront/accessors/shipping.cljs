(ns storefront.accessors.shipping
  (:require [storefront.hooks.experiments :as experiments]))

(defn display-shipping-method [data rate-name]
  (case rate-name
    "Priority Shipping" "Free Standard Shipping"
    rate-name))

(defn timeframe [rate-name]
  (case rate-name
    "Priority Shipping" "3-5 business days"
    "Express Shipping" "1-2 business days (No Weekends)"
    "Overnight Shipping" "1 business day (No Weekends)"
    ""))
