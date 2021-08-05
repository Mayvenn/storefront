(ns storefront.browser.events
  (:require goog.events
            catalog.keypaths
            [goog.events.EventType :as EventType]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.effects :as effects]
            [storefront.transitions :as transitions]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [stylist-directory.keypaths]))

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

(def popup-dismiss-events
  {:consolidated-cart-free-install events/control-consolidated-cart-free-install-dismiss
   :share-cart                     events/control-popup-hide
   :design-system                  events/control-design-system-popup-dismiss
   :addon-services-menu            events/control-addon-service-menu-dismiss
   :stylist-search-filters         events/control-stylist-search-filters-dismiss
   :cart-swap                      events/control-cart-swap-popup-dismiss
   :length-guide                   events/popup-hide-length-guide})

(defn dismiss-stylist-filter-modal-event
  [app-state]
  (when (get-in app-state stylist-directory.keypaths/stylist-search-show-filters?)
    events/control-stylist-search-filters-dismiss))

(defn dismiss-look-detail-picker-modal-event
  [app-state]
  (when (get-in app-state catalog.keypaths/detailed-look-picker-visible?)
    events/control-look-detail-picker-close))

(defn dismiss-product-detail-picker-modal-event
  [app-state]
  (when (get-in app-state catalog.keypaths/detailed-pdp-picker-visible?)
    events/control-pdp-picker-close))

(defmethod effects/perform-effects events/escape-key-pressed [_ event args _ app-state]
  (when-let [message-to-handle (get popup-dismiss-events (get-in app-state keypaths/popup))]
    (handle-message message-to-handle))
  (when-let [message-to-handle (dismiss-stylist-filter-modal-event app-state)]
    (handle-message message-to-handle))
  (when-let [message-to-handle (dismiss-look-detail-picker-modal-event app-state)]
    (handle-message message-to-handle))
  (when-let [message-to-handle (dismiss-product-detail-picker-modal-event app-state)]
    (handle-message message-to-handle)))

(defn click-away-handler [e]
  (let [target (.-target e)]
    (when-not (some #(.contains % target) (array-seq (js/document.querySelectorAll ".flyout")))
      (handle-message events/flyout-click-away))))

(defn attach-click-away-handler
  []
  (goog.events/listen js/document EventType/CLICK click-away-handler))

(defn detach-click-away-handler
  []
  (goog.events/unlisten js/document EventType/CLICK click-away-handler))

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
