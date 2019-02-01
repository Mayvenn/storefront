(ns storefront.core
  (:require [storefront.config :as config]
            [storefront.state :as state]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.components.top-level :refer [top-level-component]]
            [storefront.history :as history]
            [storefront.hooks.exception-handler :as exception-handler]
            [storefront.platform.messages :as messages]
            [storefront.effects :refer [perform-effects]]
            [storefront.frontend-effects]
            [storefront.transitions :refer [transition-state]]
            [storefront.frontend-transitions]
            [storefront.trackings :refer [perform-track]]
            [cljs.reader :refer [read-string]]
            [om.core :as om]
            [clojure.data :refer [diff]]
            [storefront.api :as api]))

(set! *warn-on-infer* true)

(when config/enable-console-print?
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

(defn- effects [app-state-before-transition app-state-after-transition [event args]]
  (doseq [event-fragment (rest (reductions conj [] event))]
    (perform-effects event-fragment event args app-state-before-transition app-state-after-transition)))

(defn- track [app-state [event args]]
  (doseq [event-fragment (rest (reductions conj [] event))]
    (perform-track event-fragment event args app-state)))

(defn- log-deltas [old-app-state new-app-state [event args]]
  (let [[deleted added _unchanged] (diff old-app-state new-app-state)]
    (js/console.groupCollapsed (clojure.string/join "-" (map name event)) args)
    (apply js/console.log (remove nil? [(when (seq deleted) "Δ-")
                                        deleted
                                        (when (seq added) "Δ+")
                                        added]))
    (js/console.trace "Stacktrace")
    (js/console.groupEnd))
  new-app-state)

(defn- transition-log [app-state message]
  (log-deltas app-state (transition app-state message) message))

(defn handle-message
  ([app-state event] (handle-message app-state event nil))
  ([app-state event args]
   (let [message [event args]]
     (try
       (let [app-state-before @app-state]
         ;; rename transition to transition-log to log messages
         (om/transact! (om/root-cursor app-state) #(transition % message))
         (effects app-state-before @app-state message))
       (track @app-state message)
       (catch :default e
         (let [state @app-state]
           (exception-handler/report e {:ex-data                    (ex-data e)
                                        :api-version                (get-in state keypaths/app-version "unknown")
                                        :handling-message           message
                                        :current-features           (get-in state keypaths/features)
                                        :current-navigation-message (get-in state keypaths/navigation-message)
                                        :current-order-number       (get-in state keypaths/order-number)
                                        :current-user-id            (get-in state keypaths/user-id)
                                        :current-store-id           (get-in state keypaths/store-stylist-id)})))))))

(defn reload-app [app-state]
  (set! messages/handle-message (partial handle-message app-state)) ;; in case it has changed
  (handle-message app-state events/app-start)
  (history/set-current-page true)
  (some-> js/window .-root_owner .forceUpdate))

(defn dom-ready [f]
  (if (not= (.-readyState js/document)
            "loading")
    (f)
    (.addEventListener js/document "DOMContentLoaded" f)))

(defn main- [app-state]
  (set! messages/handle-message (partial handle-message app-state))
  (history/start-history)
  (set! (.-root_owner js/window)
        (om/root top-level-component
                 app-state
                 {:target (.getElementById js/document "content")}))
  (reload-app app-state))

(defn deep-merge
  [& maps]
  (if (every? map? maps)
    (apply merge-with deep-merge maps)
    (last maps)))

(defn consume-preloaded-data []
  (let [pre-data (read-string js/data)]
    (js-delete js/window "data")
    (set! (.-data js/window) js/undefined)
    pre-data))

(defonce main (memoize main-))
(defonce app-state (atom (deep-merge (state/initial-state)
                                     (consume-preloaded-data))))

(defn ^:export debug-app-state []
  (clj->js @app-state))

(defn on-jsload []
  (handle-message app-state events/app-stop)
  (reload-app app-state))

(defn ^:export fail []
  (try
    (throw (js/Error. "Pokemon"))
    (catch js/Error e
      (exception-handler/report e {:api-version (get-in @app-state keypaths/app-version "unknown")}))))

(defn ^:export external-message [event args]
  (let [event (js->clj event)
        event (mapv keyword (if (coll? event) event [event]))]
    (handle-message app-state
                    event
                    (js->clj args :keywordize-keys true))))

(dom-ready #(main app-state))
