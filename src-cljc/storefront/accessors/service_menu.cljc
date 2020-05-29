(ns storefront.accessors.service-menu
  (:require [storefront.components.money-formatters :as mf]))

(def unit-type->menu-kw
  {"Free Install (with Closure)" :install-sew-in-closure
   "Free Install (with 360)"     :install-sew-in-360-frontal
   "Free Install (with Frontal)" :install-sew-in-frontal
   "Free Install (Leave Out)"    :install-sew-in-leave-out
   "Wig Customization"          :wig-customization})

(def default-service-menu
  "Install prices to use when a stylist has not yet been selected."
  {:advertised-sew-in-360-frontal "225.0"
   :advertised-sew-in-closure     "175.0"
   :advertised-sew-in-frontal     "200.0"
   :advertised-sew-in-leave-out   "150.0"
   :advertised-wig-customization  "75.0"})

(defn display-voucher-amount
  ([service-menu voucher]
   (display-voucher-amount service-menu mf/as-money-without-cents voucher))
  ([service-menu money-formatter {:as voucher :keys [discount]}]
   (case (-> discount :type)
     ;; TODO: What to do with percent off?
     "PERCENT"
     (str (-> discount :percent_off) "%")

     "UNIT"
     (some->> discount :unit_type unit-type->menu-kw (get service-menu) money-formatter)

     "AMOUNT"
     (-> discount :amount_off (/ 100) money-formatter)

     nil)))
