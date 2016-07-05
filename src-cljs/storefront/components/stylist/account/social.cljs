(ns storefront.components.stylist.account.social
  (:require [storefront.component :as component]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]))

(defn component [{:keys [saving?
                         instagram-account
                         styleseat-account]} owner opts]
  (component/create
   [:form {:on-submit
           (utils/send-event-callback events/control-stylist-account-social-submit)}
    [:div.flex.flex-column.items-center.col-12
     [:h1.h2.light.col-12.my3.center "Update your password"]

     [:div.flex.col-12
      [:div.flex-none.mr2.mt1
       {:style {:width "2.5rem" :height "2.5rem"}
        :class (if (seq instagram-account) "fill-green" "fill-dark-gray")}
       svg/instagram]
      [:div.flex-auto
       (ui/text-field "Instagram"
                      (conj keypaths/stylist-manage-account :instagram_account)
                      instagram-account
                      {:type      "text"
                       :name      "account-instagram"
                       :id        "account-instagram"
                       :data-test "account-instagram"
                       :required  true})]]

     [:div.flex.col-12
      [:div.flex-none.mr2.mt1
       {:style {:width "2.5rem" :height "2.5rem"}
        :class (if (seq styleseat-account) "fill-green" "fill-dark-gray")}
       svg/styleseat]
      [:div.flex-auto
       (ui/text-field "StyleSeat"
                      (conj keypaths/stylist-manage-account :styleseat_account)
                      styleseat-account
                      {:type      "text"
                       :name      "account-styleseat"
                       :id        "account-styleseat"
                       :data-test "account-styleseat"
                       :required  true})]]

     [:div.my2.col-12
      (ui/submit-button "Update" {:spinning? saving?
                                  :data-test "account-form-submit"})]]]))

(defn query [data]
  {:saving?           (utils/requesting? data request-keys/update-stylist-account-social)
   :instagram-account (get-in data (conj keypaths/stylist-manage-account :instagram_account))
   :styleseat-account (get-in data (conj keypaths/stylist-manage-account :styleseat_account))})
