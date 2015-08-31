(ns storefront.accessors.orders)

(defn active-payments [order]
  (filter #(not= (:state %) "invalid")
          (:payments order)))

(defn using-store-credit? [order]
  (some #(= (:source_type %) "Spree::StoreCredit")
        (active-payments order)))

(defn partially-covered-by-store-credit? [order]
  (not= (js/parseFloat (:order_total_after_store_credit order))
        0))

(defn incomplete? [order]
  (-> order :state #{"cart"} boolean))

(defn line-items
  "Returns line items from an order hashmap.
  Storefront should only be concerned about items in the last shipment.
  Line-items are from last shipment as it is the user created shipment."
  [order]
  (when order
    (->> order
         :shipments
         (last)
         :line-items)))

(defn product-items
  "Returns cart items from an order hashmap.
  Excludes shipping and items added by El Jefe.
  Cart line-items are added as the last shipment.
  Line-items are from last shipment as it is the user created shipment."
  [order]
  (->> (line-items order)
       (filter #(not= (:source (last %)) "waiter"))
       (into {})))

(defn shipping-item
  "Returns the first shipping line-item from an order hashmap.
  Includes only items added by waiter.
  Shipping items are added as the last shipment.
  Line-items are from last shipment as it is the user created shipment."
  [order]
  (->> (line-items order)
       (vals)
       (filter #(= (:source %) "waiter"))
       (first)))

(defn form-payment-methods [order-total store-credit use-store-credit]
  (let [store-credit-used (if use-store-credit (min order-total store-credit) 0)]
    (merge {}
           (when use-store-credit
             {:store-credit {:amount store-credit-used}})
           (when (> order-total store-credit-used)
             {:stripe {:amount (- order-total store-credit-used)}}))))

(defn line-item-subtotal [{:keys [quantity unit-price]}]
  (* quantity unit-price))

(defn product-quantity [order]
  (reduce + 0 (map (comp :quantity last) (product-items order))))


