(ns storefront.accessors.adjustments)

(defn non-zero-adjustment? [{:keys [name price coupon-code]}]
  (or (not (= price 0))
      (#{"FREEINSTALL"} name)
      (#{"amazon" "freeinstall" "install" "custom"} coupon-code)))
