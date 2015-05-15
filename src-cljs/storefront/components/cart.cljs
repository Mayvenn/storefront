(ns storefront.components.cart
  (:require [storefront.components.utils :as utils]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.taxons :refer [default-taxon-path]]
            [storefront.state :as state]))

(defn cart-component [data owner]
  (om/component
   (html
    [:div.cart-container
     [:p.empty-cart-message "OH NO!"]
     [:figure.empty-bag]
     [:p
      [:a.button.primary.continue.empty-cart
       (when-let [path (default-taxon-path data)]
         (utils/route-to data
                         events/navigate-category
                         {:taxon-path path}))
       "Let's Fix That"]]])))
