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
   [:div.bg-aqua.white.absolute.top-0.right-0.bottom-0.left-0.center
    [:div.px2.absolute.col.col-12 {:style {:top "15%"
                                           :background-image (str "url(" header-image ")")
                                           :background-repeat "no-repeat"
                                           :background-size "100% auto"}}
     [:div.bold header]]
    [:div.absolute.bottom-0.col.col-12.p5 (ui/aqua-button {} button-text)]]))
