(ns adventure.how-shop-hair
  (:require [storefront.events :as events]
            [storefront.component :as component]
            [adventure.keypaths :as adventure-keypaths]
            [adventure.components.multi-prompt :as multi-prompt]
            [adventure.utils.randomizer :as randomizer]
            [storefront.accessors.experiments :as experiments]
            [adventure.progress :as progress]))

(def enriched-buttons
  [{:text             "Show me looks for inspiration"
    :data-test-suffix "looks"
    :value            {:how-shop "looks"}
    :target-message   [events/navigate-adventure-hair-texture
                       {:album-keyword :shop-by-look}]}
   {:text             "Give me pre-made bundle sets"
    :data-test-suffix "bundle-sets"
    :value            {:how-shop "bundle-sets"}
    :target-message   [events/navigate-adventure-bundlesets-hair-texture]}
   {:text             "Let me shop individual bundles"
    :data-test-suffix "individual-bundles"
    :value            {:how-shop "individual-bundles"}
    :target-message   [events/navigate-adventure-a-la-carte-hair-texture]}])

(defn ^:private query [data]
  (let [adventure-choices (get-in data adventure-keypaths/adventure-choices)
        current-step      (if (-> adventure-choices :flow #{"match-stylist"}) 3 2)
        random-sequence   (get-in data adventure-keypaths/adventure-random-sequence)]
    {:prompt       "How do you want to shop for your hair?"
     :prompt-image "//ucarecdn.com/3d071ed0-5d9c-4819-b117-84eb4cfc6ed7/-/format/auto/bg.png"
     :data-test    "how-shop-hair-choice"
     :current-step current-step
     :header-data  {:title                   "The New You"
                    :progress                progress/how-shop-hair
                    :back-navigation-message [events/navigate-adventure-shop-hair]
                    :subtitle                (str "Step " current-step " of 3")}
     :buttons      (randomizer/randomize-ordering
                    random-sequence
                    progress/how-shop-hair
                    enriched-buttons)}))

(defn built-component
  [data opts]
  (component/build multi-prompt/component (query data) opts))
