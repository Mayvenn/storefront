(ns storefront.components.stylist.account
  (:require [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.component :as component]
            [clojure.string :refer [join capitalize]]
            [storefront.components.ui :as ui]
            [storefront.components.tabs :as tabs]
            [storefront.components.money-formatters :refer [as-money]]
            [storefront.components.facebook-messenger :as facebook]))

(defn edit-photo [profile-picture-url]
  [:label.navy
   [:div.mx-auto.circle.border-green.border.mb2.content-box {:style {:width "100px" :height "100px" :border-width "3px"}}
    [:div.circle.border-white.border.content-box {:style {:width "96px" :height "96px" :border-width "2px"}}
     (ui/circle-picture {:width "96px"} profile-picture-url)]]
    "Change Photo"
    [:input.hide
     (merge (utils/change-file events/control-stylist-profile-picture)
            {:name "mayvenn_stylist[profile_picture]" :type "file"})]])

(defn store-credit [available-credit]
  [:div.mb3
   [:div.medium.mb1 "Store Credit"]
   [:div.green.h0 (ui/big-money available-credit)]
   [:div.mb1 ui/nbsp]])

(defn component [{:keys [profile-picture-url
                         available-credit
                         address
                         user
                         birth-date]} owner opts]
  (component/create
   [:div.bg-pure-white.light-black.sans-serif
    [:div.p2.m-auto.overflow-hidden
     [:div.flex.justify-center.items-center.center
      [:div
       (edit-photo profile-picture-url)]

      [:div.ml3
       (store-credit available-credit)]]


     [:div.bg-white.mt3.mxn2 ;; Oppose margin from ui/container
      (component/build tabs/component {:selected-tab 0}
                       {:opts {:tab-refs ["profile" "password" "commission" "social"]
                               :labels   ["Profile" "Password" "Commission" "Social"]
                               :tabs     [0 1 2 3]}})]

     [:form {:on-submit
             (utils/send-event-callback events/control-stylist-manage-account-submit)}
      [:.flex.flex-column.items-center.col-12
       [:h1.h2.light.col-12.my3.center "Update your info"]
       [:.flex.col-12
        [:.col-6 (ui/text-field "First Name"
                                (conj keypaths/stylist-manage-account :address :firstname)
                                (:firstname address)
                                {:autofocus "autofocus"
                                 :type      "text"
                                 :name      "account-first-name"
                                 :data-test "account-first-name"
                                 :id        "account-first-name"
                                 :class     "rounded-left"
                                 :required  true})]

        [:.col-6 (ui/text-field "Last Name"
                                (conj keypaths/stylist-manage-account :address :lastname)
                                (:lastname address)
                                {:type      "text"
                                 :name      "account-last-name"
                                 :id        "account-last-name"
                                 :data-test "account-last-name"
                                 :class     "rounded-right border-width-left-0"
                                 :required  true})]]

       (ui/text-field "Mobile Phone"
                      (conj keypaths/stylist-manage-account :address :phone)
                      (:phone address)
                      {:type      "tel"
                       :name      "account-phone"
                       :id        "account-phone"
                       :data-test "account-phone"
                       :required  true})

       (ui/text-field "Email"
                      (conj keypaths/stylist-manage-account :user :email)
                      (:email user)
                      {:type      "email"
                       :name      "account-email"
                       :id        "account-email"
                       :data-test "account-email"
                       :required  true})

       [:.flex.flex-column.items-center.col-12
        (ui/text-field "Birthday"
                       (conj keypaths/stylist-manage-account :birth-date)
                       birth-date
                       {:type      "date"
                        :id        "account-birth-date"
                        :name      "account-birth-date"
                        :data-test "account-birth-date"
                        :required  true})]


       [:.my2.col-12
        (ui/submit-button "Update" {:spinning? false
                                    :data-test "account-form-submit"})]]]]]))

(defn query [data]
  {:profile-picture-url (get-in data (conj keypaths/stylist-manage-account :profile_picture_url))
   :address             (get-in data (conj keypaths/stylist-manage-account :address))
   :birth-date          (get-in data (conj keypaths/stylist-manage-account :birth-date))
   :user                (get-in data (conj keypaths/stylist-manage-account :user))
   :available-credit    (get-in data keypaths/user-total-available-store-credit)})

(defn built-component [data owner opts]
  (component/create (component/build component (query data) opts)))
