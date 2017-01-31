(ns storefront.components.account
  (:require [storefront.component :as component]
            [storefront.request-keys :as request-keys]
            [storefront.components.ui :as ui]
            [storefront.components.money-formatters :refer [as-money]]
            [storefront.components.tabs :as tabs]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn store-credit [available-credit]
  [:div.my3
   [:div.medium.mb1 "Store Credit"]
   [:div.teal.h0 (ui/big-money available-credit)]])

(defn profile-component [{:keys [focused
                                 saving?
                                 email
                                 field-errors
                                 password
                                 show-password?]} owner opts]
  (component/create
   [:form {:on-submit
           (utils/send-event-callback events/control-account-profile-submit)}
    [:h1.h3.light.my3.center.col.col-12.col-6-on-tb-dt "Update your info"]

    [:div.col.col-12.col-6-on-tb-dt
     (ui/text-field {:data-test "account-email"
                     :errors    (get field-errors ["email"])
                     :id        "account-email"
                     :keypath   keypaths/manage-account-email
                     :focused   focused
                     :label     "Email"
                     :name      "account-email"
                     :required  true
                     :type      "email"
                     :value     email})

     (ui/text-field {:data-test "account-password"
                     :errors    (get field-errors ["password"])
                     :id        "account-password"
                     :keypath   keypaths/manage-account-password
                     :focused   focused
                     :label     "New Password"
                     :name      "account-password"
                     :value     password
                     :type      "password"
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

(defn profile-query [data]
  {:saving?        (utils/requesting? data request-keys/update-account-profile)
   :email          (get-in data keypaths/manage-account-email)
   :password       (get-in data keypaths/manage-account-password)
   :show-password? (get-in data keypaths/account-show-password? true)
   :field-errors   (get-in data keypaths/field-errors)
   :focused       (get-in data keypaths/ui-focus)})

(defn component [{:keys [current-nav-event
                         available-credit
                         fetching?
                         profile]} owner opts]
  (component/create
   [:div.container.bg-white.dark-gray
    [:div.p2.m-auto.overflow-hidden
     [:div.flex.justify-center.items-center.center
      [:div.ml3
       (when available-credit (store-credit available-credit))]]

     [:div.bg-light-gray.mt3.mxn2 ;; Oppose padding on page
      (component/build tabs/component {:selected-tab current-nav-event}
                       {:opts {:tab-refs ["profile"]
                               :labels   ["Profile"]
                               :tabs     [events/navigate-account-manage]}})]

     (if fetching?
       [:div.my2.h2 ui/spinner]
       [:div.my2
        (condp = current-nav-event
         events/navigate-account-manage
         (component/build profile-component profile opts)

         nil)])]]))

(defn query [data]
  {:fetching?         (utils/requesting? data request-keys/get-account)
   :current-nav-event (get-in data keypaths/navigation-event)
   :available-credit  (get-in data keypaths/user-total-available-store-credit)
   :profile           (profile-query data)})

(defn built-component [data opts]
  (component/build component (query data) opts))
