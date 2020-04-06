(ns stylist-matching.stylist-results
  (:require #?@(:cljs [[storefront.api :as api]
                       [storefront.browser.cookie-jar :as cookie-jar]
                       [storefront.history :as history]
                       [storefront.hooks.facebook-analytics :as facebook-analytics]
                       [storefront.hooks.google-maps :as google-maps]
                       [storefront.hooks.stringer :as stringer]
                       [storefront.frontend-trackings :as frontend-trackings]
                       [stylist-matching.search.filters-modal :as filters-modal]
                       [storefront.accessors.orders :as orders]])
            adventure.keypaths
            api.orders
            [clojure.string :as string]
            [storefront.accessors.categories :as accessors.categories]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.stylists :as stylists]
            [storefront.component :as component :refer [defdynamic-component defcomponent]]
            [storefront.components.header :as components.header]
            [storefront.components.svg :as svg]
            [storefront.events :as events]
            [stylist-matching.ui.header :as header]
            [stylist-matching.ui.shopping-method-choice :as shopping-method-choice]
            [stylist-matching.ui.stylist-cards :as stylist-cards]
            [stylist-matching.ui.gallery-modal :as gallery-modal]
            stylist-directory.keypaths
            [adventure.organisms.call-out-center :as call-out-center]
            storefront.keypaths
            [storefront.components.ui :as ui]
            [spice.core :as spice]
            [storefront.effects :as effects]
            [storefront.trackings :as trackings]
            [storefront.transitions :as transitions]
            [storefront.platform.messages :as messages]
            [adventure.keypaths :as adventure.keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [clojure.string :as str]))

(def preferred-filters?
  #{:leave-out :360-frontal :closure :frontal :wig-customization})

(defn sanitize-preferred-filters
  [query-preferred-services]
  (filter preferred-filters? query-preferred-services))

(defmethod transitions/transition-state events/navigate-adventure-stylist-results-pre-purchase
  [_ _ {:keys [query-params]} app-state]
  (let [{:keys [lat long preferred-services]} query-params]
    (cond-> app-state
      (seq preferred-services)
      (assoc-in stylist-directory.keypaths/stylist-search-selected-filters
                (set (sanitize-preferred-filters (map keyword (string/split preferred-services "~")))))

      (and lat long)
      (-> (assoc-in adventure.keypaths/adventure-stylist-match-location ;; GROT
                    {:latitude  (spice/parse-double lat)
                     :longitude (spice/parse-double long)})
          (assoc-in stylist-directory.keypaths/stylist-search-selected-location
                    {:latitude  (spice/parse-double lat)
                     :longitude (spice/parse-double long)
                     :radius    "100mi"}))

      (nil? (get-in app-state stylist-directory.keypaths/stylist-search-address-input))
      (assoc-in stylist-directory.keypaths/stylist-search-address-input
                (get-in app-state adventure.keypaths/adventure-stylist-match-address)))))

(defmethod effects/perform-effects events/navigate-adventure-stylist-results
  [_ _ {:keys [query-params]} prev-app-state app-state]
  #?(:cljs
     (google-maps/insert)))

;;  Navigating to the results page causes the effect of searching for stylists
;;
;;  This allows:
;;  - Cold loads of the page, both with lat+long and stylist ids
;;  - Refining of search results
;;
(defmethod effects/perform-effects events/navigate-adventure-stylist-results-pre-purchase
  [_ _ {:keys [query-params]} prev-app-state app-state]
  #?(:cljs
     ;; TODO consider merging with post-purchase
     (let [{stylist-ids :s}                          query-params
           {:keys [latitude longitude] :as location} (get-in app-state stylist-directory.keypaths/stylist-search-selected-location)
           matched-stylists                          (get-in app-state adventure.keypaths/adventure-matched-stylists)
           preferred-services                        (get-in app-state stylist-directory.keypaths/stylist-search-selected-filters)]
       (cond
         ;; Predetermined search results
         (seq stylist-ids)
         (api/fetch-matched-stylists (get-in app-state storefront.keypaths/api-cache)
                                     stylist-ids
                                     #(messages/handle-message events/api-success-fetch-matched-stylists
                                                               %))
         ;; Querying based upon geo location
         location
         (let [query {:latitude           latitude
                      :longitude          longitude
                      :radius             "100mi"
                      :choices            (get-in app-state adventure.keypaths/adventure-choices) ; For trackings purposes only
                      :preferred-services preferred-services}
               api-fetch-stylists (if (experiments/stylist-results-sort? app-state)
                                    api/fetch-stylists-matching-filters-v2
                                    api/fetch-stylists-matching-filters)]
           (api-fetch-stylists query
                               #(messages/handle-message events/api-success-fetch-stylists-matching-filters
                                                         (merge {:query query} %))))
         ;; Previously performed search results
         ;; TODO consider removal (uri holds state and effects)
         (seq matched-stylists)
         nil

         ;; TODO consider removal since the result page has a search bar
         :else
         (history/enqueue-redirect events/navigate-adventure-find-your-stylist)))))

(defmethod transitions/transition-state events/navigate-adventure-stylist-results-post-purchase
  [_ _ args app-state]
  (let [{:keys [address1 address2 city state zipcode]} (get-in app-state storefront.keypaths/completed-order-shipping-address)]
    (when (nil? (get-in app-state stylist-directory.keypaths/stylist-search-address-input))
      (assoc-in app-state stylist-directory.keypaths/stylist-search-address-input
                (string/join " " [address1 address2 (str city ",") state zipcode])))))

(defmethod effects/perform-effects events/navigate-adventure-stylist-results-post-purchase
  [_ _ args _ app-state]
  #?(:cljs
     (let [matched-stylists (get-in app-state adventure.keypaths/adventure-matched-stylists)]
       (if (orders/service-line-item-promotion-applied? (get-in app-state storefront.keypaths/completed-order))
         (if (empty? matched-stylists)
           (messages/handle-message events/api-shipping-address-geo-lookup)
           (messages/handle-message events/adventure-stylist-search-results-post-purchase-displayed))
         (history/enqueue-redirect events/navigate-home)))))

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

#?(:cljs
   (defmethod effects/perform-effects events/stylist-results-address-component-mounted
     [_ event args _ app-state]
     (google-maps/attach "geocode"
                         "stylist-search-input"
                         stylist-directory.keypaths/stylist-search-selected-location
                         #(messages/handle-message events/stylist-results-address-selected))))

#?(:cljs
   (defmethod transitions/transition-state events/stylist-results-address-selected
     [_ _ _ app-state]
     (-> app-state
         (assoc-in stylist-directory.keypaths/stylist-search-address-input
                   (.-value (.getElementById js/document "stylist-search-input"))))))

#?(:cljs
   (defmethod trackings/perform-track events/stylist-results-address-selected
     [_ event _ app-state]
     (let [{:keys [latitude longitude]} (get-in app-state stylist-directory.keypaths/stylist-search-selected-location)]
       (stringer/track-event "adventure_location_submitted"
                             {:location_submitted (get-in app-state stylist-directory.keypaths/stylist-search-address-input)
                              :latitude           latitude
                              :longitude          longitude})) ))

#?(:cljs
   (defmethod effects/perform-effects events/stylist-results-address-selected
     [_ event args _ app-state]
     (let [[nav-event nav-args]                      (get-in app-state storefront.keypaths/navigation-message) ; pre- or post- purchase
           {:keys [latitude longitude] :as location} (get-in app-state stylist-directory.keypaths/stylist-search-selected-location)
           service-filters                           (get-in app-state stylist-directory.keypaths/stylist-search-selected-filters)]
       (history/enqueue-redirect nav-event
                                 {:query-params
                                  (merge (:query-params nav-args)
                                         {:lat                latitude
                                          :long               longitude
                                          :preferred-services (clojure.string/join "~" (map name service-filters))})}))))


(defmethod transitions/transition-state events/api-success-fetch-stylists-matching-filters
  [_ event {:keys [stylists query]} app-state]
  #?(:cljs
     (cond-> (assoc-in app-state adventure.keypaths/adventure-matched-stylists stylists)

         (seq query)
         (assoc-in stylist-directory.keypaths/stylist-search-selected-location
                   {:latitude  (:latitude query)
                    :longitude (:longitude query)
                    :radius    (:radius query)}))))

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
     (when-not (get-in app-state stylist-directory.keypaths/user-toggled-preference)
       (let [{:keys [latitude longitude radius]} (get-in app-state stylist-directory.keypaths/stylist-search-selected-location)
             location-submitted                  (get-in app-state adventure.keypaths/adventure-stylist-match-address)
             results                             (map :stylist-id (get-in app-state adventure.keypaths/adventure-matched-stylists))]
         (stringer/track-event "stylist_search_results_displayed"
                               {:results            results
                                :latitude           latitude
                                :longitude          longitude
                                :location_submitted location-submitted
                                :radius             radius
                                :current_step       2})))))

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
  [post-purchase? hide-stylist-specialty? idx stylist]
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
        {:react/key                           (str "stylist-card-" store-slug)
         :stylist-card.header/hide-stylist-specialty? hide-stylist-specialty?
         :stylist-card.header/target          (if post-purchase?
                                                [events/navigate-adventure-stylist-profile-post-purchase {:stylist-id stylist-id
                                                                                                          :store-slug store-slug}]
                                                [events/navigate-adventure-stylist-profile {:stylist-id stylist-id
                                                                                            :store-slug store-slug}])
         :stylist-card.header/id              (str "stylist-card-header-" store-slug)
         :stylist-card.thumbnail/id           (str "stylist-card-thumbnail-" store-slug)
         :stylist-card.thumbnail/ucare-id     (-> stylist :portrait :resizable-url)

         :stylist-card.title/id            "stylist-name"
         :stylist-card.title/primary       (stylists/->display-name stylist)
         :rating/value                     rating
         :booking/count                    booking-count
         :stylist-card.services-list/id    (str "stylist-card-services-" store-slug)
         :stylist-card.services-list/items [{:id         (str "stylist-service-leave-out-" store-slug)
                                             :label      "Leave Out"
                                             :value      (boolean specialty-sew-in-leave-out)
                                             :preference :leave-out}
                                            {:id         (str "stylist-service-closure-" store-slug)
                                             :label      "Closure"
                                             :value      (boolean specialty-sew-in-closure)
                                             :preference :closure}
                                            {:id         (str "stylist-service-frontal-" store-slug)
                                             :label      "Frontal"
                                             :value      (boolean specialty-sew-in-frontal)
                                             :preference :frontal}
                                            {:id         (str "stylist-service-360-" store-slug)
                                             :label      "360° Frontal"
                                             :value      (boolean specialty-sew-in-360-frontal)
                                             :preference :360-frontal}
                                            {:id         (str "stylist-service-wig-customization-" store-slug)
                                             :label      "Wig Customization"
                                             :value      (boolean specialty-wig-customization)
                                             :preference :wig-customization}]
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
  [post-purchase? hide-stylist-specialty? stylists]
  (map-indexed (partial stylist-card-query post-purchase? hide-stylist-specialty?) stylists))

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

(def preference->pill-button-copy
  {:leave-out "Leave out Install"
   :closure "Closure Install"
   :frontal "Frontal Install"
   :360-frontal "360 Frontal Install"
   :wig-customization "Wig Customization"})

;; TODO this name and query and such
(defdynamic-component location-input-and-filters-molecule
  (did-mount [_]
             (messages/handle-message events/stylist-results-address-component-mounted))
  (render [this]
          (let [{:stylist.results.location-search-box/keys
                 [id value errors keypath preferences]} (component/get-props this)
                preference-count                        (count preferences)]
            (component/html
             [:div.px3.py2.bg-white.border-bottom.border-gray.flex.flex-column
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
                                                :class  "fill-gray"})])
              [:div.flex.flex-wrap
               [:div
                (ui/button-pill {:class     "p1 mr1"
                                 :key       "filters-key"
                                 :data-test "button-show-stylist-search-filters"
                                 :on-click  (utils/send-event-callback events/control-show-stylist-search-filters)}
                                [:div.flex.items-center.px3
                                 (svg/funnel {:class  "mrp3"
                                              :height "9px"
                                              :width  "10px"})
                                 (if (= 0 preference-count)
                                   "Filters"
                                   (str "- " preference-count))])]
               (for [preference-key preferences]
                 [:div.pb1
                  {:key preference-key}
                  (ui/button-pill {:class     "p1 mr1"
                                   :on-click  identity}
                                  [:div.flex.pl1
                                   (preference-key preference->pill-button-copy)
                                   [:div.flex.items-center.pl1
                                    ^:attrs (merge {:data-test (str "remove-preference-button-" (name preference-key))}
                                                   (apply utils/fake-href [events/control-stylist-search-toggle-filter
                                                                           {:previously-checked?      true
                                                                            :stylist-filter-selection preference-key}]))
                                    (svg/close-x {:class  "stroke-white fill-gray"
                                                  :width  "13px"
                                                  :height "13px"})]])])]]))))

(defcomponent matching-count-organism
  [{:keys [stylist-count-content]} _ _]
  [:div.content-3.mt2.mb1.left.mx3
   stylist-count-content])

(defcomponent non-matching-breaker
  [{:keys [breaker-content]} _ _]
  [:div.my5.content-2
   breaker-content])

(defcomponent template
  [{:keys [popup spinning? filters-modal gallery-modal header list/results location-search-box shopping-method-choice]} _ _]
  [:div.bg-cool-gray.black.center.flex.flex-auto.flex-column
  #?(:cljs (component/build filters-modal/component filters-modal nil))

   (component/build gallery-modal/organism gallery-modal nil)
   (components.header/adventure-header (:header.back-navigation/target header)
                                       (:header.title/primary header)
                                       {:quantity (:header.cart/value header)})

   (when (:stylist.results.location-search-box/id location-search-box)
     (component/build location-input-and-filters-molecule location-search-box nil))

   (cond
     spinning?
     [:div.mt6 ui/spinner]

     results
     (display-list {:call-out               call-out-center/organism
                    :stylist-card           stylist-cards/organism
                    :matching-stylist-count matching-count-organism
                    :non-matching-breaker   non-matching-breaker}
                   results)

     :else
     (component/build shopping-method-choice/organism shopping-method-choice nil))])

(defn stylist-results-arranged
  [partition-results]
  (let [{matching true non-matching false} partition-results
        count-matching          (count matching)]
    [[{:stylist-count-content (str count-matching " Stylists Found")
       :react/key             "stylist-count-content"
       :element/type          :matching-stylist-count}]
     (insert-at-pos 3 call-out-query matching)
     (when (seq non-matching)
       [{:breaker-content "Other stylists in your area"
         :react/key       "non-matching-breaker"
         :element/type    :non-matching-breaker}])
     non-matching]))

(defn shopping-method-choice-query [hide-bundle-sets?]
  {:shopping-method-choice.error-title/id        "stylist-matching-shopping-method-choice"
   :shopping-method-choice.error-title/primary   "We need some time to find you the perfect stylist!"
   :shopping-method-choice.error-title/secondary (str
                                                  "A Mayvenn representative will contact you soon "
                                                  "to help select a Certified Mayvenn Stylist. In the meantime…")
   :list/buttons                                 (cond-> [{:shopping-method-choice.button/id       "button-looks"
                                                           :shopping-method-choice.button/label    "Shop by look"
                                                           :shopping-method-choice.button/target   [events/navigate-shop-by-look
                                                                                                    {:album-keyword :look}]
                                                           :shopping-method-choice.button/ucare-id "a9009728-efd3-4917-9541-b4514b8e4776"}]
                                                   (not hide-bundle-sets?)
                                                   (concat [{:shopping-method-choice.button/id       "button-bundle-sets"
                                                             :shopping-method-choice.button/label    "Pre-made bundle sets"
                                                             :shopping-method-choice.button/target   [events/navigate-shop-by-look
                                                                                                      {:album-keyword :all-bundle-sets}]
                                                             :shopping-method-choice.button/ucare-id "87b46db7-4c70-4d3a-8fd0-6e99e78d3c96"}])
                                                   :always
                                                   (concat [{:shopping-method-choice.button/id       "button-a-la-carte"
                                                             :shopping-method-choice.button/label    "Choose individual bundles"
                                                             :shopping-method-choice.button/target   [events/navigate-category
                                                                                                      {:page/slug           "mayvenn-install"
                                                                                                       :catalog/category-id "23"}]
                                                             :shopping-method-choice.button/ucare-id "6c39cd72-6fde-4ec2-823c-5e39412a6d54"}
                                                            {:shopping-method-choice.button/id       "button-shop-wigs"
                                                             :shopping-method-choice.button/label    "Shop Virgin Wigs"
                                                             :shopping-method-choice.button/target   [events/navigate-category
                                                                                                      {:page/slug           "wigs"
                                                                                                       :catalog/category-id "13"
                                                                                                       :query-params
                                                                                                       {:family
                                                                                                        (str "lace-front-wigs"
                                                                                                             accessors.categories/query-param-separator
                                                                                                             "360-wigs")}}]
                                                             :shopping-method-choice.button/ucare-id "71dcdd17-f9cc-456f-b763-2c1c047c30b4"}]))})

(defn page
  [app-state]
  (let [current-order          (api.orders/current app-state)
        stylist-search-results (get-in app-state adventure.keypaths/adventure-matched-stylists)
        nav-event              (get-in app-state storefront.keypaths/navigation-event)
        post-purchase?         (#{events/navigate-adventure-stylist-results-post-purchase} nav-event)
        preferences            (get-in app-state stylist-directory.keypaths/stylist-search-selected-filters)
        matches-preferences?   (fn matches-preferences?
                                 [{:keys [stylist-card.services-list/items :stylist-card.title/primary]}]
                                 (every? :value
                                         (filter (comp preferences :preference) items)))]
    (component/build template
                     {:gallery-modal          (gallery-modal-query app-state)
                      :spinning?              (or (utils/requesting-from-endpoint? app-state request-keys/fetch-matched-stylists)
                                                  (utils/requesting-from-endpoint? app-state request-keys/fetch-stylists-within-radius)
                                                  (utils/requesting-from-endpoint? app-state request-keys/fetch-stylists-matching-filters))
                      :filters-modal          #?(:cljs (filters-modal/query app-state)
                                                 :clj  nil)
                      :location-search-box    (when (and (get-in app-state storefront.keypaths/loaded-google-maps)
                                                         (not post-purchase?))
                                                {:stylist.results.location-search-box/id          "stylist-search-input"
                                                 :stylist.results.location-search-box/value       (get-in app-state stylist-directory.keypaths/stylist-search-address-input)
                                                 :stylist.results.location-search-box/keypath     stylist-directory.keypaths/stylist-search-address-input
                                                 :stylist.results.location-search-box/errors      []
                                                 :stylist.results.location-search-box/preferences (vec (get-in app-state stylist-directory.keypaths/stylist-search-selected-filters))})
                      :header                 (header-query current-order
                                                            (first (get-in app-state storefront.keypaths/navigation-undo-stack))
                                                            post-purchase?)
                      :list/results           (when (seq stylist-search-results)
                                                (if (seq preferences)
                                                  (->> stylist-search-results
                                                       (stylist-cards-query post-purchase?
                                                                            (experiments/hide-stylist-specialty? app-state))
                                                       ;; Add Breaker
                                                       (group-by matches-preferences?)
                                                       stylist-results-arranged
                                                       (mapcat identity))
                                                  (->> stylist-search-results
                                                       (stylist-cards-query post-purchase? (experiments/hide-stylist-specialty? app-state))
                                                       (insert-at-pos 3 call-out-query))))
                      :shopping-method-choice (shopping-method-choice-query (experiments/hide-bundle-sets? app-state))})))
