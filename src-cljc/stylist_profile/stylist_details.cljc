(ns stylist-profile.stylist-details
  "Stylist details (profile) that includes a map and gallery."
  (:require #?@(:cljs
                [[storefront.api :as api]
                 [storefront.browser.tags :as tags]
                 [storefront.hooks.facebook-analytics :as facebook-analytics]
                 [storefront.hooks.google-maps :as google-maps]
                 [storefront.hooks.seo :as seo]])
            adventure.keypaths
            [adventure.stylist-matching.maps :as maps]
            [api.catalog :refer [select ?discountable-install ?service]]
            api.current
            api.orders
            api.products
            api.stylist
            [clojure.string :refer [join]]
            mayvenn.live-help.core
            [mayvenn.visual.lib.call-out-box :as call-out-box]
            spice.core
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.sites :as sites]
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
            [stylist-profile.ui.card :as card]
            [stylist-profile.ui.carousel :as carousel]
            [stylist-profile.ui.experience :as experience]
            [stylist-profile.ui.footer :as footer]
            [stylist-profile.ui.ratings-bar-chart :as ratings-bar-chart]
            [stylist-profile.ui.specialties-shopping :as specialties-shopping]
            [stylist-profile.ui.sticky-select-stylist :as sticky-select-stylist]
            [stylist-profile.ui.stylist-reviews :as stylist-reviews]))

;; ---------------------------- behavior

(defmethod t/transition-state e/navigate-adventure-stylist-profile
  [_ _ {:keys [stylist-id]} app-state]
  (-> app-state
      ;; TODO(corey) use model?
      (assoc-in adventure.keypaths/stylist-profile-id (spice.core/parse-double stylist-id))
      (assoc-in stylist-directory.keypaths/paginated-reviews nil)))

(defmethod fx/perform-effects e/navigate-adventure-stylist-profile
  [_ _ {:keys [stylist-id]} _ state]
  (if (not= :shop (sites/determine-site state))
    (fx/redirect e/navigate-home)
    (do
      (handle-message e/cache|product|requested
                      {:query ?service})
      (let [cache (get-in state storefront.keypaths/api-cache)]
        #?(:cljs
           (tags/add-classname ".kustomer-app-icon" "hide"))
        #?(:cljs
           (handle-message e/cache|stylist|requested
                           {:stylist/id stylist-id
                            :on/success #(seo/set-tags state)
                            :on/failure (fn [] (handle-message e/flash-later-show-failure
                                                               {:message
                                                                (str "The stylist you are looking for is not available. "
                                                                     "Please search for another stylist in your area below. ")})
                                          (fx/redirect e/navigate-adventure-find-your-stylist))}))
        #?(:cljs
           (api/fetch-stylist-reviews cache
                                      {:stylist-id stylist-id
                                       :page       1}))
        #?(:cljs
           (google-maps/insert))))))

(defmethod trackings/perform-track e/navigate-adventure-stylist-profile
  [_ event {:keys [stylist-id]} app-state]
  #?(:cljs
     (facebook-analytics/track-event "ViewContent"
                                     {:content_type "stylist"
                                      :content_ids [(spice.core/parse-int stylist-id)]})))

(defmethod t/transition-state e/api-success-fetch-stylist-reviews
  [_ _ paginated-reviews app-state]
  (let [existing-reviews (:reviews (get-in app-state stylist-directory.keypaths/paginated-reviews))]
    (-> app-state
        (assoc-in stylist-directory.keypaths/paginated-reviews paginated-reviews)
        (update-in (conj stylist-directory.keypaths/paginated-reviews :reviews)
                   (partial concat existing-reviews)))))

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

;; ---------------------------- display

(def ^:private clear-float-atom [:div.clearfix])

(c/defcomponent template
  [{:keys [adv-header
           card
           carousel
           experience
           footer
           google-maps
           live-help
           mayvenn-header
           ratings-bar-chart
           specialties-discountable
           sticky-select-stylist
           stylist-reviews]} _ _]
  [:div.bg-white.col-12.mb6.stretch {:style {:margin-bottom "-1px"}}
   [:main
    (when mayvenn-header
      (c/build header/mobile-nav-header-component mayvenn-header))
    (when adv-header
      (header/adventure-header adv-header))
    (c/build card/organism card)
    (c/build maps/component google-maps)
    [:div.my2.m1-on-tb-dt.mb2-on-tb-dt.px3
     (carousel/organism carousel)
     (c/build experience/organism experience)
     (c/build specialties-shopping/organism specialties-discountable)]
    clear-float-atom
    (c/build call-out-box/organism live-help)
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
  [{:stylist/keys [experience setting licensed?]}]
  {:experience.title/id      "stylist-experience"
   :experience.title/primary "Experience"
   :experience.body/primary  (->> [(some-> experience
                                           (ui/pluralize-with-amount "year"))
                                   setting
                                   (when licensed? "licensed")]
                                  (remove nil?)
                                  (join ", "))})

(defn ^:private service-sku-query
  [{:sku/keys                   [title price]
    :promo.mayvenn-install/keys [requirement-copy]
    :catalog/keys               [sku-id]}]
  {:id         sku-id
   :title      title
   :subtitle   (str "(" (mf/as-money price) ")")
   :content    requirement-copy})

(defn ^:private shop-discountable-services<-
  [skus-db
   {:stylist.services/keys [offered-sku-ids
                            offered-ordering]}]
  (when (seq offered-sku-ids)
    (let [offered-services (->> (vals skus-db)
                                (select ?discountable-install)
                                (filter (comp offered-sku-ids :catalog/sku-id))
                                (sort-by (comp offered-ordering :catalog/sku-id))
                                not-empty)]
      (when offered-services
        {:specialties-shopping.title/id      "free-mayvenn-services"
         :specialties-shopping.title/primary "Free Mayvenn Services"
         :specialties-shopping/specialties
         (map service-sku-query offered-services)}))))

(defn live-help<
  [live-help?]
  (when live-help?
    {:title/icon      nil
     :title/primary   "How can we help?"
     :title/secondary "Text now to get live help with an expert about your dream look"
     :title/target    [e/flow|live-help|opened]
     :action/id       ""
     :action/label    "Chat with us"
     :action/target   [e/flow|live-help|opened]}))

;; TODO(corey) Stylist not found template? currently redirects to find-your-stylist
(defn ^:export page
  [state _]
  (let [skus-db           (get-in state storefront.keypaths/v2-skus)
        current-order     (api.orders/current state)
        current-stylist   (api.current/stylist state)
        detailed-stylist  (api.stylist/by-id state
                                             (get-in state
                                                     adventure.keypaths/stylist-profile-id))
        paginated-reviews (get-in state stylist-directory.keypaths/paginated-reviews)

        ;; Navigation
        {host-name :host}         (get-in state storefront.keypaths/navigation-uri)
        undo-history              (get-in state storefront.keypaths/navigation-undo-stack)
        from-cart-or-direct-load? (or (= (first (:navigation-message (first undo-history))) e/navigate-cart)
                                      (nil? (first undo-history)))

        live-help?                         (experiments/live-help? state)
        hide-star-distribution?            (experiments/hide-star-distribution? state)
        newly-added-stylist-ui-experiment? (and (experiments/stylist-results-test? state)
                                                (or (experiments/just-added-only? state)
                                                    (experiments/just-added-experience? state)))

        ;; Requestings
        fetching-reviews? (utils/requesting? state request-keys/fetch-stylist-reviews)]
    (c/build template
             (merge {:footer footer<-}
                    (if from-cart-or-direct-load?
                      {:mayvenn-header {:forced-mobile-layout? true
                                        :quantity              (or (:order.items/quantity current-order) 0)}}
                      {:adv-header (header<- current-order undo-history)})
                    (when detailed-stylist
                      {:carousel                 (carousel<- detailed-stylist)
                       :stylist-reviews          (reviews<- fetching-reviews?
                                                            detailed-stylist
                                                            paginated-reviews)
                       :card                     (card<- host-name
                                                         hide-star-distribution?
                                                         newly-added-stylist-ui-experiment?
                                                         detailed-stylist)
                       :live-help                (live-help< live-help?)
                       :ratings-bar-chart        (ratings-bar-chart<- hide-star-distribution?
                                                                      detailed-stylist)
                       :experience               (experience<- detailed-stylist)
                       :google-maps              (maps/map-query state)
                       :sticky-select-stylist    (sticky-select-stylist<- current-stylist
                                                                          detailed-stylist)
                       :specialties-discountable (shop-discountable-services<-
                                                  skus-db
                                                  detailed-stylist)})))))
