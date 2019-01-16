(ns adventure.budget
  (:require [storefront.events :as events]
            [storefront.component :as component]
            [adventure.components.multi-prompt :as multi-prompt]))

(defn ^:private query [data]
  {:prompt      ["How much do you usually"
                 [:br]
                 " spend on 3 bundles of hair?"]
   :prompt-image "//ucarecdn.com/454b7522-de21-4f7c-a6cd-0288574ae672/-/format/auto/-/quality/normal/aladdinMatchingOverlayImagePurpleBR203Lm3x.png"
   :data-test    "adventure-budget-choice"
   :header-data  {:title        "Basic Info"
                  :current-step 2
                  :back-link    events/navigate-adventure-time-frame
                  :subtitle     "Step 1 of 3"}
   :buttons      [{:text             "Less than $150"
                   :data-test-suffix "1"
                   :value            {:budget 1}
                   :target           events/navigate-adventure-what-next}
                  {:text             "$150 - $250"
                   :data-test-suffix "2"
                   :value            {:budget 2}
                   :target           events/navigate-adventure-what-next}
                  {:text             "$250 and up"
                   :data-test-suffix "3"
                   :value            {:budget 3}
                   :target           events/navigate-adventure-what-next}]})

(defn built-component
  [data opts]
  (component/build multi-prompt/component (query data) opts))
