(ns adventure.home
  (:require [storefront.events :as events]
            [storefront.component :as component]
            [adventure.components.basic-prompt :as basic-prompt]
            [adventure.handlers :as handlers]
            [storefront.accessors.experiments :as experiments]))

(defn ^:private query [data]
  {:prompt               "Welcome! We can't wait for you to get a free install."
   :mini-prompt          "Ready? Let's get started."
   :show-logo?           true
   :background-overrides {:style
                          {:background-image
                           "url(//ucarecdn.com/27601192-f64b-46c1-98ba-1323769180b0/-/format/auto/-/quality/normal/aladdinMatchingHeroImageAR103Lm3x.png)"}}
   :button               {:text      "Get started"
                          :data-test "adventure-home-choice-get-started"
                          :color     :teal
                          :target    events/navigate-adventure-time-frame}})

(defn built-component
  [data opts]
  (component/build basic-prompt/component (query data) opts))
