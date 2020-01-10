(ns storefront.accessors.adjustments)

(defn non-zero-adjustment? [{:keys [name price coupon-code]}]
  (or (not (= price 0))
      (#{"FREEINSTALL"} name)
      (#{"amazon" "freeinstall" "install" "custom"} coupon-code)))

(defn display-adjustment-name
  [{:keys [name]}]
  (cond
    (= name "Bundle Discount")
    "10% Bundle Discount"

    (#{"Free Install" "FREEINSTALL"} name)
    "Free Mayvenn Install"

    :else name))

(defn display-service-line-item-adjustment-name
  [{:keys [name code]} service-type]
  (when (and (nil? code) (#{"Free Install" "FREEINSTALL"} name))
    (if (= :wig-customization service-type)
      "Free Wig Customization"
      "Free Mayvenn Install")))
