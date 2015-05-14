(ns storefront.components.top-level
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.state :as state]
            [storefront.events :as events]
            [storefront.components.utils :as utils]
            [storefront.components.slideout-nav :refer [slideout-nav-component]]
            [storefront.components.header :refer [header-component]]
            [storefront.components.footer :refer [footer-component]]
            [storefront.components.home :refer [home-component]]
            [storefront.components.category :refer [category-component]]
            [storefront.components.product :refer [product-component]]
            [storefront.components.thirty-day-guarantee :refer [thirty-day-guarantee-component]]
            [storefront.components.help :refer [help-component]]
            [storefront.components.privacy :refer [privacy-component]]
            [storefront.components.tos :refer [tos-component]]
            [storefront.components.sign-in :refer [sign-in-component]]
            [storefront.components.sign-up :refer [sign-up-component]]
            [storefront.components.forgot-password :refer [forgot-password-component]]
            [storefront.components.reset-password :refer [reset-password-component]]
            [storefront.components.stylist.commissions :refer [stylist-commissions-component]]
            [storefront.components.stylist.bonus-credit :refer [stylist-bonus-credit-component]]
            [storefront.components.stylist.referrals :refer [stylist-referrals-component]]
            [storefront.components.manage-account :refer [manage-account-component]]
            [cljs.core.async :refer [put!]]))

(defn top-level-component [data owner]
  (om/component
   (html
    [:div
     (om/build slideout-nav-component data)
     [:div (cond
             (get-in data state/menu-expanded-path)
             {:on-click (utils/enqueue-event data events/control-menu-collapse)}

             (get-in data state/account-menu-expanded-path)
             {:on-click (utils/enqueue-event data events/control-account-menu-collapse)}

             :else {})
      (when-let [msg (get-in data state/flash-success-message-path)]
        [:div.flash.success msg])
      [:div.page-wrap
       (om/build header-component data)
       [:main {:role "main"}
        [:div.container
         (om/build
          (condp = (get-in data state/navigation-event-path)
            events/navigate-home home-component
            events/navigate-category category-component
            events/navigate-product product-component
            events/navigate-guarantee thirty-day-guarantee-component
            events/navigate-help help-component
            events/navigate-privacy privacy-component
            events/navigate-tos tos-component
            events/navigate-sign-in sign-in-component
            events/navigate-sign-up sign-up-component
            events/navigate-forgot-password forgot-password-component
            events/navigate-reset-password reset-password-component
            events/navigate-stylist-commissions stylist-commissions-component
            events/navigate-stylist-bonus-credit stylist-bonus-credit-component
            events/navigate-stylist-referrals stylist-referrals-component
            events/navigate-manage-account manage-account-component)
          data)]]]
      (om/build footer-component data)]])))
