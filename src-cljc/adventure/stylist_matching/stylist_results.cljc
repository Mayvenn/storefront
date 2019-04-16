(ns adventure.stylist-matching.stylist-results
  (:require [adventure.components.card-stack :as card-stack]
            [adventure.components.profile-card :as profile-card]
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


(defn ^:private stylist-profile-card-data [target-event index {:keys [gallery-images stylist-id] :as stylist}]
  (let [ucare-img-urls (map :resizable-url gallery-images)]
    {:card-data    (profile-card/stylist-profile-card-data stylist)
     :index        index
     :key          (str "stylist-card-" stylist-id)
     :gallery-data {:title "Recent Work"
                    :items (map-indexed (fn [j ucare-img-url]
                                          {:key            (str "gallery-img-" stylist-id "-" j)
                                           :ucare-img-url  ucare-img-url
                                           :target-message [events/control-adventure-stylist-gallery-open
                                                            {:ucare-img-urls                 ucare-img-urls
                                                             :initially-selected-image-index j}]})
                                        ucare-img-urls)}
     :button       {:text           "Select"
                    :data-test      "select-stylist"
                    :target-message [target-event {:stylist-id        stylist-id
                                                   :servicing-stylist stylist
                                                   :card-index        index}]}}))

(defn ^:private query-pre-purchase [data]
  {:current-step                  2
   :title                         "Pick your stylist"
   :header-data                   {:title                   "Find Your Stylist"
                                   :progress                progress/stylist-results
                                   :back-navigation-message [events/navigate-adventure-how-far]
                                   :subtitle                "Step 2 of 3"}
   :gallery-modal-data            {:ucare-img-urls                 (get-in data keypaths/adventure-stylist-gallery-image-urls) ;; empty hides the modal
                                   :initially-selected-image-index (get-in data keypaths/adventure-stylist-gallery-image-index)
                                   :close-button                   {:target-message events/control-adventure-stylist-gallery-close}}
   :cards-data                    (map-indexed (partial stylist-profile-card-data events/control-adventure-select-stylist-pre-purchase)
                                               (get-in data keypaths/adventure-matched-stylists))
   :escape-hatch/navigation-event events/navigate-adventure-shop-hair
   :escape-hatch/copy             "Shop hair"
   :escape-hatch/data-test        "shop-hair"})

(defn ^:private query-post-purchase [data]
  {:current-step                  3
   :title                         "Pick your stylist"
   :header-data                   {:title                   "Find Your Stylist"
                                   :back-navigation-message [events/navigate-adventure-how-far]
                                   :subtitle                "Step 3 of 3"}
   :gallery-modal-data            {:ucare-img-urls                 (get-in data keypaths/adventure-stylist-gallery-image-urls) ;; empty hides the modal
                                   :initially-selected-image-index (get-in data keypaths/adventure-stylist-gallery-image-index)
                                   :close-button                   {:target-message events/control-adventure-stylist-gallery-close}}
   :cards-data                    (map-indexed (partial stylist-profile-card-data events/control-adventure-select-stylist-post-purchase)
                                               (get-in data keypaths/adventure-matched-stylists))
   :escape-hatch/navigation-event events/navigate-adventure-let-mayvenn-match
   :escape-hatch/copy             "Let Mayvenn Match"
   :escape-hatch/data-test        "let-mayvenn-match"})

(defn built-component-pre-purchase
  [data opts]
  (component/build card-stack/component (query-pre-purchase data) opts))

(defn built-component-post-purchase
  [data opts]
  (component/build card-stack/component (query-post-purchase data) opts))

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
       (when (empty? matched-stylists) (messages/handle-message events/api-fetch-stylists-within-radius-pre-purchase))
       (messages/handle-message events/adventure-stylist-search-results-displayed))))

(defmethod effects/perform-effects events/navigate-adventure-stylist-results-post-purchase
  [_ _ args _ app-state]
  #?(:cljs
     (let [matched-stylists (get-in app-state keypaths/adventure-matched-stylists)]
       (when (empty? matched-stylists) (messages/handle-message events/api-fetch-stylists-within-radius-post-purchase))
       (messages/handle-message events/adventure-stylist-search-results-post-purchase-displayed))))

(defmethod effects/perform-effects events/control-adventure-select-stylist-pre-purchase
  [_ _ {:keys [stylist-id card-index servicing-stylist]} _ app-state]
  #?(:cljs
     (let [{:keys [number token]} (get-in app-state storefront.keypaths/order)]
       (cookie-jar/save-adventure (get-in app-state storefront.keypaths/cookie)
                                  (get-in app-state keypaths/adventure))
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
                                  (get-in app-state keypaths/adventure))
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
                            :service_type   (get-in app-state keypaths/adventure-choices-install-type)
                            :order_number   (:number order)
                            :stylist_rating (:rating servicing-stylist)})))

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
     (let [{:keys [latitude longitude radius]} (get-in app-state keypaths/adventure-stylist-match-location)
           service-type                        (get-in app-state keypaths/adventure-choices-install-type)
           results                             (map :stylist-id (get-in app-state keypaths/adventure-matched-stylists))]
       (stringer/track-event "stylist_search_results_displayed"
                             {:results            results
                              :latitude           latitude
                              :longitude          longitude
                              :service_type       service-type
                              :radius             radius
                              :current_step       3}))))
