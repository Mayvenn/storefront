(ns storefront.components.promotion-banner
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.accessors.promos :as promos]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]))

(def allowed-navigation-events
  #{events/navigate-home
    events/navigate-category
    events/navigate-product
    events/navigate-cart})

(defn promotion-to-advertise [data]
  (let [promotions (get-in data keypaths/promotions)]
    (or (promos/find-promotion-by-code promotions (first (get-in data keypaths/order-promotion-codes)))
        (promos/find-promotion-by-code promotions (get-in data keypaths/pending-promo-code))
        (promos/default-advertised-promotion promotions))))

(defn promotion-banner-component [data owner]
  (om/component
   (html
    (when-let [promo (promotion-to-advertise data)]
      (when (allowed-navigation-events (get-in data keypaths/navigation-event))
        [:div.advertised-promo
         (:description promo)])))))
