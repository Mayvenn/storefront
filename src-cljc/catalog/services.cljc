(ns catalog.services)

;; Labels for service attribute sets

;; Aka Free, Mayvenn Service
(def discountable
  {:catalog/department                 #{"service"}
   :service/type                       #{"base"}
   :promo.mayvenn-install/discountable #{true}
   :selector/essentials                [:catalog/department
                                        :service/type
                                        :promo.mayvenn-install/discountable]})

(def a-la-carte
  {:catalog/department                 #{"service"}
   :service/type                       #{"base"}
   :promo.mayvenn-install/discountable #{false}
   :selector/essentials                [:catalog/department
                                        :service/type
                                        :promo.mayvenn-install/discountable]})

(def addons
  {:catalog/department  #{"service"}
   :service/type        #{"addon"}
   :selector/essentials [:catalog/department
                         :service/type]})
