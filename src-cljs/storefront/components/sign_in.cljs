(ns storefront.components.sign-in
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.utils :as utils]
            [storefront.components.facebook :as facebook]
            [storefront.events :as events]
            [storefront.hooks.experiments :as experiments]
            [storefront.keypaths :as keypaths]))

(defn sign-in-component [data owner]
  (om/component
   (html
    [:div.centered-content
     (when-not (get-in data keypaths/get-satisfaction-login?)
       [:div.page-heading.center "Sign In to Your Account"])
     [:div#existing-customer.new_spree_user
      (facebook/sign-in-button (get-in data keypaths/loaded-facebook))
      [:div.or-divider [:span "or"]]
      [:form.simple_form
       {:on-submit (utils/send-event-callback events/control-sign-in-submit)}
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
       [:a.forgot-password (utils/route-to events/navigate-forgot-password) "Forgot Password?"]
       [:p
        [:input.button.primary {:type "submit"
                                :value "Login"}]]]]
     (when-not (get-in data keypaths/get-satisfaction-login?)
       [:p.center "Don't have an account? "
        [:a (utils/route-to events/navigate-sign-up) "Register Here"]])])))

(defn requires-sign-in [app-state authorized-component]
  (if (get-in app-state keypaths/user-id)
    authorized-component
    sign-in-component))

(defn redirect-getsat-component [data owner]
  (om/component
   (html
    [:div.centered-content
     ;; effects injects GetSat JS that will redirect / close this window as needed
     (if (or (nil? (get-in data keypaths/user))
             (get-in data keypaths/user-store-slug))
       [:div.page-heading.center "Signing in to the Mayvenn Stylist Community..."]
       [:div.flash.error
        "The Mayvenn Stylist Community is only for Mayvenn stylists. Become a stylist at welcome.mayvenn.com!"])])))
