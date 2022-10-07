(ns storefront.accessors.nav
  (:require [storefront.events :as events]
            [storefront.accessors.experiments :as experiments]
            [clojure.set :as set]))

(def ^:private plain-auth-events
  #{events/navigate-sign-in
    events/navigate-sign-up
    events/navigate-order-details-sign-up
    events/navigate-sign-out
    events/navigate-forgot-password
    events/navigate-reset-password
    events/navigate-force-set-password})

(def ^:private cart-events
  #{events/navigate-cart})

(def ^:private checkout-auth-events
  #{events/navigate-checkout-returning-or-guest
    events/navigate-checkout-sign-in})

(def ^:private checkout-flow-events
  #{events/navigate-checkout-add
    events/navigate-checkout-returning-or-guest
    events/navigate-checkout-address
    events/navigate-checkout-payment
    events/navigate-checkout-confirmation
    events/navigate-checkout-processing})

(def ^:private payout-events
  #{events/navigate-stylist-dashboard-cash-out-pending})

(def ^:private checkout-events
  (set/union checkout-auth-events checkout-flow-events))

(def ^:private voucher-events
  #{events/navigate-voucher-redeem events/navigate-voucher-redeemed})

(def ^:private stylist-dashboard-events
  #{events/navigate-stylist-dashboard-balance-transfer-details
    events/navigate-stylist-dashboard-cash-out-begin
    events/navigate-stylist-dashboard-cash-out-pending
    events/navigate-stylist-dashboard-cash-out-success
    events/navigate-stylist-dashboard-order-details
    events/navigate-v2-stylist-dashboard
    events/navigate-v2-stylist-dashboard-orders
    events/navigate-v2-stylist-dashboard-payments})

(def ^:private account-events
  #{events/navigate-account-manage
    events/navigate-stylist-account-payout
    events/navigate-stylist-account-password
    events/navigate-stylist-account-portrait
    events/navigate-stylist-account-profile
    events/navigate-stylist-account-social})

(def ^:private share-cart-events
  #{events/navigate-shared-cart})

(def ^:private sharing-store-events
  #{events/navigate-stylist-share-your-store})

(def auth-events
  (set/union plain-auth-events checkout-auth-events))

(def ^:private order-complete-events
  #{events/navigate-order-complete})

(def ^:private design-system
  #{events/navigate-design-system
    events/navigate-design-system-component-library})

(def return-blacklisted?
  (-> auth-events
      (conj events/navigate-not-found)))

(def minimal-footer-events
  (set/union account-events
             auth-events
             cart-events
             checkout-events
             order-complete-events
             payout-events
             sharing-store-events
             share-cart-events
             stylist-dashboard-events
             voucher-events
             design-system))

(def minimal-header-events
  (set/union cart-events
             checkout-events
             payout-events
             design-system))

(def gallery-page-events
  #{events/navigate-gallery-edit
    events/navigate-gallery-appointments
    events/navigate-gallery-image-picker
    events/navigate-gallery-photo})

(def adventures-quiz-events
  #{events/navigate-adventure-quiz})

(defn hide-footer? [event]
  (contains? (set/union gallery-page-events
                        adventures-quiz-events)
             event))

(defn show-minimal-footer? [event]
  (contains? minimal-footer-events event))

(defn show-minimal-header? [data event]
  (contains?
   (set/union share-cart-events
              minimal-header-events) event))
