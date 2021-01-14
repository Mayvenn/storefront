(ns catalog.looks
  "Shopping by Looks: index page of 'looks' for an 'album'"
  (:require #?@(:cljs [[storefront.accessors.categories :as categories]
                       [storefront.api :as api]
                       [storefront.history :as history]])
            catalog.keypaths
            clojure.string
            clojure.set
            [catalog.ui.facet-filters :as facet-filters]
            [spice.maps :as maps]
            [storefront.accessors.contentful :as contentful]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.sites :as sites]
            [storefront.effects :as effects]
            [storefront.events :as e]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.header :as header]
            [storefront.components.svg :as svg]
            [storefront.components.ugc :as component-ugc]
            [storefront.components.template :as template]
            [storefront.components.ui :as ui]
            [storefront.transitions :as t]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.ugc :as ugc]
            [spice.selector :as selector]
            [catalog.keypaths :as catalog.keypaths]
            [storefront.keypaths :as storefront.keypaths]
            [catalog.images :as catalog-images]
            [storefront.platform.messages :as messages]))

(def ^:private select
  (comp seq (partial selector/match-all {:selector/strict? true})))

;; Visual: Looks (new version under experiment)

(defn no-matches-title-molecule
  [{:no-matches.title/keys [primary secondary]}]
  [:div
   [:p.h1.py4 primary]
   [:p.h2.py6 secondary]])

(defn no-matches-action-molecule
  [{:no-matches.action/keys [primary secondary target]}]
  [:p.h4.mb10.pb10
   [:a.p-color (apply utils/fake-href target) primary]
   secondary])

(defcomponent no-matches-organism
  [data _ _]
  (when (seq data)
    [:div.col-12.my8.py4.center.bg-white
     (no-matches-title-molecule data)
     (no-matches-action-molecule data)]))

(defcomponent looks-hero-organism
  [{:looks.hero.title/keys [primary secondary]} _ _]
  [:div.center.py6.bg-warm-gray
   [:h1.title-1.canela.py3
    primary]
   (into [:p.col-10.col-6-on-tb-dt.mx-auto.proxima.content-2]
         (interpose [:br] secondary))])

(defn looks-card-hero-molecule
  [{:looks-card.hero/keys [image-url badge-url]}]
  [:div.relative
   {:style {:padding-right "3px"}}
   (ui/img {:class    "container-size"
            :style    {:object-position "50% 25%"
                       :object-fit      "cover"}
            :src      image-url
            :max-size 749})
   [:div.absolute.bottom-0.m1.justify-start
    {:style {:height "22px"
             :width  "22px"}}
    badge-url]])

(defn looks-card-title-molecule
  [{:looks-card.title/keys [primary]}]
  [:div.my2 primary])

(defcomponent looks-card-hair-item-molecule
  [{:looks-card.hair-item/keys [image-url]} _ {:keys [id]}]
  [:img.block
   {:key id
    :src image-url}])

(defcomponent looks-card-organism*
  [{:as data :looks-card/keys [height-px]
    target :looks-card.action/target} _ _]
  [:a.col-12.px1-on-tb-dt.col-4-on-tb-dt.black.pb2
   [:div.border.border-cool-gray.p2
    (apply utils/route-to target)
    [:div.flex
     {:style {:height (str height-px "px")}}
     (looks-card-hero-molecule data)
     [:div.flex.flex-column.justify-between
      (component/elements looks-card-hair-item-molecule
                          data
                          :looks-card/hair-items)]]
    (looks-card-title-molecule data)]])

(defcomponent looks-card-organism
  [data _ {:keys [id idx]}]
  (ui/screen-aware
   looks-card-organism*
   (assoc data
          :hack/above-the-fold? (zero? idx))
   {:child-handles-ref? true
    :key                id}))

(defcomponent looks-cards-organism
  [data _ _]
  (when (seq data)
    [:div.flex.flex-wrap.justify-center.justify-start-on-tb-dt.py2-on-tb-dt.px1-on-tb-dt.px3-on-mb
     (component/elements looks-card-organism
                         data
                         :looks-cards/cards)]))

(defcomponent looks-template
  [{:keys [hero filtering-summary no-matches looks-cards]} _ _]
  [:div
   (component/build looks-hero-organism hero)
   (component/build facet-filters/summary-organism filtering-summary)
   (component/build no-matches-organism no-matches)
   (component/build looks-cards-organism looks-cards)])

;; Visual: Spinning

(defcomponent spinning-template
  [_ _ _]
  (ui/large-spinner
   {:style {:height "4em"}}))

(defmethod effects/perform-effects e/populate-shop-by-look
  [_ event _ _ app-state]
  #?(:cljs
     (let [keypath [:ugc-collection :aladdin-free-install]
           cache   (get-in app-state keypaths/api-cache)]
       (api/fetch-cms-keypath
        keypath
        (fn [result]
          (messages/handle-message e/api-success-fetch-cms-keypath result)
          (when-let [cart-ids (->> (get-in result (conj keypath :looks))
                                   (mapv contentful/shared-cart-id)
                                   not-empty)]
            (api/fetch-shared-carts cache cart-ids)))))))

;; Flow Domain: Filtering Looks

(defmethod t/transition-state e/flow|looks-filtering|initialize
  [_ event args state]
  (assoc-in state catalog.keypaths/k-models-looks-filtering
            #:facet-filtering{:panel    false
                              :sections #{}
                              :filters  {}}))

(defmethod effects/perform-effects  e/flow|looks-filtering|reset
  [_ event _ _ app-state]
  #?(:cljs
     (history/enqueue-navigate e/navigate-shop-by-look
                               {:album-keyword (:album-keyword (get-in app-state keypaths/navigation-args))})))

(defmethod t/transition-state e/flow|looks-filtering|panel-toggled
  [_ _ toggled? state]
  (-> state
      (assoc-in catalog.keypaths/k-models-looks-filtering-panel toggled?)))

(defmethod t/transition-state e/flow|looks-filtering|section-toggled
  [_ _ {:as args :keys [facet-key toggled?]} state]
  (-> state
      (update-in catalog.keypaths/k-models-looks-filtering-sections
                 (fnil (if toggled? conj disj) #{})
                 facet-key)))

(defmethod effects/perform-effects e/flow|looks-filtering|filter-toggled
  [_ event {:keys [facet-key option-key toggled?]} _ app-state]
  #?(:cljs
     (let [existing-filters (get-in app-state catalog.keypaths/k-models-looks-filtering-filters)]
       (history/enqueue-navigate e/navigate-shop-by-look
                                 {:query-params  (->> existing-filters
                                                      (filter (fn [[_ v]] (seq v)))
                                                      (reduce merge {})
                                                      categories/category-selections->query-params)
                                  :album-keyword (:album-keyword (get-in app-state keypaths/navigation-args))}))))

(defmethod t/transition-state e/flow|looks-filtering|filter-toggled
  [_ _ {:keys [facet-key option-key toggled?]} state]
  (-> state
      (update-in (conj catalog.keypaths/k-models-looks-filtering-filters facet-key)
                 (fnil (if toggled? conj disj) #{})
                 option-key)))

;; Biz domains -> Viz domains

(defn no-matches<-
  [looks {:facet-filtering/keys [filters]}]
  (when (empty? (select filters looks))
    {:no-matches.title/primary    "😞"
     :no-matches.title/secondary  "Sorry, we couldn’t find any matches."
     :no-matches.action/primary   "Clear all filters"
     :no-matches.action/secondary " to see more looks."
     :no-matches.action/target    [e/flow|looks-filtering|reset]}))

(defn ^:private looks-card<-
  [images-db {:look/keys [title hero-imgs skus navigation-message]}]
  (let [height-px 240
        gap-px    3]
    {:looks-card.title/primary  title
     :looks-card.hero/image-url (:url (first hero-imgs))
     :looks-card.hero/badge-url (:platform-source (first hero-imgs))
     :looks-card.action/target  navigation-message
     :looks-card.hero/gap-px    gap-px
     :looks-card/height-px      height-px
     :looks-card/hair-items     (->> skus
                                     (map (fn [sku]
                                            (let [img-count (count skus)
                                                  gap-count (dec img-count)
                                                  img-px    (-> height-px
                                                                ;; remove total gap space
                                                                (- (* gap-px gap-count))
                                                                ;; divided among images
                                                                (/ img-count)
                                                                ;; rounded up
                                                                #?(:clj  identity
                                                                   :cljs Math/ceil))
                                                  ucare-id  (:ucare/id (catalog-images/image images-db "cart" sku))]
                                              {:looks-card.hair-item/image-url
                                               (str "https://ucarecdn.com/"
                                                    ucare-id
                                                    "/-/format/auto/-/scale_crop/"
                                                    img-px "x" img-px "/center/")}))))}))

(defn looks-cards<-
  [images-db looks {:facet-filtering/keys [filters]}]
  {:looks-cards/cards
   (->> (select filters looks)
        (mapv (partial looks-card<- images-db)))})

(def ^:private looks-hero<-
  {:looks.hero.title/primary   "Shop by Look"
   :looks.hero.title/secondary ["Get 3 or more hair items and receive a service for FREE"
                                "#MayvennMade"]})

;; Biz Domain: Looks

(defn ^:private options|name->slug
  "Used to enrich contentful looks to be selectable"
  [facets-db]
  (->> (vals facets-db)
       (mapcat :facet/options)
       (mapv (fn [[slug option]]
               [(:option/name option) slug]))
       (into {})))

(defn ^:private looks<-
  "
  What is a look?

  A) a cart - selectable sku-items (sku x quantity)
  A.1) images derived from those sku-items
  A.2) a price derived from those sku-items
  A.3) a title derived from those sku-items
  B) image(s) of the finished look
  C) social media links
  D) promotions to apply

  Usages

  - *A* needs to be selected upon as represented as a unioned sku-items
    e.g. (select criteria (attr-value-merge sku-items))
    This is for filtering the grid-list view

  - Displaying a card for the grid-list view

  - Displaying the detailed view
  "
  [state skus-db facets-db looks-shared-carts-db album-keyword]
  (let [name->slug       (options|name->slug facets-db)
        contentful-looks (->> (get-in state keypaths/cms-ugc-collection)
                              ;; NOTE(corey) This is hardcoded because obstensibly
                              ;; filtering should replace albums
                              :aladdin-free-install
                              :looks)]
    (->> contentful-looks
         (keep (fn [{:as look :keys [color origin texture]}]
                 (when-let [shared-cart-id (contentful/shared-cart-id look)]
                   (let [shared-cart              (get looks-shared-carts-db shared-cart-id)
                         tex-ori-col              {:hair/color   #{(get name->slug color)}
                                                   :hair/origin  #{(get name->slug (str origin " hair"))}
                                                   :hair/texture #{(get name->slug texture)}}
                         sku-ids-from-shared-cart (->> shared-cart  ;; TODO: ordering logic
                                                       :line-items
                                                       (mapv :catalog/sku-id)
                                                       sort)
                         skus                     (->> (select-keys skus-db sku-ids-from-shared-cart)
                                                       vals
                                                       (select {:catalog/department #{"hair"}})
                                                       vec)]
                     (merge tex-ori-col ;; TODO(corey) apply merge-with into
                            {:look/title              (or (some->> [origin texture]
                                                                   (remove nil?)
                                                                   (interpose " ")
                                                                   not-empty
                                                                   (apply str))
                                                          "Check this out!")
                             :look/hero-imgs          [{:url (:photo-url look)
                                                        :platform-source
                                                        ^:ignore-interpret-warning
                                                        (when-let [icon (svg/social-icon (:social-media-platform look))]
                                                          (icon {:class "fill-white"
                                                                 :style {:opacity 0.7}}))}]
                             :look/navigation-message [e/navigate-shop-by-look-details {:album-keyword album-keyword
                                                                                        :look-id       (:content/id look)}]
                             :look/skus               skus})))))
         vec)))

;; Visual Domain: Page

(defn page
  "Looks, 'Shop by Look'

  Visually: Grid, Spinning, or Filtering
  "
  [state _]
  (let [skus-db   (get-in state storefront.keypaths/v2-skus)
        images-db (get-in state storefront.keypaths/v2-images)
        facets-db (->> (get-in state storefront.keypaths/v2-facets)
                       (maps/index-by (comp keyword :facet/slug))
                       (maps/map-values (fn [facet]
                                          (update facet :facet/options
                                                  (partial maps/index-by :option/slug)))))

        looks-shared-carts-db (get-in state storefront.keypaths/v1-looks-shared-carts)

        selected-album-keyword (get-in state keypaths/selected-album-keyword)
        looks                  (looks<- state skus-db facets-db
                                        looks-shared-carts-db
                                        selected-album-keyword)
        ;; Flow models
        looks-filtering        (merge (get-in state catalog.keypaths/k-models-looks-filtering)
                                      {:facet-filtering/panel-toggle-event   e/flow|looks-filtering|panel-toggled
                                       :facet-filtering/section-toggle-event e/flow|looks-filtering|section-toggled
                                       :facet-filtering/filter-toggle-event  e/flow|looks-filtering|filter-toggled
                                       :facet-filtering/item-label           "Look"})]
    (cond
      ;; Spinning
      (empty? looks) ;;TODO: Or awaiting  bulk fetch carts to appear
      (->> (component/build spinning-template)
           (template/wrap-standard state
                                   e/navigate-shop-by-look))
      ;; Looks Filtering Panel
      (:facet-filtering/panel looks-filtering)
      (component/build facet-filters/panel-template
                       {:header   {:header.reset/primary "RESET"
                                   :header.reset/target  [e/flow|looks-filtering|reset]
                                   :header.done/primary  "DONE"
                                   :header.done/target   [e/flow|looks-filtering|panel-toggled false]}
                        :sections {:facet-filtering/sections
                                   (facet-filters/sections<-
                                    facets-db
                                    (->> looks ;; Note: Facets that actually exist on the looks
                                         (map #(select-keys % [:hair/origin :hair/color :hair/texture]))
                                         (apply merge-with clojure.set/union))
                                    looks-filtering)}})
      ;; Grid of Looks
      :else
      (->> {:hero              looks-hero<-
            :looks-cards       (looks-cards<- images-db
                                              looks
                                              looks-filtering)
            :no-matches        (no-matches<- looks
                                             looks-filtering)
            :filtering-summary (facet-filters/summary<- facets-db
                                                        looks
                                                        looks-filtering)}
           (component/build looks-template)
           (template/wrap-standard state e/navigate-shop-by-look)))))

;; -- Original Views

(defcomponent original-component [{:keys [looks copy spinning?]} owner opts]
  (if spinning?
    (ui/large-spinner {:style {:height "4em"}})
    [:div.bg-warm-gray
     [:div.center.py6
      [:h1.title-1.canela.py3 (:title copy)]
      [:p.col-10.col-6-on-tb-dt.mx-auto.proxima.content-2 (:description copy)]]
     [:div.flex.flex-wrap.mbn2.justify-center.justify-start-on-tb-dt.bg-cool-gray.py2-on-tb-dt.px1-on-tb-dt
      (map-indexed
       (fn [idx look]
         (ui/screen-aware component-ugc/social-image-card-component
                          (assoc look :hack/above-the-fold? (zero? idx))
                          {:opts               {:copy copy}
                           :child-handles-ref? true
                           :key                (str (:id look))}))
       looks)]]))

(defn query [data]
  (let [selected-album-kw (get-in data keypaths/selected-album-keyword)
        actual-album-kw   (ugc/determine-look-album data selected-album-kw)
        looks             (-> data (get-in keypaths/cms-ugc-collection) actual-album-kw :looks)
        color-details     (->> (get-in data keypaths/v2-facets)
                               (filter #(= :hair/color (:facet/slug %)))
                               first
                               :facet/options
                               (maps/index-by :option/slug))]
    {:looks     (mapv (partial contentful/look->social-card
                               selected-album-kw
                               color-details)
                      looks)
     :copy      (actual-album-kw ugc/album-copy)
     :spinning? (empty? looks)}))

(defn ^:export built-component [data opts]
  (let [album-kw (ugc/determine-look-album data (get-in data keypaths/selected-album-keyword))]
    (if (and (experiments/sbl-update? data)        ;; featured
             (= :shop (sites/determine-site data)) ;; dtc, shop
             (= :aladdin-free-install album-kw))   ;; main look page
      (page data opts)
      (->> (component/build original-component
                            (query data)
                            opts)
           (template/wrap-standard data
                                   e/navigate-shop-by-look)))))
