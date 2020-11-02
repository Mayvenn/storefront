(ns checkout.shop.cart-v2020-09
  (:require [api.orders :as api.orders]
            [checkout.shop.empty-cart-v2020-09 :as shop-empty]
            [checkout.shop.filled-cart-v2020-09 :as shop-filled]
            [storefront.accessors.sites :as sites]))

(defn ^:export page
  [app-state nav-event]
  (when (= :shop (sites/determine-site app-state))
    (let [{:order.items/keys [quantity]} (api.orders/current app-state)]
      (if (or (nil? quantity)
              (zero? quantity))
        (shop-empty/page app-state nav-event)
        (shop-filled/page app-state nav-event)))))
