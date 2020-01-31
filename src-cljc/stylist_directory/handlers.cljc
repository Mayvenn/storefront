(ns stylist-directory.handlers
  (:require [stylist-directory.keypaths :as keypaths]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.platform.messages :as messages]
            [storefront.transitions :as transitions]))

(defmethod transitions/transition-state events/api-success-fetch-stylist-details
  [_ _ {:keys [stylist]} app-state]
  (-> app-state
      (assoc-in (conj keypaths/stylists (:stylist-id stylist)) stylist)))

(defmethod effects/perform-effects events/api-failure-fetch-stylist-details [_ event args _ app-state]
  (messages/handle-message events/flash-later-show-failure
                           {:message (str "The stylist you are looking for is not available."
                                          " Please search for another stylist in your area below.")})
  (effects/redirect events/navigate-adventure-find-your-stylist))

(defmethod transitions/transition-state events/api-success-fetch-stylist-reviews
  [_ _ paginated-reviews app-state]
  (let [existing-reviews (:reviews (get-in app-state keypaths/paginated-reviews))]
    (-> app-state
        (assoc-in keypaths/paginated-reviews paginated-reviews)
        (update-in (conj keypaths/paginated-reviews :reviews) (partial concat existing-reviews)))))
