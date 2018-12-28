(ns adventure.home
  (:require [storefront.events :as events]
            [storefront.transitions :as transitions]
            [storefront.keypaths :as keypaths]

            [storefront.component :as component]
            [adventure.components.basic-prompt :as basic-prompt]
            [storefront.accessors.experiments :as experiments]))

(defn ^:private query [data]
  {:header              "Welcome"
   :subheader           "Ready to be matched with great hair & a great stylist?"
   :background-image    "https://via.placeholder.com/100x100"
   :background-position "center bottom"
   :button              {:text "Get Started"
                         :target events/navigate-adventure-budget}})

(defn built-component
  [data opts]
  (component/build basic-prompt/component (query data) opts))

