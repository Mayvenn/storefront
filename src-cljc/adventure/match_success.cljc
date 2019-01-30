(ns adventure.match-success
  (:require [storefront.events :as events]
            [storefront.component :as component]
            [storefront.platform.messages :refer [handle-message]]
            [adventure.components.basic-prompt :as basic-prompt]
            [adventure.handlers :as handlers]
            [adventure.home :as home]
            [adventure.keypaths :as keypaths]
            [storefront.accessors.experiments :as experiments]
            [storefront.effects :as effects]
            [storefront.transitions :as transitions]))

(defn ^:private query [data]
  (let [stylist-id (get-in data keypaths/adventure-selected-stylist-id)
        stylist    (->> (get-in data keypaths/adventure-matched-stylists)
                        (filter #(= stylist-id (:stylist-id %)))
                        first)]
    {:prompt               (str "Congrats on matching with " (-> stylist
                                                                 :address
                                                                 :firstname))
     :mini-prompt          "We'll connect you with your stylist shortly. But first, pick out your hair!"
     :show-logo?           false
     :background-overrides {:style
                            {:background-size     "200px"
                             :background-position "right 15px bottom 150px"
                             :background-image
                             "url(//ucarecdn.com/8a87f86f-948f-48da-b59d-3ca4d8c6d5a0/-/format/png/-/quality/normal/)"}}
     :header-data          {:back-link events/navigate-adventure-stylist-results}
     :button               {:text      "Show me hair"
                            :data-test "adventure-match-success-choice-show-hair"
                            :color     :white
                            :target    events/navigate-adventure-home}}))

(defn built-component
  [data opts]
  (component/build basic-prompt/component (query data) opts))

(defmethod transitions/transition-state events/control-adventure-select-stylist
  [_ _ {:as args :keys [stylist-id]} app-state]
  (-> app-state
      (assoc-in keypaths/adventure-selected-stylist-id
                stylist-id)))

(defmethod effects/perform-effects events/control-adventure-select-stylist
  [_ _ _ _ _]
  (handle-message events/navigate-adventure-match-success))
