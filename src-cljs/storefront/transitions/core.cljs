(ns storefront.transitions.core
  (:require [storefront.events :as events]
            [storefront.state :as state]
            [storefront.routes :as routes]))

(defmulti transition-state identity)
(defmethod transition-state :default [event arg app-state]
  (js/console.error "Transitioned via default, probably shouldn't. " (prn-str event))
  app-state)

(defmethod transition-state events/navigate-home [event args app-state]
  (assoc-in app-state state/navigation-event-path events/navigate-home))

(defmethod transition-state events/navigate-another [event args app-state]
  (assoc-in app-state state/navigation-event-path events/navigate-another))
