(ns adventure.home
  (:require [storefront.events :as events]
            [storefront.component :as component]
            [adventure.components.basic-prompt :as basic-prompt]
            [adventure.handlers :as handlers]
            [storefront.accessors.experiments :as experiments]))

(defn ^:private query [data]
  {:prompt           "Welcome"
   :mini-prompt      "Ready to be matched with great hair & a great stylist?"
   :background-image "http://placekitten.com/200/150"
   :button           {:text      "Get Started"
                      :data-test "adventure-home-get-started"
                      :target    events/navigate-adventure-time-frame}})

(defn built-component
  [data opts]
  (component/build basic-prompt/component (query data) opts))
