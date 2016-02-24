(ns storefront.accessors.orders)

(defn incomplete? [order]
  (-> order :state #{"cart"} boolean))

(defn line-items
  "Returns line items from an order hashmap.
  Storefront should only be concerned about items in the first shipment.
  Line-items are from first shipment as it is the user created shipment."
  [order]
  (when order
    (->> order
         :shipments
         first
         :line-items)))

(defn line-item-by-id [variant-id line-items]
  (first (filter (comp #{variant-id} :id) line-items)))

(defn product-items
  "Returns cart items from an order hashmap.
  Excludes shipping and items added by El Jefe.
  Cart line-items are added as the first shipment.
  Line-items are from first shipment as it is the user created shipment."
  [order]
  (filter #(not= (:source %) "waiter") (line-items order)))

(defn shipping-item
  "Returns the first shipping line-item from an order hashmap.
  Includes only items added by waiter.
  Shipping items are added as the first shipment.
  Line-items are from first shipment as it is the user created shipment."
  [order]
  (->> (line-items order)
       (filter #(= (:source %) "waiter"))
       first))

(defn subtract-rounded-floats [a b]
  (/ (.toFixed (- (* 100.0 a) (* 100.0 b)) 0) 100.0))

(defn form-payment-methods [order-total store-credit]
  (let [store-credit-used (min order-total store-credit)]
    (merge {}
           (when (pos? store-credit-used)
             {:store-credit {:amount store-credit-used}})
           (when (> order-total store-credit-used)
             {:stripe {:amount (subtract-rounded-floats order-total store-credit-used)}}))))

(defn line-item-subtotal [{:keys [quantity unit-price]}]
  (* quantity unit-price))

(defn product-quantity [order]
  (reduce + 0 (map :quantity (product-items order))))

(defn products-subtotal [order]
  (reduce + 0 (map line-item-subtotal (product-items order))))

(defn fully-covered-by-store-credit? [order user]
  (boolean
   (when (and order user)
     (>= (:total-available-store-credit user)
         (:total order)))))

(defn partially-covered-by-store-credit? [order user]
  (boolean
   (when (and order user)
     (< (:total-available-store-credit user)
        (:total order)))))
