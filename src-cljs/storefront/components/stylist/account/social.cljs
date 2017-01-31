(ns storefront.components.stylist.account.social
  (:require [storefront.component :as component]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]))

(defn component [{:keys [focused
                         saving?
                         instagram-account
                         styleseat-account
                         field-errors]} owner opts]
  (component/create
   [:form {:on-submit
           (utils/send-event-callback events/control-stylist-account-social-submit)}

    [:h1.h3.light.my3.center.col.col-12.col-6-on-tb-dt "Connect your social"]

    [:div.col.col-12.col-6-on-tb-dt
     [:div.flex.col-12
      [:div.flex-none.mr2.mt1
       {:style {:width "2.5rem" :height "2.5rem"}
        :class (if (seq instagram-account) "fill-teal" "fill-dark-gray")}
       svg/instagram]
      [:div.flex-auto
       (ui/text-field {:data-test "account-instagram"
                       :errors    (get field-errors ["instagram_account"])
                       :id        "account-instagram"
                       :keypath   (conj keypaths/stylist-manage-account :instagram_account)
                       :focused   focused
                       :label     "Instagram"
                       :name      "account-instagram"
                       :type      "text"
                       :value     instagram-account})]]

     [:div.flex.col-12
      [:div.flex-none.mr2.mt1
       {:style {:width "2.5rem" :height "2.5rem"}
        :class (if (seq styleseat-account) "fill-teal" "fill-dark-gray")}
       svg/styleseat]
      [:div.flex-auto
       (ui/text-field {:data-test "account-styleseat"
                       :errors    (get field-errors ["styleseat_account"])
                       :id        "account-styleseat"
                       :keypath   (conj keypaths/stylist-manage-account :styleseat_account)
                       :focused   focused
                       :label     "StyleSeat"
                       :name      "account-styleseat"
                       :type      "text"
                       :value     styleseat-account})]]]

    [:div.my2.col-12.clearfix
     ui/nbsp
     [:div.border-light-gray.border-top.hide-on-mb.mb3]
     [:div.col-12.col-5-on-tb-dt.mx-auto
      (ui/submit-button "Update" {:spinning? saving?
                                  :data-test "account-form-submit"})]]]))

(defn query [data]
  {:saving?           (utils/requesting? data request-keys/update-stylist-account-social)
   :instagram-account (get-in data (conj keypaths/stylist-manage-account :instagram_account))
   :styleseat-account (get-in data (conj keypaths/stylist-manage-account :styleseat_account))
   :field-errors      (get-in data keypaths/field-errors)
   :focused           (get-in data keypaths/ui-focus)})
