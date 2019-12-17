(ns adventure.components.wait-spinner
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]))

(defcomponent component
  [{:keys [] :as data} _ _]
  [:div.absolute.overlay.border.border-framed.m4.py6.px2
   {:style {:overflow "hidden"}}
   [:div.flex.flex-column.items-center.m2
    [:div {:style {:margin-top "44px"}}
     (ui/ucare-gif2video {:width 72} "17ea60bf-fddb-4838-b132-3d076a257703")]
    [:h1.title-2.canela.center.mt8.mb6 "Matching you" [:wbr] " with a" [:br] " Mayvenn Certified Stylist..."]
    [:ul.list-purple-diamond
     [:li.my2 "Licensed Salon Stylist"]
     [:li.my2 "Mayvenn Certified"]
     [:li.my2 "In your area"]]]])

