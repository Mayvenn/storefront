(ns stylist-matching.ui.stylist-search
  (:require #?@(:cljs [[storefront.hooks.google-maps :as google-maps]
                       storefront.keypaths
                       adventure.keypaths
                       [storefront.hooks.stringer :as stringer]])
            [mayvenn.visual.tools :refer [with]]
            [mayvenn.visual.ui.titles :as titles]
            [stylist-matching.core :refer [stylist-matching<-]]
            [stylist-matching.keypaths :as k]
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.effects :as fx]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.trackings :as trackings]
            [storefront.events :as e]
            [storefront.accessors.experiments :as experiments]))

;; --------------------- Address Input behavior

(defmethod fx/perform-effects e/adventure-address-component-mounted
  [_ _ {:keys [address-elem result-keypath]} _ _]
  #?(:cljs
     (google-maps/attach "geocode"
                         address-elem
                         result-keypath)))

(defn ^:private address-input
  [elemID]
  #?(:cljs (-> js/document
               (.getElementById elemID)
               .-value)))

(defmethod trackings/perform-track e/control-adventure-location-submit
  [_ _ _ state]
  (let [{:param/keys [location address]}        (stylist-matching<- state)
        {:keys [latitude longitude city state]} location]
    #?(:cljs
       (stringer/track-event "adventure_location_submitted"
                             {:location_submitted address
                              :city               city
                              :state              state
                              :latitude           latitude
                              :longitude          longitude}))))

(defmethod fx/perform-effects e/control-adventure-location-submit
  [_ _ _ _ state]
  ;; Address/Location search
  (messages/handle-message e/flow|stylist-matching|param-address-constrained
                           {:address (address-input "stylist-match-address")})
  (messages/handle-message e/flow|stylist-matching|param-location-constrained
                           (get-in state k/google-location))
  (if (experiments/top-stylist? state)
    (messages/handle-message e/flow|stylist-matching|diverted-to-top-stylist)
    (messages/handle-message e/flow|stylist-matching|prepared)))

;; ---------------------------------------------

;; FIXME(matching) Want to use init event, but it does more than the location
;; FIXME(matching) Not sure this ever fires
(defmethod fx/perform-effects e/clear-selected-location
  [_ _ _ _ _]
  (messages/handle-message e/flow|stylist-matching|param-location-constrained))

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

(c/defdynamic-component organism
  (did-mount
   [_]
   (messages/handle-message e/adventure-address-component-mounted
                            {:address-elem   "stylist-match-address"
                             :result-keypath k/google-location}))
  (render
   [this]
   (let [data (c/get-props this)]
     (c/html
      [:div.m5
       [:div.mb4
        (titles/canela-left (with :stylist-search.title data))]
       [:div.mb4
        (stylist-search-location-search-box data)]
       [:div
        (stylist-search-button data)]]))))
