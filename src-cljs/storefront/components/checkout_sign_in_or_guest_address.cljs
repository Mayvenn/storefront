(ns storefront.components.checkout-sign-in-or-guest-address
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.sign-in :as sign-in]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]))

(defn component [sign-in-form-data owner]
  (om/component
   (html
    (ui/narrow-container
     [:h2.center.my2.navy "New Page"]))))

(defn built-component [data opts]
  (om/build component (sign-in/query data) opts))
