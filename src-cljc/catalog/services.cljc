(ns catalog.services)

;; Labels for service attribute sets

;; Aka Free, Mayvenn Service
(def discountable
  {:catalog/department                 #{"service"}
   :service/type                       #{"base"}
   :promo.mayvenn-install/discountable #{true}})

(def a-la-carte
  {:catalog/department                 #{"service"}
   :service/type                       #{"base"}
   :promo.mayvenn-install/discountable #{false}})

(def addons
  {:catalog/department #{"service"}
   :service/type       #{"addon"}})
