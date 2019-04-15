(ns adventure.stylist-matching.matching-stylist-wait
  (:require #?@(:cljs [[om.core :as om]
                       [storefront.api :as api]
                       [storefront.components.ugc :as ugc]
                       [storefront.hooks.pixlee :as pixlee-hook]
                       [storefront.history :as history]
                       [storefront.config :as config]
                       [storefront.platform.messages :refer [handle-message]]])
            [adventure.components.wait-spinner :as wait-spinner]
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
      (assoc-in adventure-keypaths/adventure-choices-selected-stylist-id nil)
      (assoc-in adventure-keypaths/adventure-servicing-stylist nil)))

(defn ^:private ms-to-wait [app-state]
  (max 0
       (- (date/to-millis (get-in app-state adventure-keypaths/adventure-matching-stylists-timer))
          (date/to-millis (date/now)))))

;; PRE-PURCHASE FLOW

(defmethod effects/perform-effects events/navigate-adventure-matching-stylist-wait-pre-purchase [_ _ _ _ app-state]
  #?(:cljs
     (let [{:keys [latitude longitude]}               (get-in app-state adventure-keypaths/adventure-stylist-match-location)
           {:as choices :keys [how-far install-type]} (get-in app-state adventure-keypaths/adventure-choices)]
       (prn (get-in app-state adventure-keypaths/adventure-stylist-match-location))
       (prn (get-in app-state adventure-keypaths/adventure-choices))
       (if-not (and latitude longitude how-far install-type)
         (history/enqueue-redirect events/navigate-adventure-home)
         (handle-message events/api-fetch-stylists-within-radius-pre-purchase)))))

(defmethod effects/perform-effects events/api-fetch-stylists-within-radius-pre-purchase [_ event _ _ app-state]
  #?(:cljs
     (let [{:keys [latitude longitude]} (get-in app-state adventure.keypaths/adventure-stylist-match-location)
           choices (get-in app-state adventure.keypaths/adventure-choices)
           query   {:latitude     latitude
                    :longitude    longitude
                    :radius       (:how-far choices)
                    :install-type (:install-type choices)
                    :choices      choices}] ; For trackings purposes only
       (api/fetch-stylists-within-radius (get-in app-state keypaths/api-cache)
                                         query
                                         #(handle-message events/api-success-fetch-stylists-within-radius-pre-purchase
                                                          (merge {:query query} %))))))

(defmethod effects/perform-effects events/api-success-fetch-stylists-within-radius-pre-purchase [_ event _ _ app-state]
  #?(:cljs
     (let [matched-stylists (get-in app-state adventure-keypaths/adventure-matched-stylists)
           nav-event        (if (empty? matched-stylists)
                              events/navigate-adventure-out-of-area
                              events/navigate-adventure-stylist-results-pre-purchase)]
       (history/enqueue-redirect nav-event {:timeout (ms-to-wait app-state)}))))

;; POST-PURCHASE FLOW

(defmethod effects/perform-effects events/navigate-adventure-matching-stylist-wait-post-purchase [_ _ _ _ app-state]
  #?(:cljs
     (history/enqueue-redirect
      events/navigate-adventure-stylist-results-post-purchase
      {:timeout (ms-to-wait app-state)})))

(defn built-component
  [data opts]
  (component/build wait-spinner/component {} opts))
