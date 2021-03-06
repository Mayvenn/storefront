(ns promotion-helper.ui.drawer-face
  (:require [storefront.component :as c :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.platform.component-utils :as utils]))

(def drawer-face-s-color-checkmark-atom
  (svg/check-mark {:class "fill-s-color"
                   :style {:height "12px" :width "14px"}}))

(defn drawer-face-circle-molecule
  [{:promotion-helper.ui.drawer-face.circle/keys [id color value]}]
  (c/html
   [:div.circle.flex.items-center.justify-center.ml2
    {:style {:height "20px" :min-width "20px"}
     :data-test id
     :class color}
    (if (int? value) value (svg/symbolic->html value))]))

;; Note: these molecules are weird because the title is split

(defn drawer-face-primary-title-molecule
  [_]
  (c/html
   [:div.shout "Free Mayvenn Service Tracker"]))

(defn drawer-face-secondary-title-molecule
  [{}]
  (c/html
   [:div.button-font-3.mtp4.regular "Tap to learn how to get your service for free"]))

(defcomponent organism
  [{:promotion-helper.ui.drawer-face.action/keys [target id opened?]
    :as data} _ _]
  [:div.flex.items-center.justify-center.p4.bg-black.white
   (-> (apply utils/fake-href target)
       (assoc :data-test id))
   [:div.flex-auto.pr4
    [:div.flex.items-center.justify-left.proxima.button-font-2.bold
     (drawer-face-primary-title-molecule data)
     (drawer-face-circle-molecule data)]
    (drawer-face-secondary-title-molecule data)]
   ;; chevron
   [:div.fill-white.flex.items-center.justify-center
    (svg/dropdown-arrow {:height "18px"
                         :width  "18px"
                         :class (when-not opened? "rotate-180")})]])
