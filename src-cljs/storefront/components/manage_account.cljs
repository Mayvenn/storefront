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
      {:on-submit (utils/enqueue-event data events/control-manage-account-submit)}
      [:label {:for "user-email"} "Email"]
      [:br]
      [:input.title#user-email
       (merge (utils/update-text data events/control-manage-account-change :email)
              {:autofocus "autofocus"
               :type "email"
               :value (if (empty? (get-in data state/manage-account-email-path))
                        (get-in data state/user-email-path)
                        (get-in data state/manage-account-email-path))})]
      [:div#password-credentials
       [:p
        [:label {:for "user-password"} "Password"]
        [:br]
        [:input.title#user-password
         (merge (utils/update-text data events/control-manage-account-change :password)
                {:type "password"
                 :value (get-in data state/manage-account-password-path)})]]
       [:p
        [:label {:for "user-password-confirmation"} "Enter the same password"]
        [:br]
        [:input.title#user-password-confirmation
         (merge (utils/update-text data events/control-manage-account-change :password-confirmation)
                {:type "password"
                 :value (get-in data state/manage-account-password-confirmation-path)})]]]
      [:p.user-password-instructions "Leave blank to keep the same password."]
      [:p
       [:input.button.primary {:type "submit" :value "Update"}]]]])))
