(ns adventure.match-success
  (:require [storefront.events :as events]
            [storefront.component :as component]
            [adventure.components.basic-prompt :as basic-prompt]
            [storefront.keypaths :as storefront-keypaths]
            [adventure.keypaths :as keypaths]
            [storefront.effects :as effects]
            [storefront.transitions :as transitions]
            #?(:cljs [storefront.history :as history])
            [adventure.progress :as progress]))

(defn ^:private query [data]
  (let [stylist-id        (get-in data keypaths/adventure-selected-stylist-id)
        servicing-stylist (get-in data keypaths/adventure-servicing-stylist)]
    {:prompt               (str "Congrats on matching with " (-> servicing-stylist :address :firstname) "!")
     :mini-prompt          "We'll connect you with your stylist shortly. But first, pick out your hair!"
     :show-logo?           false
     :background-overrides {:style
                            {:background-size     "200px"
                             :background-position "right 15px bottom 150px"
                             :background-image
                             "url(//ucarecdn.com/8a87f86f-948f-48da-b59d-3ca4d8c6d5a0/-/format/png/-/quality/normal/)"}}
     :current-step         2
     :header-data          {:progress                progress/match-success
                            :back-navigation-message [events/navigate-adventure-stylist-results-pre-purchase]}
     :button               {:text           "Show me hair"
                            :data-test      "adventure-match-success-choice-show-hair"
                            :color          :white
                            :target-message [events/navigate-adventure-shop-hair]}}))

(defn built-component
  [data opts]
  (component/build basic-prompt/component (query data) opts))

(defmethod transitions/transition-state events/api-success-assign-servicing-stylist
  [_ _ {:keys [order servicing-stylist]} app-state]
  (-> app-state
      (assoc-in storefront-keypaths/order order)
      (assoc-in adventure.keypaths/adventure-servicing-stylist servicing-stylist)))

(defmethod effects/perform-effects events/api-success-assign-servicing-stylist [_ _ _ _ app-state]
  #?(:cljs
     (history/enqueue-redirect events/navigate-adventure-match-success)))
