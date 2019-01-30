(ns adventure.match-stylist
  (:require [adventure.components.basic-prompt :as basic-prompt]
            [adventure.keypaths :as keypaths]
            [storefront.component :as component]
            [storefront.events :as events]))

(defn ^:private query [data]
  (let [adventure-choices (get-in data keypaths/adventure-choices)
        hair-flow?        (-> adventure-choices :flow #{"match-stylist"})]
    {:prompt               "We'll match you with a top stylist, guaranteed."
     :mini-prompt          "If you don’t love the install, we’ll pay for you to get it taken down and redone. It’s a win-win!"
     :background-overrides {:class "bg-adventure-match-stylist"}
     :data-test            "adventure-match-stylist"
     :header-data          {:current-step 5
                            :subtitle     (str "Welcome to step " (if hair-flow? 2 3))
                            :back-link    events/navigate-adventure-what-next}
     :button               {:text   "Next"
                            :value  nil
                            :target events/navigate-adventure-find-your-stylist
                            :data-test "adventure-find-your-stylist"}}))


(defn built-component
  [data opts]
  (component/build basic-prompt/component (query data) opts))
