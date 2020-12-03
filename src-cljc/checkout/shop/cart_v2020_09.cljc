(ns checkout.shop.cart-v2020-09
  (:require api.current
            api.orders
            [checkout.shop.empty-cart-v2020-09 :as shop-empty]
            [checkout.shop.filled-cart-v2020-09 :as shop-filled]
            [storefront.accessors.sites :as sites]))

(defn ^:export page
  [app-state nav-event]
  (when (= :shop (sites/determine-site app-state))
    (let [{:order.items/keys [quantity]} (api.orders/current app-state)
          stylist                        (api.current/stylist app-state)]
      (if (or (not (or (nil? quantity)
                       (zero? quantity)))
              stylist)
        (shop-filled/page app-state nav-event)
        (shop-empty/page app-state nav-event)))))
