(ns storefront.hooks.calendly
  (:require [clojure.string :refer [starts-with?]]
            [storefront.browser.tags :as tags]
            [storefront.events :as e]
            [storefront.effects :as fx]
            [storefront.hooks.stringer :as stringer]
            [storefront.platform.messages
             :as messages
             :refer [handle-message] :rename {handle-message publish}]
            [storefront.trackings :as trk]
            [storefront.hooks.google-analytics :as google-analytics]))

(defn insert []
  (tags/insert-tag-with-callback
   (tags/src-tag "https://assets.calendly.com/assets/external/widget.js"
                 "calendly")
   #(publish e/inserted-calendly)))

(defmethod fx/perform-effects e/inserted-calendly
  [_ _ _ _ _]
  (publish e/instrumented-calendly {}))

(defn- from-calendly?
  [event-name]
  (and (string? event-name)
       (starts-with? event-name "calendly")))

(defn- handle-post-message
  [message]
  (let [{:as event-data event-name :event}
        (-> message.data
            (js->clj :keywordize-keys true))]
    (when (from-calendly? event-name)
      (publish (case event-name
                 "calendly.profile_page_viewed"    e/calendly-profile-page-viewed
                 "calendly.event_type_viewed"      e/calendly-event-type-viewed
                 "calendly.date_and_time_selected" e/calendly-date-and-time-selected
                 "calendly.event_scheduled"        e/calendly-event-scheduled
                 e/calendly-unknown-event)
               event-data))))

(defmethod fx/perform-effects e/instrumented-calendly
  [_ _ _ _ _]
  (.addEventListener js/window "message" handle-post-message))

(defmethod fx/perform-effects e/show-calendly
  [_ _ _ _ _]
  (->> {:url "https://calendly.com/mayvenn-consultations/call"}
       clj->js
       js/window.Calendly.initPopupWidget))

(defmethod trk/perform-track e/show-calendly
  [_ _ _ _]
  (stringer/track-event "phone_consult_calendly_cta_clicked"
                        {}))

(defmethod trk/perform-track e/phone-consult-calendly-impression
  [_ _ _ _]
  (stringer/track-event "phone_consult_calendly_impression"
                        {}))

(defmethod trk/perform-track e/calendly-profile-page-viewed
  [_ _ event-data _]
  (->> {:event-data event-data}
       (stringer/track-event "calendly-profile-page-viewed")))

(defmethod trk/perform-track e/calendly-event-type-viewed
  [_ _ event-data _]
  (->> {:event-data event-data}
       (stringer/track-event "calendly-event-type-viewed")))

(defmethod trk/perform-track e/calendly-date-and-time-selected
  [_ _ event-data _]
  (->> {:event-data event-data}
       (stringer/track-event "calendly-date-and-time-selected")))

(defmethod trk/perform-track e/calendly-event-scheduled
  [_ _ event-data state]
  (->> {:event-data event-data}
       (stringer/track-event "calendly-event-scheduled"))
  (-> state
      google-analytics/retrieve-user-ecd
      google-analytics/track-schedule-consultation))

(defmethod trk/perform-track e/calendly-unknown-event
  [_ _ event-data _]
  (->> {:event-data event-data}
       (stringer/track-event "calendly-unknown-event")))
