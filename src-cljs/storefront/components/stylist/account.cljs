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

(defn component [{:keys [profile-picture-url available-credit]} owner opts]
  (component/create
   [:div.bg-pure-white.light-black.sans-serif
    [:div.p2.m-auto.overflow-hidden
     [:div.flex.justify-center.items-center.center
      [:div
       (edit-photo profile-picture-url)]

      [:div.ml3
       (store-credit available-credit)]]


     [:div.bg-white.mt3.mxn2 ;; Oppose margin from ui/container
      (component/build tabs/component {:selected-tab 1}
                       {:opts {:tab-refs ["profile" "password" "commission" "social"]
                               :labels   ["Profile" "Password" "Commission" "Social"]
                               :tabs     [0 1 2 3]}})]

     [:form
      [:.flex.flex-column.items-center.col-12
       [:h1.h2.light.col-12.my3.center "Update your info"]
       [:.flex.col-12
        [:.col-6 (ui/text-field "First Name"
                                keypaths/checkout-shipping-address-first-name
                                ""
                                {:autofocus "autofocus"
                                 :type      "text"
                                 :name      "shipping-first-name"
                                 :data-test "shipping-first-name"
                                 :id        "shipping-first-name"
                                 :class     "rounded-left"
                                 :required  true})]

        [:.col-6 (ui/text-field "Last Name"
                                keypaths/checkout-shipping-address-last-name
                                ""
                                {:type      "text"
                                 :name      "shipping-last-name"
                                 :id        "shipping-last-name"
                                 :data-test "shipping-last-name"
                                 :class     "rounded-right border-width-left-0"
                                 :required  true})]]

       (ui/text-field "Mobile Phone"
                      keypaths/checkout-shipping-address-phone
                      ""
                      {:type      "tel"
                       :name      "shipping-phone"
                       :id        "shipping-phone"
                       :data-test "shipping-phone"
                       :required  true})

       (ui/text-field "Email"
                      keypaths/checkout-guest-email
                      "a"
                      {:type      "email"
                       :name      "shipping-email"
                       :id        "shipping-email"
                       :data-test "shipping-email"
                       :required  true})

       [:.flex.flex-column.items-center.col-12
        (ui/text-field "Birthday"
                       keypaths/checkout-guest-email
                       "1"
                       {:type      "date"
                        :id        :shipping-state
                        :name      "shipping-state"
                        :data-test "shipping-state"
                        :required  true})]


       [:.my2.col-12
        (ui/submit-button "Update" {:spinning? false
                                    :data-test "address-form-submit"})]]]]]))

(defn query [data]
  {:profile-picture-url (get-in data
                                (conj keypaths/stylist-manage-account
                                      :profile_picture_url))
   :available-credit    (get-in data keypaths/user-total-available-store-credit)})

(defn built-component [data owner opts]
  (component/create (component/build component (query data) opts)))
