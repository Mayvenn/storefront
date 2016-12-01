(ns storefront.components.nav
  (:require [storefront.events :as events]
            [clojure.set :as set]))

(def auth-events
  #{events/navigate-sign-in
    events/navigate-sign-up
    events/navigate-forgot-password
    events/navigate-reset-password})

(def cart-events
  #{events/navigate-cart})

(def checkout-events
  #{events/navigate-checkout-sign-in
    events/navigate-checkout-address
    events/navigate-checkout-payment
    events/navigate-checkout-confirmation})

(def experiment-minimal-events
  checkout-events)

(def minimal-events
  (set/union auth-events
             cart-events
             checkout-events))

(defn minimal? [nav-event experiment?]
  (let [minimal-events (if experiment?
                         experiment-minimal-events
                         minimal-events)]
    (minimal-events nav-event)))
