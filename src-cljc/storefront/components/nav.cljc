(ns storefront.components.nav
  (:require [storefront.events :as events]))

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

(def minimal-events
  checkout-events)
