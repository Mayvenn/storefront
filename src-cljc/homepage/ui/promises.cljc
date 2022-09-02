(ns homepage.ui.promises
  (:require [storefront.component :as c]
            [storefront.components.svg :as svg]))

(def icons-with-copy
  [{:icon svg/heart
    :text "Ethical Sourcing"}
   {:icon svg/calendar
    :text "30 Day Guarantee"}
   {:icon svg/experience-badge
    :text "100% Virgin Human Hair"}
   {:icon svg/shipping
    :text "Free Standard Shipping"} ])

(c/defcomponent organism
  [data _ _]
  [:div.bg-cool-gray
   [:div.flex.justify-between.px10
    (for [{:keys [icon text]} icons-with-copy]
      [:div.flex.px3.items-end
       [:div.align-middle
        {:width "18px"}
        (icon {:class  "fill-black"
               :width  "18px"
               :height "15px"})]
       [:div.title-3.proxima.pl1 text]])]])
