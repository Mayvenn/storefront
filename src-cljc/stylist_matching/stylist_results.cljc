(ns stylist-matching.stylist-results
  (:require #?@(:cljs [[storefront.hooks.google-maps :as google-maps]
                       [storefront.hooks.stringer :as stringer]
                       [storefront.history :as history]
                       [stylist-matching.search.filters-modal :as filter-menu]])
            adventure.keypaths
            [stylist-matching.core :refer [stylist-matching<- service-delimiter]]
            [stylist-matching.keypaths :as k]
            api.orders
            [clojure.string :as string]
            [storefront.accessors.sites :as sites]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.stylists :as stylists]
            [storefront.accessors.categories :as categories]
            [storefront.component :as component :refer [defdynamic-component defcomponent]]
            [storefront.components.header :as components.header]
            [storefront.components.svg :as svg]
            [storefront.events :as e]
            [stylist-matching.ui.shopping-method-choice :as shopping-method-choice]
            [stylist-matching.ui.stylist-cards :as stylist-cards]
            [stylist-matching.ui.top-stylist-cards :as top-stylist-cards]
            [stylist-matching.ui.gallery-modal :as gallery-modal]
            [stylist-matching.search.accessors.filters :as accessors.filters]
            [storefront.components.ui :as ui]
            [spice.core :as spice]
            [storefront.effects :as effects]
            [storefront.trackings :as trackings]
            [storefront.transitions :as transitions]
            [storefront.platform.messages :as messages]
            [storefront.platform.component-utils :as utils]
            [storefront.utils :as general-utils]
            [storefront.request-keys :as request-keys]
            [spice.date :as date]
            storefront.keypaths
            [mayvenn.visual.ui.titles :as titles]
            [mayvenn.visual.tools :refer [with within]]
            [mayvenn.live-help.core :as live-help]
            [storefront.events :as events]))


;;  Navigating to the results page causes the effect of searching for stylists
;;
;;  This allows:
;;  - Cold loads of the page, both with lat+long and stylist ids
;;  - Refining of search results
;;

(defmethod effects/perform-effects e/navigate-adventure-stylist-results
  [_ _ {{preferred-services :preferred-services
         moniker            :name
         stylist-ids        :s
         latitude           :lat
         longitude          :long
         address            :address} :query-params} prev-state state]
  (if (not= :shop (sites/determine-site state))
    (effects/redirect e/navigate-home)
    (do
      #?(:cljs (google-maps/insert))

      ;; Init the model if there isn't one, e.g. Direct load
      (when-not (stylist-matching<- state)
        (messages/handle-message e/flow|stylist-matching|initialized))

      ;; Pull stylist-ids (s) from URI; predetermined search results
      (when (seq stylist-ids)
        (messages/handle-message e/flow|stylist-matching|param-ids-constrained
                                 {:ids stylist-ids}))
      ;; Pull name search from URI
      (messages/handle-message e/flow|stylist-matching|set-presearch-field
                               {:name moniker})
      (messages/handle-message e/flow|stylist-matching|param-name-constrained
                               {:name moniker})

      ;; Address from URI
      (messages/handle-message e/flow|stylist-matching|set-address-field
                               {:address address})

      ;; Pull preferred services from URI; filters for service types
      (when-let [services (some-> preferred-services
                                  not-empty
                                  (string/split (re-pattern service-delimiter))
                                  set)]
        (messages/handle-message e/flow|stylist-matching|param-services-constrained
                                 {:services services}))
      ;; Pull lat/long from URI; search by proximity
      (when (and (not-empty latitude)
                 (not-empty longitude))
        (messages/handle-message e/flow|stylist-matching|param-location-constrained
                                 {:latitude  (spice/parse-double latitude)
                                  :longitude (spice/parse-double longitude)}))
      ;; FIXME(matching)
      (when-not (= (get-in prev-state storefront.keypaths/navigation-event)
                   (get-in state storefront.keypaths/navigation-event))
        (messages/handle-message e/initialize-stylist-search-filters))

      (messages/handle-message e/flow|stylist-matching|searched))))

;; --------------- gallery modal

;; TODO: this does not appear to be used anywhere consider removing?
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
  [_ _ {:keys [component]} _ _]
  #?(:cljs
     (google-maps/attach "geocode"
                         "stylist-search-input"
                         k/google-location
                         #(messages/handle-message e/stylist-results-address-selected)
                         ;; HACK: in order to bypass google maps' default enter behavior
                         ;; we are overwriting it to ensure we call our own code that's
                         ;; attched to the blur event. Otherwise it triggered a new search.
                         [(fn [elem _]
                            (js/google.maps.event.addDomListener
                             elem
                             "keydown"
                             (fn [e]
                               (when (= "Enter" (.. e -key))
                                 ((.-enterKeyPressed component) true)
                                 (.blur (.-target e))))))
                          (fn [_elem _]
                            (messages/handle-message e/stylist-results-update-location-from-address))])))

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

(defmethod effects/perform-effects e/stylist-results-update-location-from-address
  [_ _ _ _ state]
  #?(:cljs
     (let [location (get-in state k/location)
           address  (get-in state k/address)]
       (when-not location
         (google-maps/get-geo-code
          address
          k/google-location
          (fn []
            (messages/handle-message e/stylist-results-address-selected)
            (messages/handle-message e/flow|stylist-matching|searched)))))))

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
  (let [{:analytics/keys [stylist-id lat long rating just-added? years-of-experience]}
        (select-keys stylist [:analytics/stylist-id
                              :analytics/lat
                              :analytics/long
                              :analytics/rating
                              :analytics/just-added?
                              :analytics/years-of-experience])]
    {:stylist_id                 stylist-id
     :latitude                   (spice.core/parse-double lat)
     :longitude                  (spice.core/parse-double long)
     :displayed_rating           (spice.core/parse-double rating)
     :just_added                 just-added?
     :displayed_years_experience years-of-experience}))

(defmethod trackings/perform-track e/adventure-stylist-search-results-displayed
  [_ _ {:keys [cards]} state]
  (let [{:results/keys [stylists]
         :param/keys   [location address services]
         moniker       :param/name}  (stylist-matching<- state)
        {:keys [latitude longitude]} location]
    #?(:cljs
       (stringer/track-event "stylist_search_results_displayed"
                             {:results            (map stylist-results->stringer-event cards)
                              :filters_on         services
                              :latitude           latitude
                              :longitude          longitude
                              :location_submitted address
                              :radius             "100mi"
                              :current_step       2
                              :name_query         moniker}))))

(defn header<-
  [{:order.items/keys [quantity]}]
  {:header.title/id               "adventure-title"
   :header.title/primary          "Meet Your Stylist"
   :header.back-navigation/id     "adventure-back"
   :header.back-navigation/target [e/navigate-adventure-find-your-stylist]
   :header.cart/id                "mobile-cart"
   :header.cart/value             (or quantity 0)
   :header.cart/color             "white"})

(defn- address->display-string
  [{:keys [address-1 address-2 city state zipcode]}]
  (string/join " "
               [(string/join ", " (->> [address-1 address-2 city state]
                                       (remove string/blank?)))
                zipcode]))

(defn stylist-card<-
  [just-added-only? just-added-experience? stylist-results-test? idx stylist]
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
         :keys      [latitude longitude]}     salon
        {:keys [specialty-sew-in-leave-out
                specialty-sew-in-closure
                specialty-sew-in-360-frontal
                specialty-sew-in-frontal
                specialty-wig-customization]} service-menu]
    {:react/key                       (str "stylist-card-" store-slug)
     :stylist-card.header/target      [e/flow|stylist-matching|selected-for-inspection {:stylist-id stylist-id
                                                                                        :store-slug store-slug}]
     :stylist-card.header/id          (str "stylist-card-header-" store-slug)
     :stylist-card.thumbnail/id       (str "stylist-card-thumbnail-" store-slug)
     :stylist-card.thumbnail/ucare-id (-> stylist :portrait :resizable-url)

     :stylist-card.title/id      "stylist-name"
     :stylist-card.title/primary (stylists/->display-name stylist)
     :rating/value               rating
     :rating/count               rating-count
     :rating/id                  (when (not show-newly-added-stylist-ui?)
                                   (str "rating-count-" store-slug))
     :analytics/rating           (when (not show-newly-added-stylist-ui?)
                                   rating)
     :analytics/lat              latitude
     :analytics/long             longitude

     :analytics/just-added?            (when newly-added-stylist
                                         show-newly-added-stylist-ui?)
     :analytics/years-of-experience    (when (and newly-added-stylist
                                                  stylist-results-test?
                                                  just-added-experience?
                                                  years-of-experience)
                                         years-of-experience)
     :analytics/stylist-id             stylist-id
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
     :stylist-card.address-marker/value (address->display-string salon)}))

(defn stylist-cards-query
  [{:keys [just-added-only? just-added-experience? stylist-results-test?]} stylists]
  (map-indexed
   (partial stylist-card<-
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

(defn execute-named-search [name]
  (messages/handle-message e/flow|stylist-matching|presearch-canceled)
  (messages/handle-message e/flow|stylist-matching|param-name-constrained {:name name})
  (messages/handle-message e/flow|stylist-matching|prepared))

(defcomponent stylist-results-name-input-molecule
  [{:stylist-results.name-input/keys [id value errors keypath]} _ _]
  (when id
    (ui/input-with-charms
     {:left-charm  (svg/magnifying-glass {:width  "19px"
                                          :height "19px"
                                          :class  "fill-gray"})
      :input-field {:errors        errors
                    :autoComplete  "off"
                    :value         value
                    :keypath       keypath
                    :data-test     id
                    :id            id
                    :max-length    100
                    :on-click      (fn [e] (.focus (.-target e)))
                    :on-change     (fn [e]
                                     (messages/handle-message e/flow|stylist-matching|param-name-presearched
                                                              {:presearch/name (.. e -target -value)}))
                    :on-key-up     (fn name-input-enter-handler [e]
                                     (when (= "Enter" (.. e -key))
                                       (execute-named-search (.. e -target -value))))
                    :on-blur       (fn name-input-blur-handler [e]
                                     (when-not (some-> e
                                                       .-relatedTarget
                                                       (.getAttribute "class")
                                                       (string/includes? "presearch-result"))
                                       (messages/handle-message e/flow|stylist-matching|presearch-canceled)
                                       (messages/handle-message e/flow|stylist-matching|set-presearch-field)))
                    :label         "Search by Stylist Name"
                    :wrapper-class "flex items-center col-12 bg-white border-black"
                    :type          "text"}

      :right-charm
      (when (not-empty value)
        [:a.flex.items-center.pr1
         {:on-click (utils/send-event-callback e/flow|stylist-matching|presearch-cleared)}
         (svg/close-x {:class "stroke-white fill-gray"})])})))

(defdynamic-component stylist-results-address-input-molecule
  (constructor [this _]
               (set! (.-enterKeyPressed this)
                     (fn [enter-key-pressed?]
                       (component/set-state! this :enter-key-pressed? enter-key-pressed?)))
               {:enter-key-pressed? false})
  (did-mount
   [this]
   (messages/handle-message e/stylist-results-address-component-mounted {:component this}))
  (render
   [this]
   (let [{:stylist-results.address-input/keys [id value errors keypath]}
         (component/get-props this)]
     (component/html
      (ui/input-with-charms
       {:left-charm  (svg/map-pin {:width  "21px"
                                   :height "21px"
                                   :class  "fill-gray"})
        :input-field {:errors        errors
                      :autoComplete  "off"
                      :value         value
                      :keypath       keypath
                      :data-test     id
                      :on-blur       (fn [_]
                                       (messages/handle-message e/flow|stylist-matching|set-address-field {:enter-key-pressed? (:enter-key-pressed? (component/get-state this))})
                                       ((.-enterKeyPressed this) false))
                      :id            id
                      :wrapper-class "flex items-center col-12 bg-white border-black"
                      :type          "text"}})))))

(defn stylist-results-service-filters-molecule
  [{:stylist-results.service-filters/keys [preferences]}]
  (component/html
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
                      (if (= 0 (count preferences))
                        "Filters"
                        (str "- " (count preferences)))])]
    (for [{:preference-pill/keys [id target primary]} preferences]
      [:div.pb1 {:key id}
       [:div {:class "btn-pill content-3 black p1 mr1"}
        [:div.flex.pl1 primary
         [:a.flex.items-center.pl1
          ^:attrs (merge {:data-test id}
                         (apply utils/fake-href target))
          (svg/close-x {:class  "stroke-white fill-gray"
                        :width  "13px"
                        :height "13px"})]]]])]))

(defcomponent matching-count-organism
  [{:keys [stylist-count-content]} _ _]
  [:div.col-12.content-3.mt2.mb1.left-align.px3
   stylist-count-content])

(defcomponent non-matching-breaker
  [{:keys [breaker-results breaker-content]} _ _]
  [:div.my5.content-2
   (when breaker-results
    [:div
     breaker-results])
   breaker-content])

(defdynamic-component results
  (did-mount
   [this]
   (let [{:keys [stylist.analytics/cards stylist-results-returned?]}
         (component/get-props this)]
     (when stylist-results-returned?
       (messages/handle-message e/adventure-stylist-search-results-displayed
                                {:cards cards}))))
  (render
   [this]
   (let [{:keys [stylist-results-present? shopping-method-choice] :as data}
         (component/get-props this)]
     (component/html
      (if stylist-results-present?
        [:div
         (when-let [count-id (:list.stylist-counter/key data)]
           [:div
            {:key count-id}
            (ui/screen-aware matching-count-organism
                             {:stylist-count-content (:list.stylist-counter/title data)}
                             (component/component-id count-id))])
         (when (:list.matching/key data)
           [:div
            (for [{:keys [type data]} (:list.matching/cards data)]
              (case type
                :matching-stylist-card
                [:div {:key (:react/key data)}
                 (ui/screen-aware stylist-cards/organism
                                  data
                                  (component/component-id (:react/key data)))]

                :live-help-breaker
                [:div.m3 {:key "call-out-box"} (component/build live-help/banner data)]

                nil))])
         (when (:list.breaker/id data)
           [:div
            {:key       "non-matching-breaker"
             :data-test (:list.breaker/id data)}
            (component/build non-matching-breaker
                             {:breaker-results (:list.breaker/results-content data)
                              :breaker-content (:list.breaker/content data)})])
         (when (:list.non-matching/key data)
           [:div
            (for [card (:list.non-matching/cards data)]
              [:div {:key (:react/key card)}
               (ui/screen-aware stylist-cards/organism
                                card
                                (component/component-id (:react/key card)))])])]

        (component/build shopping-method-choice/organism
                         shopping-method-choice))))))

(defn stylist-search-inputs<-
  "Stylist Search inputs

  1. empty name results? (under experiment)
  2. name (under experiment)
  3. address (location)
  4. service filters"
  [matching empty-search-results? google-loaded? skus-db address-field-errors]
  (merge
   {:stylist-results.name-input/id      "stylist-search-name-input"
    :stylist-results.name-input/value   (:presearch/name matching)
    :stylist-results.name-input/keypath k/presearch-name
    :stylist-results.name-input/errors  []}
   (when google-loaded?
     {:stylist-results.address-input/id      "stylist-search-input"
      :stylist-results.address-input/value   (:google/input matching)
      :stylist-results.address-input/keypath k/google-input
      :stylist-results.address-input/errors  address-field-errors})
   (when-let [pills (->> (:param/services matching)
                         (keep
                          (fn [sku-id]
                            (when-let [sku-name (get-in skus-db [sku-id :sku/name])]
                              #:preference-pill
                              {:target  [e/control-stylist-search-toggle-filter
                                         {:previously-checked?      true
                                          :stylist-filter-selection sku-id}]
                               :id      (str "remove-preference-button-" sku-id)
                               :primary sku-name})))
                         not-empty)]
     {:stylist-results.service-filters/preferences pills})

   ;; Things that can appear in the results list
   (when empty-search-results?
     {:empty-stylist-search/id      "empty-stylist-results-id"
      :empty-stylist-search/primary "No results for this search."})
   (when (contains? (:status matching) :results.presearch/name)
     {:stylist-results.name-presearch-results/list
      (map-indexed
       (fn [index
            {moniker       :name
             result-type   :type
             salon-address :salon-address
             :as           result}]
         (merge
          {:stylist-results.name-presearch-results.result/primary moniker}
          (when (= "stylist" result-type)
            {:stylist-results.name-presearch-results.result/ucare-uri (:portrait-uri result)
             :stylist-results.name-presearch-results.result/target
             [e/control-stylist-matching-presearch-stylist-result-selected (-> result
                                                                               (select-keys [:stylist-id :store-slug])
                                                                               (assoc :result-index index))]})
          (when (= "salon" result-type)
            {:stylist-results.name-presearch-results.result/target [e/control-stylist-matching-presearch-salon-result-selected {:name moniker}]})
          (when salon-address
            {:stylist-results.name-presearch-results.result/secondary
             (address->display-string salon-address)})))
       (:results.presearch/name matching))})))

;; TODO(corey) eval the effectiveness of the various 'elements' and
;; use the best version here
(defn stylist-results-name-presearch-results-molecule
  [{elements :stylist-results.name-presearch-results/list}]
  (when (seq elements)
    (component/html
     [:div.absolute.left-0.right-0.bg-white.z1.border.border-gray
      (map-indexed
       (fn [index {:stylist-results.name-presearch-results.result/keys [ucare-uri primary secondary target]}]
         [:a.flex.px4.py2.presearch-result.inherit-color
          (merge {:key primary
                  :tab-index (str index)}

                 (apply utils/fake-href target))
          [:div.self-center.mr3
           {:style {:width "26px"}}
           (if ucare-uri
             (ui/circle-picture {:width "26px"}
                                (ui/square-image {:resizable-url ucare-uri}
                                                 26))
             (svg/mirror {:class  "fill-gray"
                          :width  "20px"
                          :height "20px"}))]
          [:div.left-align
           [:div primary]
           (when secondary
             [:div.content-4.dark-gray
              secondary])]])
        elements)])))

(defn stylist-results-empty-name-presearch-results-molecule
  [{:empty-stylist-search/keys [id primary]}]
  (when id
    (component/html
     [:div.absolute.left-0.right-0.bg-white.z1.border.border-gray
      {:data-test id}
      [:div.flex.px4.py2.justify-center
       [:div.flex.items-center {:style {:height "37px"}} primary]]])))

(defcomponent search-inputs-organism
  [data _ _]
  [:div.px3.py2.bg-white.border-bottom.border-gray
   (component/build stylist-results-name-input-molecule data)
   (when (:stylist-results.address-input/id data)
     (component/build stylist-results-address-input-molecule data))
   [:div.relative
    (stylist-results-name-presearch-results-molecule data)
    (stylist-results-empty-name-presearch-results-molecule data)
    (stylist-results-service-filters-molecule data)]])

(def scrim-atom
  [:div.absolute.overlay.bg-darken-4])

(defcomponent template
  [{:keys [spinning? gallery-modal header stylist-search-inputs scrim?] :as data} _ _]
  [:div.bg-cool-gray.black.center.flex.flex-auto.flex-column
   (component/build gallery-modal/organism gallery-modal nil)
   (components.header/adventure-header header)

   (component/build search-inputs-organism stylist-search-inputs)

   (if spinning?
     [:div.mt6 ui/spinner]
     [:div.relative.stretch
      (component/build results data)
      (when scrim?
        scrim-atom)])])

(defcomponent top-stylist-template
  [data _ _]
  (let [template-data    (with :template data)
        top-stylist-data (with :top-stylist data)]
    [:div.bg-pale-purple.black.center.flex.flex-auto.flex-column
     (component/build gallery-modal/organism (with :gallery-modal data) nil)
     (components.header/adventure-header (with :header data))
     [:div.m6
      (titles/canela-huge (with :title top-stylist-data))]
     (ui/screen-aware top-stylist-cards/organism
                      top-stylist-data
                      (component/component-id (:react/key data)))
     (let [{:keys [target primary]} (with :footer top-stylist-data)]
       [:div.mt4.mb8
        (ui/button-medium-underline-primary
         (apply utils/route-to target)
         primary)])]))

(def shopping-method-choice-query
  {:shopping-method-choice.error-title/id        "stylist-matching-shopping-method-choice"
   :shopping-method-choice.error-title/primary   "We need some time to find you the perfect stylist!"
   :shopping-method-choice.error-title/secondary (str
                                                  "A Mayvenn representative will contact you soon "
                                                  "to help select a Certified Mayvenn Stylist. In the meantime…")

   :list/buttons [{:shopping-method-choice.button/id       "button-looks"
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

(defn matches-preferences?
  [preferences {:keys [service-menu]}]
  (every? #(service-menu (accessors.filters/service-sku-id->service-menu-key %))
          preferences))

(defmethod effects/perform-effects e/control-stylist-matching-presearch-stylist-result-selected
  [_ _ args _ state]
  #?(:cljs
     (do
       (execute-named-search (get-in state k/presearch-name))
       (history/enqueue-navigate e/navigate-adventure-stylist-profile args))))

(defmethod trackings/perform-track e/control-stylist-matching-presearch-stylist-result-selected
  [_ _ args state]
  #?(:cljs
     (stringer/track-event "search_suggestion_clicked"
                           {:stylist_id       (:stylist-id args)
                            :stylist_position (:result-index args)})))

(defmethod trackings/perform-track e/control-stylist-matching-presearch-salon-result-selected
  [_ _ args state]
  #?(:cljs (stringer/track-event "search_suggestion_clicked" {:salon_name (:name args)})))

(defmethod transitions/transition-state e/flow|stylist-matching|set-presearch-field
  [_ _ args state]
  ;; "Set or Reset"
  (let [{{moniker :name} :query-params} (get-in state storefront.keypaths/navigation-args)]
    (-> state
        ;; Close search results
        (update-in k/status disj :results.presearch/name)
        ;; Place selected name in presearch field
        (assoc-in k/presearch-name (or (:name args) moniker)))))

(defmethod transitions/transition-state e/flow|stylist-matching|set-address-field
  [_ _ args state]
  (let [{{address :address} :query-params} (get-in state storefront.keypaths/navigation-args)
        new-address                        (:address args)
        enter-key-pressed?                 (:enter-key-pressed? args)]
    (if (and (nil? new-address) enter-key-pressed?)
      (-> state
          (assoc-in k/address nil)
          (assoc-in k/google-input nil)
          (assoc-in k/address-field-errors [{:long-message "Select an address from the suggested list."}]))
      (-> state
          (assoc-in k/address-field-errors [])
          (assoc-in k/address (or new-address address))
          (assoc-in k/google-input (or new-address address))))))

(defmethod effects/perform-effects e/control-stylist-matching-presearch-salon-result-selected
  [_ _ args _ state]
  (messages/handle-message e/flow|stylist-matching|set-presearch-field args)
  (execute-named-search (:name args)))

(defn ^:export page
  [app-state _]
  (let [;; Models
        current-order (api.orders/current app-state)
        matching      (stylist-matching.core/stylist-matching<- app-state)
        skus-db       (get-in app-state storefront.keypaths/v2-skus)

        ;; Experiments
        just-added-only?       (experiments/just-added-only? app-state)
        just-added-experience? (experiments/just-added-experience? app-state)
        just-added-control?    (experiments/just-added-control? app-state)
        stylist-results-test?  (experiments/stylist-results-test? app-state)

        google-loaded?        (get-in app-state storefront.keypaths/loaded-google-maps)
        presearching-name?    (contains? (:status matching) :results.presearch/name)
        empty-search-results? (and (empty? (:results.presearch/name matching))
                                   presearching-name?)

        stylist-search-results        (:results/stylists matching)
        preferences                   (:param/services matching)
        {matching-stylists     true
         non-matching-stylists false} (group-by (partial matches-preferences? preferences)
                                                stylist-search-results)
        stylist-data                  {:just-added-only?       just-added-only?
                                       :just-added-experience? just-added-experience?
                                       :stylist-results-test?  stylist-results-test?}
        top-stylist                   (first matching-stylists) ;; TODO: use real top matching algorithm
        matching-stylist-cards        (stylist-data->stylist-cards
                                       (assoc stylist-data :stylists matching-stylists))
        non-matching-stylist-cards    (stylist-data->stylist-cards
                                       (assoc stylist-data :stylists non-matching-stylists))
        filter-menu                   #?(:cljs (filter-menu/query app-state) :clj nil)
        show-filter-menu?             (some? filter-menu)
        show-top-stylist?             true
        address-field-errors          (get-in app-state k/address-field-errors)]
    (cond
      show-filter-menu?
      (component/build #?(:clj  (component/html [:div])
                          :cljs filter-menu/component) filter-menu nil)

      show-top-stylist?
      (component/build
       top-stylist-template
       (merge #_(within stylist.analytics
                        {:cards [(->> top-stylist
                                      (conj '())
                                      (assoc stylist-data :stylists)
                                      stylist-data->stylist-cards
                                      first)]})

              (within :gallery-modal
                      (gallery-modal-query app-state))

              (within :template
                      {:spinning?
                       (or (empty? (:status matching))
                           (utils/requesting-from-endpoint? app-state request-keys/fetch-matched-stylists)
                           (utils/requesting-from-endpoint? app-state request-keys/fetch-stylists-matching-filters)
                           (utils/requesting-from-endpoint? app-state request-keys/get-products)
                           (and (not (get-in app-state storefront.keypaths/loaded-convert))
                                stylist-results-test?
                                (or (not just-added-control?)
                                    (not just-added-only?)
                                    (not just-added-experience?))))})
              (within :top-stylist
                      (->> top-stylist
                           (conj '())
                           (assoc stylist-data :stylists)
                           stylist-data->stylist-cards
                           first))
              (within :top-stylist.crown
                      {:primary [:div.p-color.line-height-4 "Top Stylist"]
                       :icon    [:svg/crown {:style {:height "1.2em"
                                                     :width  "1.7em"}
                                             :class "fill-p-color mr1"}]})
              (within :top-stylist.laurels
                      {:points [{:icon    [:svg/calendar {:style {:height "1.2em"
                                                                  :width  "1.7em"}
                                                          :class "fill-s-color mr1"}]
                                 :primary [:div.content-4 "Booked 145 times"]}
                                {:icon    [:svg/chat-bubble-diamonds {:style {:height "1.2em"
                                                                              :width  "1.7em"}
                                                                      :class "fill-s-color mr1"}]
                                 :primary [:div.content-4 "100% response rate"]}
                                {:icon    [:svg/experience-badge {:style {:height "1.2em"
                                                                          :width  "1.7em"}
                                                                  :class "fill-s-color mr1"}]
                                 :primary [:div.content-4 "Progessional salon"]}
                                {:icon    [:svg/certified {:style {:height "1.2em"
                                                                   :width  "1.7em"}
                                                           :class "fill-s-color mr1"}]
                                 :primary [:div.content-4 "State licensed stylist"]}]})

              (within :header
                      (header<- current-order))
              (within :top-stylist.footer
                      {:primary "View All Stylists"
                       :target  [events/navigate-adventure-stylist-results]})
              (within :top-stylist.title
                      {:id        "top-match-copy-header"
                       :primary   "You are in luck!"
                       :secondary (str "Top Stylist alert! "
                                       (-> top-stylist :address :firstname) " " (-> top-stylist :address :lastname)
                                       " is an experienced and licensed "
                                       "stylist who is rated highly for their skill, professionalism, and "
                                       "cleanliness.")})))

      :else
      (component/build template
                       {:gallery-modal            (gallery-modal-query app-state)
                        :spinning?                (or (empty? (:status matching))
                                                      (utils/requesting-from-endpoint? app-state request-keys/fetch-matched-stylists)
                                                      (utils/requesting-from-endpoint? app-state request-keys/fetch-stylists-matching-filters)
                                                      (utils/requesting-from-endpoint? app-state request-keys/get-products)
                                                      (and (not (get-in app-state storefront.keypaths/loaded-convert))
                                                           stylist-results-test?
                                                           (or (not just-added-control?)
                                                               (not just-added-only?)
                                                               (not just-added-experience?))))
                        :scrim?                   presearching-name?
                        :stylist-search-inputs    (stylist-search-inputs<- matching
                                                                           empty-search-results?
                                                                           google-loaded?
                                                                           skus-db
                                                                           address-field-errors)
                        :header                   (header<- current-order)
                        :stylist-results-present? (seq (concat matching-stylists non-matching-stylists))

                        :stylist-results-returned?  (contains? (:status matching) :results/stylists)
                        :list.stylist-counter/title (str (count matching-stylists) " Stylists Found")
                        :list.stylist-counter/key   (when (pos? (count matching-stylists))
                                                      "stylist-count-content")
                        :list.matching/key          (when (seq matching-stylists) "stylist-matching")
                        :list.matching/cards        (cond->> matching-stylist-cards
                                                      :always
                                                      (mapv
                                                       (fn [msc]
                                                         {:type :matching-stylist-card
                                                          :data msc}))

                                                      (live-help/kustomer-started? app-state)
                                                      (general-utils/insert-at-pos
                                                       3
                                                       {:type :live-help-breaker
                                                        :data {:live-help/location "stylist-results-breaker"}}))
                        :list.breaker/id              (when (seq non-matching-stylists) "non-matching-breaker")
                        :list.breaker/results-content (when (and (seq non-matching-stylists)
                                                                 (empty? matching-stylists))
                                                        "0 results found")
                        :list.breaker/content         "Other stylists in your area"
                        :list.non-matching/key        (when (seq non-matching-stylists) "non-matching-stylists")
                        :list.non-matching/cards      non-matching-stylist-cards
                        :stylist.analytics/cards      (into matching-stylist-cards non-matching-stylist-cards)
                        :shopping-method-choice       shopping-method-choice-query}
                       {:key (str "stylist-results-" (hash stylist-search-results) "-" (hash stylist-data))}))))
