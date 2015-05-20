(ns storefront.components.checkout-delivery
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.state :as state]
            [storefront.events :as events]
            [storefront.components.checkout-steps :refer [checkout-step-bar]]))

(defn checkout-delivery-component [data owner]
  (om/component
   (html
    [:div#checkout
     (checkout-step-bar data)
     [:h1 ""]])))
