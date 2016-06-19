(ns storefront.components.top-level
  #?@(:cljs [(:require-macros [storefront.component-macros :as component])])
  (:require #?@(:clj [[storefront.component :as component]])
            #?@(:cljs [[storefront.components.cart :as cart]
                       [storefront.components.checkout-address :as checkout-address]
                       [storefront.components.checkout-complete :as checkout-complete]
                       [storefront.components.checkout-confirmation :as checkout-confirmation]
                       [storefront.components.checkout-payment :as checkout-payment]
                       [storefront.components.checkout-sign-in :as checkout-sign-in :refer [requires-sign-in-or-guest]]
                       [storefront.components.manage-account :refer [manage-account-component]]
                       [storefront.components.reset-password :as reset-password]
                       [storefront.components.categories :refer [categories-page-component]]
                       [storefront.components.bundle-builder :refer [category-component]]
                       [storefront.components.stylist.dashboard :refer [stylist-dashboard-component]]
                       [storefront.components.stylist.referrals :as stylist.referrals]
                       [storefront.components.stylist.manage-account :refer [stylist-manage-account-component]]
                       [storefront.components.forgot-password :as forgot-password]
                       [storefront.components.friend-referrals :refer [friend-referrals-component]]
                       [storefront.components.sign-in :as sign-in :refer [redirect-getsat-component requires-sign-in]]
                       [storefront.components.sign-up :as sign-up]
                       [storefront.components.popup :refer [popup-component]]])

            [storefront.components.header :as header]
            [storefront.components.footer :as footer]
            [storefront.components.home :refer [home-component]]
            [storefront.components.promotion-banner :refer [promotion-banner-component]]
            [storefront.components.slideout-nav :as slideout-nav]
            [storefront.components.thirty-day-guarantee :refer [thirty-day-guarantee-component]]
            [storefront.components.help :refer [help-component]]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn flash-component [{:keys [success failure]} _ _]
  (component/create
   [:div
    (when success [:div.flash.success success])
    (when failure [:div.flash.error failure])]))


#?(:cljs
   (defn getsat-top-level-component [data owner]
     (component/create
      [:.page-wrap
       [:.img-logo.bg-no-repeat.bg-center.bg-contain {:style {:height "45px"}}]
       (component/build flash-component
                        {:success (get-in data keypaths/flash-success-message)
                         :failure (get-in data keypaths/flash-failure-message)}
                        nil)
       [:main {:role "main"}
        [:div.legacy-container
         (component/build (requires-sign-in data redirect-getsat-component) data nil)]]])))

(defn top-level-component [data owner opts]
  (component/create
   (if (get-in data keypaths/get-satisfaction-login?)
     [:div #?(:cljs (component/build getsat-top-level-component data nil))]
     [:div
      (component/build promotion-banner-component data nil)
      #?(:cljs (popup-component data))
      [:div.page-wrap
       [:div.border-bottom.border-light-silver
        (header/built-component data)
        (slideout-nav/built-component data)]
       (component/build flash-component
                        {:success (get-in data keypaths/flash-success-message)
                         :failure (get-in data keypaths/flash-failure-message)}
                        nil)
       [:main {:role "main"}
        [:div.legacy-container
         (component/build
          (condp = (get-in data keypaths/navigation-event)
            #?@(:cljs
                [events/navigate-categories                     categories-page-component
                 events/navigate-category                       category-component
                 events/navigate-sign-in                        sign-in/built-component
                 events/navigate-sign-up                        sign-up/built-component
                 events/navigate-reset-password                 reset-password/built-component
                 events/navigate-stylist-dashboard-commissions  stylist-dashboard-component
                 events/navigate-stylist-dashboard-bonus-credit stylist-dashboard-component
                 events/navigate-stylist-dashboard-referrals    stylist-dashboard-component
                 events/navigate-stylist-manage-account         stylist-manage-account-component
                 events/navigate-account-manage                 (requires-sign-in data manage-account-component)
                 events/navigate-account-referrals              (requires-sign-in data friend-referrals-component)
                 events/navigate-friend-referrals               friend-referrals-component
                 events/navigate-cart                           cart/built-component
                 events/navigate-checkout-sign-in               checkout-sign-in/built-component
                 events/navigate-checkout-address               (requires-sign-in-or-guest data checkout-address/built-component)
                 events/navigate-checkout-payment               (requires-sign-in-or-guest data checkout-payment/built-component)
                 events/navigate-checkout-confirmation          (requires-sign-in-or-guest data checkout-confirmation/built-component)
                 events/navigate-order-complete                 checkout-complete/built-component])
            events/navigate-home                           home-component
            events/navigate-guarantee                      thirty-day-guarantee-component
            events/navigate-help                           help-component
            home-component)
          data nil)]]
       (component/build footer/footer-component (footer/footer-query data) nil)]])))
