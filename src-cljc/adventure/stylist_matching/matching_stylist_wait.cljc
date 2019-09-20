(ns adventure.stylist-matching.matching-stylist-wait
  (:require #?@(:cljs [[om.core :as om]
                       [storefront.api :as api]
                       [storefront.components.ugc :as ugc]
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
            [spice.date :as date]
            [adventure.keypaths :as adventure.keypaths]))

(defn ^:private ms-to-wait [app-state]
  (max 0
       (- (date/to-millis (get-in app-state adventure-keypaths/adventure-matching-stylists-timer))
          (date/to-millis (date/now)))))

;; PRE-PURCHASE FLOW

(defmethod effects/perform-effects events/api-fetch-stylists-within-radius-pre-purchase [_ event _ _ app-state]
  #?(:cljs
     (let [{:keys [latitude longitude]} (get-in app-state adventure.keypaths/adventure-stylist-match-location)
           choices (get-in app-state adventure.keypaths/adventure-choices)
           query   {:latitude     latitude
                    :longitude    longitude
                    :radius       "100mi"
                    :choices      choices}] ; For trackings purposes only
       (api/fetch-stylists-within-radius (get-in app-state keypaths/api-cache)
                                         query
                                         #(handle-message events/api-success-fetch-stylists-within-radius-pre-purchase
                                                          (merge {:query query} %))))))

(defmethod effects/perform-effects events/api-success-fetch-stylists-within-radius-pre-purchase [_ event _ _ app-state]
  #?(:cljs
     (when-not (get-in app-state adventure.keypaths/adventure-stylist-results-delaying?)
               (handle-message events/adventure-stylist-results-wait-resolved))))

;; POST-PURCHASE FLOW

(defmethod effects/perform-effects events/navigate-adventure-matching-stylist-wait-post-purchase [_ _ _ _ app-state]
  #?(:cljs
     (history/enqueue-redirect
      events/navigate-adventure-stylist-results-post-purchase
      {:timeout (ms-to-wait app-state)})))

(defn built-component
  [data opts]
  (component/build wait-spinner/component {} opts))
