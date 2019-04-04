(ns adventure.stylist-matching.matching-stylist-wait
  (:require #?@(:cljs [[om.core :as om]
                       [storefront.api :as api]
                       [storefront.components.ugc :as ugc]
                       [storefront.hooks.pixlee :as pixlee-hook]
                       [storefront.history :as history]
                       [storefront.config :as config]
                       [storefront.platform.messages :refer [handle-message]]])
            [storefront.transitions :as transitions]
            [storefront.events :as events]
            [storefront.effects :as effects]
            [storefront.component :as component]
            [storefront.keypaths :as keypaths]
            [adventure.keypaths :as adventure-keypaths]
            [spice.date :as date]))

(defmethod transitions/transition-state events/navigate-adventure-matching-stylist-wait [_ _ _ app-state]
  (-> app-state
      (assoc-in adventure-keypaths/adventure-matching-stylists-timer (date/add-delta (date/now) {:seconds 3}))
      (assoc-in adventure-keypaths/adventure-selected-stylist-id nil)
      (assoc-in adventure-keypaths/adventure-servicing-stylist nil)))

;; PRE-PURCHASE FLOW

(defmethod effects/perform-effects events/navigate-adventure-matching-stylist-wait-pre-purchase [_ _ _ _ app-state]
  #?(:cljs
     (let [{:keys [latitude longitude]}               (get-in app-state adventure-keypaths/adventure-stylist-match-location)
           {:as choices :keys [how-far install-type]} (get-in app-state adventure-keypaths/adventure-choices)
           order                                      (get-in app-state keypaths/order)]
       ;; NOTE: we always try to find stylists regardless of the data that is available but send all choices
       ;;       so we can analyze a buggy behavior if we arrive here without the necessary data
       (api/fetch-stylists-within-radius (get-in app-state keypaths/api-cache)
                                         {:latitude     latitude
                                          :longitude    longitude
                                          :radius       how-far
                                          :install-type install-type
                                          :choices      choices}
                                         (fn [results]
                                           (handle-message events/api-success-fetch-stylists-within-radius results)
                                           (handle-message events/wait-to-navigate-to-stylist-results)))
       ;; END NOTE
       (when-not (and latitude longitude how-far install-type)
         (history/enqueue-redirect events/navigate-adventure-home)))))

;; POST-PURCHASE FLOW

(defmethod effects/perform-effects events/navigate-adventure-matching-stylist-wait-post-purchase [_ _ _ _ app-state]
  #?(:cljs
     (handle-message events/wait-to-navigate-to-stylist-results)))

;; BOTH

(defmethod effects/perform-effects events/wait-to-navigate-to-stylist-results
  [_ _ _ _ app-state]
  #?(:cljs
     (let [adventure-flow   (get-in app-state adventure-keypaths/adventure-choices-flow)
           matched-stylists (get-in app-state adventure-keypaths/adventure-matched-stylists)
           timer      (get-in app-state adventure-keypaths/adventure-matching-stylists-timer)
           ms-to-wait (max 0
                           (- (date/to-millis timer)
                              (date/to-millis (date/now))))]
       (history/enqueue-redirect
        (cond (empty? matched-stylists)
              events/navigate-adventure-out-of-area

              (= adventure-flow "shop-hair")
              events/navigate-adventure-stylist-results-post-purchase

              :else
              events/navigate-adventure-stylist-results-pre-purchase)
        {:timeout ms-to-wait}))))

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


