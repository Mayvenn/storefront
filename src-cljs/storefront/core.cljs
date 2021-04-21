(ns ^:figwheel-hooks storefront.core
  (:require [storefront.config :as config]
            [storefront.loader :as loader]
            [storefront.state :as state]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.top-level :refer [top-level-component]]
            [storefront.history :as history]
            [storefront.hooks.exception-handler :as exception-handler]
            [storefront.platform.messages :as messages]
            [storefront.effects :refer [perform-effects]]
            [storefront.frontend-effects]
            [storefront.transitions :refer [transition-state]]
            [storefront.frontend-transitions]
            [storefront.trackings :refer [perform-track]]
            [storefront.frontend-trackings]
            [clojure.string :as string]
            clojure.walk
            [goog.object :as gobj]
            ["react" :as react]
            ["react-dom" :as react-dom]
            [clojure.data :refer [diff]]
            [cognitect.transit :as transit]))

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
  (doseq [event-fragment (reductions conj [] event)
          :when (pos? (count event-fragment))]
    (perform-effects event-fragment event args app-state-before-transition app-state-after-transition)))

(defn- track [app-state [event args]]
  (doseq [event-fragment (reductions conj [] event)
          :when (pos? (count event-fragment))]
    (perform-track event-fragment event args app-state)))

(defn- ns-clj->js [args]
  (clj->js args :keyword-fn (comp #(subs % 1) str)))

(defn- log-deltas [old-app-state new-app-state [event args]]
  (let [[deleted added _unchanged] (diff old-app-state new-app-state)]
    (js/console.groupCollapsed (string/join "-" (map name event)) (ns-clj->js args))
    (apply js/console.log (map ns-clj->js (remove nil? [(when (seq deleted) "Δ-")
                                                        deleted
                                                        (when (seq added) "Δ+")
                                                        added])))
    (js/console.trace "Stacktrace")
    (js/console.groupEnd))
  new-app-state)

(defn- transition-log [app-state message]
  (log-deltas app-state (transition app-state message) message))

(def msg-transition
  transition)

(defn handle-message
  ([app-state event] (handle-message app-state event nil))
  ([app-state event args]
   (let [message [event args]]
     (try
       (let [app-state-before @app-state]
         (swap! app-state #(msg-transition % message))
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

(declare app-template)
(defn reload-app [app-state]
  (let [element (.getElementById js/document "content")]
    (react-dom/render (react/createElement (partial app-template app-state)) element))
  (set! messages/handle-message (partial handle-message app-state)) ;; in case it has changed
  (handle-message app-state events/app-start)
  (history/set-current-page (atom true) true))

(defn dom-ready [f]
  (if (not= (.-readyState js/document)
            "loading")
    (f)
    (.addEventListener js/document "DOMContentLoaded" f)))

(defn- app-template [app-state]
  ;; NOTE: this function is not affected by figwheel's reload
  (let [tup             (react/useState #js{:state @app-state})
        app-state-value (gobj/get (aget tup 0) "state")
        setter          (aget tup 1)
        template        (component/build top-level-component app-state-value)]
    (add-watch app-state :renderer (fn [key ref old-value new-value]
                                     (when (not= old-value new-value)
                                       (setter #js{:state new-value}))))
    template))

(defn main- [app-state]
  (set! messages/handle-message (partial handle-message app-state))
  (history/start-history)
  (reload-app app-state))

(defn deep-merge
  [& maps]
  (if (every? map? maps)
    (apply merge-with deep-merge maps)
    (last maps)))

(defn consume-preloaded-data []
  (let [pre-data (transit/read (transit/reader :json)
                               js/data)]
    (js-delete js/window "data")
    (set! (.-data js/window) js/undefined)
    pre-data))

(defonce main (memoize main-))
(defonce app-state (atom (deep-merge (state/initial-state)
                                     (consume-preloaded-data))))

(defn ^:private sort-maps
  "Replaces all maps in a nested structure with sorted maps."
  [form]
  (clojure.walk/postwalk
   #(cond->> %
      (map? %) (into (sorted-map)))
   form))

(defn ^:export debug-app-state []
  (clj->js (sort-maps @app-state)
           :keyword-fn (comp #(subs % 1) str)))

(defn ^:export current-order []
  (clj->js (get-in @app-state keypaths/order)))

(defn ^:export current-features []
  (clj->js (get-in @app-state keypaths/features)))

(defn ^:before-load before-load []
  (handle-message app-state events/app-stop))

(defn ^:after-load after-load []
  (reload-app app-state))

(defn ^:export fail []
  (try
    (throw (js/Error. "Pokemon"))
    (catch js/Error e
      (exception-handler/report e {:api-version (get-in @app-state keypaths/app-version "unknown")}))))

(defn ^:export debug-messages [enabled?]
  (if enabled?
    (set! msg-transition transition-log)
    (set! msg-transition transition)))

(defn ^:export external-message [event args]
  (let [event (js->clj event)
        event (mapv keyword (if (coll? event) event [event]))]
    (handle-message app-state
                    event
                    (js->clj args :keywordize-keys true))))

(defn ^:export list-registered-effect-methods
  []
  (->> (sequence (comp
                  (map first)
                  (filter coll?)
                  (map (partial mapv name))
                  (map (partial clojure.string/join "-")))
                 (methods storefront.effects/perform-effects))
       sort
       vec
       clj->js))

(loader/set-loaded! :main)
(dom-ready #(main app-state))
