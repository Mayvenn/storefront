(ns adventure.shop-hair
  (:require [adventure.components.basic-prompt :as basic-prompt]
            [adventure.keypaths :as keypaths]
            [adventure.progress :as progress]
            [storefront.component :as component]
            [storefront.transitions :as transitions]
            [storefront.events :as events]
            [storefront.accessors.experiments :as experiments]))

(defn ^:private query [data]
  (let [servicing-stylist? (get-in data keypaths/adventure-servicing-stylist)
        current-step       (if servicing-stylist? 3 2)]
    {:prompt               [:div.pb2.line-height-2 "It’s time to pick out some amazing virgin hair."]
     :mini-prompt          [:div.pt2.line-height-2 "Wear it, cut it, style it. If you don’t love your hair, we’ll exchange it for free."]
     :background-overrides {:class "bg-adventure-shop-hair"}
     :data-test            "adventure-match-with-stylist"
     :current-step         current-step
     :header-data          {:progress                progress/shop-hair
                            :subtitle                (str "Welcome to Step " current-step )
                            :back-navigation-message [(if (= 2 current-step)
                                                        events/navigate-adventure-what-next
                                                        events/navigate-adventure-match-success)]}
     :button               {:text           "Next"
                            :data-test      "shop-hair-button"
                            :target-message [events/navigate-adventure-how-shop-hair]}}))

(defn built-component
  [data opts]
  (component/build basic-prompt/component (query data) opts))

;; Need this transition for direct-load
(defmethod transitions/transition-state events/navigate-adventure-shop-hair
  [_ _ _ app-state]
  (assoc-in app-state keypaths/adventure-choices-flow "shop-hair"))
