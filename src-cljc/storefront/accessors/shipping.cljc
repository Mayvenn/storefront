(ns storefront.accessors.shipping
  (:require [clojure.string :as string]
            [spice.date :as date]))

(defn timeframe [rate-sku]
  (case rate-sku
    "WAITER-SHIPPING-1" "3-5 business days"
    "WAITER-SHIPPING-2" "1-2 business days (No Weekend & No P.O. Box)"
    "WAITER-SHIPPING-4" "1 business day (No Weekend & No P.O. Box)"
    nil))

(defn shipping-details [shipment]
  (let [shipping-line-item (->> shipment
                                :line-items
                                (filter (comp some? timeframe :sku))
                                first)]
    {:state     (:state shipment)
     :timeframe (-> shipping-line-item :sku timeframe)
     :name      (-> shipping-line-item :sku v2-name)}))
