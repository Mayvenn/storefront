(ns storefront.components.checkout-sign-in
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.accessors.experiments :as experiments]
            [storefront.components.sign-in :as sign-in]
            [storefront.components.ui :as ui]
            [storefront.components.facebook :as facebook]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(defn full-component [sign-in-form-data owner]
  (om/component
   (html
    (ui/narrow-container
     [:h2.center.my2.navy "I'm new here"]

     (ui/large-teal-button {:on-click  (utils/send-event-callback events/control-checkout-as-guest-submit)
                            :data-test "guest-checkout-button"}
                           "Guest Checkout")

     [:div.my3 [:.col-2.m-auto.border.border-gray]]
     [:h2.center.my2.navy "Already registered?"]
     [:div.h6.center.mb2 "Sign into your account below, and checkout even faster!"]
     (om/build sign-in/form-component sign-in-form-data)))))

(defn built-full-component [data opts]
  (om/build full-component (sign-in/query data) opts))

(defn component [{:keys [facebook-loaded?] :as sign-in-form-data} owner]
  (om/component
   (html
    (ui/narrow-container
     [:h2.h3.center.my2.mb3 "Sign in to your account"]
     (om/build sign-in/password-component sign-in-form-data)
     [:div.h5.gray.light.center.mt1.mb2 "OR"]
     (facebook/sign-in-button facebook-loaded?)))))

(defn built-component [data opts]
  (if (experiments/address-login? data)
    (om/build component (sign-in/query data) opts)
    (built-full-component data opts)))
