(ns adventure.components.progress-bar
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
  [{:keys [header header-image data-test buttons]} _ _]
  (component/create
   [:div.col-12.col.bg-white
    {:style {:height "6px"}}
    "HELLO"]))

(defn query [data]
  {:this "that"})

(defn built-component
  [data opts]
  (component/build component (query data) opts))

