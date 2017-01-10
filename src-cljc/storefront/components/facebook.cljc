(ns storefront.components.facebook
  (:require [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.components.ui :as ui]))


(defn- button [btn-style loaded? click-event copy]
  (if loaded?
    (btn-style
     {:on-click (utils/send-event-callback click-event)
      :data-test "facebook-button"}
     [:div.flex.items-center.justify-center
      {:style {:max-height "1.2em"}}
      [:img.mr2 {:src "/images/FacebookWhite.png" :width 29 :height 29}]
      [:span copy]])
    [:div {:style {:height "3.25rem"}}]))

(defn small-sign-in-button [loaded?]
  (button ui/facebook-button loaded? events/control-facebook-sign-in "Sign in"))

(defn sign-in-button [loaded?]
  (button ui/large-facebook-button loaded? events/control-facebook-sign-in "Sign in with Facebook"))

(defn reset-button [loaded?]
  (button ui/large-facebook-button loaded? events/control-facebook-reset "Sign in with Facebook"))
