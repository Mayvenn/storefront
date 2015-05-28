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

(defn promotion-banner-component [data owner]
  (om/component
   (html
    (when-let [promo (first (get-in data keypaths/promotions))]
      (when (allowed-navigation-events (get-in data keypaths/navigation-event-path))
        [:div.advertised-promo
         (str
          "Save "
          (:percent_discount promo)
          "% now - use promo code: "
          (:code promo))])))))
