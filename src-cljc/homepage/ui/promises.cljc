(ns homepage.ui.promises
  (:require [storefront.component :as c]
            [storefront.components.svg :as svg]))

(def icons-with-copy
  [{:id   "hand-heart"
    :icon svg/hand-heart
    :text "Top-Notch Service"}
   {:id   "shield"
    :icon svg/shield
    :text "30 Day Guarantee"}
   {:id   "check-cloud"
    :icon svg/check-cloud
    :text "100% Virgin Human Hair"}
   {:id   "ship-truck"
    :icon svg/ship-truck
    :text "Free Standard Shipping"} ])

(c/defcomponent organism
  [data _ _]
  [:div
   [:div.bg-cool-gray.hide-on-tb-dt ; mobile
    [:div.flex.justify-around
     (for [{:keys [id icon text]} icons-with-copy]
       [:div.col-3.flex.flex-column.justify-start.items-center.gap-1.p1
        {:key id}
        (icon {:class "fill-black"
               :width "24px"
               :height "24px"})
        [:div.bold.center.shout.px1
         {:style {:font "9px/14px \"Proxima Nova\""}}
         text]])]]
   [:div.bg-cool-gray.hide-on-mb ; desktop
    [:div.flex.justify-around.py2
     (for [{:keys [id icon text]} icons-with-copy]
       [:div.flex.items-center.gap-1
        {:key id}
        (icon {:class "fill-black"
               :width "24px"
               :height "24px"})
        [:div text]])]]])
