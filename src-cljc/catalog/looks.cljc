(ns catalog.looks
  "Shopping by Looks: index page of 'looks' for an 'album'"
  (:require #?@(:cljs [[storefront.hooks.stringer :as stringer]
                       [storefront.history :as history]
                       [storefront.trackings :as trackings]])
            [api.catalog :refer [select ?discountable ?physical]]
            [catalog.images :as catalog-images]
            catalog.keypaths
            [catalog.ui.facet-filters :as facet-filters]
            clojure.set
            clojure.string
            [storefront.effects :as effects]
            [spice.maps :as maps]
            [storefront.accessors.contentful :as contentful]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.sites :as sites]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.money-formatters :as mf]
            [storefront.components.svg :as svg]
            [storefront.components.template :as template]
            [storefront.components.ugc :as component-ugc]
            [storefront.components.ui :as ui]
            [storefront.events :as e]
            storefront.keypaths
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [storefront.ugc :as ugc]))

#?(:cljs
   (defmethod trackings/perform-track e/shop-by-look|look-selected
     [_ event {:keys [variant-ids card-index]} app-state]
     (stringer/track-event "look_selected"
                           {:card_index card-index
                            :variant_id variant-ids})))

(defmethod effects/perform-effects  e/shop-by-look|look-selected
  [_ event {:keys [album-keyword look-id]} _ app-state]
  #?(:cljs
     (history/enqueue-navigate e/navigate-shop-by-look-details {:album-keyword album-keyword
                                                                :look-id       look-id})))

;; Visual: Looks (new version under experiment)

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
  [{:looks-card.title/keys [primary secondary struck]}]
  [:div.my2
   [:div primary]
   [:div secondary
    (when struck
      [:span.content-4.strike.ml1 struck])]])

(defcomponent looks-card-hair-item-molecule
  [{:looks-card.hair-item/keys [image-url length]} _ {:keys [id]}]
  [:div.relative
   [:img.block
    {:key id
     :src image-url}]
   [:div.absolute.top-0.right-0.content-4.m1
    length]])

(defcomponent looks-card-organism*
  [{:as data :looks-card/keys [height-px id]
    target :looks-card.action/target} _ _]
  [:a.col-12.px1-on-tb-dt.col-4-on-tb-dt.black.pb2
   (merge {:data-test id}
          (apply utils/route-to target))
   [:div.border.border-cool-gray.p2
    [:div.flex
     {:style {:height (str height-px "px")}}
     (looks-card-hero-molecule data)
     [:div.flex.flex-column.justify-between.mlp2
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
     (component/elements looks-card-organism data :looks-cards/cards)]))

(defcomponent looks-template
  [{:keys [hero]
    :as queried-data} _ _]
  [:div
   (component/build looks-hero-organism hero)
   (component/build facet-filters/organism queried-data
                    {:opts {:child-component looks-cards-organism}})])

;; Visual: Spinning

(defcomponent spinning-template
  [_ _ _]
  (ui/large-spinner
   {:style {:height "4em"}}))

;; Biz domains -> Viz domains

(defn ^:private looks-card<-
  [images-db {:look/keys [id total-price discounted-price title hero-imgs items target]}]
  (let [height-px                    240
        gap-px                       3
        fanned-out-by-quantity-items (->> items
                                          (mapcat (fn [s]
                                                    (repeat (:item/quantity s) s)))
                                          (take 4))]
    (merge
     {:looks-card.title/primary  title
      :looks-card.hero/image-url (:url (first hero-imgs))
      :looks-card.hero/badge-url (:platform-source (first hero-imgs))
      :looks-card.action/target  target
      :looks-card.hero/gap-px    gap-px
      :looks-card/id             (str "look-" id)
      :looks-card/height-px      height-px
      :looks-card/hair-items     (->> fanned-out-by-quantity-items
                                      (sort-by :sku/price)
                                      (map (fn [sku]
                                             (let [img-count (count fanned-out-by-quantity-items)
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
                                               {:looks-card.hair-item/length (str (first (:hair/length sku)) "\"")
                                                :looks-card.hair-item/image-url
                                                (str "https://ucarecdn.com/"
                                                     ucare-id
                                                     "/-/format/auto/-/scale_crop/"
                                                     img-px "x" img-px "/center/")}))))}
     (if discounted-price
       {:looks-card.title/secondary discounted-price
        :looks-card.title/struck    total-price}
       {:looks-card.title/secondary total-price}))))

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
  (let [contentful-looks (->> (get-in state storefront.keypaths/cms-ugc-collection)
                              ;; NOTE(corey) This is hardcoded because obstensibly
                              ;; filtering should replace albums
                              :aladdin-free-install
                              :looks)]
    (->> contentful-looks
         (keep-indexed (fn [index look]
                         (when-let [shared-cart-id (contentful/shared-cart-id look)]
                           (let [shared-cart              (get looks-shared-carts-db shared-cart-id)
                                 sku-ids-from-shared-cart (->> shared-cart
                                                               :line-items
                                                               (mapv :catalog/sku-id)
                                                               sort)
                                 sku-id->quantity         (->> shared-cart
                                                               :line-items
                                                               (maps/index-by :catalog/sku-id)
                                                               (maps/map-values :item/quantity))
                                 all-skus                 (->> (select-keys skus-db sku-ids-from-shared-cart)
                                                               vals
                                                               vec)
                                 ;; NOTE: assumes only one discountable service item for the look
                                 discountable-service-sku (->> all-skus
                                                               (select ?discountable)
                                                               first)
                                 product-items            (->> all-skus
                                                               (select ?physical)
                                                               (mapv (fn [{:as sku :keys [catalog/sku-id]}]
                                                                       (assoc sku :item/quantity (get sku-id->quantity sku-id))))
                                                               vec)
                                 tex-ori-col              (some->> product-items
                                                                   (mapv #(select-keys % [:hair/color :hair/origin :hair/texture]))
                                                                   not-empty
                                                                   (apply merge-with clojure.set/union))

                                 origin-name  (get-in facets-db [:hair/origin :facet/options (first (:hair/origin tex-ori-col)) :sku/name])
                                 texture-name (get-in facets-db [:hair/texture :facet/options (first (:hair/texture tex-ori-col)) :option/name])

                                 discountable-service-title-component (when-let [discountable-service-category
                                                                                 (some->> discountable-service-sku
                                                                                          :service/category
                                                                                          first)]
                                                                        (case discountable-service-category
                                                                          "install"      "+ FREE Install Service"
                                                                          "construction" "+ FREE Custom Wig"
                                                                          nil))
                                 total-price                          (some->> all-skus
                                                                               (mapv (fn [{:keys [catalog/sku-id sku/price]}]
                                                                                       (* (get sku-id->quantity sku-id 0) price)))
                                                                               (reduce + 0))
                                 discounted-price                     (when-let [discountable-service-price (:sku/price discountable-service-sku)]
                                                                        (- total-price discountable-service-price))
                                 look-id                              (:content/id look)]
                             (merge tex-ori-col ;; TODO(corey) apply merge-with into
                                    {:look/title (clojure.string/join " " [origin-name
                                                                           texture-name
                                                                           "Hair"
                                                                           discountable-service-title-component])

                                     ;; TODO: only handles the free service discount,
                                     ;; other promotions can be back ported here after
                                     ;; #176485395 is completed
                                     :look/total-price      (some-> total-price mf/as-money)
                                     :look/discounted-price (some-> discounted-price mf/as-money)
                                     :look/hero-imgs        [{:url (:photo-url look)
                                                              :platform-source
                                                              (when-let [icon (svg/social-icon (:social-media-platform look))]
                                                                (icon {:class "fill-white"
                                                                       :style {:opacity 0.7}}))}]
                                     :look/id               look-id
                                     :look/target           [e/shop-by-look|look-selected {:album-keyword album-keyword
                                                                                           :look-id       look-id
                                                                                           :card-index    index
                                                                                           :variant-ids   (map :legacy/variant-id all-skus)}]
                                     :look/items            product-items})))))
         vec)))

;; Visual Domain: Page

(defn page
  "Looks, 'Shop by Look'

  Visually: Grid, Spinning, or Filtering
  "
  [state _]
  (let [skus-db               (get-in state storefront.keypaths/v2-skus)
        images-db             (get-in state storefront.keypaths/v2-images)
        facets-db             (->> (get-in state storefront.keypaths/v2-facets)
                                   (maps/index-by (comp keyword :facet/slug))
                                   (maps/map-values (fn [facet]
                                                      (update facet :facet/options
                                                              (partial maps/index-by :option/slug)))))
        looks-shared-carts-db (get-in state storefront.keypaths/v1-looks-shared-carts)

        selected-album-keyword (get-in state storefront.keypaths/selected-album-keyword)
        looks                  (looks<- state skus-db facets-db
                                        looks-shared-carts-db
                                        selected-album-keyword)
        ;; Flow models
        facet-filtering-state  (-> state
                                   (get-in catalog.keypaths/k-models-facet-filtering)
                                   (assoc :facet-filtering/item-label "Look"))]
    (if
      ;; Spinning
      (or (utils/requesting? state request-keys/fetch-shared-carts)
          (empty? looks))
      (->> (component/build spinning-template)
           (template/wrap-standard state
                                   e/navigate-shop-by-look))
      ;; Grid of Looks
      (->> (merge
            {:hero looks-hero<-}
            (facet-filters/filters<-
             {:facets-db             (get-in state storefront.keypaths/v2-facets)
              :faceted-models        looks
              :facet-filtering-state facet-filtering-state
              :facets-to-filter-on   [:hair/origin :hair/color :hair/texture]
              :navigation-event      e/navigate-shop-by-look
              :navigation-args       {:album-keyword :look}
              :child-component-data  (looks-cards<- images-db looks facet-filtering-state)}))
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
  (let [selected-album-kw (get-in data storefront.keypaths/selected-album-keyword)
        actual-album-kw   (ugc/determine-look-album data selected-album-kw)
        looks             (-> data (get-in storefront.keypaths/cms-ugc-collection) actual-album-kw :looks)
        color-details     (->> (get-in data storefront.keypaths/v2-facets)
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
  (let [album-kw (ugc/determine-look-album data (get-in data storefront.keypaths/selected-album-keyword))]
    (if (and (experiments/sbl-update? data)        ;; featured
             (= :shop (sites/determine-site data)) ;; dtc, shop
             (= :aladdin-free-install album-kw))   ;; main look page
      (page data opts)
      (->> (component/build original-component
                            (query data)
                            opts)
           (template/wrap-standard data
                                   e/navigate-shop-by-look)))))
