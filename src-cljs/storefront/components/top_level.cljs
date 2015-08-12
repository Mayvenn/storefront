(ns storefront.components.top-level
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.keypaths :as keypaths]
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
            [storefront.components.sign-in :refer [sign-in-component requires-sign-in]]
            [storefront.components.sign-up :refer [sign-up-component]]
            [storefront.components.forgot-password :refer [forgot-password-component]]
            [storefront.components.reset-password :refer [reset-password-component]]
            [storefront.components.stylist.commissions :refer [stylist-commissions-component]]
            [storefront.components.stylist.bonus-credit :refer [stylist-bonus-credit-component]]
            [storefront.components.stylist.referrals :refer [stylist-referrals-component]]
            [storefront.components.stylist.manage-account :refer [stylist-manage-account-component]]
            [storefront.components.manage-account :refer [manage-account-component]]
            [storefront.components.cart :refer [cart-component]]
            [storefront.components.checkout-address :refer [checkout-address-component]]
            [storefront.components.checkout-delivery :refer [checkout-delivery-component]]
            [storefront.components.checkout-payment :refer [checkout-payment-component]]
            [storefront.components.checkout-confirmation :refer [checkout-confirmation-component]]
            [storefront.components.checkout-complete :refer [checkout-complete-component]]
            [storefront.components.promotion-banner :refer [promotion-banner-component]]
            [storefront.components.order :refer [order-component]]
            [storefront.components.my-orders :refer [my-orders-component]]))

(defn top-level-component [data owner]
  (om/component
   (html
    [:div
     (om/build slideout-nav-component data)
     [:div (cond
             (get-in data keypaths/menu-expanded)
             {:on-click (utils/send-event-callback
                         data
                         events/control-menu-collapse
                         {:keypath keypaths/menu-expanded})}

             (get-in data keypaths/account-menu-expanded)
             {:on-click (utils/send-event-callback
                         data
                         events/control-menu-collapse
                         {:keypath keypaths/account-menu-expanded})}

             (get-in data keypaths/shop-menu-expanded)
             {:on-click (utils/send-event-callback
                         data
                         events/control-menu-collapse
                         {:keypath keypaths/shop-menu-expanded})}

             :else {})
      [:div.page-wrap
       (om/build header-component data)
       (when-let [msg (get-in data keypaths/flash-success-message)]
         [:div.flash.success msg])
       (when-let [msg (get-in data keypaths/flash-failure-message)]
         [:div.flash.error msg])
       (om/build promotion-banner-component data)

       [:main {:role "main"}
        [:div.container

         (om/build
          (condp = (get-in data keypaths/navigation-event)
            events/navigate-home home-component
            events/navigate-cart cart-component
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
            events/navigate-stylist-manage-account stylist-manage-account-component
            events/navigate-manage-account (requires-sign-in data manage-account-component)
            events/navigate-checkout-address (requires-sign-in data checkout-address-component)
            events/navigate-checkout-delivery (requires-sign-in data checkout-delivery-component)
            events/navigate-checkout-payment (requires-sign-in data checkout-payment-component)
            events/navigate-checkout-confirmation (requires-sign-in data checkout-confirmation-component)
            events/navigate-order-complete (requires-sign-in data checkout-complete-component)
            events/navigate-order (requires-sign-in data order-component)
            events/navigate-my-orders (requires-sign-in data my-orders-component)
            home-component)
          data)]]]
      (om/build footer-component data)]])))
