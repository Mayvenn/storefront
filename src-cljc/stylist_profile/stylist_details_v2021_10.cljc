(ns stylist-profile.stylist-details-v2021-10
  "Stylist details (profile) that includes a map and gallery."
  (:require adventure.keypaths
            [adventure.stylist-matching.maps :as maps]
            [api.catalog :refer [select ?discountable-install ?service]]
            api.current
            api.orders
            api.products
            api.stylist
            [mayvenn.visual.tools :refer [with within]]
            [clojure.string :as string]
            spice.core
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as c]
            [storefront.components.formatters :as f]
            [storefront.components.header :as header]
            [storefront.components.money-formatters :as mf]
            [storefront.components.ui :as ui]
            [storefront.events :as e]
            storefront.keypaths
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            stylist-directory.keypaths
            ;; NEW
            [stylist-profile.ui-v2021-10.card :as card-v2]
            ;; OLD
            [stylist-profile.ui.carousel :as carousel]
            [stylist-profile.ui.experience :as experience]
            [stylist-profile.ui.footer :as footer]
            [stylist-profile.ui.ratings-bar-chart :as ratings-bar-chart]
            [stylist-profile.ui.specialties-shopping :as specialties-shopping]
            [stylist-profile.ui.sticky-select-stylist :as sticky-select-stylist]
            [stylist-profile.ui.stylist-reviews :as stylist-reviews]
            [storefront.utils.query :as query]))

;; ---------------------------- display

(def ^:private clear-float-atom [:div.clearfix])

(c/defcomponent template
  [{:keys [adv-header
           card
           carousel
           experience
           footer
           google-maps
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
    (c/build card-v2/organism card)
    [:div.my2.px3
     (carousel/organism carousel)]
    (c/build maps/component google-maps)
    [:div.my2.m1-on-tb-dt.mb2-on-tb-dt.px3
     (c/build experience/organism experience)
     (c/build specialties-shopping/organism specialties-discountable)]
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
    :sticky-select-stylist.cta/target [e/flow|stylist-matching|matched
                                       {:stylist      (:diva/stylist detailed-stylist)
                                        :result-index 0}]
    :sticky-select-stylist.cta/label  (str
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
  [{:stylist/keys         [id name portrait salon slug experience]
    :stylist.address/keys [city state]
    :stylist.rating/keys  [cardinality publishable? score]
    :as                   stylist}]
  (merge
   (within :hero
           {:background/ucare-id      "3cff075a-977d-4b69-841f-2bd497446b58"
            :circle-portrait/portrait portrait
            :title/id                 "stylist-name"
            :title/primary            name
            :star-bar/id              "star-bar"
            :star-bar/value           score
            :star-bar/opts            {:class "fill-p-color"}
            :review-count             cardinality
            :salon/id                 "salon-name"
            :salon/primary            salon
            :salon/location           (str city ", " state)})
   {:star-rating/id    (str "rating-count-" slug)
    :star-rating/value score}
   (within :laurels
           {:points [{:icon    [:svg/calendar {:style {:height "1.2em"}
                                               :class "fill-s-color mr1"}]
                      :primary (str (ui/pluralize-with-amount experience "year") " of experience")}
                     {:icon    [:svg/mayvenn-logo {:style {:height "1.2em"}
                                                   :class "fill-s-color mr1"}]
                      :primary (if (>= cardinality 3)
                                 (str "Booked " cardinality " times")
                                 "New Mayvenn stylist")}
                     {:icon    [:svg/experience-badge {:style {:height "1.2em"}
                                                       :class "fill-s-color mr1"}]
                      :primary "Professional salon"}
                     {:icon    [:svg/certified {:style {:height "1.2em"}
                                                :class "fill-s-color mr1"}]
                      :primary "State licensed stylist"}]})
   (within :cta
           {:id      "stylist-profile-cta"
            :primary (str "Select " (-> stylist :diva/stylist :store-nickname))
            :target  [e/flow|stylist-matching|matched {:stylist      (:diva/stylist stylist)
                                                       :result-index 0}]})))

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
                                  (string/join ", "))})

(def service->requirement-copy
  {"SV2-LBI-X" "FREE with purchase of 3+ Bundles"
   "SV2-CBI-X" "FREE with purchase of 2+ Bundles and a Closure"
   "SV2-FBI-X" "FREE with purchase of 2+ Bundles and a Lace Frontal"
   "SV2-3BI-X" "FREE with purchase of 2+ Bundles and a 360 Frontal"
   "SV2-WGC-X" "FREE with purchase of a Lace Front Wig or a 360 Lace Wig"})

(defn ^:private service-sku-query
  [{:sku/keys                   [title price]
    :catalog/keys               [sku-id]}]
  {:id         sku-id
   :title      title
   :subtitle   (str "(" (mf/as-money price) ")")
   :content    (get service->requirement-copy sku-id)})

(defn ^:private shop-discountable-services<-
  [skus-db
   {:stylist.services/keys [offered-sku-ids
                            offered-ordering]}]
  (when (seq offered-sku-ids)
    (let [offered-services (->> (vals skus-db)
                                (select ?discountable-install)
                                (filter (fn[service-sku]
                                          (= 1 (count (:service-awards/offered-service-slugs service-sku)))))
                                (filter (comp offered-sku-ids :legacy/derived-from-sku))
                                (sort-by (comp offered-ordering :catalog/sku-id))
                                not-empty)]
      (when offered-services
        {:specialties-shopping.title/id      "free-mayvenn-services"
         :specialties-shopping.title/primary "Free Mayvenn Services"
         :specialties-shopping/specialties
         (map service-sku-query offered-services)}))))

(defn query
  [state]
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

        hide-star-distribution?            (experiments/hide-star-distribution? state)
        newly-added-stylist-ui-experiment? (and (experiments/stylist-results-test? state)
                                                (or (experiments/just-added-only? state)
                                                    (experiments/just-added-experience? state)))
        instagram-stylist-profile?         (experiments/instagram-stylist-profile? state)
        ;; Requestings
        fetching-reviews?                  (utils/requesting? state request-keys/fetch-stylist-reviews)]
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

              :card                     (within :stylist-profile.card (card<- detailed-stylist))
              :ratings-bar-chart        (ratings-bar-chart<- hide-star-distribution?
                                                             detailed-stylist)
              :experience               (experience<- detailed-stylist)
              :google-maps              (maps/map-query state)
              :sticky-select-stylist    (sticky-select-stylist<- current-stylist
                                                                 detailed-stylist)
              :specialties-discountable (shop-discountable-services<-
                                         skus-db
                                         detailed-stylist)}))))

(defn ^:export built-component
  [app-state]
  (c/build template (query app-state)))
