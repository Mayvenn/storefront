(ns storefront.components.stylist.account.profile
  (:require [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]))

(defn component [{:keys [focused
                         saving?
                         address
                         user
                         field-errors
                         birth-date]} owner opts]
  (component/create
   [:form {:on-submit
           (utils/send-event-callback events/control-stylist-account-profile-submit)}
    [:div.col-12
     [:h1.h3.light.my3.center.col.col-12.col-6-on-tb-dt "Update your info"]
     [:div.col.col-12.col-6-on-tb-dt
      [:div.col-12.col
       (ui/text-field-group
        {:label      "First Name"
         :keypath    (conj keypaths/stylist-manage-account :address :firstname)
         :focused    focused
         :value      (:firstname address)
         :errors     (get field-errors ["address" "firstname"])
         :type       "text"
         :name       "account-first-name"
         :data-test  "account-first-name"
         :id         "account-first-name"
         :required   true}

        {:type      "text"
         :label     "Last Name"
         :keypath   (conj keypaths/stylist-manage-account :address :lastname)
         :focused   focused
         :value     (:lastname address)
         :errors    (get field-errors ["address" "lastname"])
         :name      "account-last-name"
         :id        "account-last-name"
         :data-test "account-last-name"
         :required  true})]

      [:div.clearfix]

      (ui/text-field {:data-test "account-phone"
                      :errors    (get field-errors ["address" "phone"])
                      :id        "account-phone"
                      :keypath   (conj keypaths/stylist-manage-account :address :phone)
                      :focused   focused
                      :label     "Mobile Phone"
                      :name      "account-phone"
                      :required  true
                      :type      "tel"
                      :value     (:phone address)})

      (ui/text-field {:data-test "account-email"
                      :errors    (get field-errors ["user" "email"])
                      :id        "account-email"
                      :keypath   (conj keypaths/stylist-manage-account :user :email)
                      :focused   focused
                      :label     "Email"
                      :name      "account-email"
                      :required  true
                      :type      "email"
                      :value     (:email user)})

      [:div.flex.flex-column.items-center.col-12
       (ui/text-field {:data-test "account-birth-date"
                       :errors    (get field-errors ["birth_date"])
                       :id        "account-birth-date"
                       :keypath   (conj keypaths/stylist-manage-account :birth-date)
                       :focused   focused
                       :label     "Birthday"
                       :name      "account-birth-date"
                       :required  true
                       :type      "date"
                       :value     birth-date})]]

     [:div.my2.col-12.clearfix
      ui/nbsp
      [:div.border-light-gray.border-top.hide-on-mb.mb3]
      [:div.col-12.col-5-on-tb-dt.mx-auto
       (ui/submit-button "Update" {:spinning? saving?
                                   :data-test "account-form-submit"})]]]]))

(defn query [data]
  {:saving?      (utils/requesting? data request-keys/update-stylist-account-profile)
   :address      (get-in data (conj keypaths/stylist-manage-account :address))
   :birth-date   (get-in data (conj keypaths/stylist-manage-account :birth-date))
   :user         (get-in data (conj keypaths/stylist-manage-account :user))
   :field-errors (get-in data keypaths/field-errors)
   :focused      (get-in data keypaths/ui-focus)})
