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
    (assoc-in app-state
              adventure.keypaths/adventure-stylist-match-address
              (.-value (.getElementById js/document "stylist-match-address")))))

#?(:cljs
   (defmethod effects/perform-effects events/control-adventure-location-submit
     [_ event args _ app-state]
     (let [cookie    (get-in app-state storefront.keypaths/cookie)
           adventure (get-in app-state adventure.keypaths/adventure)]
       (cookie/save-adventure cookie adventure)
       (history/enqueue-navigate events/navigate-adventure-matching-stylist-wait-pre-purchase))))

(defmethod transitions/transition-state events/navigate-adventure-find-your-stylist
  [_ event _ app-state]
  (assoc-in app-state adventure.keypaths/adventure-stylist-match-address nil))

#?(:cljs
   (defmethod effects/perform-effects events/navigate-adventure-find-your-stylist [_ _ _ _ app-state]
     (google-maps/insert)))

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
