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
            [goog.object :as gobj]
            ["react" :as react]
            ["react-dom" :as react-dom]
            [om.core :as om]
            [clojure.data :refer [diff]]
            [cognitect.transit :as transit]))

(set! *warn-on-infer* true)

;; There was an issue with Om's implementation of MapCursor: it is behind CLJS' protocol,
;; so when we upgraded CLJS to 1.10, MapCursor couldn't be handled by any of the standard
;; collection manipulation functions. Here, we've overridden the implementation of
;; `MapCursor` so that it returns a collection of `MapEntry`s, rather than a collection of
;; collections (as per CLJS requirement).
;; TODO: GROT when Om updates their implementation of MapCursor.
(extend-protocol ISeqable
  om.core/MapCursor
  (-seq [this]
    (when (pos? (count (.-value this)))
      (map (fn [[k v]] (MapEntry. k (om.core/-derive this v (.-state this) (conj (.-path this) k)) nil)) (.-value this)))))

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
    (js/console.groupCollapsed (string/join "-" (map name event)) (clj->js args))
    (apply js/console.log (map clj->js (remove nil? [(when (seq deleted) "Δ-")
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
         #_(om/transact! (om/root-cursor app-state) #(msg-transition % message))
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
  (history/set-current-page true))

(defn dom-ready [f]
  (if (not= (.-readyState js/document)
            "loading")
    (f)
    (.addEventListener js/document "DOMContentLoaded" f)))

;; Delay in msec to call React.render() after the app-state changes
;;
;; Choosen by anecdotally to try and minimize the number of time we tell react
;; to render. There isn't any reason to have react do a lot of work when we know
;; our application tends to churn through app state in quick succession.
(def ^:private render-delay 5)

;; timer until React.render is called used in the function below
(def ^:private render-timer)

(defn- app-template [app-state]
  ;; NOTE: this function is not affected by figwheel's reload
  (let [tup             (react/useState #js{:state @app-state})
        app-state-value (gobj/get (aget tup 0) "state")
        setter          (aget tup 1)
        template        (component/build top-level-component app-state-value)
        state-ref (react/useRef 0)]
    (react/useEffect
     (fn []
       (add-watch app-state :renderer (fn [key ref old-value new-value]
                                        (when (not= old-value new-value)
                                          (when render-timer
                                            (js/clearTimeout render-timer))
                                          (set! render-timer (js/setTimeout #(setter #js{:state new-value})
                                                                            render-delay)))))
       js/undefined))
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

(defn ^:export debug-app-state []
  (clj->js @app-state))

(defn ^:export current-order []
  (clj->js (get-in @app-state keypaths/order)))

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

(loader/set-loaded! :main)
(dom-ready #(main app-state))
