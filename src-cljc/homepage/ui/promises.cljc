(ns homepage.ui.promises
  (:require [storefront.component :as c]
            [storefront.components.svg :as svg]))

(def icons-with-copy
  [{:icon svg/shield
    :text "30 Day Guarantee"}
   {:icon svg/experience-badge
    :text "100% Virgin Human Hair"}
   {:icon svg/shipping
    :text "Free Standard Shipping"} ])

(c/defcomponent organism
  [data _ _]
  [:div
   [:div.bg-cool-gray.hide-on-tb-dt.pl2 ; mobile
    [:div.flex.justify-around.p1
     (for [{:keys [icon text]} icons-with-copy]
       [:div.flex.items-center
        [:span.align-top
         (icon {:class "fill-black"
                :width "16px"
                :height "16px"})]
        [:div.content-3.proxima.pl1.bold text]])]]

   [:div.bg-cool-gray.hide-on-mb ; desktop
    [:div.flex.justify-around
     (for [{:keys [icon text]} icons-with-copy]
       [:div.flex.px3.items-center
        [:span.align-top
         (icon {:class "fill-black"
                :width "16px"
                :height "16px"})]
        [:div.title-3.proxima.pl1 text]])]]])