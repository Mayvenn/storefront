(ns storefront.components.top-level
  (:require #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            #?@(:cljs [[storefront.components.cart :as cart]
                       [storefront.components.checkout-address :as checkout-address]
                       [storefront.components.checkout-complete :as checkout-complete]
                       [storefront.components.checkout-confirmation :as checkout-confirmation]
                       [storefront.components.checkout-payment :as checkout-payment]
                       [storefront.components.checkout-sign-in :as checkout-sign-in :refer [requires-sign-in-or-guest]]
                       [storefront.components.account :as account]
                       [storefront.components.reset-password :as reset-password]
                       [storefront.components.stylist.dashboard :refer [stylist-dashboard-component]]
                       [storefront.components.stylist.referrals :as stylist.referrals]
                       [storefront.components.stylist.account :as stylist.account]
                       [storefront.components.friend-referrals :refer [friend-referrals-component]]
                       [storefront.components.popup :refer [popup-component]]])

            [storefront.accessors.experiments :as experiments]
            [storefront.components.ui :as ui]
            [storefront.components.header :as header]
            [storefront.components.footer :as footer]
            [storefront.components.flash :as flash]
            [storefront.components.home :refer [home-component]]
            [storefront.components.categories :refer [categories-page-component]]
            [storefront.components.category :refer [category-component]]
            [storefront.components.promotion-banner :refer [promotion-banner-component]]
            [storefront.components.slideout-nav :as slideout-nav]
            [storefront.components.thirty-day-guarantee :refer [thirty-day-guarantee-component]]
            [storefront.components.help :refer [help-component]]
            [storefront.components.sign-in :as sign-in :refer [redirect-getsat-component requires-sign-in]]
            [storefront.components.sign-up :as sign-up]
            [storefront.components.forgot-password :as forgot-password]
            [storefront.components.stylist-banner :as stylist-banner]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

#?(:cljs
   (defn getsat-top-level-component [data owner opts]
     (component/create
      [:.page-wrap
       [:.img-logo.bg-no-repeat.bg-center.bg-contain {:style {:height "45px"}}]
       (component/build flash/component (flash/query data) opts)
       [:main {:role "main"}
        [:div.legacy-container
         (component/build (requires-sign-in data redirect-getsat-component) data opts)]]])))


(defn top-level-component [data owner opts]
  (component/create
   (if (get-in data keypaths/get-satisfaction-login?)
     [:div #?(:cljs (component/build getsat-top-level-component data opts))]
     [:div
      (component/build stylist-banner/component (stylist-banner/query data) nil)
      (component/build promotion-banner-component data nil)
      #?(:cljs (popup-component data))
      [:div.page-wrap
       [:div.border-bottom.border-light-silver
        (header/built-component data)
        (slideout-nav/built-component data)]
       (component/build flash/component (flash/query data) opts)
       [:main {:role "main"}
        [:div.legacy-container
         (component/build
          (condp = (get-in data keypaths/navigation-event)
            #?@(:cljs
                [events/navigate-reset-password                 reset-password/built-component
                 events/navigate-stylist-dashboard-commissions  stylist-dashboard-component
                 events/navigate-stylist-dashboard-bonus-credit stylist-dashboard-component
                 events/navigate-stylist-dashboard-referrals    stylist-dashboard-component
                 events/navigate-stylist-account-profile        stylist.account/built-component
                 events/navigate-stylist-account-password       stylist.account/built-component
                 events/navigate-stylist-account-commission     stylist.account/built-component
                 events/navigate-stylist-account-social         stylist.account/built-component
                 events/navigate-account-manage                 (requires-sign-in data account/built-component)
                 events/navigate-account-referrals              (requires-sign-in data friend-referrals-component)
                 events/navigate-friend-referrals               friend-referrals-component
                 events/navigate-cart                           cart/built-component
                 events/navigate-checkout-sign-in               checkout-sign-in/built-component
                 events/navigate-checkout-address               (requires-sign-in-or-guest data checkout-address/built-component)
                 events/navigate-checkout-payment               (requires-sign-in-or-guest data checkout-payment/built-component)
                 events/navigate-checkout-confirmation          (requires-sign-in-or-guest data checkout-confirmation/built-component)
                 events/navigate-order-complete                 checkout-complete/built-component])

            events/navigate-home            home-component
            events/navigate-categories      categories-page-component
            events/navigate-category        category-component
            events/navigate-guarantee       thirty-day-guarantee-component
            events/navigate-help            help-component
            events/navigate-sign-in         sign-in/built-component
            events/navigate-sign-up         sign-up/built-component
            events/navigate-forgot-password forgot-password/built-component
            home-component)
          data opts)]]
       (component/build footer/footer-component (footer/footer-query data) opts)]])))
