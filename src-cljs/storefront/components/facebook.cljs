(ns storefront.components.facebook
  (:require [storefront.components.utils :as utils]
            [storefront.events :as events]))

(defn button [data]
  [:button.fb-login-button
   {:on-click (utils/send-event-callback data events/control-facebook-sign-in)}
   [:div.fb-login-wrapper
    [:img {:src "/images/FacebookWhite.png" :width 29 :height 29}]
    [:div.fb-login-content "Sign in with Facebook"]]])
