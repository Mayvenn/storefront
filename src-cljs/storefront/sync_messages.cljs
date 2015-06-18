(ns storefront.sync-messages
  (:require [storefront.effects :refer [perform-effects]]
            [storefront.transitions :refer [transition-state]]))

(defn- transition [app-state [event args]]
  (reduce #(transition-state %2 event args %1) app-state (reductions conj [] event)))

(defn- effects [app-state [event args]]
  (doseq [event-fragment (rest (reductions conj [] event))]
    (perform-effects event-fragment event args app-state)))

(defn send-message [app-state-ref message]
  ;; (js/console.trace (clj->js event) "\n" (clj->js args)) ;; uncomment to trace messages
  (swap! app-state-ref transition message)
  (effects @app-state-ref message))
