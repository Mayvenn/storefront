(ns storefront.history
  (:require [storefront.routes :refer [path-for navigation-message-for]]
            [storefront.platform.messages :refer [handle-message]]
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

(defn set-current-page []
  (let [uri          (.getToken app-history)
        ;; TODO: If this fix (that we stop assuming location.href will be
        ;; available) works, there are several other places that use location
        ;; that also need to be fixed
        query-params (:query (url (or js/window.location.href js/document.URL "")))]
    (apply handle-message
           (navigation-message-for uri query-params))))

(defn start-history []
  (set! app-history (make-history set-current-page)))

(defn enqueue-redirect [navigation-event & [args]]
  (when-let [path (path-for navigation-event args)]
    (js/window.requestAnimationFrame #(.replaceToken app-history path))))

(defn enqueue-navigate [navigation-event & [args]]
  (when-let [path (path-for navigation-event args)]
    (js/window.requestAnimationFrame #(.setToken app-history path))))
