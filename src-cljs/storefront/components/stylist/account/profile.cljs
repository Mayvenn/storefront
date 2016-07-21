(ns storefront.components.stylist.account.profile
  (:require [storefront.component :as component]
            [storefront.components.facebook-messenger :as facebook]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]))

(defn component [{:keys [saving?
                         address
                         user
                         field-errors
                         birth-date
                         facebook]} owner opts]
  (component/create
   [:form {:on-submit
           (utils/send-event-callback events/control-stylist-account-profile-submit)}
    [:div.col-12
     [:h1.h2.light.my3.center.col.col-12.md-up-col-6 "Update your info"]
     [:div.col.col-12.md-up-col-6
      [:div.flex.col-12.col
       [:div.col-6 (ui/text-field "First Name"
                                  (conj keypaths/stylist-manage-account :address :firstname)
                                  (:firstname address)
                                  {:autofocus "autofocus"
                                   :type      "text"
                                   :name      "account-first-name"
                                   :data-test "account-first-name"
                                   :errors    (get field-errors ["address" "firstname"])
                                   :id        "account-first-name"
                                   :class     "rounded-left"
                                   :required  true})]

       [:div.col-6 (ui/text-field "Last Name"
                                  (conj keypaths/stylist-manage-account :address :lastname)
                                  (:lastname address)
                                  {:type      "text"
                                   :name      "account-last-name"
                                   :id        "account-last-name"
                                   :data-test "account-last-name"
                                   :errors    (get field-errors ["address" "lastname"])
                                   :class     "rounded-right border-width-left-0"
                                   :required  true})]]

      [:div.clearfix]

      (ui/text-field "Mobile Phone"
                     (conj keypaths/stylist-manage-account :address :phone)
                     (:phone address)
                     {:type      "tel"
                      :name      "account-phone"
                      :id        "account-phone"
                      :data-test "account-phone"
                      :errors    (get field-errors ["address" "phone"])
                      :required  true})

      (ui/text-field "Email"
                     (conj keypaths/stylist-manage-account :user :email)
                     (:email user)
                     {:type      "email"
                      :name      "account-email"
                      :id        "account-email"
                      :data-test "account-email"
                      :errors    (get field-errors ["user" "email"])
                      :required  true})

      [:div.flex.flex-column.items-center.col-12
       (ui/text-field "Birthday"
                      (conj keypaths/stylist-manage-account :birth-date)
                      birth-date
                      {:type      "date"
                       :id        "account-birth-date"
                       :name      "account-birth-date"
                       :data-test "account-birth-date"
                       :errors    (get field-errors ["birth_date"])
                       :required  true})]]

     [:div.my2.col-12.clearfix
      ui/nbsp
      [:div.border-dark-white.border-top.to-md-hide.mb3]
      [:div.col-12.md-up-col-5.mx-auto
       (ui/submit-button "Update" {:spinning? saving?
                                   :data-test "account-form-submit"})]]

     [:div.my3
      (component/build facebook/messenger-business-opt-in-component facebook nil)]]]))

(defn query [data]
  {:saving?      (utils/requesting? data request-keys/update-stylist-account-profile)
   :address      (get-in data (conj keypaths/stylist-manage-account :address))
   :birth-date   (get-in data (conj keypaths/stylist-manage-account :birth-date))
   :user         (get-in data (conj keypaths/stylist-manage-account :user))
   :field-errors (get-in data keypaths/field-errors)
   :facebook     (facebook/query data)})
