(ns checkout.core
  (:require [storefront.loader :as loader]
            storefront.components.checkout-address
            storefront.components.checkout-steps
            storefront.components.checkout-sign-in
            storefront.components.checkout-address
            storefront.components.checkout-address-auth-required
            storefront.components.checkout-payment
            storefront.components.checkout-complete
            storefront.components.places
            adventure.handlers
            adventure.checkout.wait
            checkout.confirmation
            checkout.returning-or-guest-v2020-05
            checkout.processing))

(loader/set-loaded! :checkout)
