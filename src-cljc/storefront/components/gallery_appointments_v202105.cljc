(ns storefront.components.gallery-appointments-v202105
  (:require [storefront.component :as component :refer [defcomponent defdynamic-component]]
            [storefront.components.svg :as svg]))

(defcomponent component
  [data _ _]
  [:div.mx2.content-3
   (svg/exclamation-circle {:class "mx1" :height "10px"
                            :width "10px"})
   "Click below to add photos to your past appointments!"])
