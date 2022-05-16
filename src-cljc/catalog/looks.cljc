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
            [mayvenn.visual.tools :refer [with within]]
            [mayvenn.visual.lib.image-grid :as image-grid]
            [storefront.accessors.contentful :as contentful]
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

(defcomponent looks-hero-title-organism
  [{:looks.hero.title/keys [primary secondary]} _ _]
  [:div.center.py6.bg-warm-gray
   [:h1.title-1.canela.py3
    primary]
   (into [:p.col-10.col-6-on-tb-dt.mx-auto.proxima.content-2]
         (interpose [:br] secondary))])

(defn looks-card-title-molecule
  [{:looks-card.title/keys [primary secondary struck]}]
  [:div.my2
   [:div primary]
   [:div secondary
    (when struck
      [:span.content-4.strike.ml1 struck])]])

(defcomponent looks-card-organism*
  [{:as data :looks-card/keys [id]
    target :looks-card.action/target} _ _]
  [:a.col-12.px1-on-tb-dt.col-4-on-tb-dt.black.pb2
   (merge {:data-test id}
          (apply utils/route-to target))
   [:div.border.border-cool-gray.p2
    (component/build image-grid/hero-with-little-hair-column-molecule
                     (with :looks-card.image-grid data))
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
   (component/build looks-hero-title-organism hero)
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
     {:looks-card.title/primary title
      :looks-card.action/target target
      :looks-card/id            (str "look-" id)}
     (within :looks-card.image-grid
             {:id               (str "look-" id)
              :height-in-num-px height-px
              :gap-in-num-px    gap-px})

     (within :looks-card.image-grid.hero
             {:image-url     (:url (first hero-imgs))
              :badge-url     (:platform-source (first hero-imgs))
              :gap-in-num-px gap-px
              :alt           (str "Person wearing skus " (clojure.string/join  ", " (mapv :catalog/sku-id items)))})
     (within :looks-card.image-grid.hair-column
             {:images (->> fanned-out-by-quantity-items
                           (sort-by :sku/price)
                           (map (fn [sku]
                                  {:length    (str (first (:hair/length sku)) "\"")
                                   :alt       (:sku/title sku)
                                   :image-url (:ucare/id (catalog-images/image images-db "cart" sku))})))})
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

(defn look<-
  [skus-db looks-shared-carts-db facets-db look promotions album-keyword index]
  (when-let [shared-cart-id (contentful/shared-cart-id look)]
    (let [shared-cart              (get looks-shared-carts-db shared-cart-id)
          album-copy               (get ugc/album-copy album-keyword)
          sku-ids-from-shared-cart (->> shared-cart
                                        :line-items
                                        (mapv :catalog/sku-id)
                                        sort)
          sku-id->quantity         (->> shared-cart
                                        :line-items
                                        (maps/index-by :catalog/sku-id)
                                        (maps/map-values :item/quantity))
          item-quantity            (reduce + (vals sku-id->quantity))
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
          discounted-price                     (let [discountable-service-price (:product/essential-price discountable-service-sku)]
                                                 (cond-> total-price
                                                   discountable-service-price                                     (- discountable-service-price)
                                                   ;; TODO: REMOVE AFTER BLACK FRIDAY SALE 11/30/21
                                                   (first (filter (comp (partial = "holiday") :code) promotions)) (* 0.8)))
          look-id                              (:content/id look)
          any-sold-out-skus?                   (some false? (map :inventory/in-stock? all-skus))]
      (when-not any-sold-out-skus?
        (merge tex-ori-col ;; TODO(corey) apply merge-with into
               {:look/title (clojure.string/join " " [origin-name
                                                      texture-name
                                                      "Hair"
                                                      discountable-service-title-component])

                ;; TODO: only handles the free service discount,
                ;; other promotions can be back ported here after
                ;; #176485395 is completed
                :look/cart-number      shared-cart-id
                :look/total-price      (some-> total-price mf/as-money)
                :look/discounted?      discounted-price
                :look/discounted-price (or (some-> discounted-price mf/as-money)
                                           (some-> total-price mf/as-money))
                :look/id               look-id

                ;; Look
                :look/secondary-id "item-quantity-in-look"
                :look/secondary    (str item-quantity " items in this " (:short-name album-copy))

                ;; Looks page
                :look/hero-imgs [{:url (:photo-url look)
                                  :platform-source
                                  (when-let [icon (svg/social-icon (:social-media-platform look))]
                                    (icon {:class "fill-white"
                                           :style {:opacity 0.7}}))}]
                :look/target    [e/shop-by-look|look-selected {:album-keyword album-keyword
                                                               :look-id       look-id
                                                               :card-index    index
                                                               :variant-ids   (map :legacy/variant-id all-skus)}]
                :look/items     product-items})))))

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
                              :looks
                              ;; *WARNING*, HACK: to limit how many items are
                              ;; *being rendered / fetched from the backend on this page
                              (take 99))
        promotions       (get-in state storefront.keypaths/promotions)]
    (->> contentful-looks
         (keep-indexed (fn [index look]
                         (look<- skus-db looks-shared-carts-db facets-db look promotions album-keyword index)))
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
  (let [selected-album-kw     (get-in data storefront.keypaths/selected-album-keyword)
        actual-album-kw       (ugc/determine-look-album data selected-album-kw)
        looks                 (-> data (get-in storefront.keypaths/cms-ugc-collection) actual-album-kw :looks)
        skus-db               (get-in data storefront.keypaths/v2-skus)
        looks-shared-carts-db (get-in data storefront.keypaths/v1-looks-shared-carts)
        color-details         (->> (get-in data storefront.keypaths/v2-facets)
                                   (filter #(= :hair/color (:facet/slug %)))
                                   first
                                   :facet/options
                                   (maps/index-by :option/slug))
        promotions            (get-in data storefront.keypaths/promotions)
        looks-with-prices     (map (fn[look]
                                     (let [shared-cart-id           (contentful/shared-cart-id look)
                                           sku-id->quantity         (->> (get looks-shared-carts-db shared-cart-id)
                                                                         :line-items
                                                                         (maps/index-by :catalog/sku-id)
                                                                         (maps/map-values :item/quantity))
                                           skus-in-look             (->> (select-keys skus-db (keys sku-id->quantity))
                                                                         vals
                                                                         vec)
                                           price                    (some->> skus-in-look
                                                                             (mapv (fn [{:keys [catalog/sku-id sku/price]}]
                                                                                     (* (get sku-id->quantity sku-id 0) price)))
                                                                             (reduce + 0))
                                           discountable-service-sku (->> skus-in-look
                                                                         (select ?discountable)
                                                                         first)
                                           discounted-price         (let [discountable-service-price (:product/essential-price discountable-service-sku)]
                                                                      (cond-> price
                                                                        discountable-service-price                                     (- discountable-service-price)
                                                                        ;; TODO: REMOVE AFTER BLACK FRIDAY SALE 11/30/21
                                                                        (first (filter (comp (partial = "holiday") :code) promotions)) (* 0.8)))
                                           price-money              (when price (mf/as-money price))
                                           discounted-money         (when discounted-price (mf/as-money discounted-price))]
                                       (assoc look :price price-money :discounted-price (when (not= price-money discounted-money) discounted-money))))
                                   looks)
        looks-query           (map (partial contentful/look->social-card
                                            selected-album-kw
                                            color-details)
                                   looks-with-prices)]
    {:looks     looks-query
     :copy      (actual-album-kw ugc/album-copy)
     :spinning? (empty? looks)}))

(defn ^:export built-component [data opts]
  (let [album-kw (ugc/determine-look-album data (get-in data storefront.keypaths/selected-album-keyword))]
    (if (and (= :shop (sites/determine-site data)) ;; dtc, shop
             (= :aladdin-free-install album-kw))   ;; main look page
      (page data opts)
      (->> (component/build original-component
                            (query data)
                            opts)
           (template/wrap-standard data
                                   e/navigate-shop-by-look)))))
