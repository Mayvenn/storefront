(ns adventure.home
  (:require [adventure.components.layered :as layered]
            adventure.handlers ;; Needed for its defmethods
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            adventure.shop-home))

(defn query
  [data]
  {})

(defn component
  [data _ opts]
  (component/create
   [:div
    [:div.flex.items-center
     [:div.flex-auto.py3 (ui/clickable-logo
                          {:event events/navigate-adventure-home
                           :data-test "header-logo"
                           :height    "40px"})]]
    (component/build layered/component (adventure.shop-home/query data) opts)]))

(defn built-component
  [data opts]
  (component/build component (query data) opts))


