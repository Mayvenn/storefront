(ns homepage.ui.hero
  (:require
   [storefront.component :as c]
   [storefront.components.ui :as ui]
   [ui.molecules :refer [hero]]))

(def ^:private free-standard-shipping-bar
  [:div.mx-auto {:style {:height "3em"}}
   [:div.bg-black.flex.items-center.justify-center
    {:style {:height "2.25em"
             :margin-top "-1px"
             :padding-top "1px"}}
    [:div.px2
     (ui/ucare-img {:alt "" :height "25"}
                   "38d0a770-2dcd-47a3-a035-fc3ccad11037")]
    [:div.h7.white.medium
     "FREE standard shipping"]]])

(c/defcomponent organism
  [data _ _]
  [:div
   (c/build hero data)
   free-standard-shipping-bar])
