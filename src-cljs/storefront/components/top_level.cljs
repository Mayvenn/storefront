(ns storefront.components.top-level
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.state :as state]
            [storefront.events :as events]
            [storefront.components.utils :as utils]
            [storefront.components.header :refer [header-component]]
            [storefront.components.footer :refer [footer-component]]
            [storefront.components.home :refer [home-component]]
            [cljs.core.async :refer [put!]]))

(defn top-level-component [data owner]
  (om/component
   (html
    [:div
     (om/build header-component data)
     [:main {:role "main"}
      [:div.container
       (om/build
        (condp = (get-in data state/navigation-event-path)
          events/navigate-home home-component)
        data)]]
     (om/build footer-component data)])))
