(ns stylist-profile.stylist-details-v2021-10
  "Stylist details (profile) that includes a map and gallery."
  (:require adventure.keypaths
            [adventure.stylist-matching.maps :as maps]
            [api.catalog :refer [select ?discountable-install ?service]]
            api.current
            api.orders
            api.products
            api.stylist
            [mayvenn.visual.ui.dividers :as ui-dividers]
            [mayvenn.visual.tools :refer [within with]]
            [clojure.string :as string]
            spice.core
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as c]
            [storefront.components.formatters :as f]
            [storefront.components.header :as header]
            [storefront.components.ui :as ui]
            [storefront.events :as e]
            storefront.keypaths
            stylist-directory.keypaths

            ;; NEW
            [stylist-profile.ui-v2021-10.card :as card-v2]
            [stylist-profile.ui-v2021-10.sticky-select-stylist :as sticky-select-stylist-v2]
            [stylist-profile.ui-v2021-10.gallery :as gallery]
            [stylist-profile.ui-v2021-10.services-offered :as services-offered]
            [stylist-profile.ui-v2021-10.stylist-reviews-cards :as stylist-reviews-cards-v2]

            ;; OLD
            [stylist-profile.ui.footer :as footer]))

;; ---------------------------- display

(def ^:private clear-float-atom [:div.clearfix])

(defn licenses-molecule
  [{:keys [id primary secondary]}]
  (c/html
   [:div.pt5
    [:div.title-3.proxima.shout
     {:data-test id
      :key       id}
     primary]
    [:div
     secondary]]))

(c/defcomponent template
  [{:keys [adv-header
           card
           footer
           gallery
           google-maps
           mayvenn-header
           sticky-select-stylist
           stylist-reviews] :as data} _ _]
  [:div.bg-white.col-12.mb6.stretch {:style {:margin-bottom "-1px"}}
   [:main
    (when mayvenn-header
      (c/build header/mobile-nav-header-component mayvenn-header))
    (when adv-header
      (header/adventure-header adv-header))
    (c/build card-v2/organism card)
    (c/build gallery/organism gallery)
    [:div.my2.m1-on-tb-dt.mb2-on-tb-dt.px3
     (c/build services-offered/organism (with :services-offered data))
     (licenses-molecule (with :licenses data))]
    (c/build maps/component-v2 google-maps)
    clear-float-atom
    (c/build stylist-reviews-cards-v2/organism stylist-reviews)
    ui-dividers/green]
   (c/build footer/organism footer)
   (c/build sticky-select-stylist-v2/organism sticky-select-stylist)])

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

(defn ^:private gallery<-
  [{:stylist/keys [slug id social-media] :stylist.gallery/keys [images]}
   instagram-stylist-profile?]
  (merge
   (when-let [ig-username (and instagram-stylist-profile? (:instagram social-media))]
     {:gallery.instagram/id     "instagram"
      :gallery.instagram/target [e/external-redirect-instagram-profile {:ig-username ig-username}]})
   {:gallery/items              (->> images
                                     (map-indexed
                                      (fn [j {:keys [resizable-url]}]
                                        {:key            (str "gallery-img-" id "-" j)
                                         :ucare-img-url  resizable-url
                                         :target-message [e/navigate-adventure-stylist-gallery
                                                          {:stylist-id   id
                                                           :store-slug   slug
                                                           :query-params {:offset j}}]})))
    :gallery/target [e/navigate-adventure-stylist-gallery
                     {:stylist-id   id
                      :store-slug   slug}]}))

(defn ^:private reviews<-
  [{:stylist.rating/keys [publishable? score] diva-stylist :diva/stylist}
   {stylist-reviews :reviews :as paginated-reviews}]
  (let [max-reviews-shown 3
        stylist-id        (:stylist-id diva-stylist)
        store-slug        (:store-slug diva-stylist)
        review-count      (:review-count diva-stylist)]
    (when (and publishable?
               (seq stylist-reviews))
      (merge
       {:reviews/id           "stylist-reviews"
        :reviews/rating       score
        :reviews/review-count review-count
        :reviews/reviews      (->> stylist-reviews
                                   (map-indexed (fn [ix review]
                                                  (-> review
                                                      (update :review-date f/short-date)
                                                      (assoc :target [e/navigate-adventure-stylist-profile-reviews
                                                                      {:stylist-id   stylist-id
                                                                       :store-slug   store-slug
                                                                       :query-params {:offset ix}}]))))
                                   (take max-reviews-shown))}
       (when (> review-count max-reviews-shown)
         {:reviews/cta-id     "show-all-stylist-reviews"
          :reviews/cta-target [e/navigate-adventure-stylist-profile-reviews
                               {:stylist-id stylist-id
                                :store-slug store-slug}]
          :reviews/cta-label  (str "Show all " review-count " reviews")})))))

(defn ^:private card<-
  [{:stylist/keys         [name portrait salon slug experience]
    :stylist.address/keys [city state]
    :stylist.rating/keys  [cardinality decimal-score score]
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
    :star-rating/value decimal-score}
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

(defn ^:private service-sku-query
  [{:product/keys                   [essential-title]
    :catalog/keys               [sku-id]}]
  {:id         sku-id
   :title      essential-title})

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
        (within :services-offered
                {:title/id "free-mayvenn-services"
                 :title/primary "Services Offered"
                 :services (map service-sku-query offered-services)})))))

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
        undo-history              (get-in state storefront.keypaths/navigation-undo-stack)
        from-cart-or-direct-load? (or (= (first (:navigation-message (first undo-history))) e/navigate-cart)
                                      (nil? (first undo-history)))

        instagram-stylist-profile? (experiments/instagram-stylist-profile? state)]
    (merge {:footer footer<-}
           (if from-cart-or-direct-load?
             {:mayvenn-header {:forced-mobile-layout? true
                               :quantity              (or (:order.items/quantity current-order) 0)}}
             {:adv-header (header<- current-order undo-history)})
           (when detailed-stylist
             (merge
              (shop-discountable-services<- skus-db detailed-stylist)
              {:licenses/id        "stylist-license"
               :licenses/primary   "Licenses / Certifications"
               :licenses/secondary "Cosmetology license, Mayvenn Certified"
               :gallery            (gallery<- detailed-stylist instagram-stylist-profile?)
               :stylist-reviews    (reviews<- detailed-stylist paginated-reviews)

               :card                  (within :stylist-profile.card (card<- detailed-stylist))
               :experience            (experience<- detailed-stylist)
               :google-maps           (maps/map-query state)
               :sticky-select-stylist (sticky-select-stylist<- current-stylist
                                                               detailed-stylist)})))))

(defn ^:export built-component
  [app-state]
  (c/build template (query app-state)))
