(ns storefront.accessors.orders
  (:require [clojure.set :as set]
            [spice.core :as spice]
            [storefront.platform.numbers :as numbers]))

(defn incomplete? [order]
  (and (-> order :state #{"cart"} boolean)
       (not (:frozen? order))))

(defn all-line-items
  "Returns line items from an order hashmap.

   Takes into account *ALL* shipments on the order "
  [order]
  (when order
    (->> order
         :shipments
         (mapcat :line-items))))

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

(defn shipping-method?
  [{:keys [id]}]
  (= -1 id))

(defn ^:private product?
  [{:keys [id] :as line-item}]
  (pos? id))

(defn product-items-for-shipment [shipment]
  (->> shipment
       :line-items
       (filter product?)))

(defn first-commissioned-shipment [order]
  (->> order
       :shipments
       (filter (comp #{"released" "shipped"} :state))
       first))

(defn all-product-items
  "Returns cart items from an order hashmap.
   Takes into account *ALL* shipments on the order "
  [order]
  (->> order all-line-items (filter product?)))

(defn product-items
  "Returns cart items from an order hashmap.
  Cart line-items are added as the first shipment.
  Line-items are from first shipment as it is the user created shipment."
  [order]
  (->> order line-items (filter product?)))

(defn shipping-item
  "Returns the first shipping line-item from an order hashmap.
  Includes only items added by waiter.
  Shipping items are added as the first shipment.
  Line-items are from first shipment as it is the user created shipment."
  [order]
  (->> order line-items (filter shipping-method?) first))

(defn shipping-method-details [shipping-methods shipping-item]
  (->> shipping-methods
       (filter #(= (:sku shipping-item) (:sku %)))
       first))

(defn add-rounded-floats [a b]
  (/ (.toFixed (+ (* 100.0 a) (* 100.0 b)) 0) 100.0))


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

(defn tax-adjustment [order]
  {:name "Tax (Estimated)" :price (:tax-total order 0.0)})

(defn bundle-discount? [order]
  (->> (:adjustments order)
       (map :name)
       (some #{"Bundle Discount"})))

(defn all-order-adjustments [order]
  (conj (:adjustments order)
        (tax-adjustment order)))

(defn all-applied-promo-codes [order]
  (into #{}
        (keep :coupon-code)
        (all-order-adjustments order)))

(defn no-applied-promo?
  [order]
  (or (empty? (all-applied-promo-codes order))
      (= 0 (product-quantity order))))

(defn freeinstall-applied?
  [{:as order :keys [promotion-codes]}]
  (and (bundle-discount? order)
       (contains? (all-applied-promo-codes order) "freeinstall")))

(defn freeinstall-included?
  [order]
  (and (bundle-discount? order)
       (:install-type order)))

(defn applied-install-promotion
  [{:as order :keys [promotion-codes]}]
  (some #{"freeinstall" "install"} (all-applied-promo-codes order)))

(defn freeinstall-entered?
  [{:as order :keys [promotion-codes]}]
  (= #{"freeinstall"} (set promotion-codes)))

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

(defn can-use-store-credit? [order user]
  (let [user-is-stylist? (-> user :store-slug boolean)]
    (and (pos? (:total-available-store-credit user))
         (not (and (:install-type order) user-is-stylist?)))))

(defn form-payment-methods [order user]
  (let [order-total       (:total order)
        store-credit      (:total-available-store-credit user)
        promotions        (all-applied-promo-codes order)
        store-credit-used (min order-total store-credit)]
    (cond-> {}
      (or (some #{"freeinstall"} promotions)
          (> order-total store-credit-used)) (assoc :stripe {})
      (can-use-store-credit? order user)     (assoc :store-credit {}))))

(defn display-adjustment-name [name]
  (cond
    (= name "Bundle Discount")
    "10% Bundle Discount"
    (= name "Free Install")
    "Free Mayvenn Install"
    :else name))

(defn- line-item-tuples [order]
  (->> (product-items order)
       (map (juxt :sku :quantity))))

(defn newly-added-sku-ids [previous-order new-order]
  (let [new-line-item-tuples (set (line-item-tuples new-order))
        prev-line-item-tuples (set (line-item-tuples previous-order))
        changed-line-item-tuples (set/difference new-line-item-tuples
                                                 prev-line-item-tuples)
        prev-sku-id->quantity (into {} prev-line-item-tuples)
        get-sku first]
    (->> changed-line-item-tuples
         (remove (fn [[sku quantity]]
                   (< quantity (prev-sku-id->quantity sku 0))))
         (map get-sku)
         set)))

(defn first-name-plus-last-name-initial [{:as order :keys [billing-address shipping-address]}]
  (when (seq order)
    (str (or (:first-name billing-address)
             (:first-name shipping-address))
         " "
         (first (or (:last-name billing-address)
                    (:last-name shipping-address)))
         ".")))

(defn returned-quantities [order]
  "Returns a map of returned items, with variant-id as the key and quantity as the value"
  (->> order
       :returns
       (mapcat :line-items)
       (reduce (fn [acc {:keys [id quantity]}]
                 (update acc id (fnil + 0) quantity)) {})))

(defn total-savings
  "Takes `order` and amount that stylist charges for a service and
  calculates total savings based on promotions, discounts and other
  `adjustments`"
  [order service-price]
  (->> order
       :adjustments
       (map (comp numbers/abs :price))
       (reduce + 0)
       ((fnil + 0) (numbers/abs (spice/parse-double service-price)))))
