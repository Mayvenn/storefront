(ns adventure.handlers
  (:require [storefront.platform.messages :as messages]
            [storefront.effects :as effects]
            [storefront.events :as e]
            [adventure.keypaths :as keypaths]
            [storefront.transitions :as transitions]))

(defmethod transitions/transition-state e/api-success-remove-servicing-stylist
  [_ _ _ app-state]
  (-> app-state
      (assoc-in keypaths/adventure-choices-selected-stylist-id nil)
      (assoc-in keypaths/adventure-servicing-stylist nil)))

(defmethod effects/perform-effects e/api-success-remove-servicing-stylist
  [_ _ {:keys [order]} _ app-state]
  #?(:cljs
     (messages/handle-message e/save-order {:order order})))

(defmethod transitions/transition-state e/api-success-fetch-matched-stylist
  [_ _ {:keys [stylist]} app-state]
  (assoc-in app-state
            adventure.keypaths/adventure-servicing-stylist
            stylist))
