(ns storefront.components.stylist.account.password
  (:require [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]))

(defn component [{:keys [password
                         confirmation
                         field-errors
                         saving?]} owner opts]
  (component/create
   [:form {:on-submit
           (utils/send-event-callback events/control-stylist-account-password-submit)}
    [:h2.h3.light.my3.center.col.col-12.md-up-col-6 "Update your password"]

    [:div.col.col-12.md-up-col-6
     (ui/text-field {:data-test "account-password"
                     :errors    (get field-errors ["user" "password"])
                     :id        "account-password"
                     :keypath   (conj keypaths/stylist-manage-account :user :password)
                     :label     "New Password"
                     :name      "account-password"
                     :type      "password"
                     :value     password})

     (ui/text-field {:data-test "account-password-confirmation"
                     :errors    (get field-errors ["user" "password-confirmation"])
                     :id        "account-password-confirmation"
                     :keypath   (conj keypaths/stylist-manage-account :user :password-confirmation)
                     :label     "Re-type New Password"
                     :name      "account-password-confirmation"
                     :value     confirmation
                     :type      "password"})]

    [:div.my2.col-12.clearfix
     ui/nbsp
     [:div.border-silver.border-top.to-md-hide.mb3]
     [:div.col-12.md-up-col-5.mx-auto
      (ui/submit-button "Update" {:spinning? saving?
                                  :data-test "account-form-submit"})]]]))

(defn query [data]
  {:saving?      (utils/requesting? data request-keys/update-stylist-account-password)
   :password     (get-in data (conj keypaths/stylist-manage-account :user :password))
   :confirmation (get-in data (conj keypaths/stylist-manage-account :user :password-confirmation))
   :field-errors (get-in data keypaths/field-errors)})
