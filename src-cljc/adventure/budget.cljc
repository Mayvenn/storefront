(ns adventure.budget
  (:require [storefront.events :as events]
            [storefront.component :as component]
            [adventure.components.multi-prompt :as multi-prompt]))

(defn ^:private query [data]
  {:prompt       "How much do you usually spend on 3 bundles?"
   :prompt-image "http://placekitten.com/300/200"
   :data-test    "adventure-budget-choice"
   :header-data  {:current-step 1
                  :title        "Basic Info"
                  :back-link    events/navigate-adventure-home
                  :subtitle     "Step 1 of 3"}
   :buttons      [{:text             "$"
                   :data-test-suffix "1"
                   :value            {:budget 1}
                   :target           events/navigate-adventure-get-in-contact}
                  {:text             "$$"
                   :data-test-suffix "2"
                   :value            {:budget 2}
                   :target           events/navigate-adventure-get-in-contact}
                  {:text             "$$$"
                   :data-test-suffix "3"
                   :value            {:budget 3}
                   :target           events/navigate-adventure-get-in-contact}]})

(defn built-component
  [data opts]
  (component/build multi-prompt/component (query data) opts))
