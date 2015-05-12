(ns storefront.routes
  (:require [bidi.bidi :as bidi]
            [storefront.state :as state]
            [storefront.events :as events]
            [cljs.core.async :refer [put!]]
            [cljs.reader :refer [read-string]]
            [clojure.walk :refer [keywordize-keys]]
            [goog.events]
            [goog.history.EventType :as EventType]
            [cemerick.url :refer [map->query url]])
  (:import [goog.history Html5History]
           [goog Uri]))

(extend-protocol bidi.bidi/Pattern
  cljs.core.PersistentHashMap
  (match-pattern [this env]
    (when (every? (fn [[k v]]
                    (cond
                     (or (fn? v) (set? v)) (v (get env k))
                     :otherwise (= v (get env k))))
                  (seq this))
      env))
  (unmatch-pattern [_ _] ""))

(extend-protocol bidi.bidi/Matched
  cljs.core.PersistentHashMap
  (resolve-handler [this m] (some #(bidi.bidi/match-pair % m) this))
  (unresolve-handler [this m] (some #(bidi.bidi/unmatch-pair % m) this)))

(defn edn->bidi [value]
  (keyword (prn-str value)))

(defn bidi->edn [value]
  (read-string (name value)))

(defn set-current-page [app-state]
  (let [uri (.getToken (get-in app-state state/history-path))

        {nav-event :handler params :route-params}
        (bidi/match-route (get-in app-state state/routes-path) uri)

        query-params (:query (url js/location.href))
        event-ch (get-in app-state state/event-ch-path)]
    (put! event-ch
          [(bidi->edn nav-event)
           (-> params
               (assoc :query-params query-params)
               keywordize-keys)])))

(defn history-callback [app-state]
  (fn [e]
    (set-current-page @app-state)))

;; Html5History transformer defaults to always appending location.search
;; to any token we give it.
;;
;; This allows us to override it to never append location.search
(def non-search-preserving-history-transformer
  #js
  {:createUrl (fn [pathPrefix location]
                (str pathPrefix))
   :retrieveToken (fn [pathPrefix location]
                    (.-pathname location))})

(defn make-history [callback]
  (doto (Html5History. nil non-search-preserving-history-transformer)
    (.setUseFragment false)
    (.setPathPrefix "")
    (.setEnabled true)
    (goog.events/listen EventType/NAVIGATE callback)))

(defn routes []
  ["" {"/" (edn->bidi events/navigate-home)
       ["/categories/hair/" :taxon-path] (edn->bidi events/navigate-category)
       ["/products/" :product-path] (edn->bidi events/navigate-product)
       "/guarantee" (edn->bidi events/navigate-guarantee)
       "/help" (edn->bidi events/navigate-help)
       "/policy/privacy" (edn->bidi events/navigate-privacy)
       "/policy/tos" (edn->bidi events/navigate-tos)
       "/login" (edn->bidi events/navigate-sign-in)
       "/signup" (edn->bidi events/navigate-sign-up)
       "/password/recover" (edn->bidi events/navigate-forgot-password)}])

(defn install-routes [app-state]
  (let [history (or (get-in @app-state state/history-path)
                    (make-history (history-callback app-state)))]
    (swap! app-state
           merge
           {:routes (routes)
            :history history})))

(defn set-query-string [s query-params]
  (if (seq query-params)
    (-> (Uri.parse s)
        (.setQueryData (map->query query-params))
        .toString)
    s))

(defn path-for [app-state navigation-event & [args]]
  (let [query-params (:query-params args)
        args (dissoc args :query-params)]
    (-> (apply bidi/path-for
               (get-in app-state state/routes-path)
               (edn->bidi navigation-event)
               (apply concat (seq args)))
        (set-query-string query-params))))

(defn enqueue-navigate [app-state navigation-event & [args]]
  (let [query-params (:query-params args)
        args (dissoc args :query-params)]
    (.setToken (get-in app-state state/history-path)
               (-> (path-for app-state navigation-event args)
                   (set-query-string query-params)))))
