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
            [mayvenn.visual.ui.dividers :as ui-dividers]
            [mayvenn.visual.tools :refer [within with]]
            [clojure.string :as string]
            spice.core
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.sites :as sites]
            [storefront.component :as c]
            [storefront.components.formatters :as f]
            [storefront.components.header :as header]
            [storefront.components.ui :as ui]
            [storefront.effects :as fx]
            [storefront.events :as e]
            storefront.keypaths
            [storefront.platform.messages :refer [handle-message]]
            [storefront.trackings :as trackings]
            [storefront.transitions :as t]
            stylist-directory.keypaths
            [storefront.components.svg :as svg]
            [stylist-profile.ui-v2021-10.card :as card-v2]
            [stylist-profile.ui-v2021-10.sticky-select-stylist :as sticky-select-stylist-v2]
            [stylist-profile.ui-v2021-10.gallery :as gallery]
            [stylist-profile.ui-v2021-10.services-offered :as services-offered]
            [stylist-profile.ui-v2021-10.stylist-reviews-cards :as stylist-reviews-cards-v2]
            [stylist-profile.ui-v2021-12.stylist-reviews-cards :as stylist-reviews-cards-v3]
            [stylist-profile.ui.footer :as footer]))

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
      (handle-message e/biz|follow|defined
                      {:follow/after-id e/flow|stylist-matching|matched
                       :follow/then     [e/post-stylist-matched-navigation-decided
                                         {:decision
                                          {:booking e/navigate-adventure-appointment-booking
                                           :cart    e/navigate-cart
                                           :success e/navigate-adventure-match-success}}]})
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

;; ---------------------------- display

(def ^:private clear-float-atom [:div.clearfix])

(defn licenses-molecule
  [{:keys [id primary secondary]}]
  (c/html
   [:div.pt5
    [:div.title-2.proxima.shout
     {:data-test id
      :key       id}
     primary]
    [:div
     secondary]]))

(defn sub-template
  [{:keys [adv-header
           card
           footer
           gallery
           google-maps
           mayvenn-header
           sticky-select-stylist
           stylist-reviews
           desktop?] :as data}]
  [:div.bg-white.col-12.mb6.stretch
   {:style {:margin-bottom "-1px"
            ;; For full-width:
            :margin-left   "-50vw"
            :margin-right  "-50vw"
            :right         "49%"
            :left          "49%"
            :width         "100vw"
            :position "relative"}}
   [:main
    (when mayvenn-header
      (c/build header/nav-header-component mayvenn-header))
    (when adv-header
      (header/adventure-header adv-header))
    (c/build card-v2/organism card)
    (c/build gallery/organism gallery)
    [:div.mb8.col-10.mx-auto
     (c/build services-offered/organism (with :services-offered data))
     (licenses-molecule (with :licenses data))]
    (if desktop?
      (c/build maps/component-v3 google-maps)
      (c/build maps/component-v2 google-maps))
    clear-float-atom
    [:div.hide-on-mb
     (c/build stylist-reviews-cards-v3/organism stylist-reviews)]
    [:div.hide-on-tb-dt
     (c/build stylist-reviews-cards-v2/organism stylist-reviews)]
    ui-dividers/green]
   (c/build footer/organism footer)
   (c/build sticky-select-stylist-v2/organism sticky-select-stylist)])

(c/defcomponent template
  [{:keys [desktop?] :as data} _ _]
  ;; HACK: This allows the map to render, but doesn't allow switching views
  (if desktop?
    [:div
     (sub-template data)]
    [:div.max-580.mx-auto
     (sub-template data)]))

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
  (let [items                (map-indexed
                              (fn [j {:keys [resizable-url]}]
                                {:key            (str "gallery-img-" id "-" j)
                                 :ucare-img-url  resizable-url
                                 :target-message [e/navigate-adventure-stylist-gallery {:stylist-id   id
                                                                                        :store-slug   slug
                                                                                        :query-params {:offset j}}]})
                              images)
        item-count           (count items)
        desktop-filler-count (- 6 (mod item-count 6))
        trimmed-filler-count (if (= 6 desktop-filler-count) 0 desktop-filler-count)
        desktop-items        (->> {:filler-img (svg/symbolic->html [:svg/mayvenn-logo
                                                                    {:class "fill-gray"
                                                                     :style {:height "2em"
                                                                             :width  "2em"}}])}
                                  repeat
                                  (take trimmed-filler-count)
                                  (map-indexed
                                   (fn [j m]
                                     (assoc m :key (->> item-count (+ j) dec (str "gallery-img-" id "-")))))
                                  (concat items)
                                  (take 12))]
    (merge
     (when-let [ig-username (and instagram-stylist-profile? (:instagram social-media))]
       {:gallery.instagram/id     "instagram"
        :gallery.instagram/target [e/external-redirect-instagram-profile {:ig-username ig-username}]})
     ;; Only show 3, 6, 9, or 12 photos.
     {:gallery.mobile/items  (-> items count (quot 3) (* 3) (min 12) (take items))
      :gallery.mobile/grid-attrs "grid gap-px grid-cols-3"
      :gallery.desktop/items desktop-items
      :gallery.desktop/grid-attrs "grid gap-1 grid-cols-6"
      :gallery/target        [e/navigate-adventure-stylist-gallery
                              {:stylist-id id
                               :store-slug slug}]})))

(defn ^:private reviews<-
  [{:stylist.rating/keys [publishable? score cardinality] diva-stylist :diva/stylist}
   {stylist-reviews :reviews :as paginated-reviews}
   desktop?]
  (let [max-reviews-shown         3
        max-desktop-reviews-shown 6
        stylist-id                (:stylist-id diva-stylist)
        store-slug                (:store-slug diva-stylist)
        review-count              (:count paginated-reviews)]
    (when (and publishable?
               (seq stylist-reviews))
      (merge
       {:reviews/id           (if desktop? "stylist-reviews-desktop" "stylist-reviews-mobile")
        :reviews/rating       score
        :reviews/rating-count cardinality
        :reviews/reviews      (->> stylist-reviews
                                   (map-indexed (fn [ix review]
                                                  (-> review
                                                      (update :review-date f/short-date)
                                                      (assoc :target [e/navigate-adventure-stylist-profile-reviews
                                                                      {:stylist-id   stylist-id
                                                                       :store-slug   store-slug
                                                                       :query-params {:offset ix}}]))))
                                   (take max-reviews-shown))
        :reviews/desktop      (->> stylist-reviews
                                   (map-indexed (fn [ix review]
                                                  (-> review
                                                      (update :review-date f/short-date)
                                                      (assoc :target [e/navigate-adventure-stylist-profile-reviews
                                                                      {:stylist-id   stylist-id
                                                                       :store-slug   store-slug
                                                                       :query-params {:offset ix}}]))))
                                   (take max-desktop-reviews-shown))}
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
           {:background/ucare-id         "72cb3389-444e-4f89-9ec2-4d845956d27c"
            :background/desktop-ucare-id "ec525315-6869-4e75-9d81-59e57dcb0423"
            :circle-portrait/portrait    portrait
            :title/id                    "stylist-name"
            :title/primary               name
            :star-bar/id                 "star-bar"
            :star-bar/value              score
            :star-bar/opts               {:class "fill-p-color"}
            :rating-count                cardinality
            :salon/id                    "salon-name"
            :salon/primary               salon
            :salon/location              (str city ", " state)})
   {:star-rating/id    (str "rating-count-" slug)
    :star-rating/value decimal-score}
   (within :laurels
           {:points [{:icon    [:svg/calendar {:style {:height "1.2em"}
                                               :class "fill-s-color mr1"}]
                      :primary (str (ui/pluralize-with-amount experience "year") " experience")}
                     {:icon    [:svg/mayvenn-logo {:style {:height "1.2em"}
                                                   :class "fill-s-color mr1"}]
                      :primary (if (>= cardinality 3)
                                 (str cardinality " bookings")
                                 "New Mayvenn stylist")}
                     {:icon    [:svg/experience-badge {:style {:height "1.2em"}
                                                       :class "fill-s-color mr1"}]
                      :primary "Professional salon"}
                     {:icon    [:svg/certified {:style {:height "1.2em"}
                                                :class "fill-s-color mr1"}]
                      :primary "State licensed"}]})
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

        instagram-stylist-profile? (experiments/instagram-stylist-profile? state)
        desktop?                   #?(:cljs (> (.-innerWidth js/window) 749)
                                   :clj nil)]
    (merge {:footer footer<-}
           (if from-cart-or-direct-load?
             {:mayvenn-header {:forced-mobile-layout? true
                               :quantity              (or (:order.items/quantity current-order) 0)}}
             {:adv-header (header<- current-order undo-history)})
           (when detailed-stylist
             (merge
              (shop-discountable-services<- skus-db detailed-stylist)
              {:desktop?           desktop?
               :licenses/id        "stylist-license"
               :licenses/primary   "Licenses / Certifications"
               :licenses/secondary "Cosmetology license, Mayvenn Certified"
               :gallery            (gallery<- detailed-stylist instagram-stylist-profile?)
               :stylist-reviews    (reviews<- detailed-stylist paginated-reviews desktop?)

               :card                  (within :stylist-profile.card (card<- detailed-stylist))
               :experience            (experience<- detailed-stylist)
               :google-maps           (maps/map-query state)
               :sticky-select-stylist (sticky-select-stylist<- current-stylist
                                                               detailed-stylist)})))))

(defn ^:export built-component
  [app-state]
  (c/build template (query app-state)))
