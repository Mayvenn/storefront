(ns storefront.accessors.nav
  (:require [storefront.events :as events]
            [clojure.set :as set]))

(def plain-auth-events
  #{events/navigate-sign-in
    events/navigate-sign-up
    events/navigate-sign-out
    events/navigate-forgot-password
    events/navigate-reset-password
    events/navigate-force-set-password})

(def cart-events
  #{events/navigate-cart})

(def checkout-auth-events
  #{events/navigate-checkout-returning-or-guest
    events/navigate-checkout-sign-in})

(def checkout-flow-events
  #{events/navigate-checkout-returning-or-guest
    events/navigate-checkout-address
    events/navigate-checkout-payment
    events/navigate-checkout-confirmation})

(def payout-events
  #{events/navigate-stylist-dashboard-cash-out-pending})

(def auth-events
  (set/union plain-auth-events checkout-auth-events))

(def checkout-events
  (set/union checkout-auth-events checkout-flow-events))

(def return-blacklisted?
  (conj auth-events events/navigate-not-found))

(def minimal-events
  (set/union checkout-events
             payout-events))

(defn show-minimal-footer? [event experiment-auto-complete?]
  (if experiment-auto-complete?
    (= event events/navigate-cart)
    (minimal-events event)))

(defn lead-page? [event]
  (= events/navigate-leads (take 2 event)))
