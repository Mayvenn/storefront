(ns storefront.accessors.adjustments)

(defn non-zero-adjustment? [{:keys [price coupon-code]}]
  (or (not (= price 0))
      (#{"amazon" "freeinstall" "install" "custom"} coupon-code)))
