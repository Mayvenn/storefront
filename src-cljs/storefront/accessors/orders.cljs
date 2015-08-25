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

(defn incomplete? [order] ;;TODO remove unnecessary states
  (-> order :state #{"cart" "address" "delivery" "payment" "confirm"}))

(defn line-items
  "Returns cart items from an order hashmap.
  Excludes shipping and items added by El Jefe.
  Cart line-items are added as the last shipment.
  Line-items are from last shipment as it is the user created shipment."
  [order]
  (js/console.log "Input to line-items" (clj->js order))
  (when order
    (->> order
         :shipments
         (last)
         :line-items
         (filter #(not= (:source (last %)) "waiter"))
         (into {}))))

(def shipping (comp #(get % -1) :line-items last :shipments))

(defn form-payment-methods [order-total store-credit use-store-credit]
  (let [store-credit-used (if use-store-credit (min order-total store-credit) 0)]
    (merge {}
           (when use-store-credit
             {:store-credit {:amount store-credit-used}})
           (when (> order-total store-credit-used)
             {:stripe {:amount (- order-total store-credit-used)}}))))
