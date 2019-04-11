(ns adventure.components.wait-spinner
  (:require [storefront.component :as component]))

(defn component
  [{:keys [] :as data} _ _]
  (component/create
   [:div.white.flex.flex-auto.flex-column.items-center.pt4
    {:style {:background "linear-gradient(#cdb8d9,#9a8fb4)"}}
    [:div.mt10.mb2
     [:img {:src "https://ucarecdn.com/9b6a76cc-7c8e-4715-8973-af2daa15a5da/matching-stylist-wait.gif"
            :width "90px"}]]
    [:div.col-8.h3.my2.medium.center "Matching you with a" [:br] " Mayvenn Certified Stylist..."]
    [:ul.col-7.h6.list-img-purple-checkmark
     (mapv (fn [%] [:li.mb1 %])
           ["Licensed Salon Stylist" "Mayvenn Certified" "In your area"])]]))
