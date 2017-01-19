(ns storefront.components.top-level
  (:require #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            #?@(:cljs [[storefront.components.cart :as cart]
                       [storefront.components.checkout-sign-in :as checkout-sign-in]
                       [storefront.components.checkout-returning-or-guest :as checkout-returning-or-guest]
                       [storefront.components.checkout-address :as checkout-address]
                       [storefront.components.checkout-complete :as checkout-complete]
                       [storefront.components.checkout-confirmation :as checkout-confirmation]
                       [storefront.components.checkout-payment :as checkout-payment]
                       [storefront.components.shop-by-look :as shop-by-look]
                       [storefront.components.shop-by-look-details :as shop-by-look-details]
                       [storefront.components.account :as account]
                       [storefront.components.reset-password :as reset-password]
                       [storefront.components.stylist.dashboard :as stylist.dashboard]
                       [storefront.components.stylist.account :as stylist.account]
                       [storefront.components.friend-referrals :as friend-referrals]
                       [storefront.components.style-guide :as style-guide]
                       [storefront.components.popup :as popup]
                       [storefront.config :as config]])

            [storefront.accessors.experiments :as experiments]
            [storefront.components.ui :as ui]
            [storefront.components.header :as header]
            [storefront.components.footer :as footer]
            [storefront.components.flash :as flash]
            [storefront.components.home :as home]
            [storefront.components.categories :as categories]
            [storefront.components.category :as category]
            [storefront.components.promotion-banner :as promotion-banner]
            [storefront.components.slideout-nav :as slideout-nav]
            [storefront.components.content :as content]
            [storefront.components.sign-in :as sign-in]
            [storefront.components.sign-up :as sign-up]
            [storefront.components.shared-cart :as shared-cart]
            [storefront.components.forgot-password :as forgot-password]
            [storefront.components.stylist-banner :as stylist-banner]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn main-component [nav-event]
  (condp = nav-event
    #?@(:cljs
        [events/navigate-reset-password                 reset-password/built-component
         events/navigate-shop-by-look                   shop-by-look/built-component
         events/navigate-shop-by-look-details           shop-by-look-details/built-component
         events/navigate-stylist-dashboard-commissions  stylist.dashboard/built-component
         events/navigate-stylist-dashboard-bonus-credit stylist.dashboard/built-component
         events/navigate-stylist-dashboard-referrals    stylist.dashboard/built-component
         events/navigate-stylist-account-profile        stylist.account/built-component
         events/navigate-stylist-account-password       stylist.account/built-component
         events/navigate-stylist-account-commission     stylist.account/built-component
         events/navigate-stylist-account-social         stylist.account/built-component
         events/navigate-account-manage                 (partial sign-in/requires-sign-in account/built-component)
         events/navigate-account-referrals              (partial sign-in/requires-sign-in friend-referrals/built-component)
         events/navigate-friend-referrals               friend-referrals/built-component
         events/navigate-cart                           cart/built-component
         events/navigate-checkout-returning-or-guest    checkout-returning-or-guest/built-component
         events/navigate-checkout-sign-in               checkout-sign-in/built-component
         events/navigate-checkout-address               (partial checkout-returning-or-guest/requires-sign-in-or-guest checkout-address/built-component)
         events/navigate-checkout-payment               (partial checkout-returning-or-guest/requires-sign-in-or-guest checkout-payment/built-component)
         events/navigate-checkout-confirmation          (partial checkout-returning-or-guest/requires-sign-in-or-guest checkout-confirmation/built-component)
         events/navigate-order-complete                 checkout-complete/built-component])

    events/navigate-home              home/built-component
    events/navigate-categories        categories/built-component
    events/navigate-category          category/built-component
    events/navigate-shared-cart       shared-cart/built-component
    events/navigate-content-guarantee content/built-component
    events/navigate-content-help      content/built-component
    events/navigate-content-privacy   content/built-component
    events/navigate-content-tos       content/built-component
    events/navigate-content-about-us  content/built-component
    events/navigate-sign-in           sign-in/built-component
    events/navigate-sign-up           sign-up/built-component
    events/navigate-forgot-password   forgot-password/built-component
    home/built-component))

(defn top-level-component [data owner opts]
  (component/create
   (cond

     #?@(:cljs
         [(and config/enable-style-guide?
               (= events/navigate-style-guide
                  (subvec (get-in data keypaths/navigation-event) 0
                          (min (count (get-in data keypaths/navigation-event))
                               (count events/navigate-style-guide)))))
          [:div (style-guide/built-component data nil)]])

     :else
     [:div.flex.flex-column {:style {:min-height "100vh"}}
      (stylist-banner/built-component data nil)
      (promotion-banner/built-component data nil)
      #?(:cljs (popup/built-component data nil))
      [:header {:role "banner"}
       (header/built-component data nil)
       (slideout-nav/built-component data nil)]
      (flash/built-component data nil)
      [:main.bg-white.flex-auto {:role "main" :data-test (keypaths/->component-str (get-in data keypaths/navigation-event))}
       ((main-component (get-in data keypaths/navigation-event)) data nil)]
      [:footer {:role "contentinfo"}
       (footer/built-component data nil)]])))
