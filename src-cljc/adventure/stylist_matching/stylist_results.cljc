(ns adventure.stylist-matching.stylist-results
  (:require [adventure.components.header :as header]
            [adventure.keypaths :as keypaths]
            [storefront.keypaths]
            [spice.date :as date]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.effects :as effects]
            [storefront.transitions :as transitions]
            [storefront.trackings :as trackings]
            #?@(:cljs [[storefront.history :as history]
                       [storefront.hooks.stringer :as stringer]
                       [storefront.api :as api]])
            [adventure.progress :as progress]
            [storefront.keypaths :as storefront.keypaths]
            [adventure.stylist-matching.stylist-detail-line :as stylist-detail-line]))

(defn ^:private gallery-slide [index gallery-image]
  [:div {:key (str "gallery-slide" index)}
   (ui/aspect-ratio 1 1
                    (ui/ucare-img {:class "col-12"} (:resizable-url gallery-image)))])

(defn stylist-detail-line [{:keys [salon licensed stylist-since] :as stylist}]
  (let [{:keys [salon-type]} salon]
    (into [:div.flex.flex-wrap]
          (comp
           (remove nil?)
           (interpose [:div.mxp3 "·"]))
          [(when licensed
             [:div "Licensed"])
           (when-let [salon-description (case salon-type
                                          "salon"   "In-Salon"
                                          "in-home" "In-Home"
                                          nil)]
             [:div salon-description])
           (when stylist-since
             [:div
              (ui/pluralize-with-amount
               (- (date/year (date/now)) stylist-since)
               "yr")
              " Experience"])])))

(defn stylist-card
  [{:keys [selected-image-index
           current-stylist-index
           gallery-open?]}
   {:keys [gallery-images
           address
           portrait
           rating
           salon
           stylist-id]
    :as   stylist}
   stylist-detail-line-data]
  (let [{:keys [firstname lastname]} address
        {:keys [city state name]}    salon]
    [:div.bg-white.p2.pb2.h6.my2.col-12.col-8-on-tb-dt {:key stylist-id}
     [:div.flex
      [:div.mr2.mt1 (ui/circle-picture {:width "104px"}
                                       (str (:resizable-url portrait) "-/format/auto/"))]
      [:div.flex-grow-1.left-align.dark-gray.h7.line-height-4
       [:div.h3.black.line-height-1
        [:span {:data-test "stylist-firstname"}
         firstname]
        (str " " lastname)]
       [:div (ui/star-rating rating)]
       [:div.bold (str city ", " state)]
       [:div name]
       (stylist-detail-line/component stylist-detail-line-data)]]
     [:div.my2.m1-on-tb-dt.mb2-on-tb-dt
      [:div.h7.dark-gray.bold.left-align.mb1
       "Recent Work"]
      (component/build carousel/component
                       {:slides   (map-indexed (fn [i x]
                                                 [:div
                                                  {:on-click #(messages/handle-message
                                                               events/control-adventure-stylist-gallery-open
                                                               {:stylist-gallery-index current-stylist-index
                                                                :image-index           i})
                                                   :key      (str firstname "-gallery-" i)}
                                                  (ui/aspect-ratio
                                                   1 1
                                                   [:img {:src   (str (:resizable-url x) "-/scale_crop/204x204/-/format/auto/")
                                                          :class "rounded"
                                                          :width "102"}])])
                                               gallery-images)
                        :settings {:swipe        true
                                   :initialSlide 0
                                   :arrows       true
                                   :dots         false
                                   :slidesToShow 3
                                   :infinite     true}}
                       {})]
     (ui/teal-button
      (merge {:data-test "select-stylist"}
             (utils/fake-href events/control-adventure-select-stylist {:stylist-id        stylist-id
                                                                       :servicing-stylist stylist
                                                                       :card-index        current-stylist-index}))
      [:div.flex.items-center.justify-center.inherit-color
       "Select"])
     (when gallery-open?
       (let [close-attrs (utils/fake-href events/control-adventure-stylist-gallery-close)]
         (ui/modal
          {:close-attrs close-attrs
           :col-class   "col-12"}
          [:div.relative.mx-auto
           {:style {:max-width "750px"}}
           (component/build carousel/component
                            {:slides   (map-indexed gallery-slide gallery-images)
                             :settings {:initialSlide (or selected-image-index 0)
                                        :slidesToShow 1}}
                            {})
           [:div.absolute
            {:style {:top "1.5rem" :right "1.5rem"}}
            (ui/modal-close {:class       "stroke-dark-gray fill-gray"
                             :close-attrs close-attrs})]])))]))

(defn stylist-cards-component
  [{:keys [stylist-gallery-index gallery-image-index stylists]} owner opts]
  (component/create
   [:div.px3.p1.bg-white.flex-wrap.flex.justify-center
    [:div.col-12.py2.flex-wrap.flex.justify-center
     (map-indexed
      (fn [index stylist]
        (stylist-card
         {:selected-image-index   gallery-image-index
          :current-stylist-index  index
          :gallery-open?          (= stylist-gallery-index index)}
         stylist
         (stylist-detail-line/query stylist)))
      stylists)]]))

(defn ^:private query [data]
  (let [post-purchase? (get-in data storefront.keypaths/completed-order)
        current-step   (if-not post-purchase? 2 3)]
    (cond->
        {:current-step                  current-step
         :header-data                   {:title                   "Find Your Stylist"
                                         :progress                progress/stylist-results
                                         :back-navigation-message [events/navigate-adventure-how-far]
                                         :subtitle                (str "Step " current-step " of 3")}
         :card-data                     {:stylist-gallery-index (get-in data keypaths/adventure-stylist-gallery-index)
                                         :gallery-image-index   (get-in data keypaths/adventure-stylist-gallery-image-index)
                                         :stylists              (get-in data keypaths/adventure-matched-stylists)}
         :escape-hatch/navigation-event events/navigate-adventure-shop-hair
         :escape-hatch/copy             "Shop hair"
         :escape-hatch/data-test        "shop-hair"}

      post-purchase?
      (merge
       {:escape-hatch/navigation-event events/navigate-adventure-let-mayvenn-match
        :escape-hatch/copy             "Let Mayvenn Match"
        :escape-hatch/data-test        "let-mayvenn-match"}))))

(defn ^:private component
  [{:keys [header-data card-data] :as data} _ _]
  (component/create
   (when (-> card-data :stylists seq)
     [:div.center.flex-auto.bg-light-lavender
      [:div.white
       (when header-data
         (header/built-component header-data nil))]
      [:div
       [:div.flex.items-center.bold.bg-light-lavender
        {:style {:height "75px"}}]
       [:div.bg-white
        [:div.flex.flex-auto.justify-center.pt6
         [:div.h3.bold.purple "Pick your stylist"]]
        [:div (component/build stylist-cards-component card-data nil)]
        (let [{:escape-hatch/keys [navigation-event copy data-test]} data]
          [:div.h6.dark-gray.mt3.pb4
           [:div.col-7-on-tb-dt.col-9.mx-auto.mb1
            "Not ready to pick a stylist? Let a Mayvenn expert find one for you after you buy hair."]
           [:a.teal.medium (merge {:data-test data-test} (utils/route-to navigation-event)) copy]])]]])))

(defn built-component
  [data opts]
  (component/build component (query data) opts))

(defmethod transitions/transition-state events/control-adventure-stylist-gallery-open [_ _event {:keys [stylist-gallery-index image-index]} app-state]
  (-> app-state
      (assoc-in keypaths/adventure-stylist-gallery-open? true)
      (assoc-in keypaths/adventure-stylist-gallery-index (or stylist-gallery-index 0))
      (assoc-in keypaths/adventure-stylist-gallery-image-index (or image-index 0))))

(defmethod transitions/transition-state events/control-adventure-stylist-gallery-close [_ _event _args app-state]
  (-> app-state
      (assoc-in keypaths/adventure-stylist-gallery-open? false)
      (update-in keypaths/adventure-stylist-gallery dissoc :index)
      (update-in keypaths/adventure-stylist-gallery dissoc :image-index)))

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
