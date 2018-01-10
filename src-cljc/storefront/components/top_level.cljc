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
                       [storefront.components.shop-bundle-deals :as shop-bundle-deals]
                       [storefront.components.shop-by-look :as shop-by-look]
                       [storefront.components.shop-by-look-details :as shop-by-look-details]
                       [storefront.components.stylist.commission-details :as commission-details]
                       [storefront.components.account :as account]
                       [storefront.components.reset-password :as reset-password]
                       [storefront.components.stylist.dashboard :as stylist.dashboard]
                       [storefront.components.stylist.share-your-store :as stylist.share-your-store]
                       [storefront.components.stylist.account :as stylist.account]
                       [storefront.components.stylist.portrait :as stylist.portrait]
                       [storefront.components.stylist.gallery-image-picker :as gallery-image-picker]
                       [storefront.components.friend-referrals :as friend-referrals]
                       [storefront.components.style-guide :as style-guide]
                       [storefront.components.popup :as popup]
                       [storefront.config :as config]])

            [catalog.category :as category]
            [catalog.product-details :as product-details]
            [storefront.components.content :as content]
            [storefront.components.flash :as flash]
            [storefront.components.footer :as footer]
            [storefront.components.forgot-password :as forgot-password]
            [storefront.components.gallery :as gallery]
            [storefront.components.header :as header]
            [storefront.components.home :as home]
            [storefront.components.promotion-banner :as promotion-banner]
            [storefront.components.shared-cart :as shared-cart]
            [storefront.components.sign-in :as sign-in]
            [storefront.components.sign-up :as sign-up]
            [storefront.components.slideout-nav :as slideout-nav]
            [storefront.components.stylist-banner :as stylist-banner]
            ;; TODO Maybe we should change leads namespaces to be somehting like
            ;; leads.components.home
            [leads.home :as leads.home]
            [leads.registration :as leads.registration]
            [leads.registration-resolve :as leads.registration-resolve]
            [leads.resolve :as leads.resolve]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.routes :as routes]))

(defn main-component [nav-event]
  (condp = nav-event
    #?@(:cljs
        [events/navigate-reset-password                       reset-password/built-component
         events/navigate-shop-bundle-deals                    shop-bundle-deals/built-component
         events/navigate-shop-by-look                         shop-by-look/built-component
         events/navigate-shop-by-look-details                 shop-by-look-details/built-component
         events/navigate-stylist-dashboard-earnings           stylist.dashboard/built-component
         events/navigate-stylist-dashboard-bonus-credit       stylist.dashboard/built-component
         events/navigate-stylist-dashboard-referrals          stylist.dashboard/built-component
         events/navigate-stylist-share-your-store             stylist.share-your-store/built-component
         events/navigate-stylist-account-profile              stylist.account/built-component
         events/navigate-stylist-account-portrait             stylist.portrait/built-component
         events/navigate-stylist-account-password             stylist.account/built-component
         events/navigate-stylist-account-commission           stylist.account/built-component
         events/navigate-stylist-dashboard-commission-details commission-details/built-component
         events/navigate-stylist-account-social               stylist.account/built-component
         events/navigate-gallery-image-picker                 gallery-image-picker/built-component
         events/navigate-account-manage                       (partial sign-in/requires-sign-in account/built-component)
         events/navigate-account-referrals                    (partial sign-in/requires-sign-in friend-referrals/built-component)
         events/navigate-friend-referrals                     friend-referrals/built-component
         events/navigate-cart                                 cart/built-component
         events/navigate-checkout-returning-or-guest          checkout-returning-or-guest/built-component
         events/navigate-checkout-sign-in                     checkout-sign-in/built-component
         events/navigate-checkout-address                     (partial checkout-returning-or-guest/requires-sign-in-or-initiated-guest-checkout checkout-address/built-component)
         events/navigate-checkout-payment                     (partial checkout-returning-or-guest/requires-sign-in-or-initiated-guest-checkout checkout-payment/built-component)
         events/navigate-checkout-confirmation                (partial checkout-returning-or-guest/requires-sign-in-or-initiated-guest-checkout checkout-confirmation/built-component)
         events/navigate-order-complete                       checkout-complete/built-component])

    events/navigate-home                    home/built-component
    events/navigate-category                category/built-component
    events/navigate-product-details         product-details/built-component
    events/navigate-shared-cart             shared-cart/built-component
    events/navigate-content-guarantee       content/built-component
    events/navigate-content-help            content/built-component
    events/navigate-content-privacy         content/built-component
    events/navigate-content-tos             content/built-component
    events/navigate-content-about-us        content/built-component
    events/navigate-content-ugc-usage-terms content/built-component
    events/navigate-content-program-terms   content/built-component
    events/navigate-content-our-hair        content/built-component
    events/navigate-sign-in                 sign-in/built-component
    events/navigate-sign-up                 sign-up/built-component
    events/navigate-forgot-password         forgot-password/built-component
    events/navigate-gallery                 gallery/built-component
    home/built-component))

(defn leads-component [nav-event]
  (condp = nav-event
    #?@(:cljs [])
    events/navigate-leads-home                 leads.home/built-component
    events/navigate-leads-resolve              leads.resolve/built-component
    events/navigate-leads-registration-details leads.registration/built-component
    events/navigate-leads-registration-resolve leads.registration-resolve/built-component
    home/built-component))

(defn top-level-component [data owner opts]
  (let [nav-event (get-in data keypaths/navigation-event)]
    (component/create
     (cond
       #?@(:cljs
           [(and config/enable-style-guide?
                 (= events/navigate-style-guide
                    (->> nav-event
                         (take (count events/navigate-style-guide))
                         vec)))
            [:div (style-guide/built-component data nil)]])

       (get-in data keypaths/menu-expanded)
       (slideout-nav/built-component data nil)

       (routes/sub-page? [nav-event] [events/navigate-leads])
       [:div {:data-test (keypaths/->component-str nav-event)}
        ((leads-component nav-event) data nil)]

       :else
       [:div.flex.flex-column {:style {:min-height "100vh"}}
        (stylist-banner/built-component data nil)
        (promotion-banner/built-component data nil)
        #?(:cljs (popup/built-component data nil))
        [:header (header/built-component data nil)]
        (flash/built-component data nil)
        [:main.bg-white.flex-auto {:data-test (keypaths/->component-str nav-event)}
         ((main-component nav-event) data nil)]
        [:footer
         (footer/built-component data nil)]]))))
