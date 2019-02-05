(ns adventure.get-in-contact
  (:require [storefront.events :as events]
            [storefront.component :as component]
            [adventure.components.offset-prompt :as offset-prompt]))

(defn ^:private query [data]
  {:prompt           [:div.mx-auto.col-8.center "Let’s get you in contact with a stylist ASAP"]
   :mini-prompt      "Subheader"
   :header-data      {:back-link    events/navigate-adventure-budget
                      :progress 3}
   :background-image "http://placekitten.com/315/420"
   :button           {:text   "Next"
                      :target events/navigate-adventure-what-next}})

(defn built-component
  [data opts]
  (component/build offset-prompt/component (query data) opts))
