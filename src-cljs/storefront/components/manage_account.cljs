(ns storefront.components.manage-account
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn manage-account-component [data owner]
  (om/component
   (html
    [:div#edit-account
     [:form
      {:on-submit (utils/enqueue-event data events/control-manage-account-submit)}
      [:label {:for "user-email"} "Email"]
      [:br]
      [:input.title#user-email
       (merge (utils/change-text data owner keypaths/manage-account-email)
              {:autofocus "autofocus"
               :type "email"
               :name "email"
               :value (if (empty? (get-in data keypaths/manage-account-email))
                        (get-in data keypaths/user-email)
                        (get-in data keypaths/manage-account-email))})]
      [:div#password-credentials
       [:p
        [:label {:for "user-password"} "Password"]
        [:br]
        [:input.title#user-password
         (merge (utils/change-text data owner keypaths/manage-account-password)
                {:type "password"
                 :name "password"})]]
       [:p
        [:label {:for "user-password-confirmation"} "Enter the same password"]
        [:br]
        [:input.title#user-password-confirmation
         (merge (utils/change-text data owner keypaths/manage-account-password-confirmation)
                {:type "password"
                 :name "password-confirmation"})]]]
      [:p.user-password-instructions "Leave blank to keep the same password."]
      [:p
       [:input.button.primary {:type "submit" :value "Update"}]]]])))
