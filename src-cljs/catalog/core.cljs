(ns catalog.core
  (:require catalog.category
            catalog.product-details
            storefront.components.shop-by-look
            storefront.components.shop-by-look-details
            storefront.components.shared-cart
            checkout.processing
            checkout.cart
            checkout.classic-cart
            checkout.shop.cart-v202004
            adventure.handlers ;; TODO, wherever these go, maybe they shouldn't be here
            [storefront.loader :as loader]))

(loader/set-loaded! :catalog)

