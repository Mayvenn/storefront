(ns adventure.match-stylist
  (:require [adventure.components.basic-prompt :as basic-prompt]
            [storefront.component :as component]
            [storefront.events :as events]
            [adventure.progress :as progress]))

(defn ^:private query [data]
  {:prompt               "We'll match you with a top stylist, guaranteed."
   :mini-prompt          "If you don’t love the install, we’ll pay for you to get it redone. It’s a win-win!"
   :background-overrides {:class "bg-adventure-match-stylist"}
   :data-test            "adventure-match-stylist"
   :current-step         2
   :header-data          {:progress                progress/match-stylist
                          :subtitle                "Welcome to step 2"
                          :back-navigation-message [events/navigate-adventure-what-next]}
   :button               {:text           "Next"
                          :value          nil
                          :target-message [events/navigate-adventure-find-your-stylist]
                          :data-test      "adventure-find-your-stylist"}})


(defn built-component
  [data opts]
  (component/build basic-prompt/component (query data) opts))
