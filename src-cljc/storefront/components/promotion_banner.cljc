(ns storefront.components.promotion-banner
  (:require [storefront.accessors.promos :as promos]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]))

(def allowed-navigation-events
  #{events/navigate-home
    events/navigate-category
    events/navigate-cart})

(defn promotion-to-advertise [data]
  (let [promotions (get-in data keypaths/promotions)]
    (or (promos/find-promotion-by-code promotions (first (get-in data keypaths/order-promotion-codes)))
        (promos/find-promotion-by-code promotions (get-in data keypaths/pending-promo-code))
        (promos/default-advertised-promotion promotions))))

(defn promotion-banner-component [data owner opts]
  (component/create
   (when-let [promo (promotion-to-advertise data)]
     (when (allowed-navigation-events (get-in data keypaths/navigation-event))
       [:div.advertised-promo
        (:description promo)]))))
