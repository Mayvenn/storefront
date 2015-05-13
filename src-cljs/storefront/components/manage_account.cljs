(ns storefront.components.manage-account
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.state :as state]))

(defn manage-account-component [data owner]
  (om/component
   (html
    [:div#edit-account
     [:form
      [:label {:for "user-email"} "Email"]
      [:br]
      [:input.title#user-email {:type "email" :value ""}]
      [:div#password-credentials
       [:p
        [:label {:for "user-password"} "Password"]
        [:br]
        [:input.title#user-password {:type "password" :value ""}]]
       [:p
        [:label {:for "user-password-confirmation"} "Enter the same password"]
        [:br]
        [:input.title#user-password-confirmation {:type "password" :value ""}]]]
      [:p.user-password-instructions "Leave blank to keep the same password."]
      [:p
       [:input.button.primary {:type "submit" :value "Update"}]]]])))
