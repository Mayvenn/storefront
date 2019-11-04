(ns storefront.hooks.stringer
  (:require [storefront.events :as events]
            [storefront.platform.messages :refer [handle-message]]))

(defn track-event
  ([event-name] (track-event event-name {} nil))
  ([event-name payload] (track-event event-name payload nil))
  ([event-name payload callback-event] (track-event event-name payload callback-event nil))
  ([event-name payload callback-event callback-args]
   (when (.hasOwnProperty js/window "stringer")
     (.track js/stringer event-name (clj->js payload)
             (when callback-event
               (fn [] (handle-message callback-event (merge {:tracking-event event-name :payload payload} callback-args))))))))

(defn track-page [store-experience]
  (track-event "pageview" {:store-experience store-experience}))

(defn identify
  ([args]
   (identify args nil))
  ([{:keys [id email]} callback-event]
   (when (.hasOwnProperty js/window "stringer")
     (.identify js/stringer email id)
     (when callback-event
       (handle-message callback-event
                       {:stringer.identify/id    id
                        :stringer.identify/email email})))))

(defn track-clear []
  (when (.hasOwnProperty js/window "stringer")
    (.clear js/stringer)
    (.track js/stringer "clear_identify")))

(defn fetch-browser-id []
  (.getBrowserId js/stringer #(handle-message events/stringer-browser-identified {:id %})))
