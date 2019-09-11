(ns adventure.stylist-matching.match-stylist
  (:require adventure.keypaths
            [storefront.events :as events]
            [storefront.transitions :as transitions]))

;; Need this transition for direct-load
(defmethod transitions/transition-state events/navigate-adventure-match-stylist
  [_ _ _ app-state]
  (assoc-in app-state adventure.keypaths/adventure-choices-flow "match-stylist"))
