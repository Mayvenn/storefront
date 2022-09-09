(ns homepage.ui.promises
  (:require [storefront.component :as c]
            [storefront.components.svg :as svg]))

(def icons-with-copy
  [{:icon svg/hand-heart
    :text "Top-Notch Service"}
   {:icon svg/shield
    :text "30 Day Guarantee"}
   {:icon svg/check-cloud
    :text "100% Virgin Human Hair"}
   {:icon svg/ship-truck
    :text "Free Standard Shipping"} ])

(c/defcomponent organism
  [data _ _]
  [:div
   [:div.bg-cool-gray.hide-on-tb-dt ; mobile
    [:div.flex.justify-around.
     (for [{:keys [icon text]} icons-with-copy]
       [:div.col-3.flex.flex-column.justify-start.items-center.gap-1.p1
        (icon {:class "fill-black"
               :width "24px"
               :height "24px"})
        [:div.bold.center.shout.px1
         {:style {:font "9px/14px \"Proxima Nova\""}}
         text]])]]
   [:div.bg-cool-gray.hide-on-mb ; desktop
    [:div.flex.justify-around.py2
     (for [{:keys [icon text]} icons-with-copy]
       [:div.flex.items-center.gap-1
        (icon {:class "fill-black"
               :width "24px"
               :height "24px"})
        [:div text]])]]])
