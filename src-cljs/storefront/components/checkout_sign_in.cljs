(ns storefront.components.checkout-sign-in
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.sign-in :as sign-in]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(defn component [sign-in-form-data owner]
  (om/component
   (html
    (ui/narrow-container
     [:.h2.center.my2 "I'm new here"]

     (ui/button "Guest Checkout"
                {:on-click  (utils/send-event-callback events/control-checkout-as-guest-submit)
                 :data-test "guest-checkout-button"})

     [:.my3 [:.col-2.m-auto.border.border-light-silver]]
     [:.h2.center.my2 "Already registered?"]
     [:.h5.center.mb2 "Sign into your account below, and checkout even faster!"]
     (om/build sign-in/form-component sign-in-form-data)))))

(defn built-component [data owner]
  (om/component (html (om/build component (sign-in/query data)))))

(defn requires-sign-in-or-guest [app-state authorized-component]
  (if (or (get-in app-state keypaths/user-id)
          (get-in app-state keypaths/checkout-as-guest))
    authorized-component
    built-component))
