(ns adventure.what-next
  (:require [storefront.events :as events]
            [storefront.component :as component]
            [adventure.components.multi-prompt :as multi-prompt]
            [adventure.keypaths :as adventure-keypaths]
            [adventure.utils.randomizer :as randomizer]
            [adventure.progress :as progress]))

(defn ^:private query [data]
  (let [random-sequence (get-in data adventure-keypaths/adventure-random-sequence)]
    {:prompt       [:div.h3 "Choose your next step."]
     :prompt-image "//ucarecdn.com/232d0b42-e889-4b2b-823f-86f9be0e2a0d/-/format/auto/-/quality/normal/"
     :data-test    "adventure-what-next-choice"
     :current-step 1
     :header-data  {:progress                progress/what-next
                    :title                   "Basic Info"
                    :back-navigation-message [events/navigate-adventure-budget]
                    :subtitle                "Step 1 of 3"}
     :buttons      (randomizer/randomize-ordering
                    random-sequence
                    progress/what-next
                    [{:text             "Match me with a certified stylist"
                      :data-test-suffix "match-stylist"
                      :value            {:flow "match-stylist"}
                      :target-message   [events/navigate-adventure-match-stylist]}
                     {:text             "Show me high quality hair"
                      :data-test-suffix "shop-hair"
                      :value            {:flow "shop-hair"}
                      :target-message   [events/navigate-adventure-shop-hair]}])}))


(defn built-component
  [data opts]
  (component/build multi-prompt/component (query data) opts))
