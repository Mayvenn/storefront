(ns storefront.components.my-orders
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn my-orders-component [data owner]
  (om/component
   (html
    [:h1 "helloooo"])))
