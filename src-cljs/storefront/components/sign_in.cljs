(ns storefront.components.sign-in
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn sign-in-component [data owner]
  (om/component
   (html
    [:div.centered-content
     [:h1.center "Sign In to Your Account"]
     [:p.center "Don't have an account? "
      [:a (utils/route-to data events/navigate-sign-up) "Register Here"]]
     [:div#existing-customer
      [:form.new_spree_user.simple_form
       {:on-submit (utils/enqueue-event data events/control-sign-in-submit)}
       [:div#password-credentials
        [:div.input.email
         [:label.email "Email"]
         [:input.string.email
          (merge (utils/change-text data keypaths/sign-in-email)
                 {:autofocus "autofocus"
                  :type "email"
                  :name "email"})]]
        [:div.input.password
         [:label.password "Password"]
         [:input.string.password
          (merge (utils/change-text data keypaths/sign-in-password)
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
                                :value "Login"}]]]]])))

(defn requires-sign-in [app-state authorized-component]
  (if (get-in app-state keypaths/user-id)
    authorized-component
    sign-in-component))
