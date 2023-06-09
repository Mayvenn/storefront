(ns homepage.ui-v2022-09
  (:require [storefront.component :as c]
            [storefront.components.phone-consult :as phone-consult]
            [adventure.components.layered :as layered]))

(c/defcomponent template
  [{:keys [phone-consult-cta] :as data} _ _]
  [:div
   (when (:shopping-homepage phone-consult-cta)
     (c/build phone-consult/component phone-consult-cta))
   (c/build layered/component data nil)])
