(ns adventure.components.basic-prompt
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
  [{:keys [title subtitle background-image background-position button-text button-target]} _ _]
  (component/create
   [:div.bg-aqua.white.absolute.top-0.right-0.bottom-0.left-0.center
    {:style {:background-image (str "url(" background-image ")")
             :background-position background-position
             :background-repeat "no-repeat"
             :background-size "100% auto"}}
    [:div.px2.absolute.col.col-12 {:style {:top "15%"}}
     [:div.bold title]
     [:div subtitle]]
    [:div.absolute.bottom-0.col.col-12.p5 (ui/aqua-button {} button-text)]]))
