(ns adventure.find-your-stylist
  (:require
   #?@(:cljs [[om.core :as om]
              [storefront.platform.messages :as messages]
              [storefront.effects :as effects]
              [storefront.hooks.places-autocomplete :as places-autocomplete]
              [sablono.core :as sablono]])
   [storefront.platform.component-utils :as utils]
   [adventure.components.basic-prompt :as basic-prompt]
   [adventure.components.header :as header]
   [adventure.handlers :as handlers]
   [adventure.keypaths :as keypaths]
   [storefront.keypaths :as storefront.keypaths]
   [storefront.accessors.experiments :as experiments]
   [storefront.component :as component]
   [storefront.events :as events]
   [storefront.components.ui :as ui]
   [storefront.transitions :as transitions]))

(defn ^:private location->prefill-string
  [{:as location :keys [:zipcode :city :state]}]
  (when (seq location)
    ;; City not guaranteed (c.f 12309)
    (str (when city (str city ", ")) state " " zipcode ", USA")))

(defmethod transitions/transition-state events/navigate-adventure-find-your-stylist [_ event args app-state]
  (let [preselected-location (get-in app-state keypaths/adventure-stylist-match-location)
        location-prefill     (location->prefill-string preselected-location)]
    (-> app-state
        (assoc-in keypaths/adventure-stylist-match-zipcode location-prefill))))

(defmethod transitions/transition-state events/clear-selected-location
  [_ event {:keys [keypath value]} app-state]
  (-> app-state
      (assoc-in keypaths/adventure-stylist-match-location nil)))

#?(:cljs (defmethod effects/perform-effects events/adventure-zipcode-component-mounted
           [_ event {:keys [address-elem address-keypath]} _ app-state]
           (places-autocomplete/attach "(regions)" address-elem address-keypath)))

(defn ^:private query [data]
  (let [adventure-choices (get-in data keypaths/adventure-choices)
        hair-flow?        (-> adventure-choices :flow #{"match-stylist"})]
    {:background-image      "https://ucarecdn.com/54f294be-7d57-49ba-87ce-c73394231f3c/aladdinMatchingOverlayImagePurpleGR203Lm3x.png"
     :stylist-match-zipcode (get-in data keypaths/adventure-stylist-match-zipcode)
     :places-loaded?        (get-in data storefront.keypaths/loaded-places)
     :header-data           {:current-step 6
                             :title        [:div.medium "Find Your Stylist"]
                             :subtitle     (str "Step " (if hair-flow? 2 3) " of 3")
                             :back-link    events/navigate-adventure-match-stylist}
     :selected-location     (get-in data keypaths/adventure-stylist-match-location)}))

#?(:cljs
   (defn ^:private handle-on-change [selected-location ^js/Event e]
     (messages/handle-message events/control-change-state
                              {:keypath keypaths/adventure-stylist-match-zipcode
                               :value   (.. e -target -value)})
     (when selected-location
       (messages/handle-message events/clear-selected-location))))

(defn ^:private places-component-inner
  [value selected-location]
  [:div.flex.justify-center
   [:input.h4.border-none.px3.bg-white.col-10
    (merge {:value       (or value "")
            :id          "stylist-match-zipcode"
            :data-test   "stylist-match-zip"
            :autoFocus   true
            :placeholder "zipcode"
            :pattern     "[0-9]*"  ; ios/safari numpad
            :inputMode   "numeric" ; android/chrome numpad
            :data-ref    "stylist-match-zip"}
           #?(:cljs
              {:on-submit (partial handle-on-change selected-location)
               :on-change (partial handle-on-change selected-location)}))]
   (ui/teal-button (merge {:style          {:width  "45px"
                                            :height "45px"}
                           :disabled?      (not (:zipcode selected-location))
                           :disabled-class "bg-light-gray gray"
                           :class          "flex items-center justify-center medium not-rounded x-group-item"}
                          (utils/route-to events/navigate-adventure-how-far)) "→")])
#?(:cljs
   (defn ^:private places-component-outer [{:keys [value selected-location]} owner]
     (reify
       om/IDidMount
       (did-mount [this]
         (messages/handle-message events/adventure-zipcode-component-mounted {:address-elem    "stylist-match-zipcode"
                                                                              :address-keypath keypaths/adventure-stylist-match-location}))
       om/IRender
       (render [_]
         (sablono/html (places-component-inner value selected-location))))))

(defn component
  [{:keys [header-data places-loaded? background-image stylist-match-zipcode selected-location]} owner _]
  (component/create
   [:div.bg-lavender.white.center.flex.flex-auto.flex-column
    (when header-data
      (header/built-component header-data nil))
    [:div.flex.flex-column.items-center.justify-center
     {:style {:height              "246px"
              :background-image    (str "url(" background-image ")")
              :background-position "bottom"
              :background-repeat   "no-repeat"
              :background-size     "cover"}}
     [:div.pt8
      [:div.h3.medium.mb2.col-8.mx-auto "Where do you want to get your hair done?"]

      [:div.col-12.mx-auto
       #?(:cljs
          (when places-loaded?
            (om/build places-component-outer {:value       stylist-match-zipcode
                                              :selected-location selected-location})))]]]]))

(defn built-component [data opts]
  (component/build component (query data) opts))
