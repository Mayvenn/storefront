(ns adventure.matching-stylist-wait
  (:require #?@(:cljs [[om.core :as om]
                       [storefront.api :as api]
                       [storefront.components.ugc :as ugc]
                       [storefront.hooks.pixlee :as pixlee-hook]
                       [storefront.history :as history]
                       [storefront.config :as config]])
            [storefront.transitions :as transitions]
            [storefront.events :as events]
            [storefront.effects :as effects]
            [storefront.component :as component]
            [storefront.keypaths :as keypaths]
            [adventure.keypaths :as adventure-keypaths]
            [spice.date :as date]))

(defmethod transitions/transition-state events/navigate-adventure-matching-stylist-wait [_ _ _ app-state]
  (assoc-in app-state adventure-keypaths/adventure-matching-stylists-timer (date/add-delta (date/now) {:seconds 3})))

(defmethod effects/perform-effects events/navigate-adventure-matching-stylist-wait [_ _ _ _ app-state]
  #?(:cljs
     (let [{:keys [latitude longitude]}   (get-in app-state adventure-keypaths/adventure-stylist-match-location)
           {:keys [how-far install-type]} (get-in app-state adventure-keypaths/adventure-choices)]
       (api/fetch-stylists-within-radius (get-in app-state keypaths/api-cache)
                                         {:latitude     latitude
                                          :longitude    longitude
                                          :radius       how-far
                                          :install-type install-type
                                          :limit        3}))))

(defmethod transitions/transition-state events/api-success-fetch-stylists-within-radius
  [_ _ {:keys [stylists]} app-state]
  (assoc-in app-state adventure-keypaths/adventure-matched-stylists stylists))

(defmethod effects/perform-effects events/api-success-fetch-stylists-within-radius
  [_ _ {:keys [stylists]} _ app-state]
  (let [timer      (get-in app-state adventure-keypaths/adventure-matching-stylists-timer)
        ms-to-wait (max 0
                        (- (date/to-millis timer)
                           (date/to-millis (date/now))))]
    #?(:cljs
       (history/enqueue-navigate
        (if (seq stylists)
          events/navigate-adventure-stylist-results
          events/navigate-adventure-out-of-area) {:timeout ms-to-wait}))))

(defn ^:private component
  [{:keys [] :as data} _ _]
  (component/create
   [:div.white.flex.flex-auto.flex-column.items-center.pt4
    {:style {:background "linear-gradient(#cdb8d9,#9a8fb4)"}}
    [:div.mt10.mb2
     [:img {:src "https://ucarecdn.com/9b6a76cc-7c8e-4715-8973-af2daa15a5da/matching-stylist-wait.gif"
            :width "90px"}]]
    [:div.col-8.h3.my2.medium.center "Matching you with a" [:br] " Mayvenn Certified Stylist..."]
    [:ul.col-7.h6.list-img-purple-checkmark
     (mapv (fn [%] [:li.mb1 %])
           ["Licensed Salon Stylist" "Mayvenn Certified" "In your area"])]]))

(defn built-component
  [data opts]
  (component/build component {} opts))


