(ns stylist-matching.ui.match-success-pick-hair
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]))

(def ^:private congrats-heart-atom
  (svg/heart {:class  "fill-p-color"
              :width  "41px"
              :height "37px"}))

(defcomponent organism
  [data _ _]
  [:div
   congrats-heart-atom])
