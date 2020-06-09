(ns storefront.accessors.adjustments)

(defn non-zero-adjustment? [{:keys [name price coupon-code]}]
  (or (not (= price 0))
      (#{"FREEINSTALL" "Wig Customization"} name)
      (#{"amazon" "freeinstall" "install" "custom"} coupon-code)))

(defn display-adjustment-name
  [{:keys [name]}]
  (if (= name "Bundle Discount")
    "10% Bundle Discount"
    name))
