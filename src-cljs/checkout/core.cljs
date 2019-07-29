(ns checkout.core
  (:require [storefront.loader :as loader]
            storefront.components.checkout-address
            storefront.components.checkout-returning-or-guest
            storefront.components.checkout-steps
            storefront.components.checkout-sign-in
            storefront.components.checkout-address
            storefront.components.checkout-address-auth-required
            storefront.components.checkout-payment
            storefront.components.checkout-complete
            adventure.handlers
            adventure.checkout.wait
            checkout.confirmation.summary
            checkout.confirmation
            checkout.processing))

(loader/set-loaded! :checkout)
