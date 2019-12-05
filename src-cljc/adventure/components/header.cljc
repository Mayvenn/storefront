(ns adventure.components.header
  (:require [storefront.component :as component :refer [defcomponent]]))

(defcomponent component
  [{:keys [header-attrs
           right-corner
           title]} _ _]
  (let [{:keys [id opts value]} right-corner]
    [:div#header header-attrs
     [:div.flex.items-center.justify-between
      {:style {:height "66px"}}
      [:div {:style {:width "50px"}}]
      [:div.mx-auto title]
      [:a.block.p3.flex.items-center
       (merge {:data-test id} opts)
       (when id value)]]]))

(defn built-component
  [data opts]
  (component/build component data opts))
