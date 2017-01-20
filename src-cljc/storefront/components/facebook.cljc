(ns storefront.components.facebook
  (:require [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.components.ui :as ui]))

(defn small-sign-in-button [loaded?]
  (when loaded?
    (ui/facebook-button
     {:on-click (utils/send-event-callback events/control-facebook-sign-in)
      :data-test "facebook-button"}
     [:div.flex.items-center.justify-center
      [:span "Sign in with"]
      [:img.ml2 {:src "/images/FacebookWhite.png" :width 20 :height 20}]])))

(defn- large-button [loaded? click-event]
  (if loaded?
    (ui/large-facebook-button
     {:on-click (utils/send-event-callback click-event)
      :data-test "facebook-button"}
     [:div.flex.items-center.justify-center
      {:style {:max-height "1.2em"}}
      [:img.mr2 {:src "/images/FacebookWhite.png" :width 29 :height 29}]
      [:span "Sign in with Facebook"]])
    [:div {:style {:height "3.25rem"}}]))

(defn sign-in-button [loaded?]
  (large-button loaded? events/control-facebook-sign-in))

(defn reset-button [loaded?]
  (large-button loaded? events/control-facebook-reset))
