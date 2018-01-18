(ns storefront.components.popup
  (:require [storefront.platform.component-utils :as utils]
            [storefront.components.cart :as cart]
            [storefront.components.email-capture :as email-capture]
            [storefront.components.stylist.referrals :as stylist.referrals]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn built-component [data _]
  (let [opts {:opts {:close-attrs (utils/fake-href events/control-popup-hide)}}]
    (condp = (get-in data keypaths/popup)
      :email-capture        (email-capture/built-component data opts)
      :share-cart           (cart/built-share-link-component data opts)
      :refer-stylist        (stylist.referrals/built-refer-component data opts)
      :refer-stylist-thanks (stylist.referrals/built-thanks-component data opts)
      nil)))
