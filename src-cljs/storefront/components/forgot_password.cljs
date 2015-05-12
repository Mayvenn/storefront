(ns storefront.components.forgot-password
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.state :as state]))

(defn forgot-password-component [data owner]
  (om/component
   (html
    [:div
     [:h2.header-bar-heading "Reset Your Forgotten Password"]
     [:div#forgot-password
      [:form.new_spree_user
       [:label {:for "spree_user_email"} "Enter your email:"]
       [:br]
       [:input {:type "email"}]
       [:p
        [:input.button.primary {:type "submit" :value "Reset my password"}]]]]])))
