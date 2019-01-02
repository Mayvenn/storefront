(ns adventure.budget
  (:require [storefront.events :as events]
            [storefront.transitions :as transitions]
            [storefront.keypaths :as keypaths]

            [storefront.component :as component]
            [adventure.components.multi-prompt :as multi-prompt]
            [storefront.accessors.experiments :as experiments]))

(defn ^:private query [data]
  {:header "How much do you usually spend on 3 bundles?"
   :header-image "http://placekitten.com/300/200"
   :buttons [{:text "$" :target nil}
             {:text "$$" :target nil}
             {:text "$$$" :target nil}]})

(defn built-component
  [data opts]
  (component/build multi-prompt/component (query data) opts))


