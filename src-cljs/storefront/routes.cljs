(ns storefront.routes
  (:require [bidi.bidi :as bidi]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.app-routes :refer [edn->bidi bidi->edn app-routes path-for]]
            [storefront.messages :refer [handle-message]]
            [clojure.walk :refer [keywordize-keys]]
            [goog.events]
            [goog.history.EventType :as EventType]
            [cemerick.url :refer [url]])
  (:import [goog.history Html5History]))

;; Html5History transformer defaults to always appending location.search
;; to any token we give it.
;;
;; This allows us to override it to never append location.search
(def non-search-preserving-history-transformer
  (let [opts #js {}]
    (set! (.-createUrl opts) (fn [pathPrefix location]
                               (str pathPrefix)))
    (set! (.-retrieveToken opts) (fn [pathPrefix location]
                                   (.-pathname location)))
    opts))

(defn make-history [callback]
  (doto (Html5History. nil non-search-preserving-history-transformer)
    (.setUseFragment false)
    (.setPathPrefix "")
    (.setEnabled true)
    (goog.events/listen EventType/NAVIGATE (fn [e] (callback)))))

(def app-history)

(defn navigation-message-for
  ([uri] (navigation-message-for uri nil))
  ([uri query-params]
   (let [{nav-event :handler params :route-params} (bidi/match-route app-routes uri)]
     [(if nav-event (bidi->edn nav-event) events/navigate-not-found)
      (-> params
          (merge (when query-params {:query-params query-params}))
          keywordize-keys)])))

(defn set-current-page []
  (let [uri          (.getToken app-history)
        query-params (:query (url js/location.href))]
    (apply handle-message
           (navigation-message-for uri query-params))))

(defn start-history []
  (set! app-history (make-history set-current-page)))

(defn enqueue-redirect [navigation-event & [args]]
  (when-let [path (path-for navigation-event args)]
    (.replaceToken app-history path)))

(defn enqueue-navigate [navigation-event & [args]]
  (when-let [path (path-for navigation-event args)]
    (.setToken app-history path)))

(defn current-path [app-state]
  (apply path-for (get-in app-state keypaths/navigation-message)))
