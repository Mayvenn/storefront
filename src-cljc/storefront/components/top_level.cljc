(ns storefront.components.top-level
  (:require [storefront.component :as component :refer [defcomponent]]
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
                       storefront.components.v2-homepage-popup])
            #?@(:clj
                [[design-system.home :as design-system]])
            adventure.checkout.wait
            adventure.informational.about-our-hair
            adventure.informational.certified-stylists
            adventure.informational.how-it-works
            adventure.stylist-matching.matching-stylist-wait

            adventure.stylist-matching.stylist-profile
            adventure.stylist-matching.stylist-gallery

            stylist-matching.match-stylist
            stylist-matching.find-your-stylist
            stylist-matching.out-of-area
            stylist-matching.stylist-results
            stylist-matching.match-success

            [storefront.components.ui :as ui]
            [mayvenn-made.home :as mayvenn-made.home]
            checkout.classic-cart
            [storefront.accessors.experiments :as experiments]
            [storefront.components.content :as content]
            [storefront.components.flash :as flash]
            [storefront.components.footer :as footer]
            [storefront.components.forgot-password :as forgot-password]
            [storefront.components.gallery :as gallery]
            [storefront.components.gallery-edit :as gallery-edit]
            [storefront.components.header :as header]
            [storefront.components.home :as home]
            [ui.promo-banner :as promo-banner]
            [storefront.components.sign-in :as sign-in]
            [storefront.components.sign-up :as sign-up]
            [storefront.components.slideout-nav :as slideout-nav]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.routes :as routes]
            [checkout.shop.cart :as shop-cart]))

(def nav-table
  {#?@(:cljs
       [events/navigate-reset-password                             (constantly reset-password/built-component)
        events/navigate-force-set-password                         (constantly force-set-password/built-component)
        events/navigate-shop-by-look                               #(ui/lazy-load-component :catalog 'storefront.components.shop-by-look/built-component
                                                                                            events/navigate-shop-by-look)
        events/navigate-shop-by-look-details                       #(ui/lazy-load-component :catalog 'storefront.components.shop-by-look-details/built-component
                                                                                            events/navigate-shop-by-look-details)
        events/navigate-stylist-dashboard-balance-transfer-details #(ui/lazy-load-component :dashboard 'storefront.components.stylist.balance-transfer-details/built-component
                                                                                            events/navigate-stylist-dashboard-balance-transfer-details)
        events/navigate-stylist-dashboard-order-details            #(ui/lazy-load-component :dashboard 'storefront.components.stylist.order-details/built-component events/navigate-stylist-dashboard-order-details)
        events/navigate-stylist-dashboard-cash-out-begin           #(ui/lazy-load-component :dashboard 'storefront.components.stylist.cash-out/built-component events/navigate-stylist-dashboard-cash-out-begin)
        events/navigate-stylist-dashboard-cash-out-pending         #(ui/lazy-load-component :dashboard 'storefront.components.stylist.cash-out-pending/built-component events/navigate-stylist-dashboard-cash-out-pending)
        events/navigate-stylist-dashboard-cash-out-success         #(ui/lazy-load-component :dashboard 'storefront.components.stylist.cash-out-success/built-component events/navigate-stylist-dashboard-cash-out-success)
        events/navigate-stylist-share-your-store                   #(ui/lazy-load-component :dashboard 'storefront.components.stylist.share-your-store/built-component events/navigate-stylist-share-your-store)
        events/navigate-stylist-account-profile                    #(ui/lazy-load-component :dashboard 'storefront.components.stylist.account/built-component events/navigate-stylist-account-profile)
        events/navigate-stylist-account-portrait                   #(ui/lazy-load-component :dashboard 'storefront.components.stylist.portrait/built-component events/navigate-stylist-account-portrait)
        events/navigate-stylist-account-password                   #(ui/lazy-load-component :dashboard 'storefront.components.stylist.account/built-component events/navigate-stylist-account-password)
        events/navigate-stylist-account-payout                     #(ui/lazy-load-component :dashboard 'storefront.components.stylist.account/built-component events/navigate-stylist-account-payout)
        events/navigate-stylist-account-social                     #(ui/lazy-load-component :dashboard 'storefront.components.stylist.account/built-component events/navigate-stylist-account-social)
        events/navigate-v2-stylist-dashboard-payments              #(ui/lazy-load-component :dashboard 'stylist.dashboard/built-component events/navigate-v2-stylist-dashboard-payments)
        events/navigate-v2-stylist-dashboard-orders                #(ui/lazy-load-component :dashboard 'stylist.dashboard/built-component events/navigate-v2-stylist-dashboard-orders)
        events/navigate-gallery-image-picker                       #(ui/lazy-load-component :dashboard 'storefront.components.stylist.gallery-image-picker/built-component events/navigate-gallery-image-picker)
        events/navigate-account-manage                             #(partial sign-in/requires-sign-in account/built-component)
        events/navigate-account-referrals                          #(partial sign-in/requires-sign-in friend-referrals/built-component)
        events/navigate-friend-referrals-freeinstall               (constantly friend-referrals/built-component)
        events/navigate-friend-referrals                           (constantly friend-referrals/built-component)
        events/navigate-checkout-returning-or-guest                #(ui/lazy-load-component :checkout 'storefront.components.checkout-returning-or-guest/built-component events/navigate-checkout-returning-or-guest)
        events/navigate-checkout-sign-in                           #(ui/lazy-load-component :checkout 'storefront.components.checkout-sign-in/built-component events/navigate-checkout-sign-in)
        events/navigate-checkout-address                           #(ui/lazy-load-component :checkout 'storefront.components.checkout-address-auth-required/built-component events/navigate-checkout-address)
        events/navigate-checkout-payment                           #(ui/lazy-load-component :checkout 'storefront.components.checkout-payment/built-component events/navigate-checkout-payment)
        events/navigate-checkout-confirmation                      #(ui/lazy-load-component :checkout 'checkout.confirmation/built-component events/navigate-checkout-confirmation)
        events/navigate-order-complete                             #(ui/lazy-load-component :checkout 'storefront.components.checkout-complete/built-component events/navigate-order-complete)
        events/navigate-need-match-order-complete                  #(ui/lazy-load-component :checkout 'storefront.components.checkout-complete/built-component events/navigate-need-match-order-complete)])

   events/navigate-home                    (constantly home/built-component)
   events/navigate-category                #(ui/lazy-load-component :catalog 'catalog.category/built-component events/navigate-category)
   events/navigate-product-details         #(ui/lazy-load-component :catalog 'catalog.product-details/built-component events/navigate-product-details)
   events/navigate-shared-cart             #(ui/lazy-load-component :catalog 'storefront.components.shared-cart/built-component events/navigate-shared-cart)
   events/navigate-checkout-processing     #(ui/lazy-load-component :checkout 'checkout.processing/built-component events/navigate-checkout-processing)
   events/navigate-cart                    #(ui/lazy-load-component :catalog 'checkout.classic-cart/built-component events/navigate-cart)
   events/navigate-voucher-redeem          #(ui/lazy-load-component :redeem 'voucher.redeem/built-component events/navigate-voucher-redeem)
   events/navigate-voucher-redeemed        #(ui/lazy-load-component :redeem 'voucher.redeemed/built-component events/navigate-voucher-redeemed)
   events/navigate-mayvenn-made            (constantly mayvenn-made.home/built-component)
   events/navigate-content-guarantee       (constantly content/built-component)
   events/navigate-content-help            (constantly content/built-component)
   events/navigate-content-privacy         (constantly content/built-component)
   events/navigate-content-tos             (constantly content/built-component)
   events/navigate-content-about-us        (constantly content/built-component)
   events/navigate-content-ugc-usage-terms (constantly content/built-component)
   events/navigate-content-voucher-terms   (constantly content/built-component)
   events/navigate-content-program-terms   (constantly content/built-component)
   events/navigate-content-our-hair        (constantly content/built-component)
   events/navigate-sign-in                 (constantly sign-in/built-component)
   events/navigate-sign-up                 (constantly sign-up/built-component)
   events/navigate-forgot-password         (constantly forgot-password/built-component)
   events/navigate-store-gallery           (constantly gallery/built-component)
   events/navigate-gallery-edit            (constantly gallery-edit/built-component)

   events/navigate-info-certified-stylists (constantly adventure.informational.certified-stylists/built-component)
   events/navigate-info-about-our-hair     (constantly adventure.informational.about-our-hair/built-component)
   events/navigate-info-how-it-works       (constantly adventure.informational.how-it-works/built-component)

   events/navigate-adventure-match-stylist                       (constantly stylist-matching.match-stylist/page)
   events/navigate-adventure-find-your-stylist                   (constantly stylist-matching.find-your-stylist/page)
   events/navigate-adventure-matching-stylist-wait-post-purchase (constantly adventure.stylist-matching.matching-stylist-wait/built-component)
   events/navigate-adventure-stylist-results-pre-purchase        (constantly stylist-matching.stylist-results/page)
   events/navigate-adventure-stylist-results-post-purchase       (constantly stylist-matching.stylist-results/page)
   events/navigate-adventure-out-of-area                         (constantly stylist-matching.out-of-area/page)
   events/navigate-adventure-match-success-pre-purchase          (constantly stylist-matching.match-success/page)
   events/navigate-adventure-match-success-post-purchase         (constantly stylist-matching.match-success/page)
   events/navigate-adventure-checkout-wait                       #(ui/lazy-load-component :checkout 'adventure.checkout.wait/built-component events/navigate-adventure-checkout-wait)
   events/navigate-adventure-stylist-profile                     (constantly adventure.stylist-matching.stylist-profile/built-component)
   events/navigate-adventure-stylist-profile-post-purchase       (constantly adventure.stylist-matching.stylist-profile/built-component)
   events/navigate-adventure-stylist-gallery                     (constantly adventure.stylist-matching.stylist-gallery/built-component)})

(defn main-component [nav-event]
  (doto ((nav-table nav-event (constantly home/built-component)))
    (assert (str "Expected main-component to return a component, but did not: " (pr-str nav-event)))))

(defn main-layout
  [data nav-event]
  (component/html
   (let [gray-background? (#{events/navigate-voucher-redeem events/navigate-voucher-redeemed} nav-event)
         v2-home?           (and (experiments/v2-homepage? data)
                                 (#{events/navigate-home} nav-event))]
     [:div.flex.flex-column {:style {:min-height    "100vh"
                                     :margin-bottom "-1px"}}
      [:div {:key "popup"}
       #?(:cljs (popup/built-component data nil))]

      (when-not v2-home?
        [:div {:key "promo"}
         ^:inline (promo-banner/built-static-sticky-organism data nil)])

      ^:inline (header/built-component data nil)

      [:div.relative.flex.flex-column.flex-auto
       ;; HACK: one page does not have a white background, nor enough
       ;; content to fill its inner div.
       (when gray-background?
         {:class "bg-cool-gray"})
       ^:inline (flash/built-component data nil)

       [:main.bg-white.flex-auto (merge
                                  {:data-test (keypaths/->component-str nav-event)}
                                  ;; HACK: See above hack
                                  (when gray-background?
                                    {:class "bg-cool-gray"}))
        ((main-component nav-event) data nil)]

       [:footer (footer/built-component data nil)]]])))

(defn classic-site
  [data owner opts]
  (let [nav-event (get-in data keypaths/navigation-event)]
    (cond
      (get-in data keypaths/menu-expanded) ; Slideout nav
      (slideout-nav/built-component data nil)

      (routes/sub-page? [nav-event] [events/navigate-cart]) ; Cart pages
      (checkout.classic-cart/layout data nav-event)

      :else
      (main-layout data nav-event))))

(defn aladdin-site
  [data owner opts]
  (let [nav-event (get-in data keypaths/navigation-event)]
    (cond
      (get-in data keypaths/menu-expanded) ; Slideout nav
      (slideout-nav/built-component data nil)

      (routes/sub-page? [nav-event] [events/navigate-cart]) ; Cart pages
      (checkout.classic-cart/layout data nav-event)

      :else
      (main-layout data nav-event))))

(defn shop-site
  [data owner opts]
  (let [nav-event (get-in data keypaths/navigation-event)]
    (cond ; Design System
      (routes/sub-page? [nav-event] [events/navigate-design-system])
      #?(:clj
         (design-system.home/built-top-level data nil)
         :cljs
         ((ui/lazy-load-component :design-system
                                  'design-system.home/built-top-level
                                  (get-in data keypaths/navigation-event))
          data nil))

      (get-in data keypaths/menu-expanded) ; Slideout nav
      (slideout-nav/built-component data nil)

      (routes/sub-page? [nav-event] [events/navigate-cart]) ; Cart pages
      (shop-cart/page data nav-event)

      ;; TODO this should be moved into the UI domain of stylist-matching
      (routes/sub-page? [nav-event] [events/navigate-adventure])
      [:div {:data-test (keypaths/->component-str nav-event)}
       [:div {:key "popup"}
        #?(:cljs (popup/built-component data nil))]
       [:div.flex.content-stretch
        {:style {:min-height    "100vh"
                 :margin-bottom "-30px"}
         :class "max-580 mx-auto relative"}
        ((main-component nav-event) data nil)]]

      :else
      (main-layout data nav-event))))

(defcomponent top-level-component
  [data owner opts]
  (cond
    (= "shop" (get-in data keypaths/store-slug))
    (shop-site data owner opts)

    (= "aladdin" (get-in data keypaths/store-experience))
    (aladdin-site data owner opts)

    :else
    (classic-site data owner opts)))
