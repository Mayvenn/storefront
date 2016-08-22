(ns storefront.components.facebook
  (:require [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn- button [loaded? click-event]
  (if loaded?
    [:.btn.btn-primary.bg-fb-blue.col-12
     {:on-click (utils/send-event-callback click-event)
      :data-test "facebook-button"}
     [:.flex.items-center.justify-center.white.items-center
      [:img.mr2 {:src "/images/FacebookWhite.png" :width 29 :height 29}]
      [:.h3.py1 "Sign in with Facebook"]]]
    [:div {:style {:height "3.25rem"}}]))

(defn sign-in-button [loaded?]
  (button loaded? events/control-facebook-sign-in))

(defn reset-button [loaded?]
  (button loaded? events/control-facebook-reset))
