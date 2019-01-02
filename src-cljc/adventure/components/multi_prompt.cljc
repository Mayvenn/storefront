(ns adventure.components.multi-prompt
  (:require #?@(:cljs [[om.core :as om]])
            [storefront.assets :as assets]
            [storefront.component :as component]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(defn component
  [{:keys [header header-image buttons]} _ _]
  (component/create
   [:div.bg-aqua.white.center.bold.absolute.top-0.left-0.right-0.bottom-0
    [:div
     [:img.block.col-12 {:src header-image}]
     [:div.absolute.z1.col-12.p5 {:style {:top "15%"}} header]]
    [:div.p5
     (for [button buttons]
       [:div (ui/aqua-button {} (:text button))])]]))
