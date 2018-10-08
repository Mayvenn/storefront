(ns storefront.accessors.nav
  (:require [storefront.events :as events]
            [clojure.set :as set]))

(def ^:private plain-auth-events
  #{events/navigate-sign-in
    events/navigate-sign-up
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
  #{events/navigate-checkout-returning-or-guest
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
    events/navigate-account-referrals
    events/navigate-stylist-account-payout
    events/navigate-stylist-account-password
    events/navigate-stylist-account-portrait
    events/navigate-stylist-account-profile
    events/navigate-stylist-account-social})

(def ^:private sharing-events
  #{events/navigate-friend-referrals
    events/navigate-friend-referrals-freeinstall
    events/navigate-shared-cart
    events/navigate-stylist-share-your-store})

(def auth-events
  (set/union plain-auth-events checkout-auth-events))

(def return-blacklisted?
  (conj auth-events events/navigate-not-found))

(def minimal-footer-events
  (set/union account-events
             auth-events
             cart-events
             checkout-events
             payout-events
             sharing-events
             stylist-dashboard-events
             voucher-events))

(def minimal-header-events
  (set/union cart-events
             checkout-events
             payout-events
             voucher-events))

(defn show-minimal-footer? [event]
  (contains? minimal-footer-events event))

(defn show-minimal-header? [event]
  (contains? minimal-header-events event))

(defn lead-page? [event]
  (= events/navigate-leads (take 2 event)))
