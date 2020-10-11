(ns stylist-matching.stylist-results
  (:require #?@(:cljs [[storefront.hooks.google-maps :as google-maps]
                       [storefront.hooks.stringer :as stringer]
                       [stylist-matching.search.filters-modal :as filter-menu]])
            adventure.keypaths
            [stylist-matching.core :refer [stylist-matching<- service-delimiter]]
            [stylist-matching.keypaths :as k]
            api.orders
            [clojure.string :refer [split join blank?]]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.stylists :as stylists]
            [storefront.accessors.categories :as categories]
            [storefront.component :as component :refer [defdynamic-component defcomponent]]
            [storefront.components.header :as components.header]
            [storefront.components.svg :as svg]
            [storefront.events :as e]
            [stylist-matching.ui.shopping-method-choice :as shopping-method-choice]
            [stylist-matching.ui.stylist-cards :as stylist-cards]
            [stylist-matching.ui.gallery-modal :as gallery-modal]
            [stylist-matching.search.accessors.filters :as accessors.filters]
            storefront.keypaths
            [storefront.components.ui :as ui]
            [spice.core :as spice]
            [storefront.effects :as effects]
            [storefront.trackings :as trackings]
            [storefront.transitions :as transitions]
            [storefront.platform.messages :as messages]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [spice.date :as date]))

;;  Navigating to the results page causes the effect of searching for stylists
;;
;;  This allows:
;;  - Cold loads of the page, both with lat+long and stylist ids
;;  - Refining of search results
;;
(defmethod transitions/transition-state e/navigate-adventure-stylist-results
  [_ _ _ state]
  ;; Prefill address input with previously collected address
  (let [previous-address (not-empty (:param/address (stylist-matching<- state)))]
    (cond-> state
      previous-address
      (assoc-in k/google-input previous-address))))

(defmethod effects/perform-effects e/navigate-adventure-stylist-results
  [_ _ {{:keys [preferred-services lat long s]} :query-params} prev-state state]
  ;; Init the model if there isn't one, e.g. Direct load
  (when-not (stylist-matching<- state)
    (messages/handle-message e/flow|stylist-matching|initialized))
  ;; Pull stylist-ids (s) from URI; predetermined search results
  (when (seq s)
    (messages/handle-message e/flow|stylist-matching|param-ids-constrained {:ids s}))
  ;; Pull preferred services from URI; filters for service types
  (when-let [services (some-> preferred-services not-empty (split service-delimiter) set)]
    (messages/handle-message e/flow|stylist-matching|param-services-constrained
                             {:services services}))
  ;; Pull lat/long from URI; search by proximity
  (when (and (not-empty lat)
             (not-empty long))
    (messages/handle-message e/flow|stylist-matching|param-location-constrained
                             {:latitude  (spice/parse-double lat)
                              :longitude (spice/parse-double long)}))
  ;; FIXME(matching)
  (when-not (= (get-in prev-state storefront.keypaths/navigation-event)
               (get-in state storefront.keypaths/navigation-event))
    (messages/handle-message e/initialize-stylist-search-filters))

  (messages/handle-message e/flow|stylist-matching|searched))

;; --------------- gallery modal

(defmethod transitions/transition-state e/control-adventure-stylist-gallery-open
  [_ _ {:keys [ucare-img-urls initially-selected-image-index]} state]
  (-> state
      (assoc-in adventure.keypaths/adventure-stylist-gallery-image-urls
                ucare-img-urls)
      (assoc-in adventure.keypaths/adventure-stylist-gallery-image-index
                initially-selected-image-index)))

(defmethod transitions/transition-state e/control-adventure-stylist-gallery-close
  [_ _ _ state]
  (assoc-in state
            adventure.keypaths/adventure-stylist-gallery-image-urls
            []))

;; --------------------- Address Input behavior

(defmethod effects/perform-effects e/stylist-results-address-component-mounted
  [_ _ _ _ _]
  #?(:cljs
     (google-maps/attach "geocode"
                         "stylist-search-input"
                         k/google-location
                         #(messages/handle-message e/stylist-results-address-selected))))

(defn ^:private address-input
  [elemID]
  #?(:cljs (-> js/document
               (.getElementById elemID)
               .-value)))

(defmethod trackings/perform-track e/stylist-results-address-selected
  [_ _ _ state]
  (let [{:param/keys [location address]}        (stylist-matching<- state)
        {:keys [latitude longitude city state]} location]
    #?(:cljs
       (stringer/track-event "adventure_location_submitted"
                             {:location_submitted address
                              :city               city
                              :state              state
                              :latitude           latitude
                              :longitude          longitude}))))

(defmethod effects/perform-effects e/stylist-results-address-selected
  [_ _ _ _ state]
  ;; Unconstrains stylist-id search
  (messages/handle-message e/flow|stylist-matching|param-ids-constrained)
  ;; Address/Location search
  (messages/handle-message e/flow|stylist-matching|param-address-constrained
                           {:address (address-input "stylist-search-input")})
  (messages/handle-message e/flow|stylist-matching|param-location-constrained
                           (get-in state k/google-location))
  (messages/handle-message e/flow|stylist-matching|prepared))

;; -------------------------- Matching behavior

(defmethod effects/perform-effects e/control-adventure-select-stylist
  [_ _ {:keys [card-index servicing-stylist]} _ _]
  (messages/handle-message e/flow|stylist-matching|matched
                           {:stylist      servicing-stylist
                            :result-index card-index}))

(defn stylist-results->stringer-event
  [stylist]
  (let [{:analytics/keys [stylist-id bookings-count lat long rating just-added? years-of-experience]}
        (select-keys stylist [:analytics/stylist-id
                              :analytics/bookings-count
                              :analytics/lat
                              :analytics/long
                              :analytics/rating
                              :analytics/just-added?
                              :analytics/years-of-experience])]
    {:stylist_id                 stylist-id
     :displayed_bookings_count   bookings-count
     :latitude                   (spice.core/parse-double lat)
     :longitude                  (spice.core/parse-double long)
     :displayed_rating           (spice.core/parse-double rating)
     :just_added                 just-added?
     :displayed_years_experience years-of-experience}))

(defmethod trackings/perform-track e/adventure-stylist-search-results-displayed
  [_ _ {:keys [cards]} state]
  (let [{:results/keys [stylists]
         :param/keys   [location address services]} (stylist-matching<- state)
        {:keys [latitude longitude]}                location]
    #?(:cljs
       (stringer/track-event "stylist_search_results_displayed"
                             {:results            (map stylist-results->stringer-event cards)
                              :filters_on         services
                              :latitude           latitude
                              :longitude          longitude
                              :location_submitted address
                              :radius             "100mi"
                              :current_step       2}))))

(defn header<-
  [{:order.items/keys [quantity]} back]
  {:header.title/id               "adventure-title"
   :header.title/primary          "Meet Your Stylist"
   :header.back-navigation/id     "adventure-back"
   :header.back-navigation/back   back
   :header.back-navigation/target [e/navigate-adventure-find-your-stylist]
   :header.cart/id                "mobile-cart"
   :header.cart/value             quantity
   :header.cart/color             "white"})

(defn stylist-card<-
  [hide-stylist-specialty? hide-bookings? just-added-only? just-added-experience? stylist-results-test? idx stylist]
  (let [{:keys [rating-star-counts
                salon
                service-menu
                gallery-images
                store-slug
                store-nickname
                stylist-id
                stylist-since
                rating]}                      stylist
        rating-count                          (->> rating-star-counts vals (reduce +))
        newly-added-stylist                   (< rating-count 3)
        show-newly-added-stylist-ui?          (and newly-added-stylist
                                                   (or (and stylist-results-test? just-added-only?)
                                                       (and stylist-results-test? just-added-experience?)))
        years-of-experience                   (some->> stylist-since (- (date/year (date/now))))
        {salon-name :name
         :keys      [address-1
                     address-2
                     city
                     state
                     zipcode
                     latitude
                     longitude]}                salon
        {:keys [specialty-sew-in-leave-out
                specialty-sew-in-closure
                specialty-sew-in-360-frontal
                specialty-sew-in-frontal
                specialty-wig-customization]} service-menu]
    {:react/key                                   (str "stylist-card-" store-slug)
     :stylist-card.header/hide-stylist-specialty? hide-stylist-specialty?
     :stylist-card.header/target                  [e/navigate-adventure-stylist-profile {:stylist-id stylist-id
                                                                                         :store-slug store-slug}]
     :stylist-card.header/id                      (str "stylist-card-header-" store-slug)
     :stylist-card.thumbnail/id                   (str "stylist-card-thumbnail-" store-slug)
     :stylist-card.thumbnail/ucare-id             (-> stylist :portrait :resizable-url)

     :stylist-card.title/id            "stylist-name"
     :stylist-card.title/primary       (stylists/->display-name stylist)
     :rating/value                     rating
     :rating/count                     rating-count
     :rating/id                        (when (not show-newly-added-stylist-ui?)
                                         (str "rating-count-" store-slug))
     :analytics/rating                 (when (not show-newly-added-stylist-ui?)
                                         rating)
     :analytics/lat                    latitude
     :analytics/long                   longitude
     :analytics/bookings-count         (when (not (or show-newly-added-stylist-ui?
                                                      hide-bookings?))
                                         rating-count)
     :analytics/just-added?            (when newly-added-stylist
                                         show-newly-added-stylist-ui?)
     :analytics/years-of-experience    (when (and newly-added-stylist
                                                  stylist-results-test?
                                                  just-added-experience?
                                                  years-of-experience)
                                         years-of-experience)
     :analytics/stylist-id             stylist-id
     :stylist-bookings/id              (when (not (or show-newly-added-stylist-ui?
                                                      hide-bookings?))
                                         (str "booking-count-" store-slug))
     :stylist-bookings/content         (str "Booked " (ui/pluralize-with-amount rating-count "time"))
     :stylist.just-added/id            (when show-newly-added-stylist-ui?
                                         (str "just-added-" store-slug))
     :stylist.just-added/content       "Just Added"
     :stylist-ratings/id               (when (not show-newly-added-stylist-ui?)
                                         (str "stylist-ratings-" store-slug))
     :stylist-ratings/content          rating
     :stylist-experience/id            (when (and newly-added-stylist
                                                  stylist-results-test?
                                                  just-added-experience?)
                                         (str "stylist-experience-" store-slug))
     :stylist-experience/content       (str (ui/pluralize-with-amount years-of-experience "year") " of experience")
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
     :stylist-card.cta/target          [e/control-adventure-select-stylist
                                        {:servicing-stylist stylist
                                         :card-index        idx}]

     :stylist-card.gallery/id           (str "stylist-card-gallery-" store-slug)
     :element/type                      :stylist-card
     :stylist-card.gallery/items        (let [ucare-img-urls (map :resizable-url gallery-images)]
                                          (map-indexed
                                           (fn [j ucare-img-url]
                                             {:stylist-card.gallery-item/id       (str "gallery-img-" stylist-id "-" j)
                                              :stylist-card.gallery-item/target   [e/navigate-adventure-stylist-gallery
                                                                                   {:store-slug   store-slug
                                                                                    :stylist-id   stylist-id
                                                                                    :query-params {:offset j}}]
                                              :stylist-card.gallery-item/ucare-id ucare-img-url})
                                           ucare-img-urls))
     :stylist-card.salon-name/id        salon-name
     :stylist-card.salon-name/value     salon-name
     :stylist-card.address-marker/id    (str "stylist-card-address-" store-slug)
     :stylist-card.address-marker/value (join " " [(join ", " (->> [address-1 address-2 city state]
                                                                   (remove blank?)))
                                                   zipcode])}))

(defn stylist-cards-query
  [{:keys [hide-stylist-specialty? hide-bookings?
           just-added-only? just-added-experience? stylist-results-test?]} stylists]
  (map-indexed
   (partial stylist-card<-
            hide-stylist-specialty?
            hide-bookings?
            just-added-only?
            just-added-experience?
            stylist-results-test?)
   stylists))

(defn gallery-modal-query
  [app-state]
  (let [gallery-images (get-in app-state adventure.keypaths/adventure-stylist-gallery-image-urls)
        index          (get-in app-state adventure.keypaths/adventure-stylist-gallery-image-index)]
    {:gallery-modal/target           [e/control-adventure-stylist-gallery-close]
     :gallery-modal/ucare-image-urls gallery-images
     :gallery-modal/initial-index    index}))


;; TODO this name and query and such
(defdynamic-component location-input-and-filters-molecule
  (did-mount [_]
             (messages/handle-message e/stylist-results-address-component-mounted))
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
                                 :on-click  (utils/send-event-callback e/control-show-stylist-search-filters)}
                                [:div.flex.items-center.px1
                                 (svg/funnel {:class  "mrp3"
                                              :height "9px"
                                              :width  "10px"})
                                 (if (= 0 preference-count)
                                   "Filters"
                                   (str "- " preference-count))])]
               (for [{:preference-pill/keys [id target primary]} preferences]
                 [:div.pb1 {:key id}
                  (ui/button-pill {:class "p1 mr1"
                                   :on-click identity} ;; TODO: ????
                                  [:div.flex.pl1 primary
                                   [:div.flex.items-center.pl1
                                    ^:attrs (merge {:data-test id}
                                                   (apply utils/fake-href target))
                                    (svg/close-x {:class  "stroke-white fill-gray"
                                                  :width  "13px"
                                                  :height "13px"})]])])]]))))

(defcomponent matching-count-organism
  [{:keys [stylist-count-content]} _ _]
  [:div.col-12.content-3.mt2.mb1.left-align.mx3
   stylist-count-content])

(defcomponent non-matching-breaker
  [{:keys [breaker-results breaker-content]} _ _]
  [:div.my5.content-2
   (when breaker-results
    [:div
     breaker-results])
   breaker-content])

(defdynamic-component template
  (did-mount
   [this]
   (let [{:keys [stylist.analytics/cards stylist-results-returned?]} (component/get-props this)]
     (when stylist-results-returned?
       (messages/handle-message e/adventure-stylist-search-results-displayed
                                {:cards cards}))))
  (render
   [this]
   (let [{:keys [spinning? stylist-results-present? gallery-modal
                 header location-search-box shopping-method-choice]
          :as   data} (component/get-props this)]
     (component/html
      [:div.bg-cool-gray.black.center.flex.flex-auto.flex-column

       (component/build gallery-modal/organism gallery-modal nil)
       (components.header/adventure-header header)

       (when (:stylist.results.location-search-box/id location-search-box)
         (component/build location-input-and-filters-molecule location-search-box nil))

       (cond
         spinning? [:div.mt6 ui/spinner]


         stylist-results-present?
         [:div
          (when-let [count-id (:list.stylist-counter/key data)]
            [:div
             {:key count-id}
             (ui/screen-aware matching-count-organism
                              {:stylist-count-content (:list.stylist-counter/title data)}
                              (component/component-id count-id))])
          (when (:list.matching/key data)
            [:div
             (for [card (:list.matching/cards data)]
               [:div {:key (:react/key card)}
                (ui/screen-aware stylist-cards/organism card (component/component-id (:react/key card)))])])
          (when (:list.breaker/id data)
            [:div
             {:key       "non-matching-breaker"
              :data-test (:list.breaker/id data)}
             (component/build non-matching-breaker {:breaker-results (:list.breaker/results-content data)
                                                    :breaker-content (:list.breaker/content data)})])
          (when (:list.non-matching/key data)
            [:div
             (for [card (:list.non-matching/cards data)]
               [:div {:key (:react/key card)}
                (ui/screen-aware stylist-cards/organism card (component/component-id (:react/key card)))])])]

         :else
         (component/build shopping-method-choice/organism shopping-method-choice nil))]))))

(defn shopping-method-choice-query []
  {:shopping-method-choice.error-title/id        "stylist-matching-shopping-method-choice"
   :shopping-method-choice.error-title/primary   "We need some time to find you the perfect stylist!"
   :shopping-method-choice.error-title/secondary (str
                                                  "A Mayvenn representative will contact you soon "
                                                  "to help select a Certified Mayvenn Stylist. In the meantime…")
   :list/buttons                                 [{:shopping-method-choice.button/id       "button-looks"
                                                   :shopping-method-choice.button/label    "Shop by look"
                                                   :shopping-method-choice.button/target   [e/navigate-shop-by-look
                                                                                            {:album-keyword :look}]
                                                   :shopping-method-choice.button/ucare-id "a9009728-efd3-4917-9541-b4514b8e4776"}
                                                  {:shopping-method-choice.button/id       "button-bundle-sets"
                                                   :shopping-method-choice.button/label    "Pre-made bundle sets"
                                                   :shopping-method-choice.button/target   [e/navigate-shop-by-look
                                                                                            {:album-keyword :all-bundle-sets}]
                                                   :shopping-method-choice.button/ucare-id "87b46db7-4c70-4d3a-8fd0-6e99e78d3c96"}
                                                  {:shopping-method-choice.button/id       "button-a-la-carte"
                                                   :shopping-method-choice.button/label    "Choose individual bundles"
                                                   :shopping-method-choice.button/target   [e/navigate-category
                                                                                            {:page/slug           "mayvenn-install"
                                                                                             :catalog/category-id "23"}]
                                                   :shopping-method-choice.button/ucare-id "6c39cd72-6fde-4ec2-823c-5e39412a6d54"}
                                                  {:shopping-method-choice.button/id       "button-shop-wigs"
                                                   :shopping-method-choice.button/label    "Shop Virgin Wigs"
                                                   :shopping-method-choice.button/target   [e/navigate-category
                                                                                            {:page/slug           "wigs"
                                                                                             :catalog/category-id "13"
                                                                                             :query-params
                                                                                             {:family
                                                                                              (str "lace-front-wigs"
                                                                                                   categories/query-param-separator
                                                                                                   "360-wigs")}}]
                                                   :shopping-method-choice.button/ucare-id "71dcdd17-f9cc-456f-b763-2c1c047c30b4"}]})

(defn stylist-data->stylist-cards
  [{:keys [stylists] :as data}]
  (when (seq stylists)
    (into [] (mapcat identity [(stylist-cards-query data stylists)]))))

(defn page
  [app-state]
  (let [current-order (api.orders/current app-state)
        matching      (stylist-matching.core/stylist-matching<- app-state)

        stylist-search-results        (:results/stylists matching)
        preferences                   (:param/services matching)
        matches-preferences?          (fn matches-preferences?
                                        [{:keys [service-menu]}]
                                        (every? #(service-menu (accessors.filters/service-sku-id->service-menu-key %)) preferences))
        skus                          (get-in app-state storefront.keypaths/v2-skus)
        {matching-stylists     true
         non-matching-stylists false} (group-by matches-preferences? stylist-search-results)
        hide-stylist-specialty?       (experiments/hide-stylist-specialty? app-state)
        hide-bookings?                (experiments/hide-bookings? app-state)
        just-added-only?              (experiments/just-added-only? app-state)
        just-added-experience?        (experiments/just-added-experience? app-state)
        just-added-control?           (experiments/just-added-control? app-state)
        stylist-results-test?         (experiments/stylist-results-test? app-state)
        stylist-data                  {:filter-prefences        preferences
                                       :hide-stylist-specialty? hide-stylist-specialty?
                                       :hide-bookings?          hide-bookings?
                                       :just-added-only?        just-added-only?
                                       :just-added-experience?  just-added-experience?
                                       :stylist-results-test?   stylist-results-test?}
        matching-stylist-cards        (stylist-data->stylist-cards (assoc stylist-data :stylists matching-stylists))
        non-matching-stylist-cards    (stylist-data->stylist-cards (assoc stylist-data :stylists non-matching-stylists))
        filter-menu                   #?(:cljs (filter-menu/query app-state) :clj  nil)]
    (if (:stylist-search-filters/show? filter-menu)
      (component/build #?(:clj  (component/html [:div])
                          :cljs filter-menu/component) filter-menu nil)
      (component/build template
                       {:gallery-modal            (gallery-modal-query app-state)
                        :spinning?                (or (utils/requesting-from-endpoint? app-state request-keys/fetch-matched-stylists)
                                                      (utils/requesting-from-endpoint? app-state request-keys/fetch-stylists-matching-filters)
                                                      (utils/requesting-from-endpoint? app-state request-keys/get-products)
                                                      (and (not (get-in app-state storefront.keypaths/loaded-convert))
                                                           stylist-results-test?
                                                           (or (not just-added-control?)
                                                               (not just-added-only?)
                                                               (not just-added-experience?))))
                        :location-search-box      (when (get-in app-state storefront.keypaths/loaded-google-maps)
                                                    {:stylist.results.location-search-box/id          "stylist-search-input"
                                                     :stylist.results.location-search-box/value       (get-in app-state k/google-input)
                                                     :stylist.results.location-search-box/keypath     k/google-input
                                                     :stylist.results.location-search-box/errors      []
                                                     :stylist.results.location-search-box/preferences (mapv
                                                                                                       (fn [preference]
                                                                                                         {:preference-pill/target  [e/control-stylist-search-toggle-filter
                                                                                                                                    {:previously-checked? true :stylist-filter-selection preference}]
                                                                                                          :preference-pill/id      (str "remove-preference-button-" (name preference))
                                                                                                          :preference-pill/primary (get-in skus [preference :sku/name])})
                                                                                                       (vec preferences))})
                        :header                   (header<- current-order
                                                            (first (get-in app-state storefront.keypaths/navigation-undo-stack)))
                        :stylist-results-present? (seq (concat matching-stylists non-matching-stylists))

                        :stylist-results-returned?    (contains? (:status matching) :results/stylists)
                        :list.stylist-counter/title   (str (count matching-stylists) " Stylists Found")
                        :list.stylist-counter/key     (when (pos? (count matching-stylists))
                                                        "stylist-count-content")
                        :list.matching/key            (when (seq matching-stylists) "stylist-matching")
                        :list.matching/cards          matching-stylist-cards
                        :list.breaker/id              (when (seq non-matching-stylists) "non-matching-breaker")
                        :list.breaker/results-content (when (and (seq non-matching-stylists)
                                                                 (empty? matching-stylists))
                                                        "0 results found")
                        :list.breaker/content         "Other stylists in your area"
                        :list.non-matching/key        (when (seq non-matching-stylists) "non-matching-stylists")
                        :list.non-matching/cards      non-matching-stylist-cards
                        :stylist.analytics/cards      (into matching-stylist-cards non-matching-stylist-cards)
                        :shopping-method-choice       (shopping-method-choice-query)}
                       {:key (str "stylist-results-" (hash stylist-search-results) "-" (hash stylist-data))}))))
