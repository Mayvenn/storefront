(ns catalog.core
  (:require catalog.category
            catalog.product-details
            storefront.components.shop-by-look
            storefront.components.shop-by-look-details
            storefront.components.shared-cart
            checkout.processing
            checkout.cart
            adventure.handlers
            adventure.hair-texture
            adventure.shop-hair
            adventure.how-shop-hair
            adventure.select-new-look
            adventure.bundlesets.hair-texture
            adventure.a-la-carte.hair-texture
            adventure.a-la-carte.hair-color
            adventure.a-la-carte.product-list
            adventure.a-la-carte.product-details
            adventure.look-detail
            [storefront.loader :as loader]))

(loader/set-loaded! :catalog)

