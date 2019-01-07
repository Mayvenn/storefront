(ns adventure.time-frame
  (:require [storefront.events :as events]
            [storefront.component :as component]
            [adventure.components.multi-prompt :as multi-prompt]))

(defn ^:private query [data]
  {:prompt       "Question - When are you looking to get your hair done?"
   :prompt-image "http://placekitten.com/300/200"
   :data-test    "adventure-time-frame-choice"
   :header-data  {:current-step 1
                  :title        "Basic Info"
                  :back-link    events/navigate-adventure-home
                  :subtitle     "Step 1 of 3"}
   :buttons      [{:text             "ASAP"
                   :data-test-suffix "asap"
                   :value            {:time-frame "asap"}
                   :target           events/navigate-adventure-budget}
                  {:text             "Within Weeks"
                   :data-test-suffix "weeks"
                   :value            {:time-frame "weeks"}
                   :target           events/navigate-adventure-budget}
                  {:text             "Within Months"
                   :data-test-suffix "months"
                   :value            {:time-frame "months"}
                   :target           events/navigate-adventure-budget}]})

(defn built-component
  [data opts]
  (component/build multi-prompt/component (query data) opts))
