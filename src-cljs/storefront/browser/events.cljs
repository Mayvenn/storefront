(ns storefront.browser.events
  (:require goog.events
            [goog.events.EventType :as EventType]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.effects :as effects]
            [storefront.transitions :as transitions]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]))

(def ^:private browser-event-listener js/HTMLElement.prototype.addEventListener)
(def ^:private callbacks-for-ready-state
  (atom []))

(defn ^:private capture-late-readystatechange-callbacks
  "Some third-party javascript require attaching readystatechange handlers to
  perform their behavior, but since we defer loading third-party javascript
  libraries to after our application loads (and thus after readystatechange
  change completes), then those callbacks will never fire.

  Since we can't modify the third party library code, we instead allow emulating
  'readystatechange' callbacks firing by capturing any callbacks that attempt to
  add a listener to readystatechange."
  [e]
  (when (= "complete" (.-readyState js/document))
    (set! (.-addEventListener js/document)
          (fn [name f & options]
            (if (= name "readystatechange")
              (swap! callbacks-for-ready-state conj f)
              (.apply browser-event-listener js/document (clj->js (into [name f] options))))))))

(defn invoke-late-ready-state-listeners
  "Fires any captured readystatechange callback functions and clears the
  internal list of pending readystatechange callbacks that need to be triggered.

  See: capture-late-readystatechange-callbacks"
  []
  (doseq [f @callbacks-for-ready-state]
    (f))
  (reset! callbacks-for-ready-state []))

(defn attach-capture-late-readystatechange-callbacks []
  (goog.events/listen js/document "readystatechange" capture-late-readystatechange-callbacks))

(defn unattach-capture-late-readystatechange-callbacks []
  (goog.events/unlisten js/document "readystatechange" capture-late-readystatechange-callbacks))

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

(def ^:private dom-is-ready (atom false))

(defn after-dom-ready [f]
  (if @dom-is-ready
    (f)
    (goog.events/listen js/document "readystatechange"
                        (fn [e]
                          (when (= "complete" (.-readyState js/document))
                            (reset! dom-is-ready true)
                            (f))))))

(defn esc-key-handler
  [e]
  (when (= "Escape" (.-key e))
    (handle-message events/escape-key-pressed)))

(defmethod transitions/transition-state events/escape-key-pressed
  [_ event args app-state]
  (assoc-in app-state keypaths/addons-popup-displayed? false))

(defmethod effects/perform-effects events/escape-key-pressed [_ event args _ app-state]
  (when-let [message-to-handle (get popup-dismiss-events (get-in app-state keypaths/popup))]
    (handle-message message-to-handle)))

(defn attach-esc-key-listener
  []
  (goog.events/listen js/document EventType/KEYDOWN esc-key-handler))

(defn detach-esc-key-listener
  []
  (goog.events/unlisten js/document EventType/KEYDOWN esc-key-handler))

(defn attach-global-listeners []
  ;; we don't need to worry about unlistening because the handler will do that
  (goog.events/listen js/document "readystatechange"
                      (fn [e]
                        (when (= "complete" (.-readyState js/document))
                          (set! (.-addEventListener js/document)
                                (fn [name f & options]
                                  (when (= name "readystatechange")
                                    (swap! callbacks-for-ready-state conj f))
                                  (.apply browser-event-listener js/document (clj->js (into [name f] options))))))))
  ;; Full screen events cannot be unlistened for some reason, so we need to
  ;; track when we attach ourselves
  (when-not @i-know-i-have-attached
    (swap! i-know-i-have-attached (constantly true))
    (doseq [evt-name fullscreen-events]
      (goog.events/listen js/document evt-name handle-fullscreen-change))))
