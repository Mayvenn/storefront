(ns stylist-matching.ui.stylist-search
  (:require #?@(:cljs [[storefront.hooks.google-maps :as google-maps]
                       storefront.keypaths
                       adventure.keypaths
                       [storefront.hooks.stringer :as stringer]
                       [storefront.browser.cookie-jar :as cookie]])
            [stylist-matching.core :refer [stylist-matching<-]]
            [stylist-matching.keypaths :as k]
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.effects :as fx]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.trackings :as trackings]
            [storefront.events :as e]))

(defmethod fx/perform-effects e/adventure-address-component-mounted
  [_ _ {:keys [address-elem result-keypath]} _ _]
  #?(:cljs
     (google-maps/attach "geocode" address-elem result-keypath)))

;; FIXME(matching) Want to use init event, but it does more than the location
;; FIXME(matching) Not sure this ever fires
(defmethod fx/perform-effects e/clear-selected-location
  [_ _ _ _ _]
  (messages/handle-message e/flow|stylist-matching|param-location-constrained))

(defn ^:private address-input
  [elemID]
  #?(:cljs (-> js/document
                (.getElementById elemID)
                .-value)))

(defmethod fx/perform-effects e/control-adventure-location-submit
  [_ _ _ _ state]
  (messages/handle-message e/flow|stylist-matching|param-address-constrained
                           {:address (address-input "stylist-match-address")})
  (messages/handle-message e/flow|stylist-matching|param-location-constrained
                           (get-in state k/google-location))
  (messages/handle-message e/flow|stylist-matching|prepared)
  ;; FIXME(matching) stores whats in adventure, but obviously not too important
  ;; because only the half the data is here.
  #?(:cljs
     (let [cookie    (get-in state storefront.keypaths/cookie)
           adventure (get-in state adventure.keypaths/adventure)]
       (cookie/save-adventure cookie adventure))))

(defmethod trackings/perform-track e/control-adventure-location-submit
  [_ _ _ state]
  #?(:cljs
     (let [{:stylist-matching/keys [location address]} (stylist-matching<- state)
           {:keys [latitude longitude city state]}     location]
       (stringer/track-event "adventure_location_submitted"
                             {:location_submitted address
                              :city               city
                              :state              state
                              :latitude           latitude
                              :longitude          longitude}))))

(defn ^:private change-state
  [clear? #?(:cljs ^js/Event e :clj e)]
  (when clear?
    (messages/handle-message e/clear-selected-location))
  (->> {:keypath k/google-input
        :value   (.. e -target -value)}
       (messages/handle-message e/control-change-state)))

(defn stylist-search-location-search-box
  [{:stylist-search.location-search-box/keys
    [id placeholder value clear?]}]
  (when id
    (c/html
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
    (c/html
     (ui/button-large-primary (merge {:disabled? disabled?
                                      :data-test "stylist-match-address-submit"}
                                     (apply utils/fake-href target))
                              label))))

(defn stylist-search-title-molecule
  [{:stylist-search.title/keys [id primary secondary]}]
  (when id
    (c/html
     [:div.left-align
      [:div.title-2.canela.my2.light primary]
      [:div.h5.my2.light secondary]])))

(c/defdynamic-component organism
  (did-mount [_]
             (messages/handle-message e/adventure-address-component-mounted
                                      {:address-elem   "stylist-match-address"
                                       :result-keypath k/google-location}))
  (render [this]
          (let [data (c/get-props this)]
            (c/html
             [:div.m5
              [:div.mb4
               (stylist-search-title-molecule data)]
              [:div.mb4
               (stylist-search-location-search-box data)]
              [:div
               (stylist-search-button data)]]))))
