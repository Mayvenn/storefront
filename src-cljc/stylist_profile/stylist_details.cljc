(ns stylist-profile.stylist-details
  "Stylist details (profile) that includes a map and gallery."
  (:require #?@(:cljs
                [[storefront.api :as api]
                 [storefront.browser.tags :as tags]
                 [storefront.hooks.facebook-analytics :as facebook-analytics]
                 [storefront.hooks.google-maps :as google-maps]
                 [storefront.hooks.seo :as seo]])
            [adventure.stylist-matching.maps :as maps]
            adventure.keypaths
            api.orders
            [catalog.services :as services]
            [clojure.string :refer [join]]
            spice.core
            [spice.selector :as selector]
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as c]
            [storefront.components.formatters :as f]
            [storefront.components.header :as header]
            [storefront.components.money-formatters :as mf]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as fx]
            [storefront.events :as e]
            [storefront.trackings :as trackings]
            [storefront.transitions :as t]
            storefront.keypaths
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.request-keys :as request-keys]
            stylist-directory.keypaths
            [stylist-profile.cart-swap :as cart-swap]
            [stylist-profile.stylist :as stylist]
            [stylist-profile.ui.card :as card]
            [stylist-profile.ui.carousel :as carousel]
            [stylist-profile.ui.experience :as experience]
            [stylist-profile.ui.footer :as footer]
            [stylist-profile.ui.ratings-bar-chart :as ratings-bar-chart]
            [stylist-profile.ui.specialties-list :as specialties-list]
            [stylist-profile.ui.specialties-shopping :as specialties-shopping]
            [stylist-profile.ui.sticky-select-stylist :as sticky-select-stylist]
            [stylist-profile.ui.stylist-reviews :as stylist-reviews]))

(def ^:private select
  (comp seq (partial selector/match-all {:selector/strict? true})))

;; ---------------------------- behavior

(defmethod t/transition-state e/navigate-adventure-stylist-profile
  [_ _ {:keys [stylist-id]} app-state]
  (-> app-state
      ;; TODO(corey) use model?
      (assoc-in adventure.keypaths/stylist-profile-id (spice.core/parse-double stylist-id))
      (assoc-in stylist-directory.keypaths/paginated-reviews nil)))

(defmethod fx/perform-effects e/navigate-adventure-stylist-profile
  [_ _ {:keys [stylist-id]} _ state]
  (let [cache (get-in state storefront.keypaths/api-cache)]
    #?(:cljs
       (tags/add-classname ".kustomer-app-icon" "hide"))
    #?(:cljs
       (api/fetch-stylist-details cache
                                  stylist-id))
    #?(:cljs
       (api/fetch-stylist-reviews cache
                                  {:stylist-id stylist-id
                                   :page       1}))
    #?(:cljs
       (seo/set-tags state))
    #?(:cljs
       (google-maps/insert))))

(defmethod trackings/perform-track e/navigate-adventure-stylist-profile
  [_ event {:keys [stylist-id]} app-state]
  #?(:cljs
     (facebook-analytics/track-event "ViewContent"
                                     {:content_type "stylist"
                                      :content_ids [(spice.core/parse-int stylist-id)]})))

(defmethod fx/perform-effects e/share-stylist
  [_ _ {:keys [url text title stylist-id]} _]
  #?(:cljs
     (.. (js/navigator.share (clj->js {:title title
                                       :text  text
                                       :url   url}))
         (then  (fn []
                  (handle-message e/api-success-shared-stylist
                                  {:stylist-id stylist-id})))
         (catch (fn [err]
                  (handle-message e/api-failure-shared-stylist
                                  {:stylist-id stylist-id
                                   :error      (.toString err)}))))))

(defmethod fx/perform-effects e/control-fetch-stylist-reviews
  [dispatch event args prev-app-state app-state]
  #?(:cljs
     (api/fetch-stylist-reviews (get-in app-state storefront.keypaths/api-cache)
                                {:stylist-id (get-in app-state adventure.keypaths/stylist-profile-id)
                                 :page       (-> (get-in app-state stylist-directory.keypaths/paginated-reviews)
                                                 :current-page
                                                 (or 0)
                                                 inc)})))

(defmethod fx/perform-effects e/control-stylist-profile-add-service-to-bag
  [_ _ {:keys [quantity] intended-service :sku} _ state]
  (let [intended-stylist (stylist/detailed state)
        cart-swap        (->> (merge
                               (when intended-service
                                 {:service/intended intended-service})
                               (when intended-stylist
                                 {:stylist/intended intended-stylist}))
                              (cart-swap/cart-swap<- state))]
    (if (or (:service/swap? cart-swap)
            (:stylist/swap? cart-swap))
      (handle-message e/cart-swap-popup-show cart-swap)
      (handle-message e/add-sku-to-bag
                      {:sku           intended-service
                       :stay-on-page? true
                       :service-swap? true
                       :quantity      quantity}))))

;; ---------------------------- display

(def ^:private clear-float-atom [:div.clearfix])

(c/defcomponent template
  [{:keys [carousel
           footer
           google-maps
           header
           ratings-bar-chart
           stylist-reviews
           experience
           specialties-list
           specialties-discountable
           specialties-a-la-carte
           sticky-select-stylist
           card]} _ _]
  [:div.bg-white.col-12.mb6.stretch {:style {:margin-bottom "-1px"}}
   [:main
    (header/adventure-header header)
    (c/build card/organism card)
    (c/build maps/component google-maps)
    [:div.my2.m1-on-tb-dt.mb2-on-tb-dt.px3
     (carousel/organism carousel)
     (c/build experience/organism experience)
     (c/build specialties-list/organism specialties-list)
     (c/build specialties-shopping/organism specialties-discountable)
     (c/build specialties-shopping/organism specialties-a-la-carte)]
    clear-float-atom
    (c/build ratings-bar-chart/organism ratings-bar-chart)
    (c/build stylist-reviews/organism stylist-reviews)]
   (c/build footer/organism footer)
   (c/build sticky-select-stylist/organism sticky-select-stylist)])

(defn ^:private header<-
  [{:order.items/keys [quantity]} undo-history]
  {:header.title/id               "adventure-title"
   :header.title/primary          "Meet Your Stylist"
   :header.back-navigation/id     "adventure-back"
   :header.back-navigation/back   (not-empty (first undo-history))
   :header.back-navigation/target [e/navigate-adventure-find-your-stylist]
   :header.cart/id                "mobile-cart"
   :header.cart/value             (or quantity 0)
   :header.cart/color             "white"})

(def ^:private footer<-
  {:footer.cta/id     "browse-stylists"
   :footer.cta/label  "Browse Stylists"
   :footer.cta/target [e/navigate-adventure-find-your-stylist]
   :footer.body/copy  "Meet more stylists in your area"
   :footer.body/id    "meet-more-stylists"})

(defn ^:private sticky-select-stylist<-
  [current-stylist detailed-stylist]
  (merge
   {:sticky-select-stylist.cta/id     "select-stylist"
    :sticky-select-stylist.cta/target [e/control-adventure-select-stylist
                                       {:servicing-stylist (:diva/stylist detailed-stylist)
                                        :card-index        0}]
    :sticky-select-stylist.cta/label (str
                                      (cond
                                        (empty? current-stylist)              "Select "
                                        (not= (:stylist/id current-stylist)
                                              (:stylist/id detailed-stylist)) "Switch to "
                                        :else                                 "Continue with ")
                                      (:stylist/name detailed-stylist))}))

(defn ^:private carousel<-
  [{:stylist/keys [slug id] :stylist.gallery/keys [images]}]
  {:carousel/items (->> images
                        (map-indexed
                         (fn [j {:keys [resizable-url]}]
                           {:key            (str "gallery-img-" id "-" j)
                            :ucare-img-url  resizable-url
                            :target-message [e/navigate-adventure-stylist-gallery
                                             {:stylist-id   id
                                              :store-slug   slug
                                              :query-params {:offset j}}]})))})

(defn ^:private reviews<-
  [fetching-reviews?
   {:stylist.rating/keys [publishable? score] diva-stylist :diva/stylist}
   {stylist-reviews :reviews :as paginated-reviews}]
  (when (and publishable?
             (seq stylist-reviews))
    (merge
     {:reviews/id           "stylist-reviews"
      :reviews/spinning?    fetching-reviews?
      :reviews/rating       score
      :reviews/review-count (:review-count diva-stylist)
      :reviews/reviews      (->> stylist-reviews
                                 (mapv #(update %
                                                :review-date
                                                f/short-date)))}
     (when (not= (:current-page paginated-reviews)
                 (:pages paginated-reviews))
       {:reviews/cta-id     "more-stylist-reviews"
        :reviews/cta-target [e/control-fetch-stylist-reviews]
        :reviews/cta-label  "View More"}))))

(defn ^:private card<-
  [hostname
   hide-star-distribution?
   newly-added-stylist-ui?
   {:stylist/keys         [id name portrait salon slug phone-number]
    :stylist.address/keys [city]
    :stylist.rating/keys  [cardinality publishable? score]}]
  (merge {:card.circle-portrait/portrait   portrait
          :card.transposed-title/id        "stylist-name"
          :card.transposed-title/primary   name
          :card.transposed-title/secondary salon}
         (if (and (< cardinality 3)
                  newly-added-stylist-ui?)
           {:card.just-added/id      (str "just-added-" slug)
            :card.just-added/content "Just Added"}
           {:card.star-rating/id             (str "rating-count-" slug)
            :card.star-rating/value          score
            :card.star-rating/scroll-anchor  (when (and publishable?
                                                        (pos? cardinality)
                                                        (not hide-star-distribution?))
                                               "star-distribution-table")
            :card.star-rating/rating-content (str "(" score ")")})
         {:card.phone-link/target       [;; this event is unused, removed: 09126dbb16385f72045f99836b42ce7b781a5d56
                                         e/control-adventure-stylist-phone-clicked
                                         {:stylist-id   id
                                          :phone-number phone-number}]
          :card.phone-link/phone-number phone-number
          :share-icon/target            [e/share-stylist
                                         {:stylist-id id
                                          :title      (str name " - " city)
                                          :text       (str name " is a Mayvenn Certified Stylist with top-rated reviews, great professionalism, and amazing work. "
                                                           "Check out this stylist here:")
                                          :url        (str "https://" hostname ".com/stylist/" id "-" slug
                                                           "?utm_campaign=" id "&utm_term=fi_stylist_share&utm_medium=referral")}]
          :share-icon/icon              (svg/share-icon {:height "19px"
                                                         :width  "18px"})}))

(defn ^:private ratings-bar-chart<-
  [hide-star-distribution?
   {:stylist.rating/keys [histogram maximum publishable?]}]
  (when (and publishable?
             (some pos? histogram)
             (not hide-star-distribution?))
    {:ratings-bar-chart/id "star-distribution-table"
     :ratings-bar-chart/bars
     (->> histogram
          (map-indexed
           (fn enrich-% [i quantity]
             (let [row (inc i)]
               {:ratings-bar-chart.bar/id        (str "rating-bar-" row)
                :ratings-bar-chart.bar/primary   row
                :ratings-bar-chart.bar/secondary (str "(" quantity ")")
                :ratings-bar-chart.bar/amount    (str (->> (/ quantity maximum)
                                                           double
                                                           (* 100))
                                                      "%")})))
          reverse)}))

(defn ^:private experience<-
  [hide-bookings?
   {:stylist/keys [experience setting licensed?] :stylist.rating/keys [cardinality]}]
  (merge
   {:experience.title/id      "stylist-experience"
    :experience.title/primary "Experience"
    :experience.body/primary  (->> [(some-> experience
                                            (ui/pluralize-with-amount "year"))
                                    setting
                                    (when licensed? "licensed")]
                                   (remove nil?)
                                   (join ", "))}
   (when (and (not hide-bookings?)
              (pos? cardinality))
     {:experience.body/secondary (str "Booked "
                                      (ui/pluralize-with-amount cardinality "time")
                                      " with Mayvenn")})))

(defn ^:private specialties-list<-
  [skus-db
   {:stylist.services/keys [offered-sku-ids offered-ordering]}]
  {:specialties-list.title/primary "Specialties"
   :specialties-list.title/id      "specialties-list.title"
   :specialties-list/specialties
   (->> offered-sku-ids
        (sort-by offered-ordering)
        (mapv (fn [sku-id]
                (let [service-title (:sku/title (get skus-db sku-id) "a")]
                  {:specialties-list.specialty.title/primary service-title}))))})

(defn ^:private service-sku-query
  [service-items
   adding-a-service-sku-to-bag?
   {:sku/keys                   [title price]
    :promo.mayvenn-install/keys [discountable requirement-copy]
    :catalog/keys               [sku-id] :as sku}]
  (cond-> {:id         (str "stylist-service-card-" sku-id)
           :title      title
           :subtitle   (str "(" (mf/as-money price) ")")
           :content    requirement-copy
           :cta-label  "Add"
           :cta-target [e/control-stylist-profile-add-service-to-bag
                        {:sku      sku
                         :quantity 1}]}
    (true? (first discountable))
    (merge {:subtitle "(Free)"})

    (some #(= sku-id (:catalog/sku-id %))
          service-items)
    (merge {:disabled? true
            :cta-label "Added"})

    adding-a-service-sku-to-bag?
    (merge {:disabled? true})))

(defn ^:private shop-discountable-services<-
  [skus-db {:order/keys [items]} adding-a-service-sku-to-bag?
   {:stylist.services/keys [offered-sku-ids offered-ordering]}]
  (when (seq offered-sku-ids)
    (let [services-items   (select services/discountable items)
          offered-services (->> (vals skus-db)
                                (select services/discountable)
                                (filter (comp offered-sku-ids
                                              :catalog/sku-id))
                                (sort-by (comp offered-ordering
                                               :catalog/sku-id))
                                not-empty)]
      (when offered-services
        {:specialties-shopping.title/id      "free-mayvenn-services"
         :specialties-shopping.title/primary "Free Mayvenn Services"
         :specialties-shopping/specialties
         (map (partial service-sku-query
                       services-items
                       adding-a-service-sku-to-bag?)
              offered-services)}))))

(defn ^:private shop-a-la-carte-services<-
  [skus-db {:order/keys [items]} adding-a-service-sku-to-bag?
   {:stylist.services/keys [offered-sku-ids offered-ordering]}]
  (when offered-sku-ids
    (let [services-items   (select services/a-la-carte items)
          offered-services (->> (vals skus-db)
                                (select services/a-la-carte)
                                (filter (comp offered-sku-ids
                                              :catalog/sku-id))
                                (sort-by (comp offered-ordering
                                               :catalog/sku-id))
                                not-empty)]
      (when offered-services
        {:specialties-shopping.title/id      "a-la-carte-services"
         :specialties-shopping.title/primary "À La Carte Services"
         :specialties-shopping/specialties
         (map (partial service-sku-query
                       services-items
                       adding-a-service-sku-to-bag?)
              offered-services)}))))

;; TODO(corey) Stylist not found template? currently redirects to find-your-stylist

(defn page
  [state _]
  (let [skus-db           (get-in state storefront.keypaths/v2-skus)
        current-order     (api.orders/current state)
        current-stylist   (stylist/current state)
        detailed-stylist  (stylist/detailed state)
        paginated-reviews (get-in state stylist-directory.keypaths/paginated-reviews)

        ;; Navigation
        {host-name :host} (get-in state storefront.keypaths/navigation-uri)
        undo-history      (get-in state storefront.keypaths/navigation-undo-stack)

        ;; Feature flags
        hide-bookings?                     (experiments/hide-bookings? state)
        hide-star-distribution?            (experiments/hide-star-distribution? state)
        shop-stylist-profile?              (experiments/shop-stylist-profile? state)
        newly-added-stylist-ui-experiment? (and (experiments/stylist-results-test? state)
                                                (or (experiments/just-added-only? state)
                                                    (experiments/just-added-experience? state)))

        ;; Requestings
        fetching-reviews?            (utils/requesting? state request-keys/fetch-stylist-reviews)
        adding-a-service-sku-to-bag? (utils/requesting? state
                                                        (fn [req]
                                                          (vec (first req)))
                                                        request-keys/add-to-bag)]
    (c/build template
             (merge {:header (header<- current-order undo-history)
                     :footer footer<-}
                    (when detailed-stylist
                      {:carousel              (carousel<- detailed-stylist)
                       :reviews               (reviews<- fetching-reviews?
                                                         detailed-stylist
                                                         paginated-reviews)
                       :card                  (card<- host-name
                                                      hide-star-distribution?
                                                      newly-added-stylist-ui-experiment?
                                                      detailed-stylist)
                       :ratings-bar-chart     (ratings-bar-chart<- hide-star-distribution?
                                                                   detailed-stylist)
                       :experience            (experience<- hide-bookings?
                                                            detailed-stylist)
                       :google-maps           (maps/map-query state)
                       :sticky-select-stylist (sticky-select-stylist<- current-stylist
                                                                       detailed-stylist)})
                    (if shop-stylist-profile?
                      {:specialties-discountable (shop-discountable-services<-
                                                  skus-db
                                                  current-order
                                                  adding-a-service-sku-to-bag?
                                                  detailed-stylist)
                       :specialties-a-la-carte   (shop-a-la-carte-services<-
                                                  skus-db
                                                  current-order
                                                  adding-a-service-sku-to-bag?
                                                  detailed-stylist)}
                      {:specialties-list (specialties-list<- skus-db
                                                             detailed-stylist)})))))
