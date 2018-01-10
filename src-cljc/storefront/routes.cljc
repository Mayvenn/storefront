(ns storefront.routes
  (:require [bidi.bidi :as bidi]
            [clojure.walk :refer [keywordize-keys]]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.uri :as uri]
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
   "/program-terms"   (edn->bidi events/navigate-content-program-terms)
   "/our-hair"        (edn->bidi events/navigate-content-our-hair)})

(def static-api-routes
  ["/static" static-page-routes])

(def style-guide-routes
  {"/_style"                 (edn->bidi events/navigate-style-guide)
   "/_style/color"           (edn->bidi events/navigate-style-guide-color)
   "/_style/spacing"         (edn->bidi events/navigate-style-guide-spacing)
   "/_style/buttons"         (edn->bidi events/navigate-style-guide-buttons)
   "/_style/form-fields"     (edn->bidi events/navigate-style-guide-form-fields)
   "/_style/navigation"      (edn->bidi events/navigate-style-guide-navigation)
   "/_style/navigation/tab1" (edn->bidi events/navigate-style-guide-navigation-tab1)
   "/_style/navigation/tab3" (edn->bidi events/navigate-style-guide-navigation-tab3)
   "/_style/progress"        (edn->bidi events/navigate-style-guide-progress)
   "/_style/carousel"        (edn->bidi events/navigate-style-guide-carousel)})

(def leads-routes
  {"/stylists/welcome"           (edn->bidi events/navigate-leads-home)
   "/stylists/thank-you"         (edn->bidi events/navigate-leads-resolve)
   "/stylists/flows/2/details"   (edn->bidi events/navigate-leads-registration-details)
   "/stylists/flows/2/thank-you" (edn->bidi events/navigate-leads-registration-resolve)})

(def catalog-routes
  {["/categories/" [#"\d+" :catalog/category-id] "-" :page/slug]
   (edn->bidi events/navigate-category)

   ["/categories/hair/" :named-search-slug]
   (edn->bidi events/navigate-legacy-named-search)
   ["/categories/hair/" :named-search-slug "/social"]
   (edn->bidi events/navigate-legacy-ugc-named-search)

   ["/products/" [#"\d+" :catalog/product-id] "-" :page/slug]
   (edn->bidi events/navigate-product-details)})

(def app-routes
  ["" (merge static-page-routes
             style-guide-routes
             leads-routes
             catalog-routes
             {"/"                                     (edn->bidi events/navigate-home)
              "/login"                                (edn->bidi events/navigate-sign-in)
              "/logout"                               (edn->bidi events/navigate-sign-out)
              "/signup"                               (edn->bidi events/navigate-sign-up)
              "/password/recover"                     (edn->bidi events/navigate-forgot-password)
              ["/m/" :reset-token]                    (edn->bidi events/navigate-reset-password)
              ["/c/" :shared-cart-id]                 (edn->bidi events/navigate-shared-cart)
              "/account/edit"                         (edn->bidi events/navigate-account-manage)
              "/account/referrals"                    (edn->bidi events/navigate-account-referrals)
              "/cart"                                 (edn->bidi events/navigate-cart)
              "/shop/bundle-deals"                    (edn->bidi events/navigate-shop-bundle-deals)
              "/shop/look"                            (edn->bidi events/navigate-shop-by-look)
              ["/shop/look/" :look-id]                (edn->bidi events/navigate-shop-by-look-details)
              "/stylist/earnings"                     (edn->bidi events/navigate-stylist-dashboard-earnings)
              "/stylist/store_credits"                (edn->bidi events/navigate-stylist-dashboard-bonus-credit)
              "/stylist/referrals"                    (edn->bidi events/navigate-stylist-dashboard-referrals)
              ["/stylist/commission/" :commission-id] (edn->bidi events/navigate-stylist-dashboard-commission-details)
              "/stylist/share-your-store"             (edn->bidi events/navigate-stylist-share-your-store)
              "/stylist/account/profile"              (edn->bidi events/navigate-stylist-account-profile)
              "/stylist/account/portrait"             (edn->bidi events/navigate-stylist-account-portrait)
              "/stylist/account/password"             (edn->bidi events/navigate-stylist-account-password)
              "/stylist/account/commission"           (edn->bidi events/navigate-stylist-account-commission)
              "/stylist/account/social"               (edn->bidi events/navigate-stylist-account-social)
              "/share"                                (edn->bidi events/navigate-friend-referrals)
              "/gallery"                              (edn->bidi events/navigate-gallery)
              "/gallery/add"                          (edn->bidi events/navigate-gallery-image-picker)
              "/checkout/returning_or_guest"          (edn->bidi events/navigate-checkout-returning-or-guest)
              "/checkout/login"                       (edn->bidi events/navigate-checkout-sign-in)
              "/checkout/address"                     (edn->bidi events/navigate-checkout-address)
              "/checkout/payment"                     (edn->bidi events/navigate-checkout-payment)
              "/checkout/confirm"                     (edn->bidi events/navigate-checkout-confirmation)
              ["/orders/" :number "/complete"]        (edn->bidi events/navigate-order-complete)})])

(defn path-for [navigation-event & [args]]
  (let [query-params (:query-params args)
        args         (dissoc args :query-params)
        path         (apply bidi/path-for
                            app-routes
                            (edn->bidi navigation-event)
                            (apply concat (seq args)))]
    (when path
      (uri/set-query-string path query-params))))

(defn current-path [app-state]
  (apply path-for (get-in app-state keypaths/navigation-message)))

(defn navigation-message-for
  ([uri] (navigation-message-for uri nil))
  ([uri query-params]
   (let [{nav-event :handler params :route-params} (bidi/match-route app-routes uri)]
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

(defn exact-page?
  "Returns whether page1 is the same as page2"
  [[page1-event page1-args] [page2-event page2-args]]
  (= [page1-event (or page1-args {})]
     [page2-event (or page2-args {})]))
