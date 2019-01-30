(ns adventure.match-success
  (:require [storefront.events :as events]
            [storefront.component :as component]
            [adventure.components.basic-prompt :as basic-prompt]
            [adventure.handlers :as handlers]
            [adventure.home :as home]
            [storefront.accessors.experiments :as experiments]))

(defn ^:private query [data]
  {:prompt               "Congrats on matching with !@#%$!"
   :mini-prompt          "We'll connect you with your stylist shortly. But first, pick out your hair!"
   :show-logo?           false
   :background-overrides {:style
                          {:background-size     "200px"
                           :background-position "right 15px bottom 150px"
                           :background-image
                           "url(//ucarecdn.com/8a87f86f-948f-48da-b59d-3ca4d8c6d5a0/-/format/png/-/quality/normal/)"}}
   :header-data          {:back-link    events/navigate-adventure-stylist-results}
   :button               {:text      "Show me hair"
                          :data-test "adventure-match-success-choice-show-hair"
                          :color     :white
                          :target    events/navigate-adventure-home}})

(defn built-component
  [data opts]
  (component/build basic-prompt/component (query data) opts))
