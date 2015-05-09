(ns storefront.components.sign-in
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.utils :as utils]
            [storefront.events :as events]))

(defn sign-in-component [data owner]
  (om/component
   (html
    [:div.centered-content
     [:h1.center "Sign In to Your Account"]
     [:p.center "Don't have an account? "
      [:a (utils/route-to data events/navigate-sign-up) "Register Here"]]
     [:div#existing-customer
      [:form.new_spree_user.simple_form
       [:div#password-credentials
        [:div.input.email
         [:label.email "Email"]
         [:input.string.email {:autofocus "autofocus" :type "email"}]]
        [:div.input.password
         [:label.password "Password"]
         [:input.string.password {:type "password"}]]]
       [:p
        [:input {:checked "checked" :type "checkbox"}]
        [:label "Remember me"]]
       [:a.forgot-password {:href "FIXME: forgot password"} "Forgot Password?"]
       [:p
        [:input.button.primary {:type="submit" :value "Login"}]]]]])))
