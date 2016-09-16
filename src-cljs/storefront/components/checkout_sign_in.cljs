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

     (ui/green-button {:on-click  (utils/send-event-callback events/control-checkout-as-guest-submit)
                       :data-test "guest-checkout-button"}
                      "Guest Checkout")

     [:.my3 [:.col-2.m-auto.border.border-light-silver]]
     [:.h2.center.my2 "Already registered?"]
     [:.h6.center.mb2 "Sign into your account below, and checkout even faster!"]
     (om/build sign-in/form-component sign-in-form-data)))))

(defn built-component [data opts]
  (om/build component (sign-in/query data) opts))

(defn requires-sign-in-or-guest [authorized-component data opts]
  (if (or (get-in data keypaths/user-id)
          (get-in data keypaths/checkout-as-guest))
    (authorized-component data nil)
    (built-component data nil)))
