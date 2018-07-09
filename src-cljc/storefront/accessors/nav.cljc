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

(def auth-events
  (set/union plain-auth-events checkout-auth-events))

(def return-blacklisted?
  (conj auth-events events/navigate-not-found))

(def minimal-events
  (set/union checkout-events
             payout-events
             cart-events
             voucher-events))

(defn show-minimal-footer? [event]
  (contains? minimal-events event))

(defn lead-page? [event]
  (= events/navigate-leads (take 2 event)))
