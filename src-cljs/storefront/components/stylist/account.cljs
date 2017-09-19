(ns storefront.components.stylist.account
  (:require [storefront.component :as component]
            [storefront.components.stylist.account.commission :as account.commission]
            [storefront.components.stylist.account.password :as account.password]
            [storefront.components.stylist.account.profile :as account.profile]
            [storefront.components.stylist.account.social :as account.social]
            [storefront.components.tabs :as tabs]
            [storefront.components.ui :as ui]
            [storefront.hooks.uploadcare :as uploadcare]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [storefront.accessors.experiments :as experiments]))

(defn uploadcare-photo [{:keys [status] :as portrait} saving?]
  [:a.navy
   (merge (utils/route-to events/navigate-stylist-account-portrait)
          {:data-test "change-photo-link"})
   (when saving?
     [:div.absolute
      (ui/large-spinner {:class "relative"
                         :style {:width "130px" :height "130px"
                                 :top   "-12px" :left   "-12px"}})])
   [:div.mx-auto.circle.border.mb2.content-box
    {:style {:width "100px" :height "100px" :border-width "3px"}
     :class (if saving? "border-light-gray" "border-teal")}
    [:div.circle.border-light-gray.border.content-box.border-width-2.overflow-hidden {:style {:width "96px" :height "96px"}}
     (ui/circle-picture {:width        "96px"
                         :overlay-copy (case status
                                         "pending" [:span.white.medium "Approval Pending"]
                                         "rejected" [:span.red.bold.h6 "Try a different image"]
                                         nil)}
                        (ui/square-image portrait 96))]]
   "Change Photo"])

(defn store-credit [available-credit]
  [:div.mb3
   [:div.medium.mb1 "Store Credit"]
   [:div.teal.h0 (when available-credit (ui/big-money available-credit))]
   [:div.mb1 ui/nbsp]])

(defn component [{:keys [fetching?
                         portrait-saving?
                         current-nav-event
                         portrait
                         available-credit
                         profile
                         password
                         commission
                         social
                         loaded-uploadcare?]} owner opts]
  (component/create
   [:div.bg-white.dark-gray
    [:div.container.p2.m-auto.overflow-hidden
     [:div.flex.justify-center.items-center.center
      [:div
       (when loaded-uploadcare?
         (uploadcare-photo portrait portrait-saving?))]

      [:div.ml3
       (store-credit available-credit)]]

     [:div.bg-light-gray.mt3.mxn2 ;; Oppose padding on page
      (component/build tabs/component {:selected-tab current-nav-event}
                       {:opts {:tab-refs ["profile" "password" "commission" "social"]
                               :labels   ["Profile" "Password" "Commission" "Social"]
                               :tabs     [events/navigate-stylist-account-profile
                                          events/navigate-stylist-account-password
                                          events/navigate-stylist-account-commission
                                          events/navigate-stylist-account-social]}})]

     (if fetching?
       [:div.my3.h2 ui/spinner]
       [:div.my3
        (condp = current-nav-event
          events/navigate-stylist-account-profile
          (component/build account.profile/component profile opts)

          events/navigate-stylist-account-password
          (component/build account.password/component password opts)

          events/navigate-stylist-account-commission
          (component/build account.commission/component commission opts)

          events/navigate-stylist-account-social
          (component/build account.social/component social opts)

          nil)])]]))

(defn query [data]
  {:fetching?           (utils/requesting? data request-keys/get-stylist-account)
   :portrait-saving?    (utils/requesting? data request-keys/update-stylist-account-portrait)
   :current-nav-event   (get-in data keypaths/navigation-event)
   :portrait            (get-in data (conj keypaths/stylist-manage-account :portrait))
   :available-credit    (get-in data keypaths/user-total-available-store-credit)
   :profile             (account.profile/query data)
   :password            (account.password/query data)
   :commission          (account.commission/query data)
   :social              (account.social/query data)
   :loaded-uploadcare?  (get-in data keypaths/loaded-uploadcare)})

(defn built-component [data opts]
  (component/build component (query data) opts))
