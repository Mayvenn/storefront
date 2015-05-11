(ns storefront.components.sign-up
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.state :as state]))

(defn sign-up-component [data owner]
  (om/component
   (html
    [:div.centered-content
     [:h1.center "Sign Up For An Account"]
     [:p.center "Already have an account? "
      [:a (utils/route-to data events/navigate-sign-in) "Log In"]]
     [:div#existing-customer
      [:form.new_spree_user.simple_form
       {:on-submit (utils/enqueue-event data events/control-sign-up-submit)}
       [:div#password-credentials
        [:div.input.email
         [:label.email "Email"]
         [:input.string.email
          (merge (utils/update-text data events/control-sign-up-change :email)
                 {:autofocus "autofocus"
                  :type "email"
                  :value (get-in data state/sign-up-email-path)})]]
        [:div.input.password
         [:label.password "Password"]
         [:input.string.password
          (merge (utils/update-text data events/control-sign-up-change :password)
                 {:type "password"
                  :value (get-in data state/sign-up-password-path)})]]
        [:div.input.password
         [:label.password "Password Confirmation"]
         [:input.string.password
          (merge (utils/update-text data events/control-sign-up-change :password-confirmation)
                 {:type "password"
                  :value (get-in data state/sign-up-password-confirmation-path)})]]]
       [:p
        [:input.btn.button.primary {:type "submit"
                                    :value "Create"}]]]]])))
