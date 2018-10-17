(ns storefront.accessors.shipping
  (:require [clojure.string :as string]
            [spice.date :as date]))

(defn timeframe [rate-sku]
  (case rate-sku
    "WAITER-SHIPPING-1" "3-5 business days"
    "WAITER-SHIPPING-2" "1-2 business days (No Weekend & No P.O. Box)"
    "WAITER-SHIPPING-4" "1 business day (No Weekend & No P.O. Box)"
    nil))

(defn v2-name [rate-sku]
  (case rate-sku
    "WAITER-SHIPPING-1" "3-5 Day Shipping"
    "WAITER-SHIPPING-2" "1-2 Day Shipping"
    "WAITER-SHIPPING-4" "1 Day Shipping"))

(defn enrich-shipping-method
  [shipping-method]
  "Takes shipping methods and merges the results of shipping-data back onto them
  Example:

  {:name \"Free Standard Shipping\"
   :id 1
   :price 0
   :sku \"WAITER-SHIPPING-1\"}

  =>

  {:name \"Free Standard Shipping\"
   :id 1
   :price 0
   :sku \"WAITER-SHIPPING-1\"
   :business-days [3 5]
   :copy/sub \"3-5 business\"}"
  (merge shipping-method
         (case (:sku shipping-method)
           "WAITER-SHIPPING-1" {:business-days [3 5]
                                :short-name "Standard Shipping"
                                :copy/sub (v2-name (:sku shipping-method))}
           "WAITER-SHIPPING-2" {:business-days [1 2]
                                :short-name "Express Shipping"
                                :copy/sub (v2-name (:sku shipping-method))}
           "WAITER-SHIPPING-4" {:business-days [1]
                                :short-name "Overnight Shipping"
                                :copy/sub (v2-name (:sku shipping-method))}
           nil)))

(defn shipping-details [shipment]
  (let [shipping-line-item (->> shipment
                                :line-items
                                (filter (comp some? timeframe :sku))
                                first)]
    {:state     (:state shipment)
     :timeframe (-> shipping-line-item :sku timeframe)
     :name      (-> shipping-line-item :sku v2-name)}))
