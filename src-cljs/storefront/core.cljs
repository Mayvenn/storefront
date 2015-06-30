(ns storefront.core
  (:require [storefront.state :as state]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.components.top-level :refer [top-level-component]]
            [storefront.routes :as routes]
            [storefront.exception-handler :as exception-handler]
            [storefront.messages :refer [send]]
            [storefront.effects :refer [perform-effects]]
            [storefront.transitions :refer [transition-state]]
            [om.core :as om]
            [clojure.data :refer [diff]]))

(enable-console-print!)

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

(defn handle-message
  ([app-state event] (handle-message app-state event nil))
  ([app-state event args]
   (let [message [event args]]
     ;; rename transition to transition-log to log messages
     (try
       (om/transact! (om/root-cursor app-state) #(transition % message))
       (effects @app-state message)
       (catch :default e
         (exception-handler/report e))))))


(defn main [app-state]
  (exception-handler/insert-handler)
  (swap! app-state assoc-in keypaths/handle-message (partial handle-message app-state))
  (routes/install-routes app-state)
  (om/root
   top-level-component
   app-state
   {:target (.getElementById js/document "content")})

  (handle-message app-state events/app-start)
  (routes/set-current-page @app-state))

(defonce app-state (atom (state/initial-state)))

(defn debug-force-token [token]
  (swap! app-state assoc-in [:user :token] "f766e9e3ea1f7b8bf25f1753f395cf7bd34cef0430360b7d"))

(defn ^:export debug-app-state []
  (clj->js @app-state))

(defn on-jsload []
  (handle-message app-state events/app-stop)
  (exception-handler/remove-handler)
  (main app-state))

(defn ^:export fail []
  (try
    (throw (js/Error. "Pokemon"))
    (catch js/Error e
      (exception-handler/report e))))

(main app-state)
