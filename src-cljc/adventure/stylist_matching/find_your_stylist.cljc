(ns adventure.stylist-matching.find-your-stylist
  (:require #?@(:cljs [[storefront.history :as history]
                       [storefront.hooks.stringer :as stringer]
                       [storefront.hooks.google-maps :as google-maps]
                       [storefront.browser.cookie-jar :as cookie]])
            adventure.keypaths
            storefront.keypaths
            [storefront.platform.messages :as messages]
            [storefront.events :as events]
            [storefront.transitions :as transitions]
            [storefront.effects :as effects]
            [storefront.trackings :as trackings]))

(defmethod transitions/transition-state events/clear-selected-location
  [_ event _ app-state]
  (-> app-state
      (assoc-in adventure.keypaths/adventure-stylist-match-location nil)))

#?(:cljs (defmethod effects/perform-effects events/adventure-address-component-mounted
           [_ event {:keys [address-elem address-keypath]} _ app-state]
           (google-maps/attach "geocode" address-elem address-keypath)))

#?(:cljs
   (defn ^:private handle-on-change [selected-location ^js/Event e]
     (messages/handle-message events/control-change-state
                              {:keypath adventure.keypaths/adventure-stylist-match-address
                               :value   (.. e -target -value)})
     (when selected-location
       (messages/handle-message events/clear-selected-location))))

#?(:cljs
   (defmethod transitions/transition-state events/control-adventure-location-submit
     [_ event _ app-state]
     (-> app-state
         (assoc-in adventure.keypaths/adventure-matched-stylists nil)
         (assoc-in adventure.keypaths/adventure-stylist-match-address
                   (.-value (.getElementById js/document "stylist-match-address"))))))

#?(:cljs
   (defmethod effects/perform-effects events/control-adventure-location-submit
     [_ event args _ app-state]
     (let [cookie    (get-in app-state storefront.keypaths/cookie)
           adventure (get-in app-state adventure.keypaths/adventure)]
       (cookie/save-adventure cookie adventure)
       (history/enqueue-navigate events/navigate-adventure-stylist-results-pre-purchase args))))

(defmethod transitions/transition-state events/navigate-adventure-find-your-stylist
  [_ event _ app-state]
  (assoc-in app-state adventure.keypaths/adventure-stylist-match-address nil))

(def find-your-stylist-error-codes
  {"stylist-not-found"
   (str
    "The stylist you are looking for is not available."
    " Please search for another stylist in your area below.")})

#?(:cljs
   (defmethod effects/perform-effects events/navigate-adventure-find-your-stylist
     [_ current-nav-event {:keys [query-params] :as args} _ _]
     (if-let [error-message (-> query-params :error find-your-stylist-error-codes)]
       (do
         (messages/handle-message events/redirect {:nav-message
                                                   [current-nav-event
                                                    (update-in args [:query-params] dissoc :error)]})
         (messages/handle-message events/flash-later-show-failure {:message error-message}))
       (google-maps/insert))))

(defmethod trackings/perform-track events/control-adventure-location-submit
  [_ event {:keys [current-step]} app-state]
  #?(:cljs
     (let [{:keys [latitude longitude city state]} (get-in app-state adventure.keypaths/adventure-stylist-match-location)]
       (stringer/track-event "adventure_location_submitted"
                             {:location_submitted (get-in app-state adventure.keypaths/adventure-stylist-match-address)
                              :current_step       current-step
                              :city               city
                              :state              state
                              :latitude           latitude
                              :longitude          longitude}))))
