(ns storefront.components.sign-in
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.state :as state]))

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
          (merge (utils/update-text data events/control-sign-in-change :email)
                 {:autofocus "autofocus"
                  :type "email"
                  :value (get-in data state/sign-in-email-path)})]]
        [:div.input.password
         [:label.password "Password"]
         [:input.string.password
          (merge (utils/update-text data events/control-sign-in-change :password)
                 {:type "password"
                  :value (get-in data state/sign-in-password-path)})]]]
       [:p
        [:input#remember-me
         (merge (utils/update-checkbox data (get-in data state/sign-in-remember-path)
                                       events/control-sign-in-change :remember-me)
                {:type "checkbox"
                 :name "remember-me"})]
        [:label {:for "remember-me"} "Remember me"]]
       [:a.forgot-password (utils/route-to data events/navigate-forgot-password) "Forgot Password?"]
       [:p
        [:input.button.primary {:type "submit"
                                :value "Login"}]]]]])))
