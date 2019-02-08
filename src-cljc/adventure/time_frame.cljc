(ns adventure.time-frame
  (:require [storefront.events :as events]
            [storefront.component :as component]
            [adventure.components.multi-prompt :as multi-prompt]
            [storefront.components.ui :as ui]))

(defn ^:private query [data]
  {:prompt       ["When would you like to get "
                  [:br]
                  "your hair installed?"]
   :prompt-image "//ucarecdn.com/1b443614-0897-4549-8d54-33d798072f04/-/format/auto/-/quality/normal/aladdinMatchingOverlayImagePurpleAR203Lm3x.png"
   :data-test    "adventure-time-frame-choice"
   :current-step 1
   :header-data  {:progress                1
                  :title                   "Basic Info"
                  :back-navigation-message [events/navigate-adventure-home]
                  :subtitle                "Step 1 of 3"}
   :buttons      [{:text             "As soon as possible"
                   :data-test-suffix "asap"
                   :value            {:time-frame "asap"}
                   :target-message   [events/navigate-adventure-budget]}
                  {:text             "In the next few weeks"
                   :data-test-suffix "weeks"
                   :value            {:time-frame "weeks"}
                   :target-message   [events/navigate-adventure-budget]}
                  {:text             "Months from now"
                   :data-test-suffix "months"
                   :value            {:time-frame "months"}
                   :target-message   [events/navigate-adventure-budget]}]})

(defn built-component
  [data opts]
  (component/build multi-prompt/component (query data) opts))
