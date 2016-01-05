(ns storefront.components.facebook
  (:require [storefront.components.utils :as utils]
            [storefront.events :as events]))

(defn- button [data click-event]
  [:button.fb-login-button
   {:on-click (utils/send-event-callback data click-event)}
   [:div.fb-login-wrapper
    [:img {:src "/images/FacebookWhite.png" :width 29 :height 29}]
    [:div.fb-login-content "Sign in with Facebook"]]])

(defn sign-in-button [data]
  (button data events/control-facebook-sign-in))

(defn reset-button [data]
  (button data events/control-facebook-reset))
