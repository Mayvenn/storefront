(ns adventure.stylist-matching.stylist-results
  (:require [adventure.components.card-stack :as card-stack]
            [adventure.components.profile-card-with-gallery :as profile-card-with-gallery]
            [adventure.keypaths :as keypaths]
            [storefront.keypaths]
            [storefront.component :as component]
            [storefront.events :as events]
            [storefront.platform.messages :as messages]
            [storefront.effects :as effects]
            [storefront.transitions :as transitions]
            [storefront.trackings :as trackings]
            #?@(:cljs [[storefront.browser.cookie-jar :as cookie-jar]
                       [storefront.history :as history]
                       [storefront.hooks.stringer :as stringer]
                       [storefront.api :as api]])
            [adventure.progress :as progress]))

(defn ^:private query [data]
  (let [post-purchase? (get-in data storefront.keypaths/completed-order)
        current-step   (if-not post-purchase? 2 3)
        progress       (when-not post-purchase? progress/stylist-results)]
    (merge {:current-step                  current-step
            :title                         "Pick your stylist"
            :header-data                   {:title                   "Find Your Stylist"
                                            :progress                progress
                                            :back-navigation-message [events/navigate-adventure-how-far]
                                            :subtitle                (str "Step " current-step " of 3")}
            :gallery-modal-data            {:ucare-img-urls                 (get-in data keypaths/adventure-stylist-gallery-image-urls) ;; empty hides the modal
                                            :initially-selected-image-index (get-in data keypaths/adventure-stylist-gallery-image-index)
                                            :close-button                   {:target-message events/control-adventure-stylist-gallery-close}}
            :cards-data                    (map-indexed profile-card-with-gallery/stylist-profile-card-data (get-in data keypaths/adventure-matched-stylists))
            :escape-hatch/navigation-event events/navigate-adventure-shop-hair
            :escape-hatch/copy             "Shop hair"
            :escape-hatch/data-test        "shop-hair"}
           (when post-purchase?
             {:escape-hatch/navigation-event events/navigate-adventure-let-mayvenn-match
              :escape-hatch/copy             "Let Mayvenn Match"
              :escape-hatch/data-test        "let-mayvenn-match"}))))

(defn built-component
  [data opts]
  (component/build card-stack/component (query data) opts))

(defmethod transitions/transition-state events/control-adventure-stylist-gallery-open [_ _event {:keys [ucare-img-urls initially-selected-image-index]} app-state]
  (-> app-state
      (assoc-in keypaths/adventure-stylist-gallery-image-urls ucare-img-urls)
      (assoc-in keypaths/adventure-stylist-gallery-image-index initially-selected-image-index)))

(defmethod transitions/transition-state events/control-adventure-stylist-gallery-close [_ _event _args app-state]
  (-> app-state
      (assoc-in keypaths/adventure-stylist-gallery-image-urls [])))

(defmethod transitions/transition-state events/control-adventure-select-stylist
  [_ _ {:keys [stylist-id]} app-state]
  (assoc-in app-state keypaths/adventure-choices-selected-stylist-id stylist-id))

(defmethod effects/perform-effects events/navigate-adventure-stylist-results-pre-purchase
  [_ _ args _ app-state]
  #?(:cljs
     (let [matched-stylists (get-in app-state keypaths/adventure-matched-stylists)]
       (when (empty? matched-stylists) (messages/handle-message events/api-fetch-stylists-within-radius))
       (messages/handle-message events/adventure-stylist-search-results-displayed))))

(defmethod effects/perform-effects events/navigate-adventure-stylist-results-post-purchase
  [_ _ args _ app-state]
  #?(:cljs
     (let [matched-stylists (get-in app-state keypaths/adventure-matched-stylists)]
       (if (empty? matched-stylists)
         (history/enqueue-redirect events/navigate-adventure-matching-stylist-wait)
         (messages/handle-message events/adventure-stylist-search-results-post-purchase-displayed)))))

(defmethod effects/perform-effects events/control-adventure-select-stylist
  [_ _ {:keys [stylist-id card-index servicing-stylist]} _ app-state]
  #?(:cljs
     (let [servicing-stylist-id   stylist-id
           store-stylist-id       (get-in app-state storefront.keypaths/store-stylist-id)
           {:keys [number token]} (or (get-in app-state storefront.keypaths/order)
                                      (get-in app-state storefront.keypaths/completed-order))]
       (cookie-jar/save-adventure (get-in app-state storefront.keypaths/cookie)
                                  (get-in app-state keypaths/adventure))
       (api/assign-servicing-stylist servicing-stylist-id
                                     store-stylist-id
                                     number
                                     token
                                     (fn [order]
                                       (messages/handle-message events/api-success-assign-servicing-stylist
                                                                {:order             order
                                                                 :servicing-stylist servicing-stylist})
                                       (stringer/track-event "stylist_selected"
                                                             {:stylist_id     servicing-stylist-id
                                                              :card_index     card-index
                                                              :current_step   2
                                                              :service_type   (get-in app-state keypaths/adventure-choices-install-type)
                                                              :order_number   (:number order)
                                                              :stylist_rating (:rating servicing-stylist)}))))))

(defmethod trackings/perform-track events/adventure-stylist-search-results-displayed
  [_ event args app-state]
  #?(:cljs
     (let [{:keys [latitude longitude]} (get-in app-state keypaths/adventure-stylist-match-location)
           {:keys [how-far]}            (get-in app-state keypaths/adventure-choices)
           service-type                 (get-in app-state keypaths/adventure-choices-install-type)
           location-submitted           (get-in app-state keypaths/adventure-stylist-match-address)
           results                      (map :stylist-id (get-in app-state keypaths/adventure-matched-stylists))]
       (stringer/track-event "stylist_search_results_displayed"
                             {:results            results
                              :latitude           latitude
                              :longitude          longitude
                              :location_submitted location-submitted
                              :radius             how-far
                              :service_type       service-type
                              :current_step       2}))))

(defmethod trackings/perform-track events/adventure-stylist-search-results-post-purchase-displayed
  [_ event args app-state]
  #?(:cljs
     (let [{:keys [latitude longitude radius]} (get-in app-state keypaths/adventure-stylist-match-address)
           service-type                        (get-in app-state keypaths/adventure-choices-install-type)
           results                             (map :stylist-id (get-in app-state keypaths/adventure-matched-stylists))]
       (stringer/track-event "stylist_search_results_displayed"
                             {:results      results
                              :latitude     latitude
                              :longitude    longitude
                              :service_type service-type
                              :radius       radius
                              :current_step 3}))))
