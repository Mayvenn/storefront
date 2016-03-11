(ns storefront.components.top-level
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.hooks.experiments :as experiments]
            [storefront.components.utils :as utils]
            [storefront.components.slideout-nav :refer [slideout-nav-component]]
            [storefront.components.header :refer [header-component]]
            [storefront.components.footer :refer [footer-component]]
            [storefront.components.home :refer [home-component]]
            [storefront.components.category :refer [category-component]]
            [storefront.components.categories :refer [categories-component]]
            [storefront.components.product :refer [product-component]]
            [storefront.components.thirty-day-guarantee :refer [thirty-day-guarantee-component]]
            [storefront.components.help :refer [help-component]]
            [storefront.components.sign-in :refer [sign-in-component requires-sign-in redirect-getsat-component]]
            [storefront.components.sign-up :refer [sign-up-component]]
            [storefront.components.forgot-password :refer [forgot-password-component]]
            [storefront.components.reset-password :refer [reset-password-component]]
            [storefront.components.stylist.commissions :refer [stylist-commissions-component]]
            [storefront.components.stylist.bonus-credit :refer [stylist-bonus-credit-component]]
            [storefront.components.stylist.referrals :refer [stylist-referrals-component]]
            [storefront.components.stylist.manage-account :refer [stylist-manage-account-component]]
            [storefront.components.manage-account :refer [manage-account-component]]
            [storefront.components.friend-referrals :refer [friend-referrals-component]]
            [storefront.components.cart :refer [cart-component]]
            [storefront.components.checkout-sign-in :refer [checkout-sign-in-component requires-sign-in-or-guest]]
            [storefront.components.checkout-address :refer [checkout-address-component]]
            [storefront.components.checkout-delivery :refer [checkout-delivery-component]]
            [storefront.components.checkout-payment :refer [checkout-payment-component]]
            [storefront.components.checkout-confirmation :refer [checkout-confirmation-component]]
            [storefront.components.checkout-complete :refer [checkout-complete-component]]
            [storefront.components.promotion-banner :refer [promotion-banner-component]]
            [storefront.components.order :refer [order-component]]))

(defn top-level-component [data owner]
  (om/component
   (html
    [:div
     (merge
      (if (get-in data keypaths/get-satisfaction-login?)
        {:class "community-login"}
        {})
      (cond
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

        :else {}))
     (when-not (get-in data keypaths/get-satisfaction-login?)
       (om/build promotion-banner-component data))
     [:div.page-wrap
      (om/build header-component data)
      (when-not (get-in data keypaths/get-satisfaction-login?)
        (om/build slideout-nav-component data))
      (when-let [msg (get-in data keypaths/flash-success-message)]
        [:div.flash.success msg])
      (when-let [msg (get-in data keypaths/flash-failure-message)]
        [:div.flash.error msg])
      [:main {:role "main"}
       [:div.legacy-container
        (let [requires-checkout-sign-in (if (experiments/guest-checkout? data)
                                          requires-sign-in-or-guest
                                          requires-sign-in)]
          (om/build
           (condp = (get-in data keypaths/navigation-event)
             events/navigate-home                   home-component
             events/navigate-cart                   cart-component
             events/navigate-categories             categories-component
             events/navigate-category               category-component
             events/navigate-product                product-component
             events/navigate-guarantee              thirty-day-guarantee-component
             events/navigate-help                   help-component
             events/navigate-sign-in                sign-in-component
             events/navigate-sign-in-getsat         (requires-sign-in data redirect-getsat-component)
             events/navigate-sign-up                sign-up-component
             events/navigate-forgot-password        forgot-password-component
             events/navigate-reset-password         reset-password-component
             events/navigate-stylist-commissions    stylist-commissions-component
             events/navigate-stylist-bonus-credit   stylist-bonus-credit-component
             events/navigate-stylist-referrals      stylist-referrals-component
             events/navigate-stylist-manage-account stylist-manage-account-component
             events/navigate-account-manage         (requires-sign-in data manage-account-component)
             events/navigate-account-referrals      (requires-sign-in data friend-referrals-component)
             events/navigate-friend-referrals       friend-referrals-component
             events/navigate-checkout-sign-in       checkout-sign-in-component
             events/navigate-checkout-address       (requires-checkout-sign-in data checkout-address-component)
             events/navigate-checkout-delivery      (requires-checkout-sign-in data checkout-delivery-component)
             events/navigate-checkout-payment       (requires-checkout-sign-in data checkout-payment-component)
             events/navigate-checkout-confirmation  (requires-checkout-sign-in data checkout-confirmation-component)
             events/navigate-order-complete         checkout-complete-component
             events/navigate-order                  (requires-sign-in data order-component)
             home-component)
           data))]]
      (when-not (get-in data keypaths/get-satisfaction-login?)
        (om/build footer-component data))]])))
