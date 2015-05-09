(ns storefront.routes
  (:require [bidi.bidi :as bidi]
            [storefront.state :as state]
            [storefront.events :as events]
            [cljs.core.async :refer [put!]]
            [cljs.reader :refer [read-string]]
            [goog.events]
            [goog.history.EventType :as EventType])
  (:import [goog.history Html5History]))

(declare set-current-page)

(defn history-callback [app-state]
  (fn [e]
    (set-current-page @app-state (.-token e))))

(defn make-history [callback]
  (doto (Html5History.)
    (.setUseFragment false)
    (.setPathPrefix "")
    (.setEnabled true)
    (goog.events/listen EventType/NAVIGATE callback)))

(defn edn->bidi [value]
  (keyword (prn-str value)))

(defn bidi->edn [value]
  (read-string (name value)))

(defn routes []
  ["" {"/" (edn->bidi events/navigate-home)
       ["/categories/hair/" :taxon-path] (edn->bidi events/navigate-category)
       "/guarantee" (edn->bidi events/navigate-guarantee)
       "/help" (edn->bidi events/navigate-help)
       "/policy/privacy" (edn->bidi events/navigate-privacy)
       "/policy/tos" (edn->bidi events/navigate-tos)}])

(defn install-routes [app-state]
  (let [history (or (get-in @app-state state/history-path)
                    (make-history (history-callback app-state)))]
    (swap! app-state
           merge
           {:routes (routes)
            :history history})
    (set-current-page @app-state)))

(defn set-current-page [app-state]
  (let [{nav-event :handler
         params :route-params}
        (bidi/match-route (get-in app-state state/routes-path)
                          (.getToken (get-in app-state state/history-path)))
        event-ch (get-in app-state state/event-ch-path)]
    (put! event-ch [(bidi->edn nav-event) params])))

(defn path-for [app-state navigation-event & [args]]
  (apply bidi/path-for
         (get-in app-state state/routes-path)
         (edn->bidi navigation-event)
         (flatten (seq args))))

(defn enqueue-navigate [app-state navigation-event & [args]]
  (.setToken (get-in app-state state/history-path)
             (path-for app-state navigation-event args)))
