(ns storefront.components.promotion-banner
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]))

(def allowed-navigation-events
  #{events/navigate-home
    events/navigate-category
    events/navigate-product
    events/navigate-cart})

(defn promotion-lookup-map [promotions]
  (->> promotions
       (filter :code)
       (map (juxt :code identity))
       (into {})))

(defn find-promotion-by-code [promotions code]
  ((promotion-lookup-map promotions) code))

(defn default-advertised-promotion [promotions]
  (first (filter :advertised promotions)))

(defn promotion-to-advertise [data]
  (let [promotions (get-in data keypaths/promotions)]
    (or (find-promotion-by-code promotions (first (get-in data keypaths/order-promotion-codes)))
        (find-promotion-by-code promotions (get-in data keypaths/pending-promo-code))
        (default-advertised-promotion promotions))))

(defn promotion-banner-component [data owner]
  (om/component
   (html
    (when-let [promo (promotion-to-advertise data)]
      (when (allowed-navigation-events (get-in data keypaths/navigation-event))
        [:div.advertised-promo
         (:description promo)])))))
