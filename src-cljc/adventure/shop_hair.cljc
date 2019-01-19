(ns adventure.shop-hair
  (:require [adventure.components.basic-prompt :as basic-prompt]
            [adventure.keypaths :as keypaths]
            [storefront.component :as component]
            [storefront.events :as events]))

(defn ^:private query [data]
  (let [adventure-choices (get-in data keypaths/adventure-choices)
        hair-flow?        (-> adventure-choices :flow #{"shop-hair"})]
    {:prompt               [:div.pb2.line-height-2 "It’s time to pick out some amazing virgin hair."]
     :mini-prompt          [:div.pt2.line-height-2 "Wear it, cut it, style it. If you don’t love your hair, we’ll exchange it for free."]
     :background-overrides {:class "bg-adventure-shop-hair"}
     :data-test            "adventure-match-with-stylist"
     :header-data          {:current-step 1
                            :subtitle     (str "Welcome to Step " (if hair-flow? 2 3))
                            :back-link    events/navigate-adventure-what-next}
     :button               {:text      "Next"
                            :data-test "shop-hair-button"
                            :target    events/navigate-adventure-how-shop-hair}}))

(defn built-component
  [data opts]
  (component/build basic-prompt/component (query data) opts))
