(ns storefront.accessors.adjustments)

(def mayvenn-install-adjustment-display-name "Free Mayvenn Install")
(def wig-customization-adjustment-display-name "Free Wig Customization")

(defn non-zero-adjustment? [{:keys [name price coupon-code]}]
  (or (not (= price 0))
      (#{"FREEINSTALL" "Wig Customization"} name)
      (#{"amazon" "freeinstall" "install" "custom"} coupon-code)))

(defn display-adjustment-name
  [{:keys [name]}]
  (cond
    (= name "Bundle Discount")
    "10% Bundle Discount"

    (#{"Free Install" "FREEINSTALL"} name)
    mayvenn-install-adjustment-display-name

    (#{"Wig Customization"} name)
    wig-customization-adjustment-display-name

    :else name))

(defn display-service-line-item-adjustment-name
  [{:keys [name]}]
  (cond
    (#{"Free Install" "FREEINSTALL"} name)
    mayvenn-install-adjustment-display-name

    (#{"Wig Customization"} name)
    wig-customization-adjustment-display-name))
