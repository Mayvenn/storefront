(ns adventure.stylist-matching.find-your-stylist
  (:require
   #?@(:cljs [[om.core :as om]
              [storefront.platform.messages :as messages]
              [storefront.effects :as effects]
              [storefront.history :as history]
              [storefront.hooks.stringer :as stringer]
              [storefront.hooks.google-maps :as google-maps]
              [storefront.browser.cookie-jar :as cookie]
              [sablono.core :as sablono]])
   [storefront.platform.component-utils :as utils]
   [adventure.components.header :as header]
   [adventure.keypaths :as keypaths]
   [storefront.keypaths :as storefront.keypaths]
   [storefront.component :as component]
   [storefront.events :as events]
   [storefront.components.flash :as flash]
   [storefront.components.ui :as ui]
   [storefront.transitions :as transitions]
   [storefront.trackings :as trackings]
   [adventure.progress :as progress]))

(defmethod transitions/transition-state events/clear-selected-location
  [_ event _ app-state]
  (-> app-state
      (assoc-in keypaths/adventure-stylist-match-location nil)))

#?(:cljs (defmethod effects/perform-effects events/adventure-address-component-mounted
           [_ event {:keys [address-elem address-keypath]} _ app-state]
           (google-maps/attach "geocode" address-elem address-keypath)))

(defn ^:private query [data]
  (let [current-step 2
        flash-data   (when (get-in data storefront.keypaths/flash-now-failure-message)
                       {:ui {:flash (get-in data storefront.keypaths/flash)}})]
    {:background-image      "https://ucarecdn.com/54f294be-7d57-49ba-87ce-c73394231f3c/-/format/auto/aladdinMatchingOverlayImagePurpleGR203Lm3x.png"
     :stylist-match-address (get-in data keypaths/adventure-stylist-match-address)
     :google-maps-loaded?   (get-in data storefront.keypaths/loaded-google-maps)
     :current-step          current-step
     :header-data           {:progress                progress/find-your-stylist
                             :title                   [:div.medium "Find Your Stylist"]
                             :subtitle                (str "Step " current-step " of 3")
                             :back-navigation-message [events/navigate-adventure-match-stylist]}
     :flash-data            flash-data
     :selected-location     (get-in data keypaths/adventure-stylist-match-location)}))

#?(:cljs
   (defn ^:private handle-on-change [selected-location ^js/Event e]
     (messages/handle-message events/control-change-state
                              {:keypath keypaths/adventure-stylist-match-address
                               :value   (.. e -target -value)})
     (when selected-location
       (messages/handle-message events/clear-selected-location))))

(defn ^:private places-component-inner
  [{:keys [value selected-location current-step]}]
  [:div.flex.justify-center
   [:input.h4.border-none.px3.bg-white.col-10
    (merge {:value       (or value "")
            :id          "stylist-match-address"
            :data-test   "stylist-match-address"
            :autoFocus   true
            :placeholder "search city or street address"}
           #?(:cljs
              {:on-submit (partial handle-on-change selected-location)
               :on-change (partial handle-on-change selected-location)}))]
   (let [disabled? (or (empty? value)
                       (nil? (:latitude selected-location))
                       (nil? (:longitude selected-location)))]
     (ui/teal-button (merge {:style          {:width  "45px"
                                              :height "45px"}
                             :disabled?      disabled?
                             :disabled-class "bg-light-gray gray"
                             :data-test      "stylist-match-address-submit"
                             :class          "flex items-center justify-center medium not-rounded x-group-item"}
                            (utils/fake-href events/control-adventure-location-submit {:current-step current-step}))
                     (ui/forward-arrow {:disabled? disabled?
                                        :width     "14"})))])

#?(:cljs
  (defmethod transitions/transition-state events/control-adventure-location-submit
    [_ event _ app-state]
    (assoc-in app-state
              keypaths/adventure-stylist-match-address
              (.-value (.getElementById js/document "stylist-match-address")))))

#?(:cljs
   (defmethod effects/perform-effects events/control-adventure-location-submit
     [_ event args _ app-state]
     (let [cookie    (get-in app-state storefront.keypaths/cookie)
           adventure (get-in app-state keypaths/adventure)]
       (cookie/save-adventure cookie adventure)
       (history/enqueue-navigate events/navigate-adventure-matching-stylist-wait-pre-purchase))))

(defmethod transitions/transition-state events/navigate-adventure-find-your-stylist
  [_ event _ app-state]
  (assoc-in app-state keypaths/adventure-stylist-match-address nil))

#?(:cljs
   (defmethod effects/perform-effects events/navigate-adventure-find-your-stylist [_ _ _ _ app-state]
     (messages/handle-message events/adventure-clear-servicing-stylist)
     (google-maps/insert)))

(defmethod trackings/perform-track events/control-adventure-location-submit
  [_ event {:keys [current-step]} app-state]
  #?(:cljs
     (let [{:keys [latitude longitude city state]} (get-in app-state keypaths/adventure-stylist-match-location)]
       (stringer/track-event "adventure_location_submitted"
                             {:location_submitted (get-in app-state keypaths/adventure-stylist-match-address)
                              :current_step       current-step
                              :city               city
                              :state              state
                              :latitude           latitude
                              :longitude          longitude}))))

#?(:cljs
   (defn ^:private places-component-outer [data owner]
     (reify
       om/IDidMount
       (did-mount [this]
         (messages/handle-message events/adventure-address-component-mounted {:address-elem    "stylist-match-address"
                                                                              :address-keypath keypaths/adventure-stylist-match-location}))
       om/IRender
       (render [_]
         (sablono/html (places-component-inner data))))))

(defn component
  [{:keys [header-data flash-data current-step google-maps-loaded? background-image stylist-match-address selected-location]} owner _]
  (component/create
   [:div.bg-lavender.white.center.flex.flex-auto.flex-column
    (header/built-component header-data nil)
    [:div.flex.flex-column.items-center.justify-center
     {:style {:height              "246px"
              :background-image    (str "url(" background-image ")")
              :background-position "bottom"
              :background-repeat   "no-repeat"
              :background-size     "cover"}}
     [:div.pt8
      (when flash-data
        [:div.pt8 (flash/find-your-stylist-built-component flash-data nil)])
      [:div.h3.medium.mb2.col-8.mx-auto "Where do you want to get your hair done?"]

      [:div.col-12.mx-auto
       #?(:cljs
          (if google-maps-loaded?
            (om/build places-component-outer {:value             stylist-match-address
                                              :current-step      current-step
                                              :selected-location selected-location})

            [:div.flex.items-center.justify-center.h1.mt3
             ui/spinner]))]]]]))

(defn built-component [data opts]
  (component/build component (query data) opts))
