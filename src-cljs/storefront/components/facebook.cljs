(ns storefront.components.facebook
  (:require [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn- button [data click-event]
  (if (get-in data keypaths/loaded-facebook)
    [:button.fb-login-button
     {:on-click (utils/send-event-callback data click-event)}
     [:div.fb-login-wrapper
      [:img {:src "/images/FacebookWhite.png" :width 29 :height 29}]
      [:div.fb-login-content "Sign in with Facebook"]]]
    [:div.fb-filler]))

(defn sign-in-button [data]
  (button data events/control-facebook-sign-in))

(defn reset-button [data]
  (button data events/control-facebook-reset))
