(ns storefront.debounce
  (:require
   [storefront.effects :as effects]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.transitions :as transitions]
   [storefront.platform.messages :as messages]))


(def debounce-timers
  (conj keypaths/ui :debounce-timers))

(defmethod effects/perform-effects events/debounced-event-enqueued
  [_ _ {:keys [debounced-event]} prev-app-state app-state]
  #?(:cljs
     (when-let [timer (get-in prev-app-state (conj debounce-timers debounced-event))]
       (js/clearTimeout timer))))

(defmethod transitions/transition-state events/debounced-event-enqueued
  [_ _ {:keys [timer debounced-event]} app-state]
  (assoc-in app-state (conj debounce-timers debounced-event) timer))

(defmethod effects/perform-effects events/debounced-event-initialized
  [_ _ {[debounced-event debounced-event-args] :message
        timeout                                :timeout} prev-app-state app-state]
  (when-let [debounce-timer (messages/handle-later debounced-event debounced-event-args timeout)]
    (messages/handle-message events/debounced-event-enqueued {:timer           debounce-timer
                                                              :debounced-event debounced-event})))
