(ns storefront.routes
  (:require [bidi.bidi :as bidi]
            [clojure.walk :refer [keywordize-keys]]
            [catalog.categories :as categories]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.uri :as uri]
            #?(:cljs [cljs.reader :refer [read-string]])))

(defn edn->bidi [value]
  (keyword (prn-str value)))

(defn bidi->edn [value]
  (read-string (name value)))

(def static-page-routes
  {"/guarantee"       (edn->bidi events/navigate-content-guarantee)
   "/help"            (edn->bidi events/navigate-content-help)
   "/about-us"        (edn->bidi events/navigate-content-about-us)
   "/policy/privacy"  (edn->bidi events/navigate-content-privacy)
   "/policy/tos"      (edn->bidi events/navigate-content-tos)
   "/ugc-usage-terms" (edn->bidi events/navigate-content-ugc-usage-terms)
   "/voucher-terms"   (edn->bidi events/navigate-content-voucher-terms)
   "/program-terms"   (edn->bidi events/navigate-content-program-terms)
   "/our-hair"        (edn->bidi events/navigate-content-our-hair)})

(def static-api-routes
  ["/static" static-page-routes])

(def design-system-routes
  {"/_style"                 (edn->bidi events/navigate-design-system)
   "/_style/color"           (edn->bidi events/navigate-design-system-color)
   "/_style/spacing"         (edn->bidi events/navigate-design-system-spacing)
   "/_style/buttons"         (edn->bidi events/navigate-design-system-buttons)
   "/_style/form-fields"     (edn->bidi events/navigate-design-system-form-fields)
   "/_style/navigation"      (edn->bidi events/navigate-design-system-navigation)
   "/_style/navigation/tab1" (edn->bidi events/navigate-design-system-navigation-tab1)
   "/_style/navigation/tab3" (edn->bidi events/navigate-design-system-navigation-tab3)
   "/_style/progress"        (edn->bidi events/navigate-design-system-progress)
   "/_style/carousel"        (edn->bidi events/navigate-design-system-carousel)
   "/_style/classic"         (edn->bidi events/navigate-design-system-classic)
   "/_style/adventure"       (edn->bidi events/navigate-design-system-adventure)
   "/_style/ui"              (edn->bidi events/navigate-design-system-ui)})

(def stylist-matching-routes
  {"/adv/top-stylist"                                            (edn->bidi events/navigate-adventure-top-stylist)
   "/adv/find-your-stylist"                                      (edn->bidi events/navigate-adventure-find-your-stylist)
   "/adv/stylist-results"                                        (edn->bidi events/navigate-adventure-stylist-results)
   "/adv/match-success"                                          (edn->bidi events/navigate-adventure-match-success)
   ["/stylist/" [#"\d+" :stylist-id] "-" :store-slug]            (edn->bidi events/navigate-adventure-stylist-profile)
   ["/stylist/" [#"\d+" :stylist-id] "-" :store-slug "/gallery"] (edn->bidi events/navigate-adventure-stylist-gallery)
   "/certified-stylists"                                         (edn->bidi events/navigate-info-certified-stylists)
   "/about-our-hair"                                             (edn->bidi events/navigate-info-about-our-hair)
   "/about-mayvenn-install"                                      (edn->bidi events/navigate-about-mayvenn-install)})

(def adventure-quiz-routes
  {"/adv/quiz"                                   (edn->bidi events/navigate-adventure-quiz)
   ;; TODO(ellie, 2021-07-15): Is this the right path?
   "/appointment-booking"                        (edn->bidi events/navigate-adventure-appointment-booking)
   "/quiz/unified-freeinstall/intro"             (edn->bidi events/navigate-shopping-quiz-unified-freeinstall-intro)
   "/quiz/unified-freeinstall/question"          (edn->bidi events/navigate-shopping-quiz-unified-freeinstall-question)
   "/quiz/unified-freeinstall/recommendations"   (edn->bidi events/navigate-shopping-quiz-unified-freeinstall-recommendations)
   "/quiz/unified-freeinstall/summary"           (edn->bidi events/navigate-shopping-quiz-unified-freeinstall-summary)
   "/quiz/unified-freeinstall/find-your-stylist" (edn->bidi events/navigate-shopping-quiz-unified-freeinstall-find-your-stylist)
   "/quiz/unified-freeinstall/top-stylist"       (edn->bidi events/navigate-shopping-quiz-unified-freeinstall-top-stylist)
   "/quiz/unified-freeinstall/stylist-results"   (edn->bidi events/navigate-shopping-quiz-unified-freeinstall-stylist-results)
   "/quiz/unified-freeinstall/match-success"     (edn->bidi events/navigate-shopping-quiz-unified-freeinstall-match-success)})

(def catalog-routes
  ;; NOTE: if you update category url, don't forget to update the fast-inverse-catalog-routes below
  {["/categories/" [#"\d+" :catalog/category-id] "-" :page/slug]
   (edn->bidi events/navigate-category)

   ["/categories/hair/" :named-search-slug]
   (edn->bidi events/navigate-legacy-named-search)
   ["/categories/hair/" :named-search-slug "/social"]
   (edn->bidi events/navigate-legacy-ugc-named-search)

   ["/products/" [#"\d+" :catalog/product-id] "-" :page/slug]
   (edn->bidi events/navigate-product-details)

   ["/products/" [#"[^\d].*" :legacy/product-slug]]
   (edn->bidi events/navigate-legacy-product-page)})

;; provide fast urls resolution for urls in the footer
(def fast-inverse-catalog-routes
  (into {}
        (map (fn [category]
               [[(edn->bidi events/navigate-category)
                 {:catalog/category-id (:catalog/category-id category)
                  :page/slug           (:page/slug category)}]
                (str "/categories/" (:catalog/category-id category) "-" (:page/slug category))]))
        categories/initial-categories))

(def shop? (partial contains? #{"shop"}))

(def ^:private sign-in-routes
  {"/login"  (edn->bidi events/navigate-sign-in)
   "/logout" (edn->bidi events/navigate-sign-out)
   "/signup" (edn->bidi events/navigate-sign-up)})

(def app-routes
  ["" (merge static-page-routes
             design-system-routes
             catalog-routes
             {"/" (edn->bidi events/navigate-home)}
             stylist-matching-routes
             adventure-quiz-routes
             sign-in-routes
             {"/password/recover"                                 (edn->bidi events/navigate-forgot-password)
              "/password/set"                                     (edn->bidi events/navigate-force-set-password)
              ["/m/" :reset-token]                                (edn->bidi events/navigate-reset-password)
              ["/c/" :shared-cart-id]                             (edn->bidi events/navigate-shared-cart)
              "/account/edit"                                     (edn->bidi events/navigate-account-manage)
              "/cart"                                             (edn->bidi events/navigate-cart)
              "/added-to-cart"                                    (edn->bidi events/navigate-added-to-cart)
              "/checkout/add"                                     (edn->bidi events/navigate-checkout-add)
              ["/shop/" [keyword :album-keyword]]                 (edn->bidi events/navigate-shop-by-look)
              ["/shop/" [keyword :album-keyword] "/" :look-id]    (edn->bidi events/navigate-shop-by-look-details)
              "/stylist/cash-out-now"                             (edn->bidi events/navigate-stylist-dashboard-cash-out-begin)
              ["/stylist/cash-out-pending/" :status-id]           (edn->bidi events/navigate-stylist-dashboard-cash-out-pending)
              ["/stylist/cash-out-success/" :balance-transfer-id] (edn->bidi events/navigate-stylist-dashboard-cash-out-success)

              ;; DEPRECATED - these redirect to v2 dashboard
              ["/stylist/earnings/" :balance-transfer-id] (edn->bidi events/navigate-stylist-dashboard-balance-transfer-details)
              "/stylist/earnings"                         (edn->bidi events/navigate-stylist-dashboard-earnings)
              "/stylist/store_credits"                    (edn->bidi events/navigate-stylist-dashboard-bonus-credit)

              "/stylist/share-your-store"        (edn->bidi events/navigate-stylist-share-your-store)
              "/stylist/account/profile"         (edn->bidi events/navigate-stylist-account-profile)
              "/stylist/account/portrait"        (edn->bidi events/navigate-stylist-account-portrait)
              "/stylist/account/password"        (edn->bidi events/navigate-stylist-account-password)
              "/stylist/account/payout"          (edn->bidi events/navigate-stylist-account-payout)
              "/stylist/account/social"          (edn->bidi events/navigate-stylist-account-social)
              "/stylist/redeem"                  (edn->bidi events/navigate-voucher-redeem)
              "/stylist/redeemed"                (edn->bidi events/navigate-voucher-redeemed)
              "/stylist/payout-rates"            (edn->bidi events/navigate-v2-stylist-dashboard-payout-rates)
              "/stylist/payments"                (edn->bidi events/navigate-v2-stylist-dashboard-payments)
              "/stylist/orders"                  (edn->bidi events/navigate-v2-stylist-dashboard-orders)
              ["/stylist/orders/" :order-number] (edn->bidi events/navigate-stylist-dashboard-order-details)
              "/gallery"                         (edn->bidi events/navigate-store-gallery) ;; Deprecated?
              "/gallery/edit"                    (edn->bidi events/navigate-gallery-edit)
              "/gallery/appointments"            (edn->bidi events/navigate-gallery-appointments)
              "/gallery/add"                     (edn->bidi events/navigate-gallery-image-picker)
              ["/gallery/edit/photo/" :photo-id] (edn->bidi events/navigate-gallery-photo)
              "/checkout/returning_or_guest"     (edn->bidi events/navigate-checkout-returning-or-guest)
              "/checkout/login"                  (edn->bidi events/navigate-checkout-sign-in)
              "/checkout/address"                (edn->bidi events/navigate-checkout-address)
              "/checkout/payment"                (edn->bidi events/navigate-checkout-payment)
              "/checkout/confirm"                (edn->bidi events/navigate-checkout-confirmation)
              "/checkout/processing"             (edn->bidi events/navigate-checkout-processing)
              ["/orders/" :number "/complete"]   (edn->bidi events/navigate-order-complete)})])

;; provide fast urls resolution for urls in the footer
(def ^:private fast-inverse-path-for
  (into fast-inverse-catalog-routes
        (map (fn [[path event]] [[event nil] path]))
        (merge static-page-routes stylist-matching-routes sign-in-routes)))

;; TODO(jeff,corey): history/path-for should support domains like navigation-message-for
(defn path-for*
  ([navigation-event]
   (let [encoded-navigation-event (edn->bidi navigation-event)]
     (or (fast-inverse-path-for [encoded-navigation-event nil])
         (bidi/path-for app-routes encoded-navigation-event))))
  ([navigation-event args]
   (let [encoded-navigation-event (edn->bidi navigation-event)]
     (or
      (fast-inverse-path-for [encoded-navigation-event (not-empty args)])
      (let [query-params (:query-params args)
            args         (dissoc args :query-params)
            path         (apply bidi/path-for app-routes encoded-navigation-event (apply concat (seq args)))]
        (when path
          (uri/set-query-string path query-params)))))))

(def path-for (memoize path-for*))

(defn current-path [app-state]
  (apply path-for (get-in app-state keypaths/navigation-message)))

(defn navigation-message-for
  ([uri] (navigation-message-for uri nil))
  ([uri query-params]
   (navigation-message-for uri query-params nil))
  ([uri query-params subdomain]
   (let [{nav-event :handler
          params    :route-params} (bidi/match-route app-routes
                                                     uri
                                                     :subdomain subdomain)]
     [(if nav-event (bidi->edn nav-event) events/navigate-not-found)
      (-> params
          (merge (when (seq query-params) {:query-params query-params}))
          keywordize-keys)])))

(defn sub-page?
  "Returns whether page1 is the same as page2 OR is a 'sub-page'.
  For example, [events/navigate-checkout-address] is a sub-page of
  [events/navigate-checkout]"
  [[page1-event page1-args] [page2-event page2-args]]
  (and (= (take (count page2-event) page1-event)
          page2-event)
       (every? #(= (%1 page2-args) (%1 page1-args)) (keys page2-args))))

(defn ^:private filter-args
  [raw-args]
  (or (dissoc raw-args :navigate/caused-by)
      {}))

(defn exact-page?
  "Returns whether page1 is the same as page2"
  [[page1-event page1-args] [page2-event page2-args]]
  (and (= page1-event page2-event)
       (= (filter-args page1-args) (filter-args page2-args))))

(defn should-redirect-affiliate-route?
  [experience]
  (= experience "affiliate"))

(defn environment->hostname [environment]
  (case environment
    "production" "mayvenn.com"
    "acceptance" "diva-acceptance.com"
    "storefront.localhost"))
