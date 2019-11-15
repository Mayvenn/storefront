(ns storefront.accessors.line-items)

(defn service? [{:keys [source]}]
  (= "service" source))

(defn shipping-method?
  [{:keys [source]}]
  (= "waiter" source))

(defn product?
  [{:keys [source]}]
  (= "spree" source))

(defn service-line-item-price
  "Might be nil"
  [{:keys [quantity unit-price] :as service-line-item}]
  (when (and service-line-item (pos? unit-price))
    (* quantity unit-price)))

(defn discounted-unit-price
  [{:keys [applied-promotions unit-price quantity]}]
  (let [total-amount-off  (->> applied-promotions
                               (map :amount)
                               (reduce + 0)
                               -)
        discount-per-item (/ total-amount-off quantity)]
    (- unit-price discount-per-item)))
