(ns storefront.components.checkout-sign-in
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.utils :as utils]
            [storefront.components.facebook :as facebook]
            [storefront.events :as events]
            [storefront.hooks.experiments :as experiments]
            [storefront.keypaths :as keypaths]))

(defn checkout-sign-in-component [data owner]
  (om/component
   (html
    [:div.centered-content
     [:div.guest-checkout
      [:h2.explanation-header.center "I'm new here"]
      [:div.button.primary
       {:on-click (utils/send-event-callback data events/control-checkout-as-guest-submit)}
       "Guest Checkout"]

      [:div.short-divider ]
      [:h2.explanation-header.center "Already registered?"]
      [:p.explanation.center "Sign into your account below, and checkout even faster!"]]
     [:div#existing-customer.new_spree_user
      (facebook/sign-in-button data)
      [:h2.center.or "OR"]
      [:form.simple_form
       {:on-submit (utils/send-event-callback data events/control-sign-in-submit)}
       [:div#password-credentials
        [:div.input.email
         [:label.email "Email"]
         [:input.string.email
          (merge (utils/change-text data owner keypaths/sign-in-email)
                 {:autofocus "autofocus"
                  :type "email"
                  :name "email"})]]
        [:div.input.password
         [:label.password "Password"]
         [:input.string.password
          (merge (utils/change-text data owner keypaths/sign-in-password)
                 {:type "password"
                  :name "password"})]]]
       [:p
        [:input#remember-me
         (merge (utils/change-checkbox data keypaths/sign-in-remember)
                {:type "checkbox"
                 :name "remember-me"})]
        [:label {:for "remember-me"} "Remember me"]]
       [:a.forgot-password (utils/route-to data events/navigate-forgot-password) "Forgot Password?"]
       [:p
        [:input.button.primary {:type "submit"
                                :value "Login"}]]]]
     [:p.center "Don't have an account? "
      [:a (utils/route-to data events/navigate-sign-up) "Register Here"]]])))

(defn requires-sign-in-or-guest [app-state authorized-component]
  (if (or (get-in app-state keypaths/user-id)
          (get-in app-state keypaths/checkout-as-guest))
    authorized-component
    checkout-sign-in-component))
