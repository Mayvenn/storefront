(ns storefront.history
  (:require [storefront.routes :as routes]
            [storefront.platform.messages :as messages]
            [goog.events]
            [goog.history.EventType :as EventType]
            [cemerick.url :as url])
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
    (goog.events/listen EventType/NAVIGATE (fn [e] (callback (.-isNavigation e))))))

(def app-history)

(defn set-current-page [browser-nav?]
  (let [uri                  (.getToken app-history)
        query-params         (:query (url/url (or js/window.location.href js/document.URL "")))
        [nav-event nav-args] (routes/navigation-message-for uri query-params)]
    (apply messages/handle-message
           [nav-event (assoc nav-args
                             :nav-snapshot
                             {:leaving-scroll-top js/document.body.scrollTop
                              :browser-nav?       browser-nav?})])))

(defn start-history []
  (set! app-history (make-history set-current-page)))

(defn enqueue-redirect [navigation-event & [args]]
  (when-let [path (routes/path-for navigation-event args)]
    (.setTimeout js/window #(.replaceToken app-history path))))

(defn enqueue-navigate [navigation-event & [args]]
  (when-let [path (routes/path-for navigation-event args)]
    (.setTimeout js/window #(.setToken app-history path))))
