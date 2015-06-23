(ns storefront.sync-messages
  (:require [storefront.effects :refer [perform-effects]]
            [storefront.transitions :refer [transition-state]]
            [om.core :as om]
            [clojure.data :refer [diff]]))

(defn- transition [app-state [event args]]
  (reduce (fn [app-state dispatch]
            (or (transition-state dispatch event args app-state)
                app-state))
          app-state
          (reductions conj [] event)))

(defn- effects [app-state [event args]]
  (doseq [event-fragment (rest (reductions conj [] event))]
    (perform-effects event-fragment event args app-state)))

(defn- log-deltas [old-app-state new-app-state [event args]]
  (let [[deleted added unchanged] (diff old-app-state new-app-state)]
    (when (or (seq deleted) (seq added))
      (js/console.trace (clj->js event)
                        (clj->js args)
                        (clj->js {:deleted deleted
                                  :added added}))))
  new-app-state)

(defn- transition-log [app-state message]
  (log-deltas app-state (transition app-state message) message))

(defn send-message [app-state-ref message]
  ;; rename transition to transition-log to log messages
  (om/transact! app-state-ref #(transition-log % message))
  (effects @app-state-ref message))
