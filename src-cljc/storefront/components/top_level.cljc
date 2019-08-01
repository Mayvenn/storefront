(ns storefront.components.top-level
  (:require [storefront.component :as component]
            #?@(:cljs [[storefront.components.account :as account]
                       [storefront.components.force-set-password :as force-set-password]
                       [storefront.components.friend-referrals :as friend-referrals]
                       [storefront.components.popup :as popup]
                       [storefront.components.reset-password :as reset-password]
                       [storefront.config :as config]
                       [storefront.history :as history]
                       ;; popups, must be required to load properly
                       adventure.components.email-capture
                       adventure.components.program-details-popup
                       storefront.components.email-capture
                       storefront.components.share-your-cart
                       storefront.components.v2-homepage-popup
                       storefront.components.to-adventure-popup])
            #?@(:clj
                [[design-system.home :as design-system]])
            adventure.home
            adventure.informational.about-our-hair
            adventure.informational.certified-stylists
            adventure.informational.how-it-works
            adventure.stylist-matching.match-stylist
            adventure.stylist-matching.find-your-stylist
            adventure.stylist-matching.matching-stylist-wait
            adventure.stylist-matching.stylist-results
            adventure.stylist-matching.out-of-area
            adventure.stylist-matching.match-success
            adventure.stylist-matching.match-success-post-purchase
            adventure.stylist-matching.let-mayvenn-match
            adventure.stylist-matching.stylist-profile

            [storefront.components.ui :as ui]
            [mayvenn-made.home :as mayvenn-made.home]
            [checkout.cart :as cart]
            [adventure.checkout.cart :as adventure-cart]
            [storefront.accessors.experiments :as experiments]
            [storefront.components.content :as content]
            [storefront.components.flash :as flash]
            [storefront.components.footer :as footer]
            [storefront.components.forgot-password :as forgot-password]
            [storefront.components.gallery :as gallery]
            [storefront.components.header :as header]
            [storefront.components.home :as home]
            [storefront.components.promotion-banner :as promotion-banner]
            [storefront.components.sign-in :as sign-in]
            [storefront.components.sign-up :as sign-up]
            [storefront.components.slideout-nav :as slideout-nav]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.routes :as routes]))

(defn main-component [nav-event]
  (doto (condp = nav-event
          #?@(:cljs
              [events/navigate-reset-password                             reset-password/built-component
               events/navigate-force-set-password                         force-set-password/built-component
               events/navigate-shop-by-look                               (ui/lazy-load-component :catalog 'storefront.components.shop-by-look/built-component
                                                                                                  events/navigate-shop-by-look)
               events/navigate-shop-by-look-details                       (ui/lazy-load-component :catalog 'storefront.components.shop-by-look-details/built-component
                                                                                                  events/navigate-shop-by-look-details)
               events/navigate-stylist-dashboard-balance-transfer-details (ui/lazy-load-component :dashboard 'storefront.components.stylist.balance-transfer-details/built-component
                                                                                                  events/navigate-stylist-dashboard-balance-transfer-details)
               events/navigate-stylist-dashboard-order-details            (ui/lazy-load-component :dashboard 'storefront.components.stylist.order-details/built-component events/navigate-stylist-dashboard-order-details)
               events/navigate-stylist-dashboard-cash-out-begin           (ui/lazy-load-component :dashboard 'storefront.components.stylist.cash-out/built-component events/navigate-stylist-dashboard-cash-out-begin)
               events/navigate-stylist-dashboard-cash-out-pending         (ui/lazy-load-component :dashboard 'storefront.components.stylist.cash-out-pending/built-component events/navigate-stylist-dashboard-cash-out-pending)
               events/navigate-stylist-dashboard-cash-out-success         (ui/lazy-load-component :dashboard 'storefront.components.stylist.cash-out-success/built-component events/navigate-stylist-dashboard-cash-out-success)
               events/navigate-stylist-share-your-store                   (ui/lazy-load-component :dashboard 'storefront.components.stylist.share-your-store/built-component events/navigate-stylist-share-your-store)
               events/navigate-stylist-account-profile                    (ui/lazy-load-component :dashboard 'storefront.components.stylist.account/built-component events/navigate-stylist-account-profile)
               events/navigate-stylist-account-portrait                   (ui/lazy-load-component :dashboard 'storefront.components.stylist.portrait/built-component events/navigate-stylist-account-portrait)
               events/navigate-stylist-account-password                   (ui/lazy-load-component :dashboard 'storefront.components.stylist.account/built-component events/navigate-stylist-account-password)
               events/navigate-stylist-account-payout                     (ui/lazy-load-component :dashboard 'storefront.components.stylist.account/built-component events/navigate-stylist-account-payout)
               events/navigate-stylist-account-social                     (ui/lazy-load-component :dashboard 'storefront.components.stylist.account/built-component events/navigate-stylist-account-social)
               events/navigate-v2-stylist-dashboard-payments              (ui/lazy-load-component :dashboard 'stylist.dashboard/built-component events/navigate-v2-stylist-dashboard-payments)
               events/navigate-v2-stylist-dashboard-orders                (ui/lazy-load-component :dashboard 'stylist.dashboard/built-component events/navigate-v2-stylist-dashboard-orders)
               events/navigate-gallery-image-picker                       (ui/lazy-load-component :dashboard 'storefront.components.stylist.gallery-image-picker/built-component events/navigate-gallery-image-picker)
               events/navigate-account-manage                             (partial sign-in/requires-sign-in account/built-component)
               events/navigate-account-referrals                          (partial sign-in/requires-sign-in friend-referrals/built-component)
               events/navigate-friend-referrals-freeinstall               friend-referrals/built-component
               events/navigate-friend-referrals                           friend-referrals/built-component
               events/navigate-checkout-returning-or-guest                (ui/lazy-load-component :checkout 'storefront.components.checkout-returning-or-guest/built-component events/navigate-checkout-returning-or-guest)
               events/navigate-checkout-sign-in                           (ui/lazy-load-component :checkout 'storefront.components.checkout-sign-in/built-component events/navigate-checkout-sign-in)
               events/navigate-checkout-address                           (ui/lazy-load-component :checkout 'storefront.components.checkout-address-auth-required/built-component events/navigate-checkout-address)
               events/navigate-checkout-payment                           (ui/lazy-load-component :checkout 'storefront.components.checkout-payment/built-component events/navigate-checkout-payment)
               events/navigate-checkout-confirmation                      (ui/lazy-load-component :checkout 'checkout.confirmation/built-component events/navigate-checkout-confirmation)
               events/navigate-order-complete                             (ui/lazy-load-component :checkout 'storefront.components.checkout-complete/built-component events/navigate-order-complete)
               events/navigate-need-match-order-complete                  (ui/lazy-load-component :checkout 'storefront.components.checkout-complete/built-component events/navigate-need-match-order-complete)])

          events/navigate-home                    home/built-component
          events/navigate-category                (ui/lazy-load-component :catalog 'catalog.category/built-component events/navigate-category)
          events/navigate-product-details         (ui/lazy-load-component :catalog 'catalog.product-details/built-component events/navigate-product-details)
          events/navigate-shared-cart             (ui/lazy-load-component :catalog 'storefront.components.shared-cart/built-component events/navigate-shared-cart)
          events/navigate-checkout-processing     (ui/lazy-load-component :checkout 'checkout.processing/built-component events/navigate-checkout-processing)
          events/navigate-cart                    (ui/lazy-load-component :catalog 'checkout.cart/built-component events/navigate-cart)
          events/navigate-voucher-redeem          (ui/lazy-load-component :redeem 'voucher.redeem/built-component events/navigate-voucher-redeem)
          events/navigate-voucher-redeemed        (ui/lazy-load-component :redeem 'voucher.redeemed/built-component events/navigate-voucher-redeemed)
          events/navigate-mayvenn-made            mayvenn-made.home/built-component
          events/navigate-content-guarantee       content/built-component
          events/navigate-content-help            content/built-component
          events/navigate-content-privacy         content/built-component
          events/navigate-content-tos             content/built-component
          events/navigate-content-about-us        content/built-component
          events/navigate-content-ugc-usage-terms content/built-component
          events/navigate-content-voucher-terms   content/built-component
          events/navigate-content-program-terms   content/built-component
          events/navigate-content-our-hair        content/built-component
          events/navigate-sign-in                 sign-in/built-component
          events/navigate-sign-up                 sign-up/built-component
          events/navigate-forgot-password         forgot-password/built-component
          events/navigate-gallery                 gallery/built-component

          events/navigate-info-certified-stylists adventure.informational.certified-stylists/built-component
          events/navigate-info-about-our-hair     adventure.informational.about-our-hair/built-component
          events/navigate-info-how-it-works       adventure.informational.how-it-works/built-component

          events/navigate-adventure-home                                adventure.home/built-component
          events/navigate-adventure-shop-hair                           (ui/lazy-load-component :catalog 'adventure.shop-hair/built-component events/navigate-adventure-shop-hair)
          events/navigate-adventure-how-shop-hair                       (ui/lazy-load-component :catalog 'adventure.how-shop-hair/built-component events/navigate-adventure-how-shop-hair)
          events/navigate-adventure-hair-texture                        (ui/lazy-load-component :catalog 'adventure.hair-texture/built-component events/navigate-adventure-hair-texture)
          events/navigate-adventure-bundlesets-hair-texture             (ui/lazy-load-component :catalog 'adventure.bundlesets.hair-texture/built-component events/navigate-adventure-bundlesets-hair-texture)
          events/navigate-adventure-a-la-carte-hair-texture             (ui/lazy-load-component :catalog 'adventure.a-la-carte.hair-texture/built-component events/navigate-adventure-a-la-carte-hair-texture)
          events/navigate-adventure-a-la-carte-hair-color               (ui/lazy-load-component :catalog 'adventure.a-la-carte.hair-color/built-component events/navigate-adventure-a-la-carte-hair-color)
          events/navigate-adventure-a-la-carte-product-list             (ui/lazy-load-component :catalog 'adventure.a-la-carte.product-list/built-component events/navigate-adventure-a-la-carte-product-list)
          events/navigate-adventure-product-details                     (ui/lazy-load-component :catalog 'adventure.a-la-carte.product-details/built-component events/navigate-adventure-product-details)
          events/navigate-adventure-select-new-look                     (ui/lazy-load-component :catalog 'adventure.select-new-look/built-component events/navigate-adventure-select-new-look)
          events/navigate-adventure-look-detail                         (ui/lazy-load-component :catalog 'adventure.look-detail/built-component events/navigate-adventure-look-detail)
          events/navigate-adventure-select-bundle-set                   (ui/lazy-load-component :catalog 'adventure.select-new-look/built-component events/navigate-adventure-select-bundle-set)
          events/navigate-adventure-match-stylist                       adventure.stylist-matching.match-stylist/built-component
          events/navigate-adventure-find-your-stylist                   adventure.stylist-matching.find-your-stylist/built-component
          events/navigate-adventure-matching-stylist-wait-pre-purchase  adventure.stylist-matching.matching-stylist-wait/built-component
          events/navigate-adventure-matching-stylist-wait-post-purchase adventure.stylist-matching.matching-stylist-wait/built-component
          events/navigate-adventure-stylist-results-pre-purchase        adventure.stylist-matching.stylist-results/built-component-pre-purchase
          events/navigate-adventure-stylist-results-post-purchase       adventure.stylist-matching.stylist-results/built-component-post-purchase
          events/navigate-adventure-out-of-area                         adventure.stylist-matching.out-of-area/built-component
          events/navigate-adventure-match-success-pre-purchase          adventure.stylist-matching.match-success/built-component
          events/navigate-adventure-match-success-post-purchase         adventure.stylist-matching.match-success-post-purchase/built-component
          events/navigate-adventure-checkout-wait                       (ui/lazy-load-component :checkout 'adventure.checkout.wait/built-component events/navigate-adventure-checkout-wait)
          events/navigate-adventure-let-mayvenn-match                   adventure.stylist-matching.let-mayvenn-match/built-component
          events/navigate-adventure-stylist-profile                     adventure.stylist-matching.stylist-profile/built-component
          home/built-component)
    (assert (str "Expected main-component to return a component, but did not: " (pr-str nav-event)))))

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
     (footer/built-component data nil)]]])

(defn top-level-component [data owner opts]
  (let [nav-event    (get-in data keypaths/navigation-event)
        freeinstall? (= "freeinstall" (get-in data keypaths/store-slug))]
    (component/create
     (cond
       (routes/sub-page? [nav-event] [events/navigate-design-system])
       #?(:clj
          (design-system.home/top-level data nil)
          :cljs
           ((ui/lazy-load-component :design-system
                                    'design-system.home/top-level
                                    (get-in data keypaths/navigation-event))
            data nil))

       (get-in data keypaths/menu-expanded)
       (slideout-nav/built-component data nil)

       (routes/sub-page? [nav-event] [events/navigate-cart])
       (if freeinstall?
         (adventure-cart/layout data nav-event)
         (cart/layout data nav-event))

       (and freeinstall?
            (or (routes/sub-page? [nav-event] [events/navigate-checkout])
                (routes/sub-page? [nav-event] [events/navigate-order-complete])
                (routes/sub-page? [nav-event] [events/navigate-need-match-order-complete])
                (routes/sub-page? [nav-event] [events/navigate-adventure-let-mayvenn-match])))
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
         ((main-component nav-event) data nil)]]

       :else
       (main-layout data nav-event)))))
