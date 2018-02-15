(ns storefront.components.stylist.cash-out-success
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]))

(defn component [_ owner opts]
  (om/component
   (html
    [:div.container.p4.center
     (svg/circled-check {:class "stroke-teal"
                         :style {:width "100px" :height "100px"}})
     [:h2.my3 "Cha-Ching!"]
     [:p.my4 "You have successfully cashed out your earnings. View your transfer by clicking the button below."]
     ;; TODO(jeff): add balance-transfer-id
     (ui/teal-button (utils/route-to events/navigate-stylist-dashboard-balance-transfer-details) "See Transfer")])))

(defn built-component [data opts]
  (om/build component {} opts))

