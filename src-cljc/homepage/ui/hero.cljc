(ns homepage.ui.hero
  (:require [storefront.component :as c]
            [storefront.components.svg :as svg]
            [ui.molecules :refer [hero]]))

(def ^:private free-standard-shipping-bar
  [:div.mx-auto
   [:div.bg-black.flex.items-center.justify-center
    {:style {:height "2.25em"
             :margin-top "-1px"
             :padding-top "1px"}}
    (svg/shipping {:height "18px" :width "17px"})
    [:div.h7.white.medium.pxp5
     "FREE standard shipping"]]])

(c/defcomponent organism
  [data _ _]
  [:div
   (c/build hero data)
   free-standard-shipping-bar])
