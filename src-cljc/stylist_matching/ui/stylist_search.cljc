(ns stylist-matching.ui.stylist-search
  (:require #?@(:cljs [[storefront.hooks.google-maps :as google-maps]
                       [storefront.history :as history]
                       [storefront.hooks.stringer :as stringer]
                       [storefront.browser.cookie-jar :as cookie]])
            [adventure.keypaths]
            [storefront.component :as component :refer [defcomponent defdynamic-component]]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.transitions :as transitions]
            [storefront.trackings :as trackings]
            [stylist-directory.keypaths]))

(defmethod effects/perform-effects events/adventure-address-component-mounted
  [_ event {:keys [address-elem address-keypath]} _ app-state]
  #?(:cljs
     (google-maps/attach "geocode" address-elem address-keypath)))

(defmethod transitions/transition-state events/clear-selected-location
  [_ event _ app-state]
  (-> app-state
      (assoc-in adventure.keypaths/adventure-stylist-match-location nil)))

(defmethod transitions/transition-state events/control-adventure-location-submit
  [_ event _ app-state]
  #?(:cljs
     (-> app-state
         (assoc-in adventure.keypaths/adventure-matched-stylists nil)
         (assoc-in stylist-directory.keypaths/stylist-search-selected-location nil)
         (assoc-in stylist-directory.keypaths/stylist-search-selected-filters nil)
         (assoc-in stylist-directory.keypaths/stylist-search-address-input nil)
         (assoc-in adventure.keypaths/adventure-stylist-match-address
                   (.-value (.getElementById js/document "stylist-match-address"))))))

(defmethod trackings/perform-track events/control-adventure-location-submit
  [_ event _ app-state]
  #?(:cljs
     (let [{:keys [latitude longitude city state]} (get-in app-state adventure.keypaths/adventure-stylist-match-location)]
       (stringer/track-event "adventure_location_submitted"
                             {:location_submitted (get-in app-state adventure.keypaths/adventure-stylist-match-address)
                              :city               city
                              :state              state
                              :latitude           latitude
                              :longitude          longitude}))))

(defmethod effects/perform-effects events/control-adventure-location-submit
  [_ event args _ app-state]
  #?(:cljs
     (let [cookie    (get-in app-state storefront.keypaths/cookie)
           adventure (get-in app-state adventure.keypaths/adventure)]
       (cookie/save-adventure cookie adventure)
       (history/enqueue-navigate events/navigate-adventure-stylist-results-pre-purchase args))))

(defn ^:private change-state
  [selected-location #?(:cljs ^js/Event e :clj e)]
  (when selected-location
    (messages/handle-message events/clear-selected-location))
  (->> {:keypath adventure.keypaths/adventure-stylist-match-address
        :value (.. e -target -value)}
       (messages/handle-message events/control-change-state)))

(defn stylist-search-location-search-box
  [{:stylist-search.location-search-box/keys
    [id placeholder value clear?]}]
  (when id
    (component/html
     (let [handler (partial change-state clear?)]
       [:div.bg-white
        (ui/text-field-large
         {:value       value
          :id          id
          :data-test   id
          :autoFocus   true
          :focused     false
          :placeholder placeholder
          :on-submit   handler
          :on-change   handler})]))))

(defn stylist-search-button
  [{:stylist-search.button/keys [id disabled? target label]}]
  (when id
    (component/html
     (ui/button-large-primary (merge {:disabled? disabled?
                                      :data-test "stylist-match-address-submit"}
                                     (apply utils/fake-href target))
                              label))))

(defn stylist-search-title-molecule
  [{:stylist-search.title/keys [id primary secondary]}]
  (when id
    (component/html
     [:div.left-align
      [:div.title-2.canela.my2.light primary]
      [:div.h5.my2.light secondary]])))

(defdynamic-component organism
  (did-mount [_]
             (messages/handle-message events/adventure-address-component-mounted
                                      {:address-elem    "stylist-match-address"
                                       :address-keypath adventure.keypaths/adventure-stylist-match-location}))
  (render [this]
          (let [data (component/get-props this)]
            (component/html
             [:div.m5
              [:div.mb4
               (stylist-search-title-molecule data)]
              [:div.mb4
               (stylist-search-location-search-box data)]
              [:div
               (stylist-search-button data)]]))))

(defmethod effects/perform-effects events/api-success-fetch-stylists-within-radius
  [_ _ _ app-state]
  (messages/handle-message events/adventure-stylist-search-results-displayed {}))

(defmethod effects/perform-effects events/api-success-fetch-stylists-matching-filters
  [_ _ _ app-state]
  (messages/handle-message events/adventure-stylist-search-results-displayed {}))
