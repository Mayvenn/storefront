(ns storefront.accessors.orders)

(defn incomplete? [order]
  (and (-> order :state #{"cart"} boolean)
       (not (:frozen? order))))

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
  (->> line-items
       (filter (comp #{variant-id} :id))
       first))

(def ^:private shipping-item? (comp #{"waiter"} :source))

(defn product-items-for-shipment [shipment]
  (->> shipment
       :line-items
       (remove shipping-item?)))

(defn first-commissioned-shipment [order]
  (->> order
       :shipments
       (filter (comp #{"released" "shipped"} :state))
       first))

(defn product-items
  "Returns cart items from an order hashmap.
  Excludes shipping and items added by El Jefe.
  Cart line-items are added as the first shipment.
  Line-items are from first shipment as it is the user created shipment."
  [order]
  (->> order line-items (remove shipping-item?)))

(defn shipping-item
  "Returns the first shipping line-item from an order hashmap.
  Includes only items added by waiter.
  Shipping items are added as the first shipment.
  Line-items are from first shipment as it is the user created shipment."
  [order]
  (->> order line-items (filter shipping-item?) first))

(defn shipping-method-details [shipping-methods shipping-item]
  (->> shipping-methods
       (filter #(= (:sku shipping-item) (:sku %)))
       first))

(defn add-rounded-floats [a b]
  (/ (.toFixed (+ (* 100.0 a) (* 100.0 b)) 0) 100.0))

(defn form-payment-methods [order-total store-credit]
  (let [store-credit-used (min order-total store-credit)]
    (merge {}
           (when (pos? store-credit-used)
             {:store-credit {}})
           (when (> order-total store-credit-used)
             {:stripe {}}))))

(defn line-item-subtotal [{:keys [quantity unit-price]}]
  (* quantity unit-price))

(defn line-item-quantity [line-items]
  (->> line-items
       (map :quantity)
       (reduce +)))

(defn product-quantity [order]
  (line-item-quantity (product-items order)))

(defn products-subtotal [order]
  (reduce + 0 (map line-item-subtotal (product-items order))))

(defn commissioned-products-subtotal [order]
  (reduce + 0 (->> order
                   first-commissioned-shipment
                   product-items-for-shipment
                   (map line-item-subtotal))))

(defn non-store-credit-payment-amount [order]
  (->> order
       :payments
       (remove (comp #{"store-credit"} :payment-type))
       (map :amount)
       (apply +)))

(defn fully-covered-by-store-credit? [order user]
  (boolean
   (when (and order user)
     (>= (:total-available-store-credit user)
         (:total order)))))

(defn tax-adjustment [order]
  {:name "Tax (Estimated)" :price (:tax-total order)})

(defn all-order-adjustments [order]
  (conj (:adjustments order)
        (tax-adjustment order)))

(defn bundle-discount? [order]
  (->> (:adjustments order)
       (map :name)
       (some #{"Bundle Discount"})))

(defn display-adjustment-name [name]
  (if (= name "Bundle Discount")
    "10% Bundle Discount"
    name))
