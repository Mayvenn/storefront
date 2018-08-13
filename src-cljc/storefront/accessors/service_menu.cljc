(ns storefront.accessors.service-menu
  (:require [storefront.components.money-formatters :as mf]))

(defn parse-type [unit-type]
  (some->> unit-type
           (re-find #"\((.*)\)")
           second))

(def unit-type->menu-kw
  {"with Closure" :install-sew-in-closure
   "with 360"     :install-sew-in-360-frontal
   "with Frontal" :install-sew-in-frontal
   "Leave Out"    :install-sew-in-leave-out})

(defn display-voucher-amount
  ([service-menu voucher]
   (display-voucher-amount service-menu mf/as-money-without-cents voucher))
  ([service-menu money-formatter {:as voucher :keys [discount]}]
   (case (-> discount :type)
     ;; TODO: What to do with percent off?
     "PERCENT"
     (str (-> discount :percent_off) "%")

     "UNIT"
     (some-> discount :unit_type parse-type unit-type->menu-kw (get service-menu) money-formatter)

     "AMOUNT"
     (-> discount :amount_off (/ 100) money-formatter)

     nil)))
