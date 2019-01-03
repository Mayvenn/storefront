(ns adventure.when-info
  (:require [storefront.events :as events]
            [storefront.component :as component]
            [adventure.components.basic-prompt :as basic-prompt]))

(defn ^:private query [data]
  {:header              "Let’s get you in contact with a stylist ASAP"
   :subheader           "Subheader"
   :background-image    "http://placekitten.com/300/300"
   :background-position "center bottom"
   :button              {:text "Next"
                         :target nil}})

(defn built-component
  [data opts]
  (component/build basic-prompt/component (query data) opts))
