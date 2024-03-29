(ns storefront.components.top-level
  (:require [storefront.component :as component :refer [defcomponent]]
            #?@(:cljs [[storefront.components.account :as account]
                       [storefront.components.email-capture :as email-capture]
                       [storefront.components.force-set-password :as force-set-password]
                       [storefront.components.popup :as popup]
                       [storefront.components.reset-password :as reset-password]
                       [checkout.shop.addon-services-menu]

                       ;; popups, must be required to load properly
                       storefront.components.share-your-cart])

            #?@(:clj [[design-system.home :as design-system]])

            [mayvenn.stylist-pay.core :as stylist-pay]

            wigs-101.guide
            retail.stores
            retail.store

            [appointment-booking.core :as booking.core]
            mayvenn.shopping-quiz.unnamed-v1
            mayvenn.shopping-quiz.unified-freeinstall
            mayvenn.shopping-quiz.crm-persona
            [email-verification.core :as email-verification]
            [storefront.components.ui :as ui]
            [storefront.components.content :as content]
            [storefront.components.flash :as flash]
            [storefront.components.footer :as footer]
            [storefront.components.forgot-password :as forgot-password]
            [storefront.components.gallery :as gallery]
            [storefront.components.gallery-photo :as gallery-photo]
            [storefront.components.header :as header]
            [ui.promo-banner :as promo-banner]
            storefront.components.shared-cart
            [storefront.components.pixels :as pixels]
            [storefront.components.sign-in :as sign-in]
            [storefront.components.sign-up :as sign-up]
            [storefront.components.order-details-sign-up :as order-details-sign-up]
            [storefront.components.slideout-nav :as slideout-nav]
            [storefront.components.landing-page :as landing-page]
            [storefront.components.stylist-social-media :as stylist-social-media]
            [storefront.components.guide-clipin-extensions :as guide-clipin-extensions]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            stylist-directory.keypaths
            [storefront.routes :as routes]
            [mayvenn.live-help.core :as live-help]
            [storefront.components.privacy :as privacy]))

;; HACK until the day there are no more built-components
(defn ^:private first-arg-only [inner-fn]
  (fn [& args] (inner-fn (first args))))

(def nav-table
  {#?@(:cljs
       [events/navigate-reset-password                             (constantly reset-password/built-component)
        events/navigate-force-set-password                         (constantly force-set-password/built-component)
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
        events/navigate-v2-stylist-dashboard-payout-rates          #(ui/lazy-load-component :dashboard 'stylist.dashboard-payout-rates/page events/navigate-v2-stylist-dashboard-payout-rates)
        events/navigate-gallery-image-picker                       #(ui/lazy-load-component :dashboard 'storefront.components.stylist.gallery-image-picker/built-component events/navigate-gallery-image-picker)
        events/navigate-account-manage                             #(partial sign-in/requires-sign-in account/built-component)
        events/navigate-checkout-returning-or-guest                #(ui/lazy-load-component :checkout 'storefront.components.checkout-returning-or-guest/built-component events/navigate-checkout-returning-or-guest)
        events/navigate-checkout-sign-in                           #(ui/lazy-load-component :checkout 'storefront.components.checkout-sign-in/built-component events/navigate-checkout-sign-in)
        events/navigate-checkout-address                           #(ui/lazy-load-component :checkout 'storefront.components.checkout-address-auth-required/built-component events/navigate-checkout-address)
        events/navigate-checkout-payment                           #(ui/lazy-load-component :checkout 'storefront.components.checkout-payment/built-component events/navigate-checkout-payment)
        events/navigate-checkout-confirmation                      #(ui/lazy-load-component :checkout 'checkout.confirmation/built-component events/navigate-checkout-confirmation)
        events/navigate-order-complete                             #(ui/lazy-load-component :checkout 'storefront.components.checkout-complete/built-component events/navigate-order-complete)])

   events/navigate-home                               #(ui/lazy-load-component :homepage
                                                                         'homepage.core/page
                                                                         events/navigate-home)
   events/navigate-shop-by-look                       #(ui/lazy-load-component :catalog 'catalog.looks/built-component
                                                                         events/navigate-shop-by-look)
   events/navigate-shop-by-look-details               #(ui/lazy-load-component :catalog 'catalog.look-details-v202105/built-component
                                                                         events/navigate-shop-by-look-details)
   events/navigate-category                           #(ui/lazy-load-component :catalog  'catalog.category/built-component events/navigate-category)
   events/navigate-product-details                    #(ui/lazy-load-component :catalog  'catalog.product-details/page events/navigate-product-details)
   events/navigate-shared-cart                        #(ui/lazy-load-component :catalog  'storefront.components.shared-cart/built-component events/navigate-shared-cart)
   events/navigate-checkout-processing                #(ui/lazy-load-component :checkout 'checkout.processing/built-component events/navigate-checkout-processing)
   events/navigate-cart                               #(ui/lazy-load-component :catalog  'checkout.classic-cart/built-component events/navigate-cart)
   events/navigate-checkout-add                       #(ui/lazy-load-component :checkout 'checkout.add/built-component events/navigate-checkout-add)
   events/navigate-voucher-redeem                     #(ui/lazy-load-component :redeem   'voucher.redeem/built-component events/navigate-voucher-redeem)
   events/navigate-voucher-redeemed                   #(ui/lazy-load-component :redeem   'voucher.redeemed/built-component events/navigate-voucher-redeemed)
   events/navigate-wigs-101-guide                     (constantly wigs-101.guide/built-component)
   events/navigate-wig-hair-guide                     (constantly wigs-101.guide/built-component-hair)
   events/navigate-wig-buying-guide                   (constantly wigs-101.guide/built-component-buying)
   events/navigate-wig-installation-guide             (constantly wigs-101.guide/built-component-installation)
   events/navigate-wig-care-guide                     (constantly wigs-101.guide/built-component-care)
   events/navigate-wig-styling-guide                  (constantly wigs-101.guide/built-component-styling)
   events/navigate-retail-walmart                     (constantly retail.stores/built-component)
   events/navigate-retail-walmart-grand-prairie       (constantly retail.store/built-component)
   events/navigate-retail-walmart-katy                (constantly retail.store/built-component)
   events/navigate-retail-walmart-houston             (constantly retail.store/built-component)
   events/navigate-retail-walmart-mansfield           (constantly retail.store/built-component)
   events/navigate-retail-walmart-dallas              (constantly retail.store/built-component)
   events/navigate-content-guarantee                  (constantly content/built-component)
   events/navigate-content-help                       (constantly content/built-component)
   events/navigate-content-privacy                    (constantly privacy/page)
   events/navigate-content-privacyv2                  (constantly content/built-component)
   events/navigate-content-privacyv1                  (constantly content/built-component)
   events/navigate-content-tos                        (constantly content/built-component)
   events/navigate-content-sms                        (constantly content/built-component)
   events/navigate-content-about-us                   (constantly content/built-component)
   events/navigate-content-ugc-usage-terms            (constantly content/built-component)
   events/navigate-content-voucher-terms              (constantly content/built-component)
   events/navigate-content-program-terms              (constantly content/built-component)
   events/navigate-content-our-hair                   (constantly content/built-component)
   events/navigate-guide-clipin-extensions            (constantly guide-clipin-extensions/built-component)
   events/navigate-content-return-and-exchange-policy (constantly content/built-component)
   events/navigate-sign-in                            (constantly sign-in/built-component)
   events/navigate-sign-up                            (constantly sign-up/built-component)
   events/navigate-account-email-verification         (constantly email-verification/built-component)
   events/navigate-order-details-sign-up              (constantly order-details-sign-up/built-component)
   events/navigate-forgot-password                    (constantly forgot-password/built-component)
   events/navigate-store-gallery                      (constantly gallery/built-component)
   events/navigate-gallery-edit                       #(ui/lazy-load-component :dashboard 'storefront.components.gallery-v202105/built-component events/navigate-gallery-edit)
   events/navigate-gallery-appointments               #(ui/lazy-load-component :dashboard 'storefront.components.gallery-v202105/built-component events/navigate-gallery-appointments)
   events/navigate-gallery-photo                      (constantly gallery-photo/built-component)
   events/navigate-yourlooks-order-details            #(ui/lazy-load-component :dashboard 'order-details.core/page events/navigate-yourlooks-order-details)
   events/navigate-yourlooks-order-history            #(ui/lazy-load-component :dashboard 'order-history.core/page events/navigate-yourlooks-order-history)

   events/navigate-adventure-find-your-stylist       #(ui/lazy-load-component :catalog
                                                                              'stylist-matching.find-your-stylist/page
                                                                              events/navigate-adventure-find-your-stylist)
   events/navigate-adventure-stylist-results         #(ui/lazy-load-component :catalog
                                                                              'stylist-matching.stylist-results/page
                                                                              events/navigate-adventure-stylist-results)
   events/navigate-adventure-match-success           #(ui/lazy-load-component :catalog
                                                                              'stylist-matching.match-success/page
                                                                              events/navigate-adventure-match-success)
   events/navigate-adventure-stylist-profile         #(ui/lazy-load-component :catalog
                                                                              'stylist-profile.core/page
                                                                              events/navigate-adventure-stylist-profile)
   events/navigate-adventure-stylist-profile-reviews #(ui/lazy-load-component :catalog
                                                                              'stylist-profile.stylist-reviews-v2021-10/page
                                                                              events/navigate-adventure-stylist-profile-reviews)
   events/navigate-adventure-stylist-gallery         #(ui/lazy-load-component :catalog
                                                                              'adventure.stylist-matching.stylist-gallery/built-component
                                                                              events/navigate-adventure-stylist-gallery)
   events/navigate-quiz-crm-persona-questions                            #(ui/lazy-load-component :catalog
                                                                                                  'mayvenn.shopping-quiz.crm-persona/page
                                                                                                  events/navigate-quiz-crm-persona-questions)
   events/navigate-quiz-crm-persona-results                              #(ui/lazy-load-component :catalog
                                                                                                  'mayvenn.shopping-quiz.crm-persona/page
                                                                                                  events/navigate-quiz-crm-persona-results)
   events/navigate-shopping-quiz-unified-freeinstall-intro               #(ui/lazy-load-component
                                                                           :catalog
                                                                           'mayvenn.shopping-quiz.unified-freeinstall/page
                                                                           events/navigate-shopping-quiz-unified-freeinstall-intro)
   events/navigate-shopping-quiz-unified-freeinstall-question            #(ui/lazy-load-component
                                                                           :catalog
                                                                           'mayvenn.shopping-quiz.unified-freeinstall/page
                                                                           events/navigate-shopping-quiz-unified-freeinstall-question)
   events/navigate-shopping-quiz-unified-freeinstall-recommendations     #(ui/lazy-load-component
                                                                           :catalog
                                                                           'mayvenn.shopping-quiz.unified-freeinstall/page
                                                                           events/navigate-shopping-quiz-unified-freeinstall-recommendations)
   events/navigate-shopping-quiz-unified-freeinstall-summary             #(ui/lazy-load-component
                                                                           :catalog
                                                                           'mayvenn.shopping-quiz.unified-freeinstall/page
                                                                           events/navigate-shopping-quiz-unified-freeinstall-summary)
   events/navigate-shopping-quiz-unified-freeinstall-find-your-stylist   #(ui/lazy-load-component
                                                                           :catalog
                                                                           'mayvenn.shopping-quiz.unified-freeinstall/page
                                                                           events/navigate-shopping-quiz-unified-freeinstall-find-your-stylist)
   events/navigate-shopping-quiz-unified-freeinstall-stylist-results     #(ui/lazy-load-component
                                                                           :catalog
                                                                           'mayvenn.shopping-quiz.unified-freeinstall/page
                                                                           events/navigate-shopping-quiz-unified-freeinstall-stylist-results)
   events/navigate-shopping-quiz-unified-freeinstall-appointment-booking #(ui/lazy-load-component
                                                                           :catalog
                                                                           'mayvenn.shopping-quiz.unified-freeinstall/page
                                                                           events/navigate-shopping-quiz-unified-freeinstall-appointment-booking)
   events/navigate-shopping-quiz-unified-freeinstall-match-success       #(ui/lazy-load-component
                                                                           :checkout
                                                                           'mayvenn.shopping-quiz.unified-freeinstall/page
                                                                           events/navigate-shopping-quiz-unified-freeinstall-match-success)
   events/navigate-landing-page                                          (constantly landing-page/built-component)
   events/navigate-stylist-social-media                                  (constantly stylist-social-media/built-component)
   events/navigate-design-system                                         #(ui/lazy-load-component :design-system
                                                                                                  'design-system.home/built-top-level
                                                                                                  events/navigate-design-system)
   events/navigate-design-system-component-library                       #(ui/lazy-load-component :design-system
                                                                                                  'design-system.component-library/built-component
                                                                                                  events/navigate-design-system-component-library)})

(defn main-component
  [nav-event]
  (doto ((get nav-table
              nav-event
              #(ui/lazy-load-component :homepage
                                       'homepage.core/page
                                       events/navigate-home)))
    (assert
     (str "Expected main-component to return a component, but did not: "
          (pr-str nav-event)))))

;; NOTE(corey,stella) We came up with a inverted version of this so that each
;; page can control what 'layout' it needs. Cf. storefront.components.template
(defn main-layout
  [data nav-event]
  (component/html
   [:div.flex.flex-column.stretch {:style {:margin-bottom "-1px"}}
    [:div {:key "popup"}
     #?(:cljs (popup/built-component data nil))]

    [:div {:key "promo"}
     ^:inline (promo-banner/built-static-organism data nil)]

    ^:inline (header/built-component data nil)

    [:div.relative.flex.flex-column.flex-auto
     ^:inline (flash/built-component data nil)

     [:main.bg-white.flex-auto
      {:data-test (keypaths/->component-str nav-event)}
      ((main-component nav-event) data nil)]

     [:footer (footer/built-component data nil)]]
    (live-help/bug-component data)]))

(defn classic-site
  [data owner opts]
  (let [nav-event (get-in data keypaths/navigation-event)]
    (cond
      (get-in data keypaths/menu-expanded) ; Slideout nav
      (slideout-nav/built-component data nil)

      (routes/sub-page? [nav-event] [events/navigate-cart]) ; Cart pages
      ((ui/lazy-load-component :catalog 'checkout.classic-cart/layout events/navigate-cart) data nil)

      (contains? #{events/navigate-shop-by-look} nav-event)
      ((main-component nav-event) data nil)

      :else 
      (main-layout data nav-event))))

(defn shop-site
  [data owner opts]
  (component/html
   (let [nav-event (get-in data keypaths/navigation-event)]
     (cond
       ;; Slideout nav
       (boolean (get-in data keypaths/menu-expanded))
       (slideout-nav/built-component data nil)

       ;; Cart pages for Shop
       (routes/sub-page? [nav-event] [events/navigate-cart])
       ((ui/lazy-load-component :checkout 'checkout.shop.cart-v2020-09/page nav-event) data nav-event)

       ;; TODO this should be moved into the UI domain of stylist-matching
       (or
        (routes/sub-page? [nav-event] [events/navigate-adventure])
        (routes/sub-page? [nav-event] [events/navigate-shopping-quiz])
        (routes/sub-page? [nav-event] [events/navigate-quiz-crm-persona-questions])
        (routes/sub-page? [nav-event] [events/navigate-quiz-crm-persona-results]))
       [:div {:data-test (keypaths/->component-str nav-event)
              :class (when (some #(= nav-event %) [events/navigate-adventure-stylist-profile])
                       "stylist-profile-details")}
        [:div {:key "popup"}
         #?(:cljs (popup/built-component data nil))]
        [:div.stretch
         {:style {:margin-bottom "-30px"}
          ;; TODO temporary hack for desktop stylist profile
          ;;      As we re-align our pages to have their own control (ie. the todo above)
          ;;      we can add events here that we're transitioning to be desktop friendly
          :class "mx-auto relative"}
         ((main-component nav-event) data nil)]
        (when #?(:clj true
                 :cljs (-> data (get-in stylist-directory.keypaths/stylist-search-show-filters?) not))
          (live-help/bug-component data))]

       (contains? #{events/navigate-shop-by-look} nav-event)
       ((main-component nav-event) data nil)

       :else
       (main-layout data nav-event)))))

(defcomponent top-level-component
  [data owner opts]
  [:div
   #?(:cljs (email-capture/built-component data nil))
   (pixels/built-component data nil)
   (cond
     (= events/navigate-mayvenn-stylist-pay (get-in data keypaths/navigation-event))
     (stylist-pay/page data)
 
     (or (= "shop" (get-in data keypaths/store-slug))
         (= "retail-location" (get-in data keypaths/store-experience)))
     (shop-site data owner opts)

     :else
     (classic-site data owner opts))])
