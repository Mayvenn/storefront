(ns storefront.components.accordion-v2022-10
  (:require [mayvenn.visual.tools :refer [with]]
            [storefront.component :as c]
            [storefront.platform.component-utils :as utils]
            [storefront.components.svg :as svg]
            [storefront.events :as events]
            [storefront.transitions :as transitions]
            [storefront.components.ui :as ui]))

;; TODO c'mon do this right
(def accordion--open [:accordion-open])
(def accordion--closed [:accordion-closed])

(c/defcomponent drawer
  [data _ {:keys [face-component contents-component idx id] :as opts}]
  [:div
   [:a.block.inherit-color
    (utils/fake-href accordion--open {:idx idx})
    (c/build face-component (:face data))]
   (c/build contents-component (:contents data))])

(c/defcomponent component
  [{:keys [tabs id keypath drawers] :as data} _ opts]
  (when (and id drawers)
    [:div.mx4
     (c/elements drawer data :drawers :default opts)]))
