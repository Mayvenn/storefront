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
  [{:keys [header subheader background-image background-position button]} _ _]
  (component/create
   [:div.bg-aqua.white.center.flex.flex-column.self-stretch
    [:div.px2.flex.flex-column.items-center.justify-center
     {:style {:height "246px"}}
     [:div.bold header]
     [:div subheader]]
    [:div.flex.flex-auto.items-end.p5
     {:style {:background-image  (str "url(" background-image ")")
              :background-size   "cover"}}
     (ui/aqua-button (merge {:data-test (:data-test button)}
                            (utils/route-to (:target button)))
                     (:text button))]]))
