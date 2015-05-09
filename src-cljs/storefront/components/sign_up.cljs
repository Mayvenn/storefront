(ns storefront.components.sign-up
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.utils :as utils]
            [storefront.events :as events]))

(defn sign-up-component [data owner]
  (om/component
   (html
    [:div.centered-content
     [:h1.center "Sign Up For An Account"]
     [:p.center "Already have an account? "
      [:a (utils/route-to data events/navigate-sign-in) "Log In"]]
     [:div#existing-customer
      [:form.new_spree_user.simple_form
       [:div#password-credentials
        [:div.input.email
         [:label.email "Email"]
         [:input.string.email {:autofocus "autofocus" :type "email"}]]
        [:div.input.password
         [:label.password "Password"]
         [:input.string.password {:type "password"}]]
        [:div.input.password
         [:label.password "Password Confirmation"]
         [:input.string.password {:type "password"}]]]
       [:p
        [:input.btn.button.primary {:type="submit" :value "Create"}]]]]])))
