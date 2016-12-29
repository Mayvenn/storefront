(ns storefront.components.checkout-sign-in
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.accessors.experiments :as experiments]
            [storefront.components.sign-in :as sign-in]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(defn component [sign-in-form-data owner]
  (om/component
   (html
    (ui/narrow-container
     [:h2.center.my2.navy "I'm new here"]

     (ui/large-teal-button {:on-click  (utils/send-event-callback events/control-checkout-as-guest-submit)
                            :data-test "guest-checkout-button"}
                           "Guest Checkout")

     [:div.my3 [:.col-2.m-auto.border.border-dark-silver]]
     [:h2.center.my2.navy "Already registered?"]
     [:div.h6.center.mb2 "Sign into your account below, and checkout even faster!"]
     (om/build sign-in/form-component sign-in-form-data)))))

(defn simple-component [sign-in-form-data owner]
  (om/component
   (html
    (ui/narrow-container
     [:h2.center.my2.mb3.navy "Sign in to your account"]
     (om/build sign-in/password-component sign-in-form-data)))))

(defn built-component [data opts]
  (if (experiments/address-login? data)
    (om/build simple-component (sign-in/query data) opts)
    (om/build component (sign-in/query data) opts)))

(defn requires-sign-in-or-guest [authorized-component data opts]
  (if (or (get-in data keypaths/user-id)
          (get-in data keypaths/checkout-as-guest))
    (authorized-component data nil)
    (built-component data nil)))
