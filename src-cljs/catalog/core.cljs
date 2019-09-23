(ns catalog.core
  (:require catalog.category
            catalog.product-details
            storefront.components.shop-by-look
            storefront.components.shop-by-look-details
            storefront.components.shared-cart
            checkout.processing
            checkout.cart
            adventure.handlers ;; TODO, wherever these go, maybe they shouldn't be here
            [storefront.loader :as loader]))

(loader/set-loaded! :catalog)

