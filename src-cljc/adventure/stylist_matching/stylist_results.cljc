(ns adventure.stylist-matching.stylist-results
  (:require adventure.keypaths
            [storefront.effects :as effects]
            [storefront.events :as events]
            storefront.keypaths
            [storefront.trackings :as trackings]
            [storefront.transitions :as transitions]
            #?@(:cljs [[storefront.api :as api]
                       [storefront.browser.cookie-jar :as cookie-jar]
                       [storefront.history :as history]
                       [storefront.hooks.stringer :as stringer]
                       [storefront.platform.messages :as messages]])))

(defmethod transitions/transition-state events/control-adventure-stylist-gallery-open
  [_ _event {:keys [ucare-img-urls initially-selected-image-index]} app-state]
  (-> app-state
      (assoc-in adventure.keypaths/adventure-stylist-gallery-image-urls ucare-img-urls)
      (assoc-in adventure.keypaths/adventure-stylist-gallery-image-index initially-selected-image-index)))

(defmethod transitions/transition-state events/control-adventure-stylist-gallery-close [_ _event _args app-state]
  (-> app-state
      (assoc-in adventure.keypaths/adventure-stylist-gallery-image-urls [])))

(defmethod transitions/transition-state events/control-adventure-select-stylist
  [_ _ {:keys [stylist-id]} app-state]
  (assoc-in app-state adventure.keypaths/adventure-choices-selected-stylist-id stylist-id))

(defmethod effects/perform-effects events/navigate-adventure-stylist-results-pre-purchase
  [_ _ args _ app-state]
  #?(:cljs
     (let [{:keys [latitude longitude]} (get-in app-state (conj adventure.keypaths/adventure-choices :location))
           matched-stylists             (get-in app-state adventure.keypaths/adventure-matched-stylists)]
       (if (and latitude longitude)
         (do
           (when (empty? matched-stylists)
             (messages/handle-message events/api-fetch-stylists-within-radius-pre-purchase))
           (messages/handle-message events/adventure-stylist-search-results-displayed))
         (history/enqueue-redirect events/navigate-adventure-find-your-stylist)))))


(defmethod effects/perform-effects events/navigate-adventure-stylist-results-post-purchase
  [_ _ args _ app-state]
  #?(:cljs
     (let [matched-stylists (get-in app-state adventure.keypaths/adventure-matched-stylists)
           completed-order-number  (get-in app-state storefront.keypaths/completed-order-number)]
       (if completed-order-number
         (do
           (when (empty? matched-stylists)
             (messages/handle-message events/api-fetch-stylists-within-radius-post-purchase))
           (messages/handle-message events/adventure-stylist-search-results-post-purchase-displayed))
         (history/enqueue-redirect events/navigate-adventure-find-your-stylist)))))

(defmethod effects/perform-effects events/control-adventure-select-stylist-pre-purchase
  [_ _ {:keys [stylist-id card-index servicing-stylist]} _ app-state]
  #?(:cljs
     (let [{:keys [number token]} (get-in app-state storefront.keypaths/order)]
       (cookie-jar/save-adventure (get-in app-state storefront.keypaths/cookie)
                                  (get-in app-state adventure.keypaths/adventure))
       (api/assign-servicing-stylist stylist-id
                                     (get-in app-state storefront.keypaths/store-stylist-id)
                                     number
                                     token
                                     (fn [order]
                                       (messages/handle-message events/api-success-assign-servicing-stylist-pre-purchase
                                                                {:order             order
                                                                 :servicing-stylist servicing-stylist
                                                                 :card-index        card-index}))))))

(defmethod effects/perform-effects events/control-adventure-select-stylist-post-purchase
  [_ _ {:keys [stylist-id card-index servicing-stylist]} _ app-state]
  #?(:cljs
     (let [{:keys [number token]} (get-in app-state storefront.keypaths/completed-order)]
       (cookie-jar/save-adventure (get-in app-state storefront.keypaths/cookie)
                                  (get-in app-state adventure.keypaths/adventure))
       (api/assign-servicing-stylist stylist-id
                                     (get-in app-state storefront.keypaths/store-stylist-id)
                                     number
                                     token
                                     (fn [order]
                                       (messages/handle-message events/api-success-assign-servicing-stylist-post-purchase
                                                                {:order             order
                                                                 :servicing-stylist servicing-stylist
                                                                 :card-index        card-index}))))))

(defmethod trackings/perform-track events/api-success-assign-servicing-stylist
  [_ event {:keys [servicing-stylist order card-index]} app-state]
  #?(:cljs
     (stringer/track-event "stylist_selected"
                           {:stylist_id     (:stylist-id servicing-stylist)
                            :card_index     card-index
                            :current_step   (if (= event events/api-success-assign-servicing-stylist-post-purchase)
                                              3
                                              2)
                            :order_number   (:number order)
                            :stylist_rating (:rating servicing-stylist)})))

(defmethod trackings/perform-track events/adventure-stylist-search-results-displayed
  [_ event args app-state]
  #?(:cljs
     (let [{:keys [latitude longitude]} (get-in app-state adventure.keypaths/adventure-stylist-match-location)
           location-submitted           (get-in app-state adventure.keypaths/adventure-stylist-match-address)
           results                      (map :stylist-id (get-in app-state adventure.keypaths/adventure-matched-stylists))]
       (stringer/track-event "stylist_search_results_displayed"
                             {:results            results
                              :latitude           latitude
                              :longitude          longitude
                              :location_submitted location-submitted
                              :radius             "100mi"
                              :current_step       2}))))

(defmethod trackings/perform-track events/adventure-stylist-search-results-post-purchase-displayed
  [_ event args app-state]
  #?(:cljs
     (let [{:keys [latitude longitude radius]} (get-in app-state adventure.keypaths/adventure-stylist-match-location)
           results                             (map :stylist-id (get-in app-state adventure.keypaths/adventure-matched-stylists))]
       (stringer/track-event "stylist_search_results_displayed"
                             {:results            results
                              :latitude           latitude
                              :longitude          longitude
                              :radius             radius
                              :current_step       3}))))
