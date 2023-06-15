(ns catalog.look-details-v202105
  "Shopping by Looks: Detail page for an individual 'look'"
  (:require #?@(:cljs [[storefront.api :as api]
                       [storefront.accessors.orders :as orders]
                       [storefront.frontend-trackings :as trackings]
                       [storefront.trackings]
                       [storefront.hooks.quadpay :as quadpay]
                       [storefront.hooks.stringer :as stringer]
                       [storefront.hooks.google-analytics :as google-analytics]
                       [storefront.platform.messages :as messages]])
            [adventure.components.layered :as layered]
            [api.catalog :refer [select ?model-image ?cart-product-image]]
            api.orders
            api.products
            [catalog.facets :as facets]
            catalog.products
            [catalog.selector.sku :as sku-selector]
            [clojure.string :as string]
            clojure.set
            [adventure.faq :as adv-faq]
            [spice.maps :as maps]
            [storefront.accessors.contentful :as contentful]
            [storefront.accessors.images :as images]
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
            [spice.selector :as selector]
            [ui.molecules]
            [spice.core :as spice]))

;; A customizable look is a product merging all of the products that the base
;; look's skus come from It has two levels of selectors, those that affect all
;; physical items in the look and those that affect each item individually
;; {:selector/essentials [:hair/origin :hair/texture]
;;  :selector/electives  [:hair/color]
;;  :per-item [{:selector/essentials [:hair/family]
;;              :selector/electives  [:hair/length]}]}
;; For now services are handled separately since we don't customize the services

(defn distinct-by
  ;; TODO use the one in spice
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
  [selection-event
   selections {:option/keys [slug] :as option}]
  (merge
   #:product{:option option}
   #:option{:id               (str "picker-color-" (facets/hacky-fix-of-bad-slugs-on-facets slug))
            :selection-target [selection-event
                               {:selection [:hair/color]
                                :value     slug}]
            :checked?         (= (:hair/color selections) slug)
            :label            (:option/name option)
            :value            slug
            :bg-image-src     (:option/rectangle-swatch option)
            :available?       true
            :image-src        (:option/sku-swatch option)}))

(defn ^:private product-option->length-picker-option
  [selection-event
   availability
   {selected-hair-color :hair/color
    per-items           :per-item}
   index
   {:option/keys [slug]
    option-name  :option/name
    :as          product-option}]
  (let [{selected-hair-length :hair/length
         selected-hair-family :hair/family} (get per-items index)
        selection-path                      [:per-item index :hair/length]
        available?                          (boolean
                                             (get-in availability [selected-hair-family
                                                                   selected-hair-color
                                                                   slug]))
        sold-out?                           (not (boolean
                                                  (get-in availability [selected-hair-family
                                                                        selected-hair-color
                                                                        slug
                                                                        :inventory/in-stock?])))]
    (merge
     #:product{:option product-option}
     #:option{:id               (str "picker-length-" index "-" slug)
              :selection-target [selection-event
                                 {:selection selection-path
                                  :value     slug}]
              :checked?         (= selected-hair-length slug)
              :label            option-name
              :value            slug
              :available?       available?}
     (when sold-out?
       #:option{:label-attrs      {:class "dark-gray"}
                :label            (str option-name " - Sold Out")
                :selection-target nil})
     (when-not available?
       #:option{:label-attrs      {:class "dark-gray"}
                :label            (str option-name " - Unavailable")
                :selection-target nil}))))

(defn ^:private hair-length-option<
  [selection-event selections availability index per-item]
  (update per-item :hair/length
          (partial mapv
                   (partial product-option->length-picker-option
                            selection-event
                            availability
                            selections
                            index))))

(defn ^:private merge-selection-criteria [selections]
  (merge
   (maps/map-values hash-set (dissoc selections :per-item))
   (->> selections
        :per-item
        (mapv (partial maps/map-values hash-set))
        (apply merge-with clojure.set/union))))

(defn ^:private sku->data-sku-reference
  [s]
  (select-keys s [:catalog/sku-id :legacy/variant-id]))
;; Initialization
(defn- generate-product-options
  [skus-db images-db facets product]
  (let [product-skus (mapv #(get skus-db %) (:selector/sku-ids product))]
    (sku-selector/product-options facets product product-skus images-db)))

(defn ^:private ->look-product-options
  [products-db
   skus-db
   images-db
   facets
   physical-line-items]
  (into []
        (comp
         (map :catalog/sku-id)
         (map #(get skus-db %))
         (map (comp first :selector/from-products))
         (map #(get products-db %))
         (map (partial generate-product-options skus-db images-db facets)))
        physical-line-items))

(defn ^:private initialize-picker-options
  [selection-event
   selections
   availability
   look-options]
  {:hair/color (->> look-options
                    (mapcat :hair/color)
                    (map #(dissoc % :price :stocked?))
                    (distinct-by :option/slug)
                    (sort-by :filter/order)
                    (mapv (partial color-option<
                                   selection-event
                                   selections)))
   :per-item   (->> look-options
                    (map #(select-keys % [:hair/length]))
                    (map-indexed (partial hair-length-option<
                                          selection-event
                                          selections
                                          availability))
                    vec)})

(defn ^:private slice-sku-db
  "Returns the portion of the sku-db that meets the texture, origin, families of
  the look along with the service when available"
  [full-skus-db services selections]
  (let [criteria (-> (merge-selection-criteria selections)
                     (assoc :catalog/department #{"hair"})
                     (dissoc :hair/length :hair/color))]
    (merge (->> full-skus-db
                vals
                (selector/match-all {} criteria)
                (maps/index-by :catalog/sku-id))
           (->> services
                (mapv :catalog/sku-id)
                (select-keys full-skus-db)))))

(defn ^:private enrich-and-sort-shared-cart-items
  [skus-db shared-cart]
  (->> shared-cart
       :line-items
       (map (fn [{:keys [catalog/sku-id item/quantity]}]
              (-> (get skus-db sku-id)
                  (assoc :item/quantity quantity))))
       (sort-by :sku/price)))

(defn ^:private fan-out-items
  [items]
  (mapcat #(repeat (:item/quantity %) (dissoc % :item/quantity)) items))

(defmethod transitions/transition-state events/initialize-look-details
  [_ _ {:keys [shared-cart]} app-state]
  (let [{physical-line-items false
         services            true} (->> shared-cart
                                        (enrich-and-sort-shared-cart-items (get-in app-state keypaths/v2-skus))
                                        fan-out-items
                                        (group-by (comp #(string/starts-with? % "SV2") :catalog/sku-id)))
        initial-selections         (let [{:hair/keys [origin texture color]} (-> physical-line-items first)]
                                     ;; TODO make this more tolerable
                                     {:hair/texture (first texture)
                                      :hair/color   (first color)
                                      :hair/origin  (first origin)
                                      :per-item     (into []
                                                          (comp
                                                           (map #(select-keys % [:hair/origin :hair/family :hair/length :hair/base-material]))
                                                           (map (partial maps/map-values first)))
                                                          physical-line-items)})
        sliced-sku-db              (slice-sku-db (get-in app-state keypaths/v2-skus) services initial-selections)
        availability               (catalog.products/index-by-selectors
                                    [:hair/family :hair/color :hair/length]
                                    (vals sliced-sku-db))
        options                    (initialize-picker-options
                                    events/control-look-detail-picker-option-select
                                    initial-selections
                                    availability
                                    (->look-product-options
                                     (get-in app-state keypaths/v2-products)
                                     sliced-sku-db
                                     (get-in app-state keypaths/v2-images)
                                     (facets/by-slug app-state)
                                     physical-line-items))]
    (-> app-state
        (assoc-in catalog.keypaths/detailed-look-selected-picker nil)
        (assoc-in catalog.keypaths/detailed-look-picker-visible? nil)
        (assoc-in catalog.keypaths/detailed-look-selections initial-selections)
        (assoc-in catalog.keypaths/detailed-look-options options)
        (assoc-in catalog.keypaths/detailed-look-services services)
        (assoc-in catalog.keypaths/detailed-look-skus-db sliced-sku-db)
        (assoc-in catalog.keypaths/detailed-look-availability availability))))

(defn ^:private selections->product-selections
  [selections]
  (mapv
   #(merge (select-keys selections [:hair/color :hair/texture :hair/origin])
           (select-keys % [:hair/length :hair/family]))
   (:per-item selections)))

(defn ^:private product-selections->skus
  [availability product-selections]
  (mapv
   (fn [{:hair/keys [color length family]}]
     (get-in availability [family color length]))
   product-selections))

(defn ^:private ->data-event-format
  [selections availability services]
  (let [product-selections (selections->product-selections selections)]
    {:products           (->> product-selections
                              (product-selections->skus availability)
                              (mapv sku->data-sku-reference)
                              (map #(when (seq %) %)))
     :services           (mapv (comp not-empty sku->data-sku-reference) services)
     :product-selections product-selections}))

#?(:cljs
   (defmethod storefront.trackings/perform-track events/initialize-look-details
     [_ _ {:keys [shared-cart]} app-state]
     (stringer/track-event "look_customization-initialized"
                           (merge
                            {:shared-cart-id     (:number shared-cart)
                             :look-id            (:content/id (contentful/selected-look app-state))}
                            (->data-event-format
                             (get-in app-state catalog.keypaths/detailed-look-selections)
                             (get-in app-state catalog.keypaths/detailed-look-availability)
                             (get-in app-state catalog.keypaths/detailed-look-services))))))

#?(:cljs
   (defmethod effects/perform-effects events/initialize-look-details
     [_ event _ _ app-state]
     (let [skus-matching-selections (->> (get-in app-state catalog.keypaths/detailed-look-selections)
                                         selections->product-selections
                                         (product-selections->skus (get-in app-state catalog.keypaths/detailed-look-availability))
                                         (filter (complement nil?)))]
       (when (seq skus-matching-selections)
         (messages/handle-later events/look|viewed {:skus skus-matching-selections})))))

;; END Initialization

(defn ^:private add-to-cart-button
  [{:cta/keys [id target disabled? disabled-content label spinning?]}]
  (ui/button-large-primary
   (merge (apply utils/fake-href target)
          {:data-test        id
           :disabled?        disabled?
           :disabled-content disabled-content
           :spinning?        spinning?})
   label))

(defn carousel ^:private [data]
  (when-let [dependencies (not-empty (:carousel/data data))]
    (when (not-any? nil? (:carousel/images data))
      (component/build carousel/component
                       {:dependencies dependencies}
                       {:opts {:settings {:nav         true
                                          :edgePadding 0
                                          :controls    true
                                          :items       1}
                               :slides   (map ui/img (:carousel/images data))}}))))

(defn ^:private yotpo-reviews-summary-component [{:keys [yotpo-data-attributes]}]
  (when yotpo-data-attributes
    (component/build reviews/reviews-summary-component {:yotpo-data-attributes yotpo-data-attributes} nil)))

(defn ^:private yotpo-reviews-component [{:keys [yotpo-data-attributes]}]
  (when yotpo-data-attributes
    [:div.col-10-on-dt.mx-auto
     (component/build reviews/reviews-component {:yotpo-data-attributes yotpo-data-attributes} nil)]))

(defn ^:private get-number-of-lines
  [el]
  #?(:cljs (let [line-height (-> (.-lineHeight (js/getComputedStyle el))
                                 (string/replace "px" "")
                                 spice/parse-int)]
             (/ (.-offsetHeight el) line-height))
     :clj 0))

(component/defdynamic-component ^:private look-card-caption
  (constructor [this]
    (set! (.-show-more this)
          (.bind
           #(component/set-state! this :show-more?
                                   (not (:show-more? (component/get-state this))))))
    {:show-more?   false
     :truncatable? false})
  (did-mount [this]
    (component/set-state! this :truncatable? (< 3 (get-number-of-lines (.-caption (.-refs this))))))
  (render [this]
          (let [{:look-card/keys [secondary]}     (component/get-props this)
                {:keys [truncatable? show-more?]} (component/get-state this)
                {caption-attrs :caption-attrs
                 cta-label     :cta/label
                 cta-id        :cta/id}           (when truncatable?
                                                    (if show-more?
                                                      {:cta/id    "toggle-caption-closed"
                                                       :cta/label "Read Less"}
                                                      {:cta/id        "toggle-caption-open"
                                                       :caption-attrs {:style {:display            "-webkit-box"
                                                                               :-webkit-box-orient "vertical"
                                                                               :overflow           "hidden"
                                                                               :-webkit-line-clamp 3}}
                                                       :cta/label     "Read More"}))]
           (component/html
             (if-not (string/blank? secondary)
               [:div.mt1
                [:div.content-4.proxima.gray-700
                 (merge {:ref "caption"}
                        caption-attrs)
                 secondary]
                (when cta-id
                  (ui/button-small-underline-primary
                   {:href     nil
                    :on-click #?(:cljs (.-show-more this)
                                 :clj identity)}
                   cta-label))]
               [:div {:ref "caption"}])))))

(component/defcomponent ^:private look-card
  [{:look-card/keys [primary] :as queried-data} _ _]
  [:div.slides-middle.p3.bg-white.mb3
   (carousel queried-data)
   [:div.pt1
    [:div.flex.items-center
     [:div.flex-auto.proxima primary]
     [:div.ml1.line-height-1 {:style {:width  "21px"
                                      :height "21px"}}
      ^:inline (svg/instagram {:class "fill-dark-gray"})]]
    (yotpo-reviews-summary-component queried-data)
    (component/build look-card-caption queried-data)]])

(component/defcomponent ^:private small-cta
  [{:cta/keys [id target small-label disabled? disabled-content spinning?]} _ _]
  (ui/button-small-primary (merge (apply utils/fake-href target)
                                  {:data-test        id
                                   :disabled?        disabled?
                                   :disabled-content disabled-content
                                   :spinning?        spinning?})
                           small-label))

(component/defcomponent ^:private look-title
  ;; TODO better names for these keys
  [{:look-title/keys [primary
                      secondary
                      tertiary
                      quaternary]
    :as data} _ _]
  [:div
   [:h1.proxima.title-2.shout primary]
   [:div.flex.justify-between.mt2
    [:div
     secondary
     (when tertiary
       [:span.strike.content-4.ml2
        tertiary])
     [:div.shout.button-font-4 quaternary]]
    [:div.right-align (component/build small-cta data nil)]]])

(component/defcomponent ^:private look-total
  [{:look-total/keys [primary secondary tertiary]}_ _]
  [:div.center.pt4.bg-white-on-mb
   (when primary
     [:div.center.flex.items-center.justify-center.bold.shout
      (svg/discount-tag {:height "30px"
                         :width  "30px"})
      primary])
   (when tertiary [:div.strike.content-3.proxima.mt2 tertiary])
   [:div.title-1.proxima.bold.my1 secondary]])

(defn ^:private look-details-body
  [{:keys       [spinning?
                 picker-modal
                 color-picker
                 length-pickers
                 how-it-works
                 faq] :as queried-data}]
  [:div.bg-refresh-gray
   [:div
    (component/build picker/modal picker-modal)
    [:div.bg-white-on-mb.p2 (ui.molecules/return-link queried-data)]
    [:div
     [:div.col.col-6-on-tb-dt
      (component/build look-card queried-data "look-card")]
     (if spinning?
       [:div.flex.justify-center.items-center
         (ui/large-spinner {:style {:height "4em"}})]
       [:div.col-on-tb-dt.col-6-on-tb-dt
        [:div.px3
         (component/build look-title queried-data)]
        [:div.px3.my4 ;; TODO extract this component
         [:div.proxima.title-3.shout "Color"]
         (picker/component color-picker)]
        [:div.px3.my4
         [:div.proxima.title-3.shout "Lengths"]
         (map picker/component length-pickers)]
        [:div.bg-white-on-mb.pb8.px3
         (component/build look-total queried-data nil)
         [:div.col-11.mx-auto.mbn2
          (add-to-cart-button queried-data)
          #?(:cljs (component/build quadpay/component queried-data nil))]]])
     (when how-it-works
       (component/build layered/shop-bulleted-explainer how-it-works))]]
   (when faq
     [:div.bg-pale-purple.col-12.col (adv-faq/component faq)])
   [:div
    (yotpo-reviews-component queried-data)]])

(defn ^:private get-model-image
  [images-catalog {:keys [copy/title] :as skuer}]
  (when-let [image (->> (images/for-skuer images-catalog skuer)
                        (select ?model-image)
                        (sort-by :order)
                        first)]
    {:class    "col-12 mb4"
     :alt      title
     :max-size 749
     :src      (:url image)}))

(defn ^:private get-cart-product-image
  [images-catalog {:keys [copy/title] :as skuer}]
  (when-let [image (->> (images/for-skuer images-catalog skuer)
                        (select ?cart-product-image)
                        (sort-by :order)
                        first)]
    {:class    "col-12 mb4"
     :alt      title
     :max-size 749
     :src      (:url image)}))

(defn ^:private imgs [images-catalog look skus]
  (let [sorted-line-items (shared-cart/sort-by-depart-and-price skus)]
    (list
     {:class    "col-12"
      :max-size 749
      :src      (:image-url look)
      :alt "Person wearing "}
     (get-model-image images-catalog (first sorted-line-items))
     (get-cart-product-image images-catalog (first sorted-line-items)))))


(defn ^:private picker-modal<
  [picker-options picker-visible? selected-picker]
  (let [picker-type (last selected-picker)
        options     (get-in picker-options selected-picker)]
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
   skus-db
   selections
   picker-options
   availability]
  {:color-picker (let [{:option/keys [rectangle-swatch name slug]}
                       (get-in facets-db [:hair/color
                                          :facet/options
                                          (get-in selections [:hair/color])])]
                   {:id               "picker-color"
                    :value-id         (str "picker-selected-color-" (facets/hacky-fix-of-bad-slugs-on-facets slug))
                    :image-src        rectangle-swatch
                    :primary          name
                    :options          (:hair/color picker-options)
                    :selected-value   slug
                    :selection-target [events/control-look-detail-picker-option-select {:selection [:hair/color]}]
                    :open-target      [events/control-look-detail-picker-open {:picker-id [:hair/color]}]})

   :length-pickers
   (map-indexed
    (fn [index length-options]
      (let [{selected-hair-length :hair/length
             item-hair-family     :hair/family} (get-in selections [:per-item index])
            hair-family-and-color-skus
            (select
             (merge
              (dissoc selections :per-item)
              {:hair/family item-hair-family})
             skus-db)

            hair-material
            (case (first (:hair/base-material (first hair-family-and-color-skus)))
              "hd-lace" "HD Lace"
              "lace"    "Standard Lace"
              "silk"    "Silk"
              nil)

            hair-length-facet-option
            (get-in facets-db [:hair/length
                               :facet/options
                               selected-hair-length])

            hair-family-facet-option
            (get-in facets-db [:hair/family
                               :facet/options
                               (get-in selections [:per-item index :hair/family])])

            picker-data {:id               (str "picker-length-" index)
                         :value-id         (str "picker-selected-length-" index "-" (:option/slug hair-length-facet-option))
                         :image-src        (->> hair-family-and-color-skus first :selector/images (select ?cart-product-image) first :url)
                         :options          length-options
                         :primary          (str (:option/name hair-length-facet-option) (when hair-material (str " " hair-material)) " " (:sku/name hair-family-facet-option))
                         :selected-value   (:option/slug hair-length-facet-option)
                         :selection-target [events/control-look-detail-picker-option-select {:selection [:per-item index :hair/length]}]
                         :open-target      [events/control-look-detail-picker-open {:picker-id [:per-item index :hair/length]}]}]
        (cond
          (not (boolean
                (get-in availability [item-hair-family
                                      (:hair/color selections)
                                      selected-hair-length])))
          (-> picker-data
              (update :primary str " - Unavailable")
              (assoc :primary-attrs {:class "gray-700"}
                     :image-attrs {:style {:opacity "50%"}}))

          (not (boolean
                (get-in availability [item-hair-family
                                      (:hair/color selections)
                                      selected-hair-length
                                      :inventory/in-stock?])))
          (-> picker-data
              (update :primary str " - Sold Out")
              (assoc :primary-attrs {:class "gray-700"}
                     :image-attrs {:style {:opacity "50%"}}))

          :else picker-data)))
    (->> picker-options :per-item (mapv :hair/length)))})

(defn query [data]
  (let [skus-db                     (get-in data catalog.keypaths/detailed-look-skus-db)
        shared-cart                 (get-in data keypaths/shared-cart-current)
        album-keyword               (get-in data keypaths/selected-album-keyword)
        look                        (get-in data (conj keypaths/cms-ugc-collection-all-looks
                                                       (get-in data keypaths/selected-look-id)))
        contentful-look             (contentful/look->look-detail-social-card album-keyword
                                                                              (contentful/selected-look data))
        back                        (first (get-in data keypaths/navigation-undo-stack))
        album-copy                  (get ugc/album-copy album-keyword)
        back-event                  (:default-back-event album-copy)
        promotion-codes             (get-in data storefront.keypaths/promotions)
        shared-cart-promotion-codes (:promotion-codes shared-cart)

        ;; Looks query
        facets-db (facets/by-slug data)

        ;; Picker
        picker-options (get-in data catalog.keypaths/detailed-look-options)

        ;; look items
        {uncustomized-look-product-items "hair"} (->> shared-cart
                                                      (enrich-and-sort-shared-cart-items skus-db)
                                                      fan-out-items
                                                      (group-by (comp first :catalog/department)))
        look-selections                             (get-in data catalog.keypaths/detailed-look-selections)
        skus-matching-selections                    (->> (get-in data catalog.keypaths/detailed-look-selections)
                                                         selections->product-selections
                                                         (product-selections->skus (get-in data catalog.keypaths/detailed-look-availability))
                                                         (filter (complement nil?)))
        unavailable-lengths-selected?               (not= (count uncustomized-look-product-items)
                                                          (count skus-matching-selections))

        raw-order (api.orders/look-customization->order data
                                                        {:line-items      (->> skus-matching-selections
                                                                               (group-by :catalog/sku-id)
                                                                               (maps/map-values (fn [skus]
                                                                                                  {:sku           (first skus)
                                                                                                   :item/quantity (count skus)}))
                                                                               vals)
                                                         :promotion-codes shared-cart-promotion-codes})

        items                 (:order/items raw-order)
        {:keys [adjustments line-items-total total]} (:waiter/order raw-order)]
    (merge #?(:cljs (reviews/query-look-detail shared-cart data))
           {:spinning? (or (not contentful-look)
                           (nil? skus-db)
                           (utils/requesting? data request-keys/fetch-shared-cart))}

           (let [total-price      (mf/as-money-or-dashes (when-not unavailable-lengths-selected? line-items-total))
                 discounted-price (mf/as-money-or-dashes (when-not unavailable-lengths-selected? total))
                 discounted?      (not= total-price discounted-price)
                 applied-promo    (first (filter #(= (-> adjustments first :coupon-code) (:code %)) promotion-codes))
                 title            (clojure.string/join " " [(:origin look)
                                                            (get-in facets-db
                                                                    [:hair/texture
                                                                     :facet/options
                                                                     (:hair/texture look-selections)
                                                                     :option/name])
                                                            "Hair"])
                 secondary        (if discounted? discounted-price total-price)
                 tertiary         (when discounted? total-price)]
             {:look-title/primary    title
              :look-title/quaternary (str (count uncustomized-look-product-items)
                                          " products in this " (:short-name album-copy))
              :look-title/secondary  secondary
              :look-title/tertiary   tertiary

              :look-total/primary (cond
                                    (= "holiday" (:code applied-promo))
                                    (:description applied-promo)

                                    applied-promo
                                    (:description applied-promo)

                                    :else
                                    (-> adjustments first :name))
              :look-total/secondary secondary
              :look-total/tertiary  tertiary})

           {:look-card/primary   (:title contentful-look)
            :look-card/secondary (:description contentful-look)

            :cta/id                    "add-to-cart-submit"
            :cta/disabled?             (or (not contentful-look)
                                           unavailable-lengths-selected?
                                           (utils/requesting? data request-keys/new-order-from-sku-ids))
            :cta/target                [events/control-create-order-from-customized-look
                                        {:look-id         (:id contentful-look)
                                         :items           (into {} (map (fn [item]
                                                                          {(:catalog/sku-id item) (:item/quantity item)})
                                                                        items))
                                         :promotion-codes shared-cart-promotion-codes}]
            :cta/disabled-content      (when unavailable-lengths-selected?
                                         "Unavailable")
            :cta/spinning?             (utils/requesting? data request-keys/new-order-from-sku-ids)
            :cta/label                 "Add Products To Bag"
            :cta/small-label           "Add To Bag"
            :carousel/images           (imgs (get-in data keypaths/v2-images) contentful-look items)
            :carousel/data             {:look look :shared-cart shared-cart}
            :return-link/copy          "Back"
            :return-link/id            "back"
            :return-link/event-message (if (and (not back) back-event)
                                         [back-event]
                                         [events/navigate-shop-by-look {:album-keyword album-keyword}])
            :return-link/back          back}

           {:quadpay/show?       (get-in data keypaths/loaded-quadpay)
            :quadpay/order-total total
            :quadpay/directive   :just-select}

           {:picker-modal (picker-modal< picker-options
                                         (get-in data catalog.keypaths/detailed-look-picker-visible?)
                                         (get-in data catalog.keypaths/detailed-look-selected-picker))}
           (pickers< facets-db
                     (vals skus-db)
                     (get-in data catalog.keypaths/detailed-look-selections)
                     picker-options
                     (get-in data catalog.keypaths/detailed-look-availability)))))

(defcomponent component
  [queried-data _ _]
  [:div.container (look-details-body queried-data)])

(defn ^:export built-component [data opts]
  (component/build component (query data) opts))

#?(:cljs
   (defmethod storefront.trackings/perform-track events/control-look-detail-picker-open
     [_ _ {:keys [picker-id] :as args} _]
     (let [picker-name (name (last picker-id))]
       (stringer/track-event "look_facet-clicked" (merge {:facet-selected picker-name}
                                                         (when (= "length" picker-name)
                                                           {:position (second picker-id)}))))))

#?(:cljs
   (defmethod storefront.trackings/perform-track events/control-look-detail-picker-option-select
     [_ _ {:keys [selection value]} app-state]
     (let [picker-name (name (last selection))]
       (stringer/track-event "look_facet-changed"
                             (merge (case picker-name
                                      "color"
                                      {:selected-color value}

                                      "length"
                                      {:position        (second selection)
                                       :selected-length value}
                                      nil)
                                    {:facet-selected picker-name}
                                    (->data-event-format
                                     (get-in app-state catalog.keypaths/detailed-look-selections)
                                     (get-in app-state catalog.keypaths/detailed-look-availability)
                                     (get-in app-state catalog.keypaths/detailed-look-services)))))))

(defmethod transitions/transition-state events/control-look-detail-picker-option-select
  [_ event {:keys [selection value]} app-state]
  (let [availability   (get-in app-state catalog.keypaths/detailed-look-availability)
        new-selections (-> app-state
                           (get-in catalog.keypaths/detailed-look-selections)
                           (assoc-in selection value))]
    (cond->
        (-> app-state
            (assoc-in catalog.keypaths/detailed-look-selections new-selections)
            (update-in (concat catalog.keypaths/detailed-look-options selection)
                       (partial mapv (fn [option] (assoc option :option/checked? (= (:option/value option) value))))))
      (= [:hair/color] selection)
      (update-in (conj catalog.keypaths/detailed-look-options :per-item)
                 (fn [per-item-options]
                   (->> per-item-options
                        (map-indexed
                         (fn [index per-item-options]
                           (update per-item-options
                                   :hair/length
                                   (partial mapv
                                            (comp (partial product-option->length-picker-option
                                                           event
                                                           availability
                                                           new-selections
                                                           index)
                                                  :product/option)))))
                        vec))))))

(defmethod effects/perform-effects events/control-look-detail-picker-option-select
  [_ event _ _ app-state]
  #?(:cljs
     (let [skus-matching-selections (->> (get-in app-state catalog.keypaths/detailed-look-selections)
                                         selections->product-selections
                                         (product-selections->skus (get-in app-state catalog.keypaths/detailed-look-availability))
                                         (filter (complement nil?)))]
       (messages/handle-message events/control-look-detail-picker-close)
       (messages/handle-message events/look|viewed {:skus skus-matching-selections}))))

(defmethod transitions/transition-state events/control-look-detail-picker-open
  [_ event {:keys [picker-id]} app-state]
  (-> app-state
      (assoc-in catalog.keypaths/detailed-look-selected-picker picker-id)
      (assoc-in catalog.keypaths/detailed-look-picker-visible? true)))

(defmethod transitions/transition-state events/control-look-detail-picker-close
  [_ event _ app-state]
  (assoc-in app-state catalog.keypaths/detailed-look-picker-visible? false))

(defmethod effects/perform-effects events/control-create-order-from-customized-look
  [_ _ {:keys [items promotion-codes look-id]} _ state]
  #?(:cljs
     (let [removed-items (orders/product-items (get-in state keypaths/order))]
       (api/new-order-from-sku-ids
        (get-in state keypaths/session-id)
        {:store-stylist-id     (get-in state keypaths/store-stylist-id)
         :user-id              (get-in state keypaths/user-id)
         :user-token           (get-in state keypaths/user-token)
         :servicing-stylist-id (get-in state keypaths/order-servicing-stylist-id)
         :sku-id->quantity     items
         :promotion-codes      promotion-codes
         :ignore-promo-absence true}
        (fn success-handler
          [{:keys [order]}]
          (doseq [{:keys [sku quantity]} removed-items]
            (messages/handle-message events/order-line-item-removed {:sku-id   sku
                                                                     :quantity quantity
                                                                     :order    order}))
          (messages/handle-message
           events/api-success-update-order
           {:order    order
            :navigate events/navigate-cart})
          (trackings/track-cart-initialization
           "look-customization"
           look-id
           {:environment      (get-in state keypaths/environment)
            :skus-db          (get-in state keypaths/v2-skus)
            :image-catalog    (get-in state keypaths/v2-images)
            :store-experience (get-in state keypaths/store-experience)
            :order            order
            :user-ecd         (google-analytics/retrieve-user-ecd state)})
          (doseq [promo-code promotion-codes]
            (messages/handle-message events/order-promo-code-added {:order-number (:number order)
                                                                    :promo-code   promo-code})))))))
