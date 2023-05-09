(ns storefront.components.guide-clipin-extensions
  (:require [storefront.component :as c]
            [storefront.keypaths :as k]
            [storefront.effects :as fx]
            [storefront.events :as e]))

(c/defcomponent component
  [{:keys []} owner opts]
  [:div "TK"])

(defn query [app-state]
  {})

(defn built-component [app-state opts]
  (c/build component (query app-state) opts))