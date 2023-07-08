(ns storefront.hooks.calendly
  (:require [storefront.browser.tags :as tags]
            [storefront.events :as e]
            [storefront.effects :as fx]
            [storefront.hooks.stringer :as stringer]
            [storefront.platform.messages
             :as messages
             :refer [handle-message] :rename {handle-message publish}]
            [storefront.trackings :as trk]
            cljs.core))


(defn insert []
  (tags/insert-tag-with-callback
   (tags/src-tag "https://assets.calendly.com/assets/external/widget.js"
                 "calendly")
   #(publish e/inserted-calendly)))

(defmethod fx/perform-effects e/inserted-calendly
  [_ event _ prev-state state]
  (publish e/instrumented-calendly {}))

(defn calendly-event?
  [e]
  (and e.data.event
       (= (.indexOf e.data.event "calendly") 0)))

(defmethod fx/perform-effects e/instrumented-calendly
  [_ event _ prev-state state]
  (.addEventListener js/window "message"
                     (fn [e]
                       (when (spice.core/spy "EVENT?" (calendly-event? e))
                         (let [event (js->clj e.data :keywordize-keys true)]
                           (prn event)
                           (case (->> event :event)
                            "calendly.profile_page_viewed"
                            (publish e/calendly-profile-page-viewed event)
                            "calendly.event_type_viewed"
                            (publish e/calendly-event-type-viewed event)
                            "calendly.date_and_time_selected"
                            (publish e/calendly-date-and-time-selected event)
                            "calendly.event_scheduled"
                            (publish e/calendly-event-scheduled event)
                            (publish e/calendly-unknown-event event)))))))

(defmethod fx/perform-effects e/show-calendly
  [dispatch event args prev-app-state app-state]
  (->> {:url "https://calendly.com/d/z7y-4h9-7jg/consultation-call"}
       clj->js
       js/window.Calendly.initPopupWidget))

(defmethod trk/perform-track e/show-calendly
  [_ _ _ _]
  (stringer/track-event "phone_consult_calendly_pressed" {}))

(defmethod trk/perform-track e/phone-consult-calendly-impression
  [_ _ _ _]
  (stringer/track-event "phone_consult_calendly_impression" {}))

(defmethod trk/perform-track e/calendly-profile-page-viewed
  [_ _ event _]
  (stringer/track-event "calendly-profile-page-viewed" {:event-data event}))

(defmethod trk/perform-track e/calendly-event-type-viewed
  [_ _ event _]
  (stringer/track-event "calendly-event-type-viewed" {:event-data event}))

(defmethod trk/perform-track e/calendly-date-and-time-selected
  [_ _ event _]
  (stringer/track-event "calendly-date-and-time-selected" {:event-data event}))

(defmethod trk/perform-track e/calendly-event-scheduled
  [_ _ event _]
  (stringer/track-event "calendly-event-scheduled" {:event-data event}))

(defmethod trk/perform-track e/calendly-unknown-event
  [_ _ event _]
  (stringer/track-event "calendly-unknown-event" {:event-data event}))
