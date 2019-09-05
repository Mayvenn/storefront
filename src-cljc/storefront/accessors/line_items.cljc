(ns storefront.accessors.line-items)

(defn discounted-unit-price
  [{:keys [applied-promotions unit-price quantity]}]
  (let [total-amount-off  (->> applied-promotions
                               (map :amount)
                               (reduce + 0)
                               -)
        discount-per-item (/ total-amount-off quantity)]
    (- unit-price discount-per-item)))
