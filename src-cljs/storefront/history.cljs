(ns storefront.history
  (:require [storefront.routes :as routes]
            [storefront.platform.messages :as messages]
            [storefront.events :as events]
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

(def first-nav-state? (atom true))

(defn ^:private make-history
  [callback]
  (doto (Html5History. nil non-search-preserving-history-transformer)
    (.setUseFragment false)
    (.setPathPrefix "")
    (.setEnabled true)
    (goog.events/listen EventType/NAVIGATE
                        (fn [e]
                          (callback first-nav-state? (.-isNavigation e))))))

(def app-history)

(defn ->subdomain [host] (some-> host (.split ".") reverse (nth 2 nil)))

(defn current-uri
  "Get the path from the history token,
   the rest from current location"
  []
  (let [uri (url/url (or js/window.location.href
                         js/document.URL
                         ""))]
    {:path      (.getToken app-history)
     :host      (:host uri)
     :query     (:query uri)
     :subdomain (->subdomain (:host uri))}))

(defn set-current-page
  [first-nav? browser-nav?]
  (let [{:keys [subdomain query path]} (current-uri)
        event                          (if browser-nav?
                                         events/browser-navigate
                                         events/control-navigate)
        first-nav-val?                 @first-nav?
        [navigation-event navigation-args]
        (routes/navigation-message-for path
                                       query
                                       subdomain)]
    (messages/handle-message event {:navigation-message
                                    [navigation-event
                                     (merge navigation-args
                                            (when first-nav-val?
                                              {:navigate/caused-by :first-nav}))]})
    (reset! first-nav? false)))

(defn start-history []
  (set! app-history (make-history set-current-page)))

(defn enqueue-redirect [navigation-event & [args]]
  (when-let [path (routes/path-for navigation-event args)]
    (js/setTimeout #(.replaceToken app-history path)
                   (:timeout args 0))))

(defn enqueue-navigate [navigation-event & [args]]
  (when-let [path (routes/path-for navigation-event args)]
    (js/setTimeout (fn []
                     ;; NOTE (EW) If the current token is equal to what is already there (ie navigating to where you already were),
                     ;; setToken will not trigger navigation behavior.
                     ;; If you use replaceToken, the history stack doesn't record history
                     ;; but the navigation events still trigger.
                     ;; I am unsure if this is the best way to achieve this goal.
                     (if (= (.getToken app-history) path)
                       (.replaceToken app-history path)
                       (.setToken app-history path)))
                   (:timeout args 0))))
