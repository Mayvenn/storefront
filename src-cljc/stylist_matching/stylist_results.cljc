(ns stylist-matching.stylist-results
  (:require #?@(:cljs [[storefront.api :as api]
                       [storefront.browser.cookie-jar :as cookie-jar]
                       [storefront.history :as history]
                       [storefront.hooks.facebook-analytics :as facebook-analytics]
                       [storefront.hooks.stringer :as stringer]
                       [storefront.frontend-trackings :as frontend-trackings]
                       [storefront.accessors.orders :as orders]
                       [storefront.platform.messages :as messages]])
            [adventure.components.wait-spinner :as wait-spinner]
            adventure.keypaths
            api.orders
            [clojure.string :as string]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.stylists :as stylists]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.header :as components.header]
            [storefront.components.svg :as svg]
            [storefront.events :as events]
            [stylist-matching.ui.header :as header]
            [stylist-matching.ui.stylist-cards :as stylist-cards]
            [stylist-matching.ui.gallery-modal :as gallery-modal]
            [adventure.organisms.call-out-center :as call-out-center]
            [storefront.keypaths :as storefront.keypaths]
            [storefront.components.ui :as ui]
            [spice.core :as spice]
            [storefront.effects :as effects]
            [storefront.trackings :as trackings]
            [storefront.transitions :as transitions]
            adventure.keypaths))

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

(defmethod transitions/transition-state events/navigate-adventure-stylist-results-pre-purchase
  [_ _ args app-state]
  (let [{:keys [lat long]} (:query-params args)]
    (cond-> app-state
      (and lat long)
      (assoc-in adventure.keypaths/adventure-stylist-match-location {:latitude  (spice/parse-double lat)
                                                                     :longitude (spice/parse-double long)})

      (nil? (get-in app-state adventure.keypaths/adventure-matched-stylists))
      (assoc-in adventure.keypaths/adventure-stylist-results-delaying? true))))

(defmethod effects/perform-effects events/navigate-adventure-stylist-results-pre-purchase
  [_ _ {:keys [query-params]} _ app-state]
  #?(:cljs
     (let [{stylist-ids :s}                          query-params
           {:keys [latitude longitude] :as location} (get-in app-state adventure.keypaths/adventure-stylist-match-location)
           matched-stylists                          (get-in app-state adventure.keypaths/adventure-matched-stylists)
           api-cache                                 (get-in app-state storefront.keypaths/api-cache)]
       (cond
         (seq stylist-ids)
         (do
           (messages/handle-later events/adventure-stylist-results-delay-completed {} 3000)
           (api/fetch-matched-stylists api-cache
                                       stylist-ids
                                       #(messages/handle-message events/api-success-fetch-matched-stylists %)))

         (and location (nil? matched-stylists))
         (let [query {:latitude  latitude
                      :longitude longitude
                      :radius    "100mi"
                      :choices   (get-in app-state adventure.keypaths/adventure-choices)}] ; For trackings purposes only
           (messages/handle-later events/adventure-stylist-results-delay-completed {} 3000)
           (api/fetch-stylists-within-radius api-cache
                                             query
                                             #(messages/handle-message events/api-success-fetch-stylists-within-radius-pre-purchase
                                                                       (merge {:query query} %))))

         (seq matched-stylists)
         nil

         :else
         (history/enqueue-redirect events/navigate-adventure-find-your-stylist)))))

(defmethod transitions/transition-state events/adventure-stylist-results-delay-completed
  [_ _ _ app-state]
  (-> app-state
      (update-in adventure.keypaths/adventure dissoc :stylist-results-delaying?)))

(defmethod effects/perform-effects events/adventure-stylist-results-delay-completed
  [_ _ args _ app-state]
  #?(:cljs
     (when-not (nil? (get-in app-state adventure.keypaths/adventure-matched-stylists))
       (messages/handle-message events/adventure-stylist-results-wait-resolved {}))))

(defmethod effects/perform-effects events/adventure-stylist-results-wait-resolved
  [_ _ args _ app-state]
  #?@(:cljs
      [(messages/handle-message events/adventure-stylist-search-results-displayed)
       (when (empty? (get-in app-state adventure.keypaths/adventure-matched-stylists))
         (effects/redirect events/navigate-adventure-out-of-area))]))

(defmethod effects/perform-effects events/navigate-adventure-stylist-results-post-purchase
  [_ _ args _ app-state]
  #?(:cljs
     (let [matched-stylists (get-in app-state adventure.keypaths/adventure-matched-stylists)]
       (if (orders/service-line-item-promotion-applied? (get-in app-state storefront.keypaths/completed-order))
         (if (empty? matched-stylists)
           (messages/handle-message events/api-shipping-address-geo-lookup)
           (messages/handle-message events/adventure-stylist-search-results-post-purchase-displayed))
         (history/enqueue-redirect events/navigate-home)))))

(defmethod effects/perform-effects events/control-adventure-select-stylist-pre-purchase
  [_ _ {:keys [card-index servicing-stylist]} _ app-state]
  #?(:cljs
     (let [{:keys [number token]} (get-in app-state storefront.keypaths/order)]
       (cookie-jar/save-adventure (get-in app-state storefront.keypaths/cookie)
                                  (get-in app-state adventure.keypaths/adventure))
       (api/assign-servicing-stylist (:stylist-id servicing-stylist)
                                     (get-in app-state storefront.keypaths/store-stylist-id)
                                     number
                                     token
                                     (fn [order]
                                       (messages/handle-message events/api-success-assign-servicing-stylist-pre-purchase
                                                                {:order             order
                                                                 :servicing-stylist servicing-stylist
                                                                 :card-index        card-index}))))))

(defmethod effects/perform-effects events/control-adventure-select-stylist-post-purchase
  [_ _ {:keys [card-index servicing-stylist]} _ app-state]
  #?(:cljs
     (let [{:keys [number token]} (get-in app-state storefront.keypaths/completed-order)]
       (cookie-jar/save-adventure (get-in app-state storefront.keypaths/cookie)
                                  (get-in app-state adventure.keypaths/adventure))
       (api/assign-servicing-stylist (:stylist-id servicing-stylist)
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
  #?@(:cljs
      [(facebook-analytics/track-event "AddToCart" {:content_type "stylist"
                                                    :content_ids  [(:stylist-id servicing-stylist)]
                                                    :num_items    1})
       (stringer/track-event "stylist_selected"
                             {:stylist_id     (:stylist-id servicing-stylist)
                              :card_index     card-index
                              :current_step   (if (= event events/api-success-assign-servicing-stylist-post-purchase)
                                                3
                                                2)
                              :order_number   (:number order)
                              :stylist_rating (:rating servicing-stylist)})
       (frontend-trackings/track-pseudo-add-default-base-service-to-bag app-state)]))

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

(defn header-query
  [{:order.items/keys [quantity]}
   back post-purchase?]
  (cond-> {:header.title/id               "adventure-title"
           :header.title/primary          "Meet Your Stylist"
           :header.back-navigation/id     "adventure-back"
           :header.back-navigation/back   back
           :header.back-navigation/target [events/navigate-adventure-find-your-stylist]}
    (not post-purchase?)
    (merge {:header.cart/id    "mobile-cart"
            :header.cart/value quantity
            :header.cart/color "white"})))

(defn stylist-card-query
  [post-purchase? idx stylist]
  (let [{:keys [rating-star-counts
                salon
                service-menu
                gallery-images
                store-slug
                store-nickname
                stylist-id
                rating
                booking-count]}               stylist
        rating-count                          (->> rating-star-counts vals (reduce +))
        {salon-name :name
         :keys      [address-1
                     address-2
                     city
                     state
                     zipcode]}                salon
        {:keys [specialty-sew-in-leave-out
                specialty-sew-in-closure
                specialty-sew-in-360-frontal
                specialty-sew-in-frontal
                specialty-wig-customization]} service-menu
        cta-event                             (if post-purchase?
                                                events/control-adventure-select-stylist-post-purchase
                                                events/control-adventure-select-stylist-pre-purchase)]
    (cond->
     {:react/key                       (str "stylist-card-" store-slug)
      :stylist-card.header/target      (if post-purchase?
                                         [events/navigate-adventure-stylist-profile-post-purchase {:stylist-id stylist-id
                                                                                                   :store-slug store-slug}]
                                         [events/navigate-adventure-stylist-profile {:stylist-id stylist-id
                                                                                     :store-slug store-slug}])
      :stylist-card.header/id          (str "stylist-card-header-" store-slug)
      :stylist-card.thumbnail/id       (str "stylist-card-thumbnail-" store-slug)
      :stylist-card.thumbnail/ucare-id (-> stylist :portrait :resizable-url)

      :stylist-card.title/id            "stylist-name"
      :stylist-card.title/primary       (stylists/->display-name stylist)
      :rating/value                     rating
      :booking/count                    booking-count
      :stylist-card.services-list/id    (str "stylist-card-services-" store-slug)
      :stylist-card.services-list/items [{:id    (str "stylist-service-leave-out-" store-slug)
                                          :label "Leave Out"
                                          :value (boolean specialty-sew-in-leave-out)}
                                         {:id    (str "stylist-service-closure-" store-slug)
                                          :label "Closure"
                                          :value (boolean specialty-sew-in-closure)}
                                         {:id    (str "stylist-service-frontal-" store-slug)
                                          :label "Frontal"
                                          :value (boolean specialty-sew-in-frontal)}
                                         {:id    (str "stylist-service-360-" store-slug)
                                          :label "360° Frontal"
                                          :value (boolean specialty-sew-in-360-frontal)}
                                         {:id    (str "stylist-service-wig-customization-" store-slug)
                                          :label "Wig Customization"
                                          :value (boolean specialty-wig-customization)}]
      :stylist-card.cta/id              (str "select-stylist-" store-slug)
      :stylist-card.cta/label           (str "Select " store-nickname)
      :stylist-card.cta/target          [cta-event
                                         {:servicing-stylist stylist
                                          :card-index        idx}]

      :stylist-card.gallery/id           (str "stylist-card-gallery-" store-slug)
      :element/type                      :stylist-card
      :stylist-card.gallery/items        (let [ucare-img-urls (map :resizable-url gallery-images)]
                                           (map-indexed
                                            (fn [j ucare-img-url]
                                              {:stylist-card.gallery-item/id       (str "gallery-img-" stylist-id "-" j)
                                               :stylist-card.gallery-item/target   [events/navigate-adventure-stylist-gallery
                                                                                    {:store-slug   store-slug
                                                                                     :stylist-id   stylist-id
                                                                                     :query-params {:offset j}}]
                                               :stylist-card.gallery-item/ucare-id ucare-img-url})
                                            ucare-img-urls))
      :stylist-card.salon-name/id        salon-name
      :stylist-card.salon-name/value     salon-name
      :stylist-card.address-marker/id    (str "stylist-card-address-" store-slug)
      :stylist-card.address-marker/value (string/join " "
                                                      [(string/join ", "
                                                                    (remove string/blank? [address-1 address-2 city state]))
                                                       zipcode])}

      (and (:mayvenn-rating-publishable stylist)
           (> rating-count 0))
      (merge {:ratings/rating-count rating-count}))))

(defn stylist-cards-query
  [post-purchase? stylists]
  (map-indexed (partial stylist-card-query post-purchase?) stylists))

(def call-out-query
  {:call-out-center/bg-class    "bg-cool-gray"
   :call-out-center/title       "Want to book with your own stylist?"
   :call-out-center/subtitle    "Recommend them to become Mayvenn Certified"
   :cta/id                      "recommend-stylist"
   :cta/target                  [events/external-redirect-typeform-recommend-stylist]
   :cta/label                   "Submit Your Stylist"
   :element/type                :call-out
   :react/key                   :recommend-stylist})

(defn ^:private insert-at-pos
  "TODO this needs to be refined"
  [position i coll]
  (let [[h & r] (partition-all position coll)]
    (flatten (into [h] (concat [i] r)))))

(defn ^:private display-list
  "TODO this needs to be refined"
  [dispatches items & fall-back]
  (for [item  items
        :let  [component (get dispatches (:element/type item) fall-back)]
        :when component]
    [:div {:key (:react/key item)}
     (ui/screen-aware component item (component/component-id (:react/key item)))]))

(defn gallery-modal-query
  [app-state]
  (let [gallery-images (get-in app-state adventure.keypaths/adventure-stylist-gallery-image-urls)
        index          (get-in app-state adventure.keypaths/adventure-stylist-gallery-image-index)]
    {:gallery-modal/target           [events/control-adventure-stylist-gallery-close]
     :gallery-modal/ucare-image-urls gallery-images
     :gallery-modal/initial-index    index}))

(defcomponent location-input-field-molecule
  [{:stylist.results.location-search-box/keys
    [id value errors keypath]} _ _]
  (when id
    [:div.px3.pt2.bg-white.border-bottom.border-gray
     (ui/input-with-charm
      {:errors        errors
       :value         value
       :keypath       keypath
       :data-test     id
       :id            id
       :wrapper-class "flex items-center col-12 bg-white border-black"
       :type          "text"}
      [:div.flex.items-center.px2.border.border-black
       {:style {:border-left "none"}}
       ^:inline (svg/magnifying-glass {:width  "19px"
                                       :height "19px"
                                       :class  "fill-gray"})])]))

(defcomponent template
  [{:keys [gallery-modal header list/results location-search-box]} _ _]
  [:div.bg-cool-gray.black.center.flex.flex-auto.flex-column
   (component/build gallery-modal/organism gallery-modal nil)
   (components.header/adventure-header (:header.back-navigation/target header)
                                       (:header.title/primary header)
                                       {:quantity (:header.cart/value header)})
   (component/build location-input-field-molecule location-search-box nil)

   (display-list {:call-out     call-out-center/organism
                  :stylist-card stylist-cards/organism}
                 results)])

(def post-purchase? #{events/navigate-adventure-stylist-results-post-purchase})

(defn page
  [app-state]
  (let [current-order          (api.orders/current app-state)
        stylist-search-results (get-in app-state adventure.keypaths/adventure-matched-stylists)
        nav-event              (get-in app-state storefront.keypaths/navigation-event)
        post-purchase?         (post-purchase? nav-event)
        spinning?              (or (get-in app-state adventure.keypaths/adventure-stylist-results-delaying?)
                                   (empty? stylist-search-results))]
    (if spinning?
      (component/build wait-spinner/component app-state)
      (component/build template
                       {:gallery-modal       (gallery-modal-query app-state)
                        :location-search-box (when (experiments/stylist-filters? app-state)
                                               {:stylist.results.location-search-box/id      "stylist-search-input"
                                                :stylist.results.location-search-box/value   (get-in app-state adventure.keypaths/adventure-stylist-match-address)
                                                :stylist.results.location-search-box/keypath adventure.keypaths/adventure-stylist-match-address
                                                :stylist.results.location-search-box/errors  []})
                        :header              (header-query current-order
                                                           (first (get-in app-state storefront.keypaths/navigation-undo-stack))
                                                           post-purchase?)
                        :list/results        (insert-at-pos 3
                                                            call-out-query
                                                            (stylist-cards-query post-purchase?
                                                                                 stylist-search-results))}))))
