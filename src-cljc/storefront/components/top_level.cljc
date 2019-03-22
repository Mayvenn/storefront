(ns storefront.components.top-level
  (:require [storefront.component :as component]
            #?@(:cljs [[checkout.confirmation :as checkout-confirmation]
                       [storefront.components.account :as account]
                       [storefront.components.checkout-address :as checkout-address]
                       [storefront.components.checkout-complete :as checkout-complete]
                       [storefront.components.checkout-payment :as checkout-payment]
                       [storefront.components.checkout-returning-or-guest :as checkout-returning-or-guest]
                       [storefront.components.checkout-sign-in :as checkout-sign-in]
                       [storefront.components.force-set-password :as force-set-password]
                       [storefront.components.friend-referrals :as friend-referrals]
                       [storefront.components.popup :as popup]
                       [storefront.components.reset-password :as reset-password]
                       [storefront.components.shop-by-look :as shop-by-look]
                       [storefront.components.shop-by-look-details :as shop-by-look-details]
                       [storefront.components.style-guide :as style-guide]
                       [storefront.components.stylist.account :as stylist.account]
                       [storefront.components.stylist.balance-transfer-details :as balance-transfer-details]
                       [storefront.components.stylist.cash-out :as stylist.cash-out]
                       [storefront.components.stylist.cash-out-pending :as stylist.cash-out-pending]
                       [storefront.components.stylist.cash-out-success :as stylist.cash-out-success]
                       [storefront.components.stylist.gallery-image-picker :as gallery-image-picker]
                       [storefront.components.stylist.order-details :as stylist.order-details]
                       [storefront.components.stylist.portrait :as stylist.portrait]
                       [storefront.components.stylist.share-your-store :as stylist.share-your-store]
                       [storefront.components.stylist.v2-dashboard :as v2.dashboard]
                       [storefront.config :as config]
                       [storefront.history :as history]
                       adventure.components.email-capture
                       adventure.components.program-details-popup
                       storefront.components.email-capture
                       storefront.components.share-your-cart
                       storefront.components.v2-homepage-popup])
            [adventure.home :as adventure.home]
            [adventure.what-next :as adventure.what-next]
            [adventure.match-stylist :as adventure.match-stylist]
            [adventure.find-your-stylist :as adventure.find-your-stylist]
            [adventure.how-far :as adventure.how-far]
            [adventure.matching-stylist-wait :as adventure.matching-stylist-wait]
            [adventure.shop-hair :as adventure.shop-hair]
            [adventure.how-shop-hair :as adventure.how-shop-hair]
            [adventure.hair-texture :as adventure.hair-texture]
            [adventure.bundlesets.hair-texture :as adventure.bundlesets.hair-texture]
            adventure.a-la-carte.hair-texture
            adventure.a-la-carte.hair-color
            adventure.a-la-carte.product-list
            adventure.a-la-carte.product-details
            [adventure.install-type :as adventure.install-type]
            [adventure.select-new-look :as adventure.select-new-look]
            [adventure.look-detail :as adventure.look-detail]
            [adventure.stylist-results :as adventure.stylist-results]
            [adventure.checkout.cart :as adventure-cart]
            [adventure.out-of-area :as adventure.out-of-area]
            [adventure.match-success :as adventure.match-success]
            [catalog.category :as category]
            [catalog.product-details :as product-details]
            [checkout.cart :as cart]
            [checkout.processing :as checkout-processing]
            [voucher.redeem :as voucher-redeem]
            [voucher.redeemed :as voucher-redeemed]
            [mayvenn-made.home :as mayvenn-made.home]
            [storefront.accessors.experiments :as experiments]
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
            [storefront.components.ui :as ui]
            [storefront.components.dtc-banner :as dtc-banner]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.routes :as routes]))

(defn main-component [nav-event]
  (condp = nav-event
    #?@(:cljs
        [events/navigate-reset-password                             reset-password/built-component
         events/navigate-force-set-password                         force-set-password/built-component
         events/navigate-shop-by-look                               shop-by-look/built-component
         events/navigate-shop-by-look-details                       shop-by-look-details/built-component
         events/navigate-stylist-dashboard-balance-transfer-details balance-transfer-details/built-component
         events/navigate-stylist-dashboard-order-details            stylist.order-details/built-component
         events/navigate-stylist-dashboard-cash-out-begin           stylist.cash-out/built-component
         events/navigate-stylist-dashboard-cash-out-pending         stylist.cash-out-pending/built-component
         events/navigate-stylist-dashboard-cash-out-success         stylist.cash-out-success/built-component
         events/navigate-stylist-share-your-store                   stylist.share-your-store/built-component
         events/navigate-stylist-account-profile                    stylist.account/built-component
         events/navigate-stylist-account-portrait                   stylist.portrait/built-component
         events/navigate-stylist-account-password                   stylist.account/built-component
         events/navigate-stylist-account-payout                     stylist.account/built-component
         events/navigate-stylist-account-social                     stylist.account/built-component
         events/navigate-v2-stylist-dashboard-payments              v2.dashboard/built-component
         events/navigate-v2-stylist-dashboard-orders                v2.dashboard/built-component
         events/navigate-gallery-image-picker                       gallery-image-picker/built-component
         events/navigate-account-manage                             (partial sign-in/requires-sign-in account/built-component)
         events/navigate-account-referrals                          (partial sign-in/requires-sign-in friend-referrals/built-component)
         events/navigate-friend-referrals-freeinstall               friend-referrals/built-component
         events/navigate-friend-referrals                           friend-referrals/built-component
         events/navigate-checkout-returning-or-guest                checkout-returning-or-guest/built-component
         events/navigate-checkout-sign-in                           checkout-sign-in/built-component
         events/navigate-checkout-address                           (partial checkout-returning-or-guest/requires-sign-in-or-initiated-guest-checkout checkout-address/built-component)
         events/navigate-checkout-payment                           (partial checkout-returning-or-guest/requires-sign-in-or-initiated-guest-checkout checkout-payment/built-component)
         events/navigate-checkout-confirmation                      (partial checkout-returning-or-guest/requires-sign-in-or-initiated-guest-checkout checkout-confirmation/built-component)
         events/navigate-order-complete                             checkout-complete/built-component
         events/navigate-order-complete-need-match                  checkout-complete/built-component])

    events/navigate-home                              home/built-component
    events/navigate-category                          category/built-component
    events/navigate-product-details                   product-details/built-component
    events/navigate-shared-cart                       shared-cart/built-component
    events/navigate-checkout-processing               checkout-processing/built-component
    events/navigate-cart                              cart/built-component
    events/navigate-voucher-redeem                    voucher-redeem/built-component
    events/navigate-voucher-redeemed                  voucher-redeemed/built-component
    events/navigate-mayvenn-made                      mayvenn-made.home/built-component
    events/navigate-content-guarantee                 content/built-component
    events/navigate-content-help                      content/built-component
    events/navigate-content-privacy                   content/built-component
    events/navigate-content-tos                       content/built-component
    events/navigate-content-about-us                  content/built-component
    events/navigate-content-ugc-usage-terms           content/built-component
    events/navigate-content-program-terms             content/built-component
    events/navigate-content-our-hair                  content/built-component
    events/navigate-sign-in                           sign-in/built-component
    events/navigate-sign-up                           sign-up/built-component
    events/navigate-forgot-password                   forgot-password/built-component
    events/navigate-gallery                           gallery/built-component
    events/navigate-adventure-home                    adventure.home/built-component
    events/navigate-adventure-what-next               adventure.what-next/built-component
    events/navigate-adventure-match-stylist           adventure.match-stylist/built-component
    events/navigate-adventure-find-your-stylist       adventure.find-your-stylist/built-component
    events/navigate-adventure-how-far                 adventure.how-far/built-component
    events/navigate-adventure-matching-stylist-wait   adventure.matching-stylist-wait/built-component
    events/navigate-adventure-shop-hair               adventure.shop-hair/built-component
    events/navigate-adventure-how-shop-hair           adventure.how-shop-hair/built-component
    events/navigate-adventure-hair-texture            adventure.hair-texture/built-component
    events/navigate-adventure-bundlesets-hair-texture adventure.bundlesets.hair-texture/built-component
    events/navigate-adventure-a-la-carte-hair-texture adventure.a-la-carte.hair-texture/built-component
    events/navigate-adventure-a-la-carte-hair-color   adventure.a-la-carte.hair-color/built-component
    events/navigate-adventure-a-la-carte-product-list adventure.a-la-carte.product-list/built-component
    events/navigate-adventure-product-details         adventure.a-la-carte.product-details/built-component
    events/navigate-adventure-install-type            adventure.install-type/built-component
    events/navigate-adventure-select-new-look         adventure.select-new-look/built-component
    events/navigate-adventure-look-detail             adventure.look-detail/built-component
    events/navigate-adventure-select-bundle-set       adventure.select-new-look/built-component
    events/navigate-adventure-stylist-results         adventure.stylist-results/built-component
    events/navigate-adventure-out-of-area             adventure.out-of-area/built-component
    events/navigate-adventure-match-success           adventure.match-success/built-component
    home/built-component))

(defn sticky-promo-bar [data]
  (when (or (experiments/sticky-promo-bar? data)
            (experiments/sticky-promo-bar-everywhere? data))
    (component/build promotion-banner/sticky-component (promotion-banner/query data) nil)))

(defn main-layout [data nav-event]
  (let [silver-background? (#{events/navigate-voucher-redeem events/navigate-voucher-redeemed} nav-event)
        v2-home?           (and (experiments/v2-homepage? data)
                                (#{events/navigate-home} nav-event))]
    [:div.flex.flex-column {:style {:min-height    "100vh"
                                    :margin-bottom "-1px"}}
     (dtc-banner/built-component data nil)
     (when-not v2-home?
       [:div
        (promotion-banner/built-component data nil)
        (sticky-promo-bar data)])

     [:div {:key "popup"}
      #?(:cljs (popup/built-component data nil))]

     (header/built-component data nil)
     [:div.relative.flex.flex-column.flex-auto
      ;; Hack: one page does not have a white background, nor enough
      ;; content to fill its inner div.
      (when silver-background?
        {:class "bg-light-silver"})
      (flash/built-component data nil)

      [:main.bg-white.flex-auto (merge
                                 {:data-test (keypaths/->component-str nav-event)}
                                 ;; Hack: See above hack
                                 (when silver-background?
                                   {:class "bg-light-silver"}))
       ((main-component nav-event) data nil)]

      [:footer (footer/built-component data nil)]]]))

(defn adventure-checkout-layout [data nav-event]
  [:div.flex.flex-column {:style {:min-height    "100vh"
                                  :margin-bottom "-60px"}}
   [:div
    (promotion-banner/built-component data nil)
    (sticky-promo-bar data)]

   [:div {:key "popup"}
    #?(:cljs (popup/built-component data nil))]

   (header/adventure-built-component data nil)
   [:div.relative.flex.flex-column.flex-auto
    (flash/built-component data nil)

    [:main.bg-white.flex-auto {:data-test (keypaths/->component-str nav-event)}
     ((main-component nav-event) data nil)]
    [:footer
     (footer/built-component data nil)]
    (when (experiments/adv-chat? data) ui/adventure-chat-icon)]])

(defn top-level-component [data owner opts]
  (let [nav-event    (get-in data keypaths/navigation-event)
        freeinstall? (= "freeinstall" (get-in data keypaths/store-slug))]
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

       (routes/sub-page? [nav-event] [events/navigate-cart])
       (if freeinstall?
         (adventure-cart/layout data nav-event)
         (cart/layout data nav-event))

       (and freeinstall?
            (or (routes/sub-page? [nav-event] [events/navigate-checkout])
                (routes/sub-page? [nav-event] [events/navigate-order-complete])
                (routes/sub-page? [nav-event] [events/navigate-order-complete-need-match])))
       (adventure-checkout-layout data nav-event)

       (or (routes/sub-page? [nav-event] [events/navigate-adventure])
           (and freeinstall?
                (routes/sub-page? [nav-event] [events/navigate-content])))
       [:div {:data-test (keypaths/->component-str nav-event)}
        [:div {:key "popup"}
         #?(:cljs (popup/built-component data nil))]
        [:div.flex.content-stretch
         (merge
          {:style {:min-height    "100vh"
                   :margin-bottom "-30px"}}
          (when-not (= nav-event events/navigate-adventure-home)
            {:class "max-580 mx-auto relative"}))
         ((main-component nav-event) data nil)
         (when (experiments/adv-chat? data) ui/adventure-chat-icon)]]

       :else
       (main-layout data nav-event)))))
