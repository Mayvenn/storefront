(ns promotion-helper.ui.drawer-face
  (:require [storefront.component :as c :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.platform.component-utils :as utils]))

(defn drawer-face-circle-molecule
  [{:promotion-helper.ui.drawer-face.circle/keys [color value]}]
  (c/html
   [:div.circle.flex.items-center.justify-center.ml2
    {:style {:height "20px" :width "20px"}
     :class color}
    value]))

;; Note: these molecules are weird because the title is split

(defn drawer-face-primary-title-molecule
  [_]
  (c/html
   [:div.shout "Free Mayvenn Service Tracker"]))

(defn drawer-face-secondary-title-molecule
  [{}]
  (c/html
   [:div.button-font-3.mtp4.regular "Swipe up to learn how to get your service for free"]))

(defcomponent organism
  [data _ _]
  [:div.flex.items-center.justify-center.pl3.pr4.py2.bg-black.white
   (apply utils/fake-href
          (:promotion-helper.ui.drawer-face.action/target data))
   [:div.flex-auto.pr4
    [:div.flex.items-center.justify-left.proxima.button-font-2.bold
     (drawer-face-primary-title-molecule data)
     (drawer-face-circle-molecule data)]
    (drawer-face-secondary-title-molecule data)]
   ;; chevron
   [:div.fill-white.flex.items-center.justify-center
    (svg/dropdown-arrow {:height "18px"
                         :width  "18px"})]])
