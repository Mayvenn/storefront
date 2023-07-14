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

(defn from-calendly?
  [event]
  (and event (= (.indexOf event "calendly") 0)))

(defmethod fx/perform-effects e/instrumented-calendly
  [_ _ _ _ _]
  (.addEventListener js/window "message"
                     (fn [e]
                       (let [{:keys [event]} (js->clj e.data :keywordize-keys true)]
                         (when (from-calendly? event)
                           (publish
                            (case event
                              "calendly.profile_page_viewed"    e/calendly-profile-page-viewed
                              "calendly.event_type_viewed"      e/calendly-event-type-viewed
                              "calendly.date_and_time_selected" e/calendly-date-and-time-selected
                              "calendly.event_scheduled"        e/calendly-event-scheduled
                              e/calendly-unknown-event)
                            event))))))

(defmethod fx/perform-effects e/show-calendly
  [dispatch event args prev-app-state app-state]
  (->> {:url "https://calendly.com/mayvenn-consultations/call"}
       clj->js
       js/window.Calendly.initPopupWidget))

(defmethod trk/perform-track e/show-calendly
  [_ _ _ _]
  (stringer/track-event "phone_consult_calendly_cta_clicked" {}))

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
