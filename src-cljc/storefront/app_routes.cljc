(ns storefront.app-routes
  (:require [bidi.bidi :as bidi]
            [storefront.events :as events]
            [storefront.platform.uri :as uri]
            [storefront.config :as config]
            #?(:cljs [cljs.reader :refer [read-string]])))

(defn edn->bidi [value]
  (keyword (prn-str value)))

(defn bidi->edn [value]
  (read-string (name value)))

(def static-page-routes
  {"/guarantee"                             (edn->bidi events/navigate-content-guarantee)
   "/help"                                  (edn->bidi events/navigate-content-help)
   "/about-us"                              (edn->bidi events/navigate-content-about-us)
   "/policy/privacy"                        (edn->bidi events/navigate-content-privacy)
   "/policy/tos"                            (edn->bidi events/navigate-content-tos)})

(def static-api-routes
  ["/static" static-page-routes])

(def app-routes
  ["" (merge static-page-routes
             {"/"                                      (edn->bidi events/navigate-home)
              "/categories"                            (edn->bidi events/navigate-categories)
              ["/categories/hair/" :named-search-slug] (edn->bidi events/navigate-category)
              ["/products/" :product-slug]             (edn->bidi events/navigate-product)
              "/login"                                 (edn->bidi events/navigate-sign-in)
              "/login/getsat"                          (edn->bidi events/navigate-getsat-sign-in)
              "/signup"                                (edn->bidi events/navigate-sign-up)
              "/password/recover"                      (edn->bidi events/navigate-forgot-password)
              ["/m/" :reset-token]                     (edn->bidi events/navigate-reset-password)
              ["/c/" :shared-cart-id]                  (edn->bidi events/navigate-shared-cart)
              "/account/edit"                          (edn->bidi events/navigate-account-manage)
              "/account/referrals"                     (edn->bidi events/navigate-account-referrals)
              "/cart"                                  (edn->bidi events/navigate-cart)
              "/shop/look"                             (edn->bidi events/navigate-shop-by-look)
              "/stylist/commissions"                   (edn->bidi events/navigate-stylist-dashboard-commissions)
              "/stylist/store_credits"                 (edn->bidi events/navigate-stylist-dashboard-bonus-credit)
              "/stylist/referrals"                     (edn->bidi events/navigate-stylist-dashboard-referrals)
              "/stylist/account/profile"               (edn->bidi events/navigate-stylist-account-profile)
              "/stylist/account/password"              (edn->bidi events/navigate-stylist-account-password)
              "/stylist/account/commission"            (edn->bidi events/navigate-stylist-account-commission)
              "/stylist/account/social"                (edn->bidi events/navigate-stylist-account-social)
              "/share"                                 (edn->bidi events/navigate-friend-referrals)
              "/checkout/login"                        (edn->bidi events/navigate-checkout-sign-in)
              "/checkout/address"                      (edn->bidi events/navigate-checkout-address)
              "/checkout/payment"                      (edn->bidi events/navigate-checkout-payment)
              "/checkout/confirm"                      (edn->bidi events/navigate-checkout-confirmation)
              ["/orders/" :number "/complete"]         (edn->bidi events/navigate-order-complete)
              "/_style"                                (edn->bidi events/navigate-style-guide)
              "/_style/color"                          (edn->bidi events/navigate-style-guide-color)
              "/_style/buttons"                        (edn->bidi events/navigate-style-guide-buttons)
              "/_style/form-fields"                    (edn->bidi events/navigate-style-guide-form-fields)
              "/_style/navigation"                     (edn->bidi events/navigate-style-guide-navigation)
              "/_style/navigation/tab1"                (edn->bidi events/navigate-style-guide-navigation-tab1)
              "/_style/navigation/tab3"                (edn->bidi events/navigate-style-guide-navigation-tab3)})])

(defn path-for [navigation-event & [args]]
  (let [query-params (:query-params args)
        args         (dissoc args :query-params)
        path         (apply bidi/path-for
                            app-routes
                            (edn->bidi navigation-event)
                            (apply concat (seq args)))]
    (when path
      (uri/set-query-string path query-params))))

(defn current-page? [[current-event current-args] target-event & [args]]
  (and (= (take (count target-event) current-event) target-event)
       (reduce #(and %1 (= (%2 args) (%2 current-args))) true (keys args))))
