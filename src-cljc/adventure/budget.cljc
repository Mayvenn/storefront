(ns adventure.budget
  (:require [storefront.events :as events]
            [storefront.transitions :as transitions]
            [storefront.keypaths :as keypaths]

            [storefront.component :as component]
            [adventure.components.basic-prompt :as basic-prompt]
            [storefront.accessors.experiments :as experiments]))

(defn ^:private query [data]
  {:header "How much do you usually spend on 3 bundles?"
   :header-image "https://via.placeholder.com/150x100"
   :buttons [{:text "asdf" :target nil}]})

(defn built-component
  [data opts]
  (component/build basic-prompt/component (query data) opts))


