(ns storefront.transitions.core
  (:require [storefront.events :as events]
            [storefront.state :as state]))

(defmulti transition-state identity)
(defmethod transition-state :default [event arg app-state]
  (js/console.error "Transitioned via default, probably shouldn't. " (prn-str event))
  app-state)

(defmethod transition-state events/navigate-home [event args app-state]
  (assoc-in app-state state/navigation-point-path :home))

(defmethod transition-state events/navigate-another [event args app-state]
  (assoc-in app-state state/navigation-point-path :another))
