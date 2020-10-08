(ns adventure.handlers
  (:require #?@(:cljs
                [[storefront.platform.messages :as messages]])
            [storefront.effects :as effects]
            [storefront.events :as events]
            [adventure.keypaths :as keypaths]
            [storefront.transitions :as transitions]))

(defmethod transitions/transition-state events/api-success-remove-servicing-stylist
  [_ _ _ app-state]
  (-> app-state
      (assoc-in keypaths/adventure-choices-selected-stylist-id nil)
      (assoc-in keypaths/adventure-servicing-stylist nil)))

(defmethod effects/perform-effects events/api-success-remove-servicing-stylist
  [_ _ {:keys [order]} _ app-state]
  #?(:cljs
     (messages/handle-message events/save-order {:order order})))

(defmethod transitions/transition-state events/api-success-fetch-matched-stylist
  [_ _ {:keys [stylist]} app-state]
  (assoc-in app-state
            adventure.keypaths/adventure-servicing-stylist
            stylist))

(defmethod transitions/transition-state events/api-success-fetch-matched-stylists
  [_ _ {:keys [stylists]} app-state]
  (assoc-in app-state
            keypaths/adventure-matched-stylists
            stylists))

;; FIXME(matching) effects handler for events/api-success-fetch-matched-stylists
;; -> e/flow|stylist-matching|resulted

