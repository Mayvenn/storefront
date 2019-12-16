(ns adventure.components.wait-spinner
  (:require [storefront.component :as component :refer [defcomponent]]))

(defcomponent component
  [{:keys [] :as data} _ _]
  [:div.border.border-framed.absolute.overlay.m4.px6.flex.flex-column.items-center
   [:img {:style {:margin-top "74px"}
          :src   "https://ucarecdn.com/9b6a76cc-7c8e-4715-8973-af2daa15a5da/matching-stylist-wait.gif"
          :width "90px"}]
   [:h1.title-2.canela.center.mt8.mb6 "Matching you with a" [:br] " Mayvenn Certified Stylist..."]
   [:ul.list-purple-diamond
    [:li.my2 "Licensed Salon Stylist"]
    [:li.my2 "Mayvenn Certified"]
    [:li.my2 "In your area"]]])

