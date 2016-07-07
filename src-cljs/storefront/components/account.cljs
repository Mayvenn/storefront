(ns storefront.components.account
  (:require [storefront.component :as component]
            [storefront.request-keys :as request-keys]
            [storefront.components.ui :as ui]
            [storefront.components.facebook-messenger :as facebook]
            [storefront.components.money-formatters :refer [as-money]]
            [storefront.components.tabs :as tabs]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn store-credit [available-credit]
  [:div.my3
   [:div.medium.mb1 "Store Credit"]
   [:div.green.h0 (ui/big-money available-credit)]])

(defn profile-component [{:keys [saving?
                                 errors
                                 email
                                 password
                                 confirmation]} owner opts]
  (component/create
   [:form {:on-submit
           (utils/send-event-callback events/control-account-profile-submit)}
    [:h1.h2.light.my3.center.col.col-12.md-up-col-6 "Update your info"]

    [:div.col.col-12.md-up-col-6
     (ui/text-field "Email"
                    keypaths/manage-account-email
                    email
                    {:type      "email"
                     :name      "account-email"
                     :id        "account-email"
                     :data-test "account-email"
                     :required  true})

     (ui/text-field "New Password"
                    keypaths/manage-account-password
                    password
                    {:type      "password"
                     :name      "account-password"
                     :id        "account-password"
                     :data-test "account-password"
                     :errors (get errors [:user :password])})

     (ui/text-field "Re-type New Password"
                    keypaths/manage-account-password-confirmation
                    confirmation
                    {:type      "password"
                     :name      "account-password-confirmation"
                     :id        "account-password-confirmation"
                     :data-test "account-password-confirmation"
                     :errors (get errors [:user :password-confirmation])})]

    [:div.my2.col-12.clearfix
     ui/nbsp
     [:div.border-dark-white.border-top.to-md-hide.mb3]
     [:div.col-12.md-up-col-5.mx-auto
      (ui/submit-button "Update" {:spinning? saving?
                                  :data-test "account-form-submit"})]]]))

(defn profile-query [data]
  {:saving?      (utils/requesting? data request-keys/update-account-profile)
   :email        (get-in data keypaths/manage-account-email)
   :password     (get-in data keypaths/manage-account-password)
   :confirmation (get-in data keypaths/manage-account-password-confirmation)
   :errors       (get-in data keypaths/errors)})

(defn component [{:keys [current-nav-event
                         available-credit
                         fetching?
                         profile]} owner opts]
  (component/create
   [:div.bg-pure-white.light-black.sans-serif
    [:div.p2.m-auto.overflow-hidden
     [:div.flex.justify-center.items-center.center
      [:div.ml3
       (store-credit available-credit)]]

     [:div.bg-white.mt3.mxn2 ;; Oppose padding on page
      (component/build tabs/component {:selected-tab current-nav-event}
                       {:opts {:tab-refs ["profile"]
                               :labels   ["Profile"]
                               :tabs     [events/navigate-account-manage]}})]

     (if fetching?
       [:div.my2.h1 ui/spinner]
       [:div.my2
        (condp = current-nav-event
         events/navigate-account-manage
         (component/build profile-component profile opts)


          nil)])]]
   #_[:div
    [:div#edit-account
     [:form
      {:on-submit (utils/send-event-callback events/control-manage-account-submit)}
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
                 :name "password-confirmation"})]]
       [:p.user-password-instructions "Leave blank to keep the same password."]]

      [:p
       [:input.button.primary {:type "submit" :value "Update"}]]]

     (when-let [available-credit (get-in data keypaths/user-total-available-store-credit)]
       [:fieldset
        [:legend {:align "center"} "Store Credit"]
        [:p.user-password-instructions "Available store credit is " (as-money available-credit)]])

     [:div.my2
      (component/build facebook/messenger-business-opt-in-component (facebook/query data) nil)]]]))

(defn query [data]
  {:fetching?         (utils/requesting? data request-keys/get-account)
   :current-nav-event (get-in data keypaths/navigation-event)
   :available-credit  (get-in data keypaths/user-total-available-store-credit)
   :profile           (profile-query data)})

(defn built-component [data owner opts]
  (component/create (component/build component (query data) opts)))
