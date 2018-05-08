(ns storefront.components.popup
  (:require [storefront.platform.component-utils :as utils]
            [storefront.components.share-your-cart :as share-your-cart]
            [storefront.components.email-capture :as email-capture]
            [storefront.components.free-install :as free-install]
            [storefront.components.seventy-five-off-install :as seventy-five-off-install]
            [storefront.components.stylist.referrals :as stylist.referrals]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn built-component [data _]
  (let [opts {:opts {:close-attrs (utils/fake-href events/control-popup-hide)}}]
    (case (get-in data keypaths/popup)
      :free-install             (free-install/built-component data opts)
      :seventy-five-off-install (seventy-five-off-install/built-component data opts)
      :email-capture            (email-capture/built-component data opts)
      :share-cart               (share-your-cart/built-component data opts)
      :refer-stylist            (stylist.referrals/built-refer-component data opts)
      :refer-stylist-thanks     (stylist.referrals/built-thanks-component data opts)
      nil)))
