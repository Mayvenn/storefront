(ns storefront.components.stylist.account.password
  (:require [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]))

(defn component [{:keys [focused
                         password
                         show-password?
                         field-errors
                         saving?]} owner opts]
  (component/create
   [:form {:on-submit
           (utils/send-event-callback events/control-stylist-account-password-submit)}
    [:h1.h3.light.my3.center.col.col-12.col-6-on-tb-dt "Update your password"]

    [:div.col.col-12.col-6-on-tb-dt
     (ui/text-field {:data-test "account-password"
                     :errors    (get field-errors ["user" "password"])
                     :id        "account-password"
                     :keypath   (conj keypaths/stylist-manage-account :user :password)
                     :focused   focused
                     :label     "New Password"
                     :name      "account-password"
                     :type      "password"
                     :value     password
                     :hint      (when show-password? password)})

     [:div.dark-gray.mtn2.mb2.col-12.left
      (ui/check-box {:label   "Show password"
                     :keypath keypaths/account-show-password?
                     :value   show-password?})]]

    [:div.my2.col-12.clearfix
     ui/nbsp
     [:div.border-light-gray.border-top.hide-on-mb.mb3]
     [:div.col-12.col-5-on-tb-dt.mx-auto
      (ui/submit-button "Update" {:spinning? saving?
                                  :data-test "account-form-submit"})]]]))

(defn query [data]
  {:saving?        (utils/requesting? data request-keys/update-stylist-account-password)
   :password       (get-in data (conj keypaths/stylist-manage-account :user :password))
   :show-password? (get-in data keypaths/account-show-password? true)
   :field-errors   (get-in data keypaths/field-errors)
   :focused        (get-in data keypaths/ui-focus)})
