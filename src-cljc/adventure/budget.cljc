(ns adventure.budget
  (:require [storefront.events :as events]
            [storefront.component :as component]
            [adventure.components.multi-prompt :as multi-prompt]))

(defn ^:private query [data]
  {:header "How much do you usually spend on 3 bundles?"
   :header-image "http://placekitten.com/300/200"
   :data-test "adventure-budget-choice"
   :buttons [{:text "$" :value 1 :target events/navigate-adventure-when-info}
             {:text "$$" :value 2 :target events/navigate-adventure-when-info}
             {:text "$$$" :value 3 :target events/navigate-adventure-when-info}]})

(defn built-component
  [data opts]
  (component/build multi-prompt/component (query data) opts))
