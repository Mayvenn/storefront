(ns storefront.core
  (:require [storefront.config :as config]
            [storefront.state :as state]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.components.top-level :refer [top-level-component]]
            [storefront.routes :as routes]
            [storefront.hooks.exception-handler :as exception-handler]
            [storefront.messages :as messages]
            [storefront.effects :refer [perform-effects]]
            [storefront.transitions :refer [transition-state]]
            [om.core :as om]
            [clojure.data :refer [diff]]))

(when config/development?
  (enable-console-print!))

(defn- transition [app-state [event args]]
  (reduce (fn [app-state dispatch]
            (try
              (or (transition-state dispatch event args app-state)
                  app-state)
              (catch js/TypeError te
                (exception-handler/report te {:context {:dispatch dispatch
                                                        :event event
                                                        :args args}}))))
          app-state
          (reductions conj [] event)))

(defn- effects [app-state [event args]]
  (doseq [event-fragment (rest (reductions conj [] event))]
    (perform-effects event-fragment event args app-state)))

(defn- log-deltas [old-app-state new-app-state [event args]]
  (let [[deleted added unchanged] (diff old-app-state new-app-state)]
    (when (or (seq deleted) (seq added))
      (js/console.groupCollapsed (clj->js event) (clj->js args))
      (js/console.log "Delta" (clj->js {:deleted deleted
                                        :added added}))
      (js/console.trace "Stacktrace")
      (js/console.groupEnd)))
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
         ;; TODO: All this context is for debugging login redirect stack
         ;; overflows... remove when no longer needed
         (let [data @app-state
               nav-message (get-in data keypaths/navigation-message)
               return-message (get-in data keypaths/return-navigation-message)
               previous-message (get-in data keypaths/previous-navigation-message)]
           (exception-handler/report e {:context {:navigation
                                                  {:navigation-message nav-message
                                                   :return-navigation-message return-message
                                                   :previous-navigation-message previous-message}}})))))))

(defn reload-app [app-state]
  (set! messages/handle-message (partial handle-message app-state))
  (routes/start-history)
  (handle-message app-state events/app-start)
  (routes/set-current-page))

(defn main- [app-state]
  (om/root
   top-level-component
   app-state
   {:target (.getElementById js/document "content")})
  (reload-app app-state))

(defonce main (memoize main-))
(defonce app-state (atom (state/initial-state)))

(defn debug-force-token [token]
  (swap! app-state assoc-in keypaths/user-token "f766e9e3ea1f7b8bf25f1753f395cf7bd34cef0430360b7d"))

(defn ^:export debug-app-state []
  (clj->js @app-state))

(defn on-jsload []
  (handle-message app-state events/app-stop)
  (reload-app app-state))

(defn ^:export fail []
  (try
    (throw (js/Error. "Pokemon"))
    (catch js/Error e
      (exception-handler/report e))))

(defn ^:export external-message [event args]
  (let [event (if (coll? event) event [event])]
    (handle-message app-state
                    (map keyword event)
                    (js->clj args :keywordize-keys true))))

(main app-state)
