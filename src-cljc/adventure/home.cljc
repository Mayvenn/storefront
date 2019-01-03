(ns adventure.home
  (:require [storefront.events :as events]
            [storefront.component :as component]
            [adventure.components.basic-prompt :as basic-prompt]
            [storefront.accessors.experiments :as experiments]))

(defn ^:private query [data]
  {:header              "Welcome"
   :subheader           "Ready to be matched with great hair & a great stylist?"
   :background-image    "http://placekitten.com/100/100"
   :background-position "center bottom"
   :button              {:text "Get Started"
                         :data-test "adventure-home-get-started"
                         :target events/navigate-adventure-budget}})

(defn built-component
  [data opts]
  (component/build basic-prompt/component (query data) opts))

