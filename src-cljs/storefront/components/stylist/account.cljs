(ns storefront.components.stylist.account
  (:require [storefront.component :as component]
            [storefront.components.stylist.account.social :as account.social]
            [storefront.components.stylist.account.commission :as account.commission]
            [storefront.components.stylist.account.password :as account.password]
            [storefront.components.stylist.account.profile :as account.profile]
            [storefront.components.tabs :as tabs]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

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

(defn component [{:keys [current-nav-event
                         profile-picture-url
                         available-credit
                         profile
                         password
                         commission
                         social]} owner opts]
  (component/create
   [:div.bg-pure-white.light-black.sans-serif
    [:div.p2.m-auto.overflow-hidden
     [:div.flex.justify-center.items-center.center
      [:div
       (edit-photo profile-picture-url)]

      [:div.ml3
       (store-credit available-credit)]]


     [:div.bg-white.mt3.mxn2 ;; Oppose padding on page
      (component/build tabs/component {:selected-tab current-nav-event}
                       {:opts {:tab-refs ["profile" "password" "commission" "social"]
                               :labels   ["Profile" "Password" "Commission" "Social"]
                               :tabs     [events/navigate-stylist-account-profile
                                          events/navigate-stylist-account-password
                                          events/navigate-stylist-account-commission
                                          events/navigate-stylist-account-social]}})]

     (condp = current-nav-event
       events/navigate-stylist-account-profile
       (component/build account.profile/component profile opts)

       events/navigate-stylist-account-password
       (component/build account.password/component password opts)

       events/navigate-stylist-account-commission
       (component/build account.commission/component commission opts)

       events/navigate-stylist-account-social
       (component/build account.social/component social opts)

       nil)]]))

(defn query [data]
  {:current-nav-event   (get-in data keypaths/navigation-event)
   :profile-picture-url (get-in data (conj keypaths/stylist-manage-account :profile_picture_url))
   :available-credit    (get-in data keypaths/user-total-available-store-credit)
   :profile             (account.profile/query data)
   :password            (account.password/query data)
   :commission          (account.commission/query data)
   :social              (account.social/query data)})

(defn built-component [data owner opts]
  (component/create (component/build component (query data) opts)))
