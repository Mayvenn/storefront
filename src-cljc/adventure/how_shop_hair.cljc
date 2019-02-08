(ns adventure.how-shop-hair
  (:require [storefront.events :as events]
            [storefront.component :as component]
            [adventure.keypaths :as adventure-keypaths]
            [adventure.components.multi-prompt :as multi-prompt]
            [storefront.accessors.experiments :as experiments]))

(defn ^:private permutations [s]
  (lazy-seq
   (if (seq (rest s))
     (apply concat (for [x s]
                     (map #(cons x %) (permutations (remove #{x} s)))))
     [s])))

(defn ^:private randomize-buttons [progress random-sequence buttons]
  (let [perms   (permutations buttons)
        mod-val (nth random-sequence (dec progress))]
    (nth perms (mod mod-val (count perms)))))

(defn enriched-buttons [shop-individual-buttons?]
  (into [{:text             "Show me looks for inspiration"
          :data-test-suffix "looks"
          :value            {:how-shop :looks}
          :target           {:event events/navigate-adventure-select-new-look
                             :args  {:album-keyword :adventure}}}
         {:text             "Give me pre-made bundle sets"
          :data-test-suffix "bundle-sets"
          :value            {:how-shop :bundle-sets}
          :target           {:event events/navigate-adventure-select-new-look
                             :args  {:album-keyword :adventure-bundle-set}}}]
        (when shop-individual-buttons?
          [{:text             "Let me shop individual bundles"
            :data-test-suffix "individual-bundles"
            :value            {:how-shop :individual-bundles}
            :target           nil}])))

(defn ^:private query [data]
  (let [adventure-choices (get-in data adventure-keypaths/adventure-choices)
        current-step      (if (-> adventure-choices :flow #{"match-stylist"}) 3 2)
        progress          11
        random-sequence   (get-in data adventure-keypaths/adventure-random-sequence)]
    {:prompt       "How do you want to shop for your hair?"
     :prompt-image "//ucarecdn.com/3d071ed0-5d9c-4819-b117-84eb4cfc6ed7/-/format/auto/bg.png"
     :data-test    "how-shop-hair-choice"
     :current-step current-step
     :header-data  {:title     "The New You"
                    :progress  progress
                    :back-link events/navigate-adventure-shop-hair
                    :subtitle  (str "Step " current-step " of 3")}
     :buttons      (randomize-buttons
                    progress
                    random-sequence
                    (enriched-buttons (experiments/adventure-shop-individual-bundles? data)))}))

(defn built-component
  [data opts]
  (component/build multi-prompt/component (query data) opts))
