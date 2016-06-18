(ns storefront.app-routes
  (:require [bidi.bidi :as bidi]
            [storefront.events :as events]
            [storefront.platform.uri :as uri]
            #?(:cljs [cljs.reader :refer [read-string]])))

(defn edn->bidi [value]
  (keyword (prn-str value)))

(defn bidi->edn [value]
   (read-string (name value)))

(def app-routes
  ["" {"/"                               (edn->bidi events/navigate-home)
       "/categories"                     (edn->bidi events/navigate-categories)
       ["/categories/hair/" :taxon-slug] (edn->bidi events/navigate-category)
       ["/products/" :product-slug]      (edn->bidi events/navigate-product)
       "/guarantee"                      (edn->bidi events/navigate-guarantee)
       "/help"                           (edn->bidi events/navigate-help)
       "/policy/privacy"                 (edn->bidi events/navigate-privacy)
       "/policy/tos"                     (edn->bidi events/navigate-tos)
       "/login"                          (edn->bidi events/navigate-sign-in)
       "/login/getsat"                   (edn->bidi events/navigate-getsat-sign-in)
       "/signup"                         (edn->bidi events/navigate-sign-up)
       "/password/recover"               (edn->bidi events/navigate-forgot-password)
       ["/m/" :reset-token]              (edn->bidi events/navigate-reset-password)
       ["/c/" :shared-cart-id]           (edn->bidi events/navigate-shared-cart)
       "/account/edit"                   (edn->bidi events/navigate-account-manage)
       "/account/referrals"              (edn->bidi events/navigate-account-referrals)
       "/cart"                           (edn->bidi events/navigate-cart)
       "/stylist/commissions"            (edn->bidi events/navigate-stylist-dashboard-commissions)
       "/stylist/store_credits"          (edn->bidi events/navigate-stylist-dashboard-bonus-credit)
       "/stylist/referrals"              (edn->bidi events/navigate-stylist-dashboard-referrals)
       "/stylist/edit"                   (edn->bidi events/navigate-stylist-manage-account)
       "/share"                          (edn->bidi events/navigate-friend-referrals)
       "/checkout/login"                 (edn->bidi events/navigate-checkout-sign-in)
       "/checkout/address"               (edn->bidi events/navigate-checkout-address)
       "/checkout/payment"               (edn->bidi events/navigate-checkout-payment)
       "/checkout/confirm"               (edn->bidi events/navigate-checkout-confirmation)
       ["/orders/" :number "/complete"]  (edn->bidi events/navigate-order-complete)}])

(defn path-for [navigation-event & [args]]
  (let [query-params (:query-params args)
        args         (dissoc args :query-params)
        path         (apply bidi/path-for
                            app-routes
                            (edn->bidi navigation-event)
                            (apply concat (seq args)))]
    (when path
      (uri/set-query-string path query-params))))
