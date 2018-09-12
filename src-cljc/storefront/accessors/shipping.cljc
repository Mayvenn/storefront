(ns storefront.accessors.shipping)

(defn timeframe [rate-sku]
  (case rate-sku
    "WAITER-SHIPPING-1" "3-5 business days"
    "WAITER-SHIPPING-2" "1-2 business days (No Weekend & No P.O. Box)"
    "WAITER-SHIPPING-4" "1 business day (No Weekend & No P.O. Box)"
    nil))
