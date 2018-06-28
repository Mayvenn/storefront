(ns storefront.browser.events
  (:require goog.events
            [storefront.platform.messages :refer [handle-message]]
            [storefront.events :as events]))


(defn handle-fullscreen-change [evt]
  (if-let [is-fullscreen? (or js/document.webkitFullscreenElement
                              js/document.mozFullScreenElement
                              js/document.msFullscreenElement
                              js/document.fullscreenElement)]
    (handle-message events/browser-fullscreen-enter)
    (handle-message events/browser-fullscreen-exit)))

(def ^:private fullscreen-events
  ["fullscreenchange" "webkitfullscreenchange" "mozfullscreenchange" "msfullscreenchange"])

(def ^:private i-know-i-have-attached (atom false))

(defn attach-global-listeners []
  ;; Full screen events cannot be unlistened for some reason, so we need to
  ;; track when we attach ourselves
  (when-not @i-know-i-have-attached
    (swap! i-know-i-have-attached (constantly true))
    (doseq [evt-name fullscreen-events]
      (goog.events/listen js/document evt-name handle-fullscreen-change))))
