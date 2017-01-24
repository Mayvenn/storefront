(ns storefront.components.facebook
  (:require [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.components.ui :as ui]))

(defn narrow-sign-in-button [loaded?]
  (when loaded?
    (ui/facebook-button
     {:on-click (utils/send-event-callback events/control-facebook-sign-in)
      :data-test "facebook-button"}
     [:div.col-8-on-tb-dt.mx-auto
      [:div.flex.items-center.justify-around
       [:span "Sign in with"]
       [:img {:src "/images/FacebookWhite.png" :width 20 :height 20}]]])))

(defn- wide-button [loaded? click-event]
  (if loaded?
    (ui/facebook-button
     {:on-click (utils/send-event-callback click-event)
      :data-test "facebook-button"}
     [:div.flex.items-center.justify-center
      [:span "Sign in with Facebook"]
      [:img.ml2 {:src "/images/FacebookWhite.png" :width 20 :height 20}]])
    [:div {:style {:height "2.6666em"}}]))

(defn sign-in-button [loaded?]
  (wide-button loaded? events/control-facebook-sign-in))

(defn reset-button [loaded?]
  (wide-button loaded? events/control-facebook-reset))
