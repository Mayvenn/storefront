(ns storefront.accessors.service-menu
  (:require [storefront.components.money-formatters :as mf]))

(def unit-type->menu-kw
  {"Freeinstall (with Closure)" :install-sew-in-closure
   "Freeinstall (with 360)"     :install-sew-in-360-frontal
   "Freeinstall (with Frontal)" :install-sew-in-frontal
   "Freeinstall (Leave Out)"    :install-sew-in-leave-out
   "Wig Customization"          :wig-customization})

(def unit-type->campaign-name
  {"Freeinstall (with Closure)" "Free Install - Closure"
   "Freeinstall (with 360)"     "Free Install - 360"
   "Freeinstall (with Frontal)" "Free Install - Frontal"
   "Freeinstall (Leave Out)"    "Free Install - Leave Out"
   "Wig Customization"          "Wig Customization"})

(def default-service-menu
  "Install prices to use when a stylist has not yet been selected."
  {:advertised-sew-in-360-frontal "225.0"
   :advertised-sew-in-closure     "175.0"
   :advertised-sew-in-frontal     "200.0"
   :advertised-sew-in-leave-out   "150.0"
   :advertised-wig-customization  "75.0"})

(defn discount->campaign-name [discount]
  (some->> discount :unit_type unit-type->campaign-name))

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
