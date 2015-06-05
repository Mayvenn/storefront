(ns storefront.checkout
  (:require [storefront.events :as events]))

(def steps
  [{:event events/navigate-checkout-address
    :name "address"}
   {:event events/navigate-checkout-delivery
    :name "delivery"}
   {:event events/navigate-checkout-payment
    :name "payment"}
   {:event events/navigate-checkout-confirmation
    :name "confirm"}])
