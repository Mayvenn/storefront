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
  (-> order :state #{"cart" "address" "delivery" "payment" "confirm"}))
