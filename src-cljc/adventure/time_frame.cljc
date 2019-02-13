(ns adventure.time-frame
  (:require [storefront.events :as events]
            [storefront.component :as component]
            [adventure.components.multi-prompt :as multi-prompt]
            [storefront.components.ui :as ui]
            [adventure.progress :as progress]))

(defn ^:private query [data]
  {:prompt       ["When would you like to get "
                  [:br]
                  "your hair installed?"]
   :prompt-image "//ucarecdn.com/1b443614-0897-4549-8d54-33d798072f04/-/format/auto/-/quality/normal/aladdinMatchingOverlayImagePurpleAR203Lm3x.png"
   :data-test    "adventure-time-frame-choice"
   :current-step 1
   :header-data  {:progress                progress/time-frame
                  :title                   "Basic Info"
                  :back-navigation-message [events/navigate-adventure-home]
                  :subtitle                "Step 1 of 3"}
   :buttons      [{:text             "In the next 30 days"
                   :data-test-suffix "in-the-next-30-days"
                   :value            {:time-frame "in-the-next-30-days"}
                   :target-message   [events/navigate-adventure-budget]}
                  {:text             "In 2 to 3 months"
                   :data-test-suffix "in-2-to-3-months"
                   :value            {:time-frame "in-2-to-3-months"}
                   :target-message   [events/navigate-adventure-budget]}
                  {:text             "Sometime this year"
                   :data-test-suffix "sometime-this-year"
                   :value            {:time-frame "sometime-this-year"}
                   :target-message   [events/navigate-adventure-budget]}]})

(defn built-component
  [data opts]
  (component/build multi-prompt/component (query data) opts))
