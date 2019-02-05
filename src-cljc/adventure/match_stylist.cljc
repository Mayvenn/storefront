(ns adventure.match-stylist
  (:require [adventure.components.basic-prompt :as basic-prompt]
            [storefront.component :as component]
            [storefront.events :as events]))

(defn ^:private query [data]
  {:prompt               "We'll match you with a top stylist, guaranteed."
   :mini-prompt          "If you don’t love the install, we’ll pay for you to get it taken down and redone. It’s a win-win!"
   :background-overrides {:class "bg-adventure-match-stylist"}
   :data-test            "adventure-match-stylist"
   :current-step         2
   :header-data          {:progress  5
                          :subtitle  "Welcome to step 2"
                          :back-link events/navigate-adventure-what-next}
   :button               {:text      "Next"
                          :value     nil
                          :target    events/navigate-adventure-find-your-stylist
                          :data-test "adventure-find-your-stylist"}})


(defn built-component
  [data opts]
  (component/build basic-prompt/component (query data) opts))
