(ns catalog.core
  (:require catalog.category
            catalog.product-details
            catalog.looks
            catalog.look-details
            storefront.components.shared-cart
            checkout.processing
            checkout.cart
            checkout.added-to-cart
            checkout.classic-cart
            checkout.shop.cart-v202004
            checkout.behavior
            adventure.handlers ;; TODO, wherever these go, maybe they shouldn't be here
            [storefront.loader :as loader]))

(loader/set-loaded! :catalog)
