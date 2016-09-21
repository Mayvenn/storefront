(ns storefront.components.facebook
  (:require [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.components.ui :as ui]))


(defn- button [loaded? click-event]
  (if loaded?
    (ui/large-facebook-button
     {:on-click (utils/send-event-callback click-event)
      :data-test "facebook-button"}
     [:div.flex.items-center.justify-center
      [:img.mr2 {:src "/images/FacebookWhite.png" :width 29 :height 29}]
      [:span "Sign in with Facebook"]])
    [:div {:style {:height "3.25rem"}}]))

(defn sign-in-button [loaded?]
  (button loaded? events/control-facebook-sign-in))

(defn reset-button [loaded?]
  (button loaded? events/control-facebook-reset))
