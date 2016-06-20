(ns storefront.components.popup
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.cart :as cart]
            [storefront.platform.component-utils :as utils]
            [storefront.components.stylist.referrals :as stylist.referrals]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn popup-component [data]
  (condp = (get-in data keypaths/popup)
    :share-cart (om/build cart/share-link-component (cart/query-share-link data)
                          {:opts {:on-close (utils/send-event-callback events/control-popup-hide)}})
    :refer-stylist (om/build stylist.referrals/refer-component (stylist.referrals/query-refer data)
                             {:opts {:on-close (utils/send-event-callback events/control-popup-hide)}})
    :refer-stylist-thanks (om/build stylist.referrals/thanks-component nil)
    nil))
