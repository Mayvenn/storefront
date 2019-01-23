(ns adventure.how-shop-hair
  (:require [storefront.events :as events]
            [storefront.component :as component]
            [adventure.keypaths :as adventure-keypaths]
            [adventure.components.multi-prompt :as multi-prompt]))

(defn ^:private query [data]
  (let [adventure-choices (get-in data adventure-keypaths/adventure-choices)
        hair-flow?        (-> adventure-choices :flow #{"match-stylist"})]
    {:prompt       "How do you want to shop for your hair?"
     :prompt-image "//ucarecdn.com/3d071ed0-5d9c-4819-b117-84eb4cfc6ed7/-/format/auto/bg.png"
     :data-test    "how-shop-hair-choice"
     :header-data  {:title        "The New You"
                    :current-step 2
                    :back-link    events/navigate-adventure-shop-hair
                    :subtitle     (str "Step " (if-not hair-flow? 2 3) " of 3")}
     :buttons      [{:text             "Show me looks for inspiration"
                     :data-test-suffix "looks"
                     :value            {:how-shop :looks}
                     :target           {:event events/navigate-adventure-select-new-look
                                        :args  {:album-keyword :adventure}}}
                    {:text             "Give me pre-made bundle sets"
                     :data-test-suffix "bundle-sets"
                     :value            {:how-shop :bundle-sets}
                     :target           {:event events/navigate-adventure-select-new-look
                                        :args  {:album-keyword :adventure-bundle-set}}}
                    {:text             "Let me shop individual bundles"
                     :data-test-suffix "individual-bundles"
                     :value            {:how-shop :individual-bundles}
                     :target           nil}
                    {:text             "Create a custom look for me"
                     :data-test-suffix "custom-look"
                     :value            {:how-shop "custom-look"}
                     :target           nil}]}))

(defn built-component
  [data opts]
  (component/build multi-prompt/component (query data) opts))
