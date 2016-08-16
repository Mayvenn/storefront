(ns storefront.components.popup
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.platform.component-utils :as utils]
            [storefront.components.cart :as cart]
            [storefront.components.stylist.referrals :as stylist.referrals]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn component [{:keys [current-popup share-link refer]} _ _]
  (om/component
   (html
    [:div
     (condp = current-popup
       :share-cart           (om/build cart/share-link-component share-link
                                       {:opts {:on-close (utils/send-event-callback events/control-popup-hide)}})
       :refer-stylist        (om/build stylist.referrals/refer-component refer
                                       {:opts {:on-close (utils/send-event-callback events/control-popup-hide)}})
       :refer-stylist-thanks (om/build stylist.referrals/thanks-component nil)
       nil)])))

(defn query [data]
  {:current-popup (get-in data keypaths/popup)
   :share-link    (cart/query-share-link data)
   :refer         (stylist.referrals/query-refer data)})

(defn built-component [data opts]
  (om/build component (query data) opts))
