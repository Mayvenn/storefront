(ns catalog.core
  (:require catalog.category
            catalog.product-details

            ;; Looks
            catalog.looks
            catalog.look-details-v202105

            ;; Shared Carts
            storefront.components.shared-cart

            ;; Cart
            checkout.processing
            checkout.cart
            checkout.classic-cart
            checkout.shop.cart-v2020-09
            checkout.behavior

            ;; Stylist matching/profile
            appointment-booking.core ; TODO is this right?

            mayvenn.shopping-quiz.crm-persona

            stylist-matching.find-your-stylist
            stylist-matching.stylist-results
            stylist-profile.core
            stylist-matching.core
            stylist-matching.match-success
            adventure.stylist-matching.stylist-gallery
            [storefront.loader :as loader]))

(loader/set-loaded! :catalog)
