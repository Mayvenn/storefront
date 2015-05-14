(ns storefront.components.reset-password
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.state :as state]))

(defn reset-password-component [data owner]
  (om/component
   (html
    [:div
     [:h2.header-bar-heading "Update Your Password"]
     [:div#change-password
      [:form.new_spree_user
       {:on-submit (utils/enqueue-event data events/control-reset-password-submit)}
       [:p
        [:label {:for "spree_user_password"} "Password"]
        [:br]
        [:input#spree_user_password
         (merge
          (utils/update-text data events/control-reset-password-change :password)
          {:type "password"
           :name "password"
           :value (get-in data state/reset-password-password-path)})]]
       [:p
        [:label {:for "spree_user_email"} "Password Confirmation"]
        [:br]
        [:input#spree_user_email
         (merge
          (utils/update-text data events/control-reset-password-change :password-confirmation)
          {:type "password"
           :name "password-confirmation"
           :value (get-in data state/reset-password-password-confirmation-path)})]]
       [:p
        [:input.button.primary {:type "submit" :value "Update"}]]]]])))
