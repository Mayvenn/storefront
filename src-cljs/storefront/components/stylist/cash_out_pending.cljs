(ns storefront.components.stylist.cash-out-pending
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.events :as events]
            [storefront.components.money-formatters :as mf]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.components.ui :as ui]
            [storefront.transitions :as transitions]
            [storefront.api :as api]
            [storefront.effects :as effects]))

(defn component [_ owner opts]
  (om/component
   (html
    [:div.container.p4.center
     (ui/large-spinner {:style {:height "6em"}})
     [:h2.my3 "Transfer in progress"]
     [:p "We are currently transferring your funds. Please stay on this page until the transfer completes."]])))

(defn query [data]
  {})

(defn built-component [data opts]
  (om/build component (query data) opts))


