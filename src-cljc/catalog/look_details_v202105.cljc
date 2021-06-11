(ns catalog.look-details-v202105
  "Shopping by Looks: Detail page for an individual 'look'"
  (:require #?@(:cljs [[storefront.hooks.quadpay :as quadpay]
                       [storefront.platform.messages :as messages]
                       ;; popups, must be required to load properly
                       looks.customization-modal])
            [api.catalog :refer [select ?discountable ?model-image ?cart-product-image]]
            api.orders
            api.products
            [catalog.facets :as facets]
            catalog.products
            [catalog.looks :as looks]
            [catalog.selector.sku :as sku-selector]
            [clojure.string :as string]
            clojure.set
            [spice.maps :as maps]
            [storefront.accessors.contentful :as contentful]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.images :as images]
            [storefront.accessors.products :as products]
            [storefront.accessors.shared-cart :as shared-cart]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.money-formatters :as mf]
            [storefront.components.picker.picker-two :as picker]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.reviews :as reviews]
            [storefront.request-keys :as request-keys]
            [storefront.transitions :as transitions]
            [storefront.ugc :as ugc]
            [catalog.keypaths :as catalog.keypaths]
            [spice.selector :as selector]))

;; A customizable look is a product merging all of the products that the base
;; look's skus come from It has two levels of selectors, those that affect all
;; physical items in the look and those that affect each item individually
;; {:selector/essentials [:hair/origin :hair/texture]
;;  :selector/electives  [:hair/color]
;;  :per-item [{:selector/essentials [:hair/family]
;;              :selector/electives  [:hair/length]}]}
;; For now services are handled separately since we don't customize the services

(defn distinct-by
  "TODO: add to spice"
  [f coll]
  (->> coll
       (reduce
        (fn [acc i]
          (let [v (f i)]
            (if (contains? (:state acc) v)
              acc
              (-> acc
                  (update :state conj v)
                  (update :new-coll conj i)))))
        {:state    #{}
         :new-coll []})
       :new-coll))

(defn ^:private color-option<
  [selections {:option/keys [slug] :as option}]
  #:option{:id               (str "picker-color-" (facets/hacky-fix-of-bad-slugs-on-facets slug))
           :selection-target [events/control-look-detail-picker-option-select
                              {:selection [:hair/color]
                               :value     slug}]
           :checked?         (= (:hair/color selections) slug)
           :label            (:option/name option)
           :value            slug
           :bg-image-src     (:option/rectangle-swatch option)
           :image-src        (:option/sku-swatch option)})

(defn ^:private get-length-availability
  [availability selected-color hair-family hair-length]
  (boolean
   (get-in availability [hair-family
                         selected-color
                         hair-length])))

(defn ^:private hair-length-option<
  [{selected-hair-color :hair/color
    per-items           :per-item} availability index per-item]
  (let [{selected-hair-length :hair/length
         selected-hair-family :hair/family} (get per-items index)]
    (update per-item :hair/length
            (partial map
                     (fn [{:as option :option/keys [slug]}]
                       (let [selection-path [:per-item index :hair/length]]
                         #:option{:id               (str "picker-length-" index "-" slug)
                                  :selection-target [events/control-look-detail-picker-option-select
                                                     {:selection selection-path
                                                      :value     slug}]
                                  :checked?         (= selected-hair-length slug)
                                  :label            (:option/name option)
                                  :value            slug
                                  :secondary-label  "- Unavailable"
                                  :available? (get-length-availability availability
                                                                       selected-hair-color
                                                                       selected-hair-family
                                                                       slug)}))))))

(defn ^:private merge-selection-criteria [selections]
  (merge
   (maps/map-values hash-set (dissoc selections :per-item))
   (->> selections
        :per-item
        (mapv (partial maps/map-values hash-set))
        (apply merge-with clojure.set/union))))

;; Initialization
(defn- generate-product-options
  [skus-db images-db facets product]
  (let [product-skus (mapv #(get skus-db %) (:selector/sku-ids product))]
    (sku-selector/product-options facets product product-skus images-db)))

(defn- initialize-look-picker-options
  [products-db
   skus-db
   images-db
   facets
   selections
   physical-line-items
   availability]
  (let [look-options (into []
                           (comp
                            (map :catalog/sku-id)
                            (map #(get skus-db %))
                            (map (comp first :selector/from-products))
                            (map #(get products-db %))
                            (map (partial generate-product-options skus-db images-db facets)))
                           physical-line-items)]

    {:hair/color (->> look-options
                      (mapcat :hair/color)
                      (map #(dissoc % :price :stocked?))
                      (distinct-by :option/slug)
                      (sort-by :filter/order)
                      (mapv (partial color-option< selections)))
     :per-item   (->> look-options
                      (map #(select-keys % [:hair/length]))
                      (map-indexed (partial hair-length-option< selections availability))
                      vec)}))

(defn ^:private
  slice-sku-db
  "Returns the portion of the sku-db that meets the texture, origin, families of
  the look along with the service when available"
  [full-skus-db shared-cart selections]
  (let [criteria (-> (merge-selection-criteria selections)
                     (assoc :catalog/department #{"hair"})
                     (dissoc :hair/length :hair/color))]
    (merge (->> full-skus-db
                vals
                (selector/match-all {} criteria)
                (maps/index-by :catalog/sku-id))
           (->> shared-cart
                :line-items
                (mapv :catalog/sku-id)
                (select-keys full-skus-db)))))

(defmethod transitions/transition-state events/initialize-look-details
  [_ event {:as args :keys [shared-cart]} app-state]
  (let [physical-line-items (->> shared-cart
                                 :line-items
                                 (remove (comp #(string/starts-with? % "SV2") :catalog/sku-id))
                                 (map (fn [{:keys [catalog/sku-id item/quantity]}]
                                        (-> (get-in app-state (conj keypaths/v2-skus sku-id))
                                            (assoc :item/quantity quantity))))
                                 (sort-by :sku/price)
                                 (mapcat #(repeat (:item/quantity %) (dissoc % :item/quantity))))
        initial-selections  (let [{:hair/keys [origin texture color]} (-> physical-line-items first)]
                              ;; TODO make this more tolerable
                              {:hair/origin  (first origin)
                               :hair/texture (first texture)
                               :hair/color   (first color)
                               :per-item     (->> physical-line-items
                                                  (map #(select-keys % [:hair/family :hair/length]))
                                                  (map (partial maps/map-values first))
                                                  vec)})
        sliced-sku-db       (slice-sku-db (get-in app-state keypaths/v2-skus) shared-cart initial-selections)
        availability        (reduce (fn [acc {:hair/keys [family color length]}]
                                      (cond-> acc
                                        (and family color length)
                                        (update-in [(first family) (first color)] (fnil conj #{}) (first length)))) {} (vals sliced-sku-db))
        options             (initialize-look-picker-options (get-in app-state keypaths/v2-products)
                                                            sliced-sku-db
                                                            (get-in app-state keypaths/v2-images)
                                                            (facets/by-slug app-state)
                                                            initial-selections
                                                            physical-line-items
                                                            availability)]
    (-> app-state
        (assoc-in catalog.keypaths/detailed-look-selected-picker nil)
        (assoc-in catalog.keypaths/detailed-look-picker-visible? nil)
        (assoc-in catalog.keypaths/detailed-look-selections initial-selections)
        (assoc-in catalog.keypaths/detailed-look-options options)
        (assoc-in catalog.keypaths/detailed-look-skus-db sliced-sku-db)
        (assoc-in catalog.keypaths/detailed-look-availability availability))))

;; END Initialization

(defn ^:private add-to-cart-button
  [sold-out? creating-order? look {:keys [number]}]
  (ui/button-large-primary
   (merge (utils/fake-href events/control-create-order-from-look
                           {:shared-cart-id number
                            :look-id        (:id look)})
          {:data-test        "add-to-cart-submit"
           :disabled?        (or (not look) sold-out?)
           :disabled-content (when sold-out? "Sold Out")
           :spinning?        creating-order?})
   "Add items to bag"))

(defn carousel ^:private [data imgs]
  (component/build carousel/component
                   {:dependencies data}
                   {:opts {:settings {:nav         true
                                      :edgePadding 0
                                      :controls    true
                                      :items       1}
                           :slides   imgs}}))

(component/defcomponent ^:private look-card-v202105
  [{:keys [shared-cart look yotpo-data-attributes] :as queried-data} _ _]
  [:div.bg-refresh-gray.p3
   [:div.bg-white
    [:div.slides-middle.col-on-tb-dt.col-6-on-tb-dt.p3
     [:div
      (when shared-cart
        (carousel {:look look :shared-cart shared-cart} (:carousel/images queried-data)))]
     [:div.pb3.pt1
      [:div.flex.items-center
       [:div.flex-auto.proxima {:style {:word-break "break-all"}}
        (:title look)]
       [:div.ml1.line-height-1 {:style {:width  "21px"
                                        :height "21px"}}
        ^:inline (svg/instagram {:class "fill-dark-gray"})]]
      (when yotpo-data-attributes
        (component/build reviews/reviews-summary-component {:yotpo-data-attributes yotpo-data-attributes} nil))
      (when-not (string/blank? (:description look))
        [:p.mt1.content-4.proxima.dark-gray (:description look)])]]]])

(component/defcomponent ^:private look-title
  [{:look/keys [title total-price discounted-price id secondary-id secondary cart-number discounted?] :as look} _ _]
  [:div.py2
   [:div.proxima.title-2.shout
    title]
   [:div.flex.justify-between.mt2
    [:div
     [:div
      discounted-price
      (when discounted?
        [:span.strike.content-4.ml2
         total-price])]
     [:div.shout.button-font-4 secondary]]
    ;; TODO: update event to control-initalize-cart-from-look when color/length can be changed
    [:div.right-align (ui/button-small-primary (merge (utils/fake-href events/control-create-order-from-look
                                                                       {:shared-cart-id cart-number
                                                                        :look-id        (:id id)})
                                                      {:data-test        "add-to-cart-submit"
                                                       :disabled?        false #_(or (not look) sold-out?)
                                                       :disabled-content false #_(when sold-out? "Sold Out")
                                                       :spinning?        false #_creating-order?})
                                               "Add to bag")]]])

(defn ^:private look-details-body
  [{:keys [creating-order? sold-out? look shared-cart fetching-shared-cart?
           base-price discounted-price  discount-text
           picker-modal
           color-picker
           length-pickers
           yotpo-data-attributes] :as queried-data}]
  [:div.clearfix
   (when look
     (component/build look-card-v202105 queried-data "look-card"))
   (if fetching-shared-cart?
     [:div.flex.justify-center.items-center (ui/large-spinner {:style {:height "4em"}})]
     (when shared-cart
       [:div.px2.bg-refresh-gray
        (component/build picker/modal picker-modal)
        (component/build look-title queried-data)

        [:div.col-on-tb-dt.col-6-on-tb-dt.px3-on-tb-dt
         [:div.my4 ;; TODO extract this component
          [:div.proxima.title-3.shout "Color"]
          (picker/component color-picker)]
         [:div.my4 ;; TODO extract this component
          [:div.proxima.title-3.shout "Lengths"]
          (map picker/component length-pickers)]
         [:div.center.pt4
          (when discount-text
            [:div.center.flex.items-center.justify-center.title-2.bold
             (svg/discount-tag {:height "28px"
                                :width  "28px"})
             discount-text])
          (when-not (= discounted-price base-price)
            [:div.strike.content-3.proxima.mt2 (mf/as-money base-price)])
          [:div.title-1.proxima.bold.my1 (mf/as-money discounted-price)]]
         [:div.col-11.mx-auto
          (add-to-cart-button sold-out? creating-order? look shared-cart)]
         #?(:cljs
            (component/build quadpay/component
                             {:quadpay/show?       (:quadpay-loaded? queried-data)
                              :quadpay/order-total discounted-price
                              :quadpay/directive   :just-select}
                             nil))
         (component/build reviews/reviews-component {:yotpo-data-attributes yotpo-data-attributes} nil)]]))])

(defn ^:private add-product-title-and-color-to-line-item [products facets line-item]
  (merge line-item {:product-title (->> line-item
                                        :catalog/sku-id
                                        (products/find-product-by-sku-id products)
                                        :copy/title)
                    :color-name    (-> line-item
                                       :hair/color
                                       first
                                       (facets/get-color facets)
                                       :option/name)}))

(defn ^:private get-model-image
  [images-catalog {:keys [copy/title] :as skuer}]
  (when-let [image (->> (images/for-skuer images-catalog skuer)
                        (select ?model-image)
                        (sort-by :order)
                        first)]
    (ui/img {:class    "col-12 mb4"
             :alt      title
             :max-size 749
             :src      (:url image)})))

(defn ^:private get-cart-product-image
  [images-catalog {:keys [copy/title] :as skuer}]
  (when-let [image (->> (images/for-skuer images-catalog skuer)
                        (select ?cart-product-image)
                        (sort-by :order)
                        first)]
    (ui/img {:class    "col-12 mb4"
             :alt      title
             :max-size 749
             :src      (:url image)})))

(defn ^:private imgs [images-catalog look skus]
  (let [sorted-line-items (shared-cart/sort-by-depart-and-price skus)]
    (list
     (ui/img {:class    "col-12"
              :max-size 749
              :src      (:image-url look)})
     (get-model-image images-catalog (first sorted-line-items))
     (get-cart-product-image images-catalog (first sorted-line-items)))))

(defn ^:private picker-modal<
  [picker-options picker-visible? selected-picker]
  (let [picker-type (last selected-picker)
        options (get-in picker-options selected-picker)]
    {:picker-modal/title        (case picker-type
                                  :hair/color  "Color"
                                  :hair/length "Length"
                                  nil)
     :picker-modal/type         picker-type
     :picker-modal/options      options
     ;; NOTE: There is a difference between selected and visible. We toggle
     ;; picker visibility to signal that the modal should close but we don't remove
     ;; the options so the close animation isn't stopped prematurely due to the
     ;; child options re-rendering.
     :picker-modal/visible?     (and picker-visible? options selected-picker)
     :picker-modal/close-target [events/control-look-detail-picker-close]}))

(defn ^:private pickers<
  [facets-db
   ;; TODO: cache a focused look sku db for this UI
   skus-db
   selections
   picker-options]
  (let [merged-criterion (merge
                          (maps/map-values hash-set (dissoc selections :per-item))
                          (->> selections
                               :per-item
                               (mapv (partial maps/map-values hash-set))
                               (apply merge-with clojure.set/union)))
        sku-db-subset    (select merged-criterion skus-db)]
    {:color-picker (let [{:option/keys [rectangle-swatch name slug]}
                         (get-in facets-db [:hair/color
                                            :facet/options
                                            (get-in selections [:hair/color])])]
                     {:id               "picker-color"
                      :value-id         (str "picker-selected-color-" (facets/hacky-fix-of-bad-slugs-on-facets slug))
                      :image-src        rectangle-swatch
                      :primary          name
                      :options          (->> picker-options :hair/color)
                      :selected-value   slug
                      :selection-target [events/control-look-detail-picker-option-select {:selection [:hair/color]}]
                      :open-target      [events/control-look-detail-picker-open {:picker-id [:hair/color]}]})

     :length-pickers
     (map-indexed
      (fn [index length-options]
        (let [{:keys [selector/images] :as sku}
              (first
               (select
                (merge
                 (dissoc selections :per-item)
                 (get-in selections [:per-item index]))
                sku-db-subset))

              hair-length-facet-option
              (get-in facets-db [:hair/length
                                 :facet/options
                                 (get-in selections [:per-item index :hair/length])])

              hair-family-facet-option
              (get-in facets-db [:hair/family
                                 :facet/options
                                 (get-in selections [:per-item index :hair/family])])]
          {:id               (str "picker-length-" index)
           :value-id         (str "picker-selected-length-" index "-" (:option/slug hair-length-facet-option))
           :image-src        (->> images
                                  (select ?cart-product-image)
                                  first
                                  :url)
           :primary          (str (:option/name hair-length-facet-option) " " (:sku/name hair-family-facet-option)

                                  (when (nil? sku)
                                    "- Unavailable"))
           :options          length-options
           :selected-value   (:option/slug hair-length-facet-option)
           :selection-target [events/control-look-detail-picker-option-select {:selection [:per-item index :hair/length]}]
           :open-target      [events/control-look-detail-picker-open {:picker-id [:per-item index :hair/length]}]}))
      (->> picker-options :per-item (mapv :hair/length)))}))

(defn query [data]
  (let [skus-db         (get-in data catalog.keypaths/detailed-look-skus-db)
        shared-cart     (get-in data keypaths/shared-cart-current)
        products        (get-in data keypaths/v2-products)
        facets          (get-in data keypaths/v2-facets)
        line-items      (some->> shared-cart
                                 :line-items
                                 (shared-cart/enrich-line-items-with-sku-data skus-db)
                                 (map (partial add-product-title-and-color-to-line-item products facets)))
        album-keyword   (get-in data keypaths/selected-album-keyword)
        look            (get-in data (conj keypaths/cms-ugc-collection-all-looks (get-in data keypaths/selected-look-id)))
        contentful-look (contentful/look->look-detail-social-card album-keyword
                                                                  (contentful/selected-look data))
        back            (first (get-in data keypaths/navigation-undo-stack))
        album-copy      (get ugc/album-copy album-keyword)
        back-event      (:default-back-event album-copy)
        {:order/keys                                  [items]
         {:keys [adjustments line-items-total total]} :waiter/order}
        (api.orders/shared-cart->order data skus-db shared-cart)

        discountable-services (select ?discountable items)

        ;; Looks query
        looks-shared-carts-db (get-in data storefront.keypaths/v1-looks-shared-carts)
        facets-db             (facets/by-slug data)
        look-data             (looks/look<- skus-db looks-shared-carts-db facets-db look album-keyword nil)

        ;; Picker
        picker-options (get-in data catalog.keypaths/detailed-look-options)]
    (merge look-data
           ;; TODO: vvv demonstrate that this is functioning
           #?(:cljs (reviews/query-look-detail shared-cart data))
           ;; END TODO
           {:shared-cart               shared-cart
            :look                      contentful-look
            :creating-order?           (utils/requesting? data request-keys/create-order-from-shared-cart)
            :skus                      skus-db
            :sold-out?                 (not-every? :inventory/in-stock? line-items)
            :fetching-shared-cart?     (or (not contentful-look) (utils/requesting? data request-keys/fetch-shared-cart))
            :base-price                line-items-total
            :discounted-price          total
            :quadpay-loaded?           (get-in data keypaths/loaded-quadpay)
            :discount-text             (if (some (comp (partial = 0) :promo.mayvenn-install/hair-missing-quantity) discountable-services)
                                         "Hair + FREE Service"
                                         (->> adjustments
                                              (filter (comp not (partial contains? #{"freeinstall"}) :name))
                                              first
                                              :name))
            :carousel/images           (imgs (get-in data keypaths/v2-images) contentful-look items)
            :return-link/event-message (if (and (not back) back-event)
                                         [back-event]
                                         [events/navigate-shop-by-look {:album-keyword album-keyword}])
            :return-link/back          back}

           {:picker-modal (picker-modal< picker-options
                                         (get-in data catalog.keypaths/detailed-look-picker-visible?)
                                         (get-in data catalog.keypaths/detailed-look-selected-picker))}
           (pickers< facets-db
                     (vals skus-db)
                     (get-in data catalog.keypaths/detailed-look-selections)
                     picker-options))))

(defcomponent component
  [queried-data _ _]
  [:div.container.mb4
   (look-details-body queried-data)])

(defn ^:export built-component [data opts]
  (component/build component (query data) opts))

(defn ^:private update-length-availability
  [availability selections selected-color per-item-options]
  (->> per-item-options
       (map-indexed
        (fn [index per-item-options]
          (update per-item-options
                  :hair/length
                  (partial mapv
                           #(assoc %
                                   :option/available?
                                   (get-length-availability
                                        availability
                                        selected-color
                                        (get-in selections [:per-item index :hair/family])
                                        (:option/value %)))))))
       vec))

(defmethod transitions/transition-state events/control-look-detail-picker-option-select
  [_ event {:keys [selection value]} app-state]
  (let [availability (get-in app-state catalog.keypaths/detailed-look-availability)
        selections (get-in app-state catalog.keypaths/detailed-look-selections)]
    (cond->
        (-> app-state
            (assoc-in (concat catalog.keypaths/detailed-look-selections selection) value)
            (update-in (concat catalog.keypaths/detailed-look-options selection)
                       (partial mapv (fn [option] (assoc option :option/checked? (= (:option/value option) value))))))
      (= [:hair/color] selection)
      (update-in (conj catalog.keypaths/detailed-look-options :per-item)
                 (partial update-length-availability availability selections value)))))

(defmethod effects/perform-effects events/control-look-detail-picker-option-select
  [_ event {:keys [selection value]} _ _]
  #?(:cljs (messages/handle-message events/control-look-detail-picker-close)))

(defmethod transitions/transition-state events/control-look-detail-picker-open
  [_ event {:keys [picker-id]} app-state]
  (-> app-state
      (assoc-in catalog.keypaths/detailed-look-selected-picker picker-id)
      (assoc-in catalog.keypaths/detailed-look-picker-visible? true)))

(defmethod transitions/transition-state events/control-look-detail-picker-close
  [_ event _ app-state]
  (assoc-in app-state catalog.keypaths/detailed-look-picker-visible? false))
