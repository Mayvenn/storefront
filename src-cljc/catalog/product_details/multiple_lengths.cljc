(ns catalog.product-details.multiple-lengths
  (:require #?@(:cljs [[goog.dom]
                       [goog.events]
                       [goog.style]
                       [storefront.browser.scroll :as scroll]
                       [storefront.components.popup :as popup]
                       [storefront.components.svg :as svg]
                       [storefront.hooks.stringer :as stringer]
                       [storefront.trackings :as trackings]])
            [api.catalog :refer [select]]
            api.current
            api.orders
            api.products
            [catalog.facets :as facets]
            catalog.keypaths
            [catalog.product-details-ugc :as ugc]
            [catalog.products :as products]
            [catalog.selector.sku :as sku-selector]
            [catalog.ui.add-to-cart :as add-to-cart]
            [catalog.ui.molecules :as catalog.M]
            [homepage.ui.faq :as faq]
            [mayvenn.visual.tools :refer [with within]]
            [mayvenn.visual.ui.titles :as titles]
            [spice.selector :as selector]
            [storefront.accessors.contentful :as contentful]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.images :as images]
            [storefront.accessors.products :as accessors.products]
            [storefront.accessors.sites :as sites]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.money-formatters :as mf]
            [storefront.components.picker.picker-two :as picker-two]
            [storefront.components.svg :as svg]
            [storefront.components.tabbed-information :as tabbed-information]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.platform.reviews :as review-component]
            [storefront.request-keys :as request-keys]
            [storefront.trackings :as trackings]
            [storefront.transitions :as transitions]
            storefront.ugc))

(defn page [wide-left wide-right-and-narrow]
  [:div.clearfix.mxn2
   [:div.col-on-tb-dt.col-7-on-tb-dt.px2
    [:div.hide-on-mb wide-left]]
   [:div.col-on-tb-dt.col-5-on-tb-dt.px2 wide-right-and-narrow]])

(defn full-bleed-narrow [body]
  [:div.hide-on-tb-dt body])

(def sold-out-button
  [:div.pt1.pb3.px3
   (ui/button-large-primary {:on-click  utils/noop-callback
                             :data-test "sold-out"
                             :disabled? true}
                            "Sold Out")])

(def unavailable-button
  [:div.pt1.pb3.px3
   (ui/button-large-primary {:on-click  utils/noop-callback
                             :data-test "unavailable"
                             :disabled? true}
                            "Unavailable")])

(def shipping-and-guarantee
  (component/html
   [:div.border-top.border-bottom.border-gray.p2.my2.center.shout.medium.h6
    "Free shipping & 30 day guarantee"]))

(defn image-body [i {:keys [url alt]}]
  (ui/aspect-ratio
   640 580
   (if (zero? i)
     (ui/img {:src url :class "col-12" :width "100%" :alt alt})
     (ui/defer-ucare-img
       {:class       "col-12"
        :alt         alt
        :width       640
        :placeholder (ui/large-spinner {:style {:height     "60px"
                                                :margin-top "130px"}})}
       url))))

(defn carousel [images _]
  (component/build carousel/component
                   {:images images}
                   {:opts {:settings {:edgePadding 0
                                      :items       1}
                           :slides   (map-indexed image-body images)}}))

(defn get-selected-options [selections options]
  (reduce
   (fn [acc selection]
     (assoc acc selection
            (first (filter #(= (selection selections) (:option/slug %))
                           (selection options)))))
   {}
   (keys selections)))

(defcomponent product-summary-organism
  "Displays basic information about a particular product"
  [data _ _]
  [:div.mt3.mx3
   [:div.flex.items-center.justify-between
    [:div.flex-auto
     (titles/proxima-left (with :title data))]
    [:div.col-3
     (catalog.M/price-block data)]]
   (catalog.M/yotpo-reviews-summary data)])

(component/defcomponent ^:private total
  [{:total/keys [primary secondary tertiary]}_ _]
  [:div.center.pt4.bg-white-on-mb
   (when primary
     [:div.center.flex.items-center.justify-center.bold.shout
      (svg/discount-tag {:height "30px"
                         :width  "30px"})
      primary])
   [:div.title-1.proxima.bold.my1 secondary]])

(defcomponent component
  [{:keys [carousel-images
           product
           reviews
           selected-sku
           ugc
           faq-section
           add-to-cart
           picker-modal] :as data} _ opts]
  (let [unavailable? (not (seq selected-sku))
        sold-out?    (not (:inventory/in-stock? selected-sku))]
    (if-not product
      [:div.flex.h2.p1.m1.items-center.justify-center
       {:style {:height "25em"}}
       (ui/large-spinner {:style {:height "4em"}})]
      [:div
       [:div.container.pdp-on-tb
        (when (:offset ugc)
          [:div.absolute.overlay.z4.overflow-auto
           {:key "popup-ugc"}
           (component/build ugc/popup-component ugc opts)])
        [:div
         {:key "page"}
         (page
          (component/html
           [:div ^:inline (carousel carousel-images product)
            (component/build ugc/component ugc opts)])
          (component/html
           [:div
            [:div
             (full-bleed-narrow
              [:div (carousel carousel-images product)])]
            (component/build product-summary-organism data)
            [:div.px2
             [:div
              (component/build picker-two/modal picker-modal)
              [:div.bg-refresh-gray.pb2.pt1
               [:div.px3.my4
                [:div.proxima.title-3.shout "Color"]
                (picker-two/component (with :color.picker data))

                ;;TODO(heather): material picker row?

                [:div.proxima.title-3.shout "Lengths"]
                [:div
                 (for [multi-lengths-data (:queries (with :multi-lengths.picker data))]
                   (picker-two/component multi-lengths-data))]]]]
             (let [{:keys [id event]} (with :add-length data)]
               (when id
                 [:div.center.py1 (ui/button-medium-underline-primary (merge
                                                                       (utils/fake-href event)
                                                                       {:data-test "add-length"}) "Add Another Length")]))]
            [:div
             (component/build total data nil)
             (cond
               unavailable? unavailable-button
               sold-out?    sold-out-button
               :else        (component/build add-to-cart/organism add-to-cart))]
            (when (products/stylist-only? product)
              shipping-and-guarantee)
            (component/build tabbed-information/component data)
            (component/build catalog.M/non-hair-product-description data opts)
            [:div.hide-on-tb-dt.m3
             [:div.mxn2.mb3 (component/build ugc/component ugc opts)]]]))]]

       (when (seq reviews)
         [:div.container.col-7-on-tb-dt.px2
          (component/build review-component/reviews-component reviews opts)])
       (when faq-section
         [:div.container
          (component/build faq/organism faq-section opts)])])))

(defn ugc-query [product sku data]
  (let [shop?              (= :shop (sites/determine-site data))
        album-keyword      (storefront.ugc/product->album-keyword shop? product)
        cms-ugc-collection (get-in data (conj keypaths/cms-ugc-collection album-keyword))]
    (when-let [social-cards (when product
                              (->> cms-ugc-collection
                                   :looks
                                   (mapv (partial contentful/look->pdp-social-card album-keyword))
                                   not-empty))]
      {:carousel-data {:product-id   (:catalog/product-id product)
                       :product-name (:copy/title product)
                       :page-slug    (:page/slug product)
                       :sku-id       (:catalog/sku-id sku)
                       :social-cards social-cards}
       :offset        (:offset (get-in data keypaths/navigation-query-params))
       :close-message [events/navigate-product-details
                       {:catalog/product-id (:catalog/product-id product)
                        :page/slug          (:page/slug product)
                        :query-params       {:SKU (:catalog/sku-id sku)}}]})))

(defn ^:private get-essential-critera-from-skuer [skuer]
  (select-keys skuer (:selector/essentials skuer)))

(defn find-carousel-images [product product-skus images-catalog selections selected-sku]
  (->> (selector/match-all {}
                           (or (not-empty (get-essential-critera-from-skuer selected-sku))
                               (not-empty selections)
                               (not-empty (get-essential-critera-from-skuer (first product-skus))))
                           (images/for-skuer images-catalog product))
       (selector/match-all {:selector/strict? true}
                           {:use-case #{"carousel"}
                            :image/of #{"model" "product"}})
       (sort-by :order)))

(defn default-selections
  "Using map of current selections (which can be empty)
  for un-selected values picks first option from the available options.
  e.g.: if there's no `hair/color` in `selections` map - it sets it to whatever the first in the list, e.g.: \"black\""
  [facets product product-skus images]
  (let [options (sku-selector/product-options facets product product-skus images)]
    ;; PERF(jeff): can use transients here
    (reduce
     (fn [a k]
       (if (get a k)
         a
         (assoc a k (-> options k first :option/slug))))
     {}
     (keys options))))

#?(:cljs
   [(defmethod popup/query :length-guide [state]
      (when-let [image (get-in state catalog.keypaths/length-guide-image)]
        {:length-guide/image-url    (:url image)
         :length-guide/close-target [events/popup-hide-length-guide]
         :length-guide/primary      "Length Guide"}))
    (defmethod popup/component :length-guide
      [{:length-guide/keys [image-url close-target primary]} _ _]
      (ui/modal
       {:close-attrs
        {:on-click #(apply messages/handle-message close-target)}}
       [:div.bg-white
        [:div.bg-white.col-12.flex.justify-between.items-center
         [:div {:style {:min-width "42px"}}]
         [:div primary]
         [:a.p3
          (merge (apply utils/fake-href close-target)
                 {:data-test "close-length-guide-modal"})
          (svg/x-sharp {:style {:width  "12px"
                                :height "12px"}})]]
        (ui/aspect-ratio 1.105 1
                         (ui/img
                          {:class    "col-12"
                           :max-size 600
                           :src      image-url}))]))])

(defn first-when-only [coll]
  (when (= 1 (count coll))
    (first coll)))

(defn determine-sku-from-selections
  [app-state new-selections]
  (let [product-id     (get-in app-state catalog.keypaths/detailed-product-id)
        product        (products/product-by-id app-state product-id)
        product-skus   (products/extract-product-skus app-state product)
        old-selections (get-in app-state catalog.keypaths/detailed-product-selections)]
    (->> product-skus
         (selector/match-all {}
                             (merge old-selections new-selections))
         first-when-only)))

(defn add-to-cart-query
  [app-state]
  (let [selected-sku                       (get-in app-state catalog.keypaths/detailed-product-selected-sku)
        selections                         (get-in app-state catalog.keypaths/detailed-product-selections)
        quadpay-loaded?                    (get-in app-state keypaths/loaded-quadpay)
        selected-skus                      (->> (get-in app-state catalog.keypaths/detailed-product-multiple-lengths-selections)
                                                (filterv not-empty)
                                                (mapv
                                                 #(determine-sku-from-selections app-state (merge selections %))))
        sku-price                          (->> selected-skus
                                                (mapv :sku/price)
                                                (filterv identity)
                                                (apply +))
        sku-id->quantity                   (into {}
                                                 (map (fn [[sku-id skus]] [sku-id (count skus)])
                                                      (group-by :catalog/sku-id selected-skus)))
        multiple-skus                      (< 1 (count selected-skus))]
    (merge
     {:cta/id    "add-to-cart"
      :cta/label (if multiple-skus
                   "Add Products to Bag"
                   "Add Product to Bag")

      ;; Fork here to use bulk add to cart
      :cta/target                  (if multiple-skus
                                     [events/control-bulk-add-skus-to-bag
                                      {:sku-id->quantity sku-id->quantity}]
                                     [events/control-add-sku-to-bag
                                      {:sku      selected-sku
                                       :quantity (get-in app-state keypaths/browse-sku-quantity 1)}])
      :cta/spinning?               (or (utils/requesting? app-state (conj request-keys/add-to-bag (:catalog/sku-id selected-sku)))
                                       (utils/requesting? app-state (conj request-keys/add-to-bag (set (keys sku-id->quantity)))))
      :cta/disabled?               (some #(not (:inventory/in-stock? %)) selected-skus)
      :add-to-cart.quadpay/price   sku-price
      :add-to-cart.quadpay/loaded? quadpay-loaded?})))

(defn ^:private tab-section<
  [data
   {:keys [content-path fallback-content-path]
    :as   section}]
  (when-let [content (or (get-in data content-path)
                         (and fallback-content-path
                              (get-in data fallback-content-path)))]
    (-> section
        (select-keys [:heading :link/content :link/target :link/id])
        (assoc :content content))))

(defn ^:private picker-query
  [{:keys [facets selections options length-selections sku-availability]}]
  (let [selected-color   (get-in facets [:hair/color :facet/options (:hair/color selections)])
        selected-lengths (map #(get-in facets [:hair/length :facet/options (:hair/length %)]) length-selections)
        availability     (get sku-availability (:option/slug selected-color))
        image-src        (->> options
                              :hair/color
                              (filter #(= (:option/slug selected-color) (:option/slug %)) )
                              first
                              :option/sku-swatch)]
    (merge
     (within :color.picker (let [{:option/keys [rectangle-swatch name slug]}
                                 (get-in facets [:hair/color
                                                 :facet/options
                                                 (get-in selections [:hair/color])])]
                             {:id               "picker-color"
                              :value-id         (str "picker-selected-color-" (facets/hacky-fix-of-bad-slugs-on-facets slug))
                              :image-src        rectangle-swatch
                              :primary          name
                              :options          (map (fn[option]
                                                       {:option/value      (:option/slug option)
                                                        :option/label      (:option/name option)
                                                        :option/available? (:stocked? option)})
                                                     (:hair/color options))
                              :selected-value   slug
                              :selection-target [events/control-product-detail-picker-option-select {:selection        :hair/color
                                                                                                     :navigation-event events/navigate-product-details}]
                              :open-target      [events/control-product-detail-picker-open {:facet-slug [:hair/color]}]}))
     (within :multi-lengths.picker {:queries (map-indexed (fn [idx selection]
                                                            (let [unavailable? (not (boolean
                                                                                     (get availability (:option/slug selection))))
                                                                  sold-out?    (not (boolean
                                                                                     (get-in availability [(:option/slug selection)
                                                                                                           :inventory/in-stock?])))
                                                                  picker-base  {:id        (str "picker-length-" idx)
                                                                                :value-id  (str "picker-length-" (:hair/length selection) "-" idx)
                                                                                :image-src image-src
                                                                                :primary   (if-let [picker-label (:option/name selection)]
                                                                                             (str picker-label " Bundle")
                                                                                             "Choose Length (optional)")
                                                                                :options   (concat
                                                                                            (if (and selection (not= idx 0))
                                                                                              [{:option/value      ""
                                                                                                :option/label      "Remove Length"
                                                                                                :option/available? true}]
                                                                                              [{:option/value      nil
                                                                                                :option/label      nil
                                                                                                :option/available? true}])

                                                                                            (mapv (fn[option]

                                                                                                    (let [unavailable? (not (boolean
                                                                                                                             (get availability (:option/slug option))))
                                                                                                          sold-out?    (not (boolean
                                                                                                                             (get-in availability [(:option/slug option)
                                                                                                                                                   :inventory/in-stock?])))]
                                                                                                      (merge
                                                                                                      {:option/value      (:option/slug option)
                                                                                                       :option/label      (:option/name option)
                                                                                                       :option/available? (not (or sold-out? unavailable?))}
                                                                                                      (cond
                                                                                                        unavailable?
                                                                                                        {:option/label-attrs {:class "dark-gray"}
                                                                                                         :option/label       (str (:option/name option) "- Unavailable")}
                                                                                                        sold-out?
                                                                                                        {:option/label-attrs {:class "dark-gray"}
                                                                                                         :option/label       (str (:option/name option) "- Sold Out")}))))
                                                                                                  (:hair/length options)))
                                                                                :selected-value   (:hair/length selection)
                                                                                :selection-target [events/control-product-detail-picker-option-length-select {:index            idx
                                                                                                                                                              :selection        :hair/length
                                                                                                                                                              :navigation-event events/navigate-product-details}]
                                                                                :open-target      [events/control-product-detail-picker-open {:facet-slug   [:hair/length]
                                                                                                                                              :length-index idx}]}]
                                                              (cond
                                                                (and selection unavailable?)
                                                                (-> picker-base
                                                                    (update :primary str " - Unavailable")
                                                                    (assoc :primary-attrs {:class "red"}  ;; TODO: too low contrast
                                                                           :image-attrs {:style {:opacity "50%"}}))

                                                                (and selection sold-out?)
                                                                (-> picker-base
                                                                    (update :primary str " - Sold Out")
                                                                    (assoc :primary-attrs {:class "red"}  ;; TODO: too low contrast
                                                                           :image-attrs {:style {:opacity "50%"}}))

                                                                :else picker-base)))
                                                          selected-lengths)}))))

(defn ^:private color-product-options->color-picker-modal-options
  [options selections]
  (mapv (fn [{:option/keys [slug sku-swatch rectangle-swatch name] :as option}]
          #:option{:id               (str "picker-color-" (facets/hacky-fix-of-bad-slugs-on-facets slug))
                   :selection-target [events/control-product-detail-picker-option-select
                                      (merge {:selection        :hair/color
                                              :value            slug
                                              :navigation-event events/navigate-product-details})]
                   :checked?         (= (:hair/color (first selections)) slug)
                   :label            name
                   :value            slug
                   :bg-image-src     rectangle-swatch
                   :available?       true
                   :image-src        sku-swatch})
        options))

(defn ^:private length-product-options->length-picker-modal-options
  [options selections length-index availability]
  (let [selected-hair-color  (:hair/color (first selections))
        selected-hair-length (:hair/length (get selections length-index))]
    (when length-index
      (concat
       (when (and (> length-index 0)
                  (not-empty (nth selections length-index)))
         [{:option/value            ""
           :option/id               "picker-length-remove"
           :option/label            "Remove Length"
           :option/available?       true
           :option/selection-target [events/control-product-detail-picker-option-length-select {:index            length-index
                                                                                                :selection        :hair/length
                                                                                                :navigation-event events/navigate-product-details
                                                                                                :value            ""}]}])
       (map (fn [{:option/keys [slug name] :as option}]
              (let [available? (boolean (get-in availability [selected-hair-color slug]))
                    sold-out?  (not (boolean (get-in availability [selected-hair-color
                                                                   slug
                                                                   :inventory/in-stock?])))]
                (merge
                 #:product{:option option}
                 #:option{:id               (str "picker-length-" slug)
                          :selection-target [events/control-product-detail-picker-option-length-select
                                             {:selection        :hair/length
                                              :value            slug
                                              :navigation-event events/navigate-product-details
                                              :index            length-index}]
                          :checked?         (= selected-hair-length slug)
                          :label            name
                          :value            slug
                          :available?       available?}
                 (when sold-out?
                   #:option{:label-attrs      {:class "dark-gray"}
                            :label            (str name " - Sold Out")
                            :selection-target nil})
                 (when-not available?
                   #:option{:label-attrs      {:class "dark-gray"}
                            :label            (str name " - Unavailable")
                            :selection-target nil}))))
            options)))))

(defn ^:private picker-modal<
  [product-options picker-visible? selected-picker length-index multi-length-selections availability length-guide-image]
  (let [picker-type        (last selected-picker)
        options            (get product-options picker-type)
        options-for-color  (color-product-options->color-picker-modal-options options multi-length-selections)
        options-for-length (length-product-options->length-picker-modal-options options multi-length-selections length-index availability)]
    {:picker-modal/title              (case picker-type
                                        :hair/color  "Color"
                                        :hair/length "Length"
                                        nil)
     :picker-modal/type               picker-type
     :picker-modal/options            (case picker-type
                                        :hair/color  options-for-color
                                        :hair/length options-for-length
                                        nil)
     :picker-modal/length-guide-image length-guide-image
     ;; NOTE: There is a difference between selected and visible. We toggle
     ;; picker visibility to signal that the modal should close but we don't remove
     ;; the options so the close animation isn't stopped prematurely due to the
     ;; child options re-rendering.
     :picker-modal/visible?           (and picker-visible? options selected-picker)
     :picker-modal/close-target       [events/control-product-detail-picker-close]}))

(defn ^:private total-query
  [multi-length-total item-count shop?]
  (let [still-to-go (- 3 item-count)]
    #:total{:primary (cond (and shop? (> still-to-go 0))
                           (str "Add " still-to-go " more for Free Install")

                           shop?
                           "Hair + Free Install"

                           :else nil)
            :secondary (mf/as-money-or-dashes multi-length-total)}))

(defn query [data]
  (let [selections              (get-in data catalog.keypaths/detailed-product-selections)
        product                 (products/current-product data)
        product-skus            (products/extract-product-skus data product)
        images-catalog          (get-in data keypaths/v2-images)
        facets                  (facets/by-slug data)
        selected-sku            (get-in data catalog.keypaths/detailed-product-selected-sku)
        carousel-images         (find-carousel-images product product-skus images-catalog
                                                      ;;TODO These selection election keys should not be hard coded
                                                      (select-keys selections [:hair/color
                                                                               :hair/base-material])
                                                      selected-sku)
        length-guide-image      (->> product
                                     (images/for-skuer images-catalog)
                                     (select {:use-case #{"length-guide"}})
                                     first)
        product-options         (get-in data catalog.keypaths/detailed-product-options)
        ugc                     (ugc-query product selected-sku data)
        sku-price               (or (:product/essential-price selected-sku)
                                    (:sku/price selected-sku))
        review-data             (review-component/query data)
        shop?                   (or (= "shop" (get-in data keypaths/store-slug))
                                    (= "retail-location" (get-in data keypaths/store-experience)))
        hair?                   (accessors.products/hair? product)
        faq                     (when-let [pdp-faq-id (accessors.products/product->faq-id product)]
                                  (get-in data (conj keypaths/cms-faq pdp-faq-id)))
        multi-length-selections (get-in data catalog.keypaths/detailed-product-multiple-lengths-selections)
        selected-picker         (get-in data catalog.keypaths/detailed-product-selected-picker)
        sku-availability        (catalog.products/index-by-selectors
                                 [:hair/color :hair/length]
                                 product-skus)
        chosen-skus             (->> (get-in data catalog.keypaths/detailed-product-multiple-lengths-selections)
                                     (filterv not-empty)
                                     (mapv
                                      #(determine-sku-from-selections data (merge selections %))))
        multi-length-total      (if (some nil? chosen-skus)
                                  nil
                                  (reduce +
                                          (->>
                                           chosen-skus
                                           (mapv #(:sku/price %)))))]
  (merge
   {:reviews                            review-data
    :yotpo-reviews-summary/product-name (some-> review-data :yotpo-data-attributes :data-name)
    :yotpo-reviews-summary/product-id   (some-> review-data :yotpo-data-attributes :data-product-id)
    :yotpo-reviews-summary/data-url     (some-> review-data :yotpo-data-attributes :data-url)
    :title/primary                      (:copy/title product)
    :ugc                                ugc
    :fetching-product?                  (utils/requesting? data (conj request-keys/get-products
                                                                      (:catalog/product-id product)))
    :adding-to-bag?                     (utils/requesting? data (conj request-keys/add-to-bag
                                                                      (:catalog/sku-id selected-sku)))
    :sku-quantity                       (get-in data keypaths/browse-sku-quantity 1)
    :options                            product-options
    :product                            product
    :selections                         selections
    :selected-options                   (get-selected-options selections product-options)
    :selected-sku                       selected-sku
    :facets                             facets
    :faq-section                        (when (and shop? faq)
                                          (let [{:keys [question-answers]} faq]
                                            {:faq/expanded-index (get-in data keypaths/faq-expanded-section)
                                             :list/sections      (for [{:keys [question answer]} question-answers]
                                                                   {:faq/title   (:text question)
                                                                    :faq/content answer})}))
    :carousel-images                    carousel-images
    :selected-picker                    selected-picker
    ;; Problem? separate picker modal and picker-query queries.
    :picker-modal                       (when selected-picker
                                          (picker-modal< product-options
                                                         (get-in data catalog.keypaths/detailed-product-picker-visible?)
                                                         (get-in data catalog.keypaths/detailed-product-selected-picker)
                                                         (get-in data catalog.keypaths/detailed-product-lengths-index)
                                                         multi-length-selections
                                                         sku-availability
                                                         length-guide-image))}
   (picker-query {:facets            facets
                  :selections        selections
                  :options           product-options
                  :length-selections multi-length-selections
                  :sku-availability  sku-availability})
     (total-query multi-length-total (count chosen-skus) shop?)
     (when (-> (get-in data catalog.keypaths/detailed-product-multiple-lengths-selections) count (< 5))
       #:add-length{:id    "add-length"
                    :event events/control-product-detail-picker-add-length})
     (when sku-price
       {:price-block/primary   "Starting at"
        :price-block/secondary (mf/as-money sku-price)})

     (if hair?
       (let [active-tab-name (get-in data keypaths/product-details-information-tab)]
         #:tabbed-information{:id      "product-description-tabs"
                              :keypath keypaths/product-details-information-tab
                              :tabs    [{:title    "Hair Info"
                                         :id       :hair-info
                                         :active?  (= active-tab-name :hair-info)
                                         :icon     {:opts {:height "20px"
                                                           :width  "20px"}
                                                    :id   "info-color-circle"}
                                         :sections (keep (partial tab-section< {:product            product
                                                                                :selected-sku       selected-sku
                                                                                :chosen-sku-weights (->> chosen-skus
                                                                                                         (map (juxt :hair/length :hair/weight))
                                                                                                         not-empty
                                                                                                         (map (fn [[length weight]]
                                                                                                                (str (first length) "\" - " (first weight))))
                                                                                                         sort)
                                                                                :chosen-skus        chosen-skus})
                                                         [(merge
                                                           {:heading      "Model Wearing"
                                                            :content-path [:product :copy/model-wearing]}
                                                           (when length-guide-image
                                                             {:link/content "Length Guide"
                                                              :link/target  [events/popup-show-length-guide
                                                                             {:length-guide-image length-guide-image
                                                                              :location           "hair-info-tab"}]
                                                              :link/id      "hair-info-tab-length-guide"}))
                                                          {:heading               "Unit Weight"
                                                           :content-path          [:chosen-sku-weights]
                                                           :fallback-content-path [:product :copy/weights]}
                                                          {:heading      "Hair Quality"
                                                           :content-path [:product :copy/quality]}
                                                          {:heading      "Hair Origin"
                                                           :content-path [:product :copy/origin]}
                                                          {:heading      "Hair Weft Type"
                                                           :content-path [:product :copy/weft-type]}
                                                          {:heading      "Part Design"
                                                           :content-path [:product :copy/part-design]}
                                                          {:heading      "Features"
                                                           :content-path [:product :copy/features]}
                                                          {:heading      "Available Materials"
                                                           :content-path [:product :copy/materials]}
                                                          {:heading      "Lace Size"
                                                           :content-path [:product :copy/lace-size]}
                                                          {:heading      "Silk Size"
                                                           :content-path [:product :copy/silk-size]}
                                                          {:heading      "Cap Size"
                                                           :content-path [:product :copy/cap-size]}
                                                          {:heading      "Wig Density"
                                                           :content-path [:product :copy/density]}
                                                          {:heading      "Tape-in Glue Information"
                                                           :content-path [:product :copy/tape-in-glue]}])}
                                        {:title    "Description"
                                         :id       :description
                                         :active?  (= active-tab-name :description)
                                         :icon     {:opts {:height "18px"
                                                           :width  "18px"}
                                                    :id   "description"}
                                         :primary  (:copy/description product)
                                         :sections (keep (partial tab-section< {:product      product
                                                                                :selected-sku selected-sku
                                                                                :chosen-skus  chosen-skus})
                                                         [{:heading      "Hair Type"
                                                           :content-path [:product :copy/hair-type]}
                                                          {:heading      "What's Included"
                                                           :content-path [:product :copy/whats-included]}
                                                          {:heading      "Available Services"
                                                           :content-path [:product :copy/available-services]}])}
                                        {:title    "Care"
                                         :id       :care
                                         :active?  (= active-tab-name :care)
                                         :icon     {:opts {:height "20px"
                                                           :width  "20px"}
                                                    :id   "heart"}
                                         :sections (keep (partial tab-section< {:product      product
                                                                                :selected-sku selected-sku
                                                                                :chosen-skus  chosen-skus})
                                                         [{:heading      "Maintenance Level"
                                                           :content-path [:product :copy/maintenance-level]}
                                                          {:heading      "Can it be Dyed?"
                                                           :content-path [:product :copy/dyeable?]}])}]})
       (let [{:keys [copy/description
                     copy/colors
                     copy/weights
                     copy/density
                     copy/materials
                     copy/whats-included
                     copy/summary
                     copy/duration]} product]
         #:product-description {:duration             duration
                                :summary              summary
                                :description          description
                                :materials            materials
                                :colors               colors
                                :density              density
                                :whats-included       whats-included
                                :weights              (when-not density
                                                        weights)
                                :learn-more-nav-event (when-not (contains? (:stylist-exclusives/family product) "kits")
                                                        events/navigate-content-our-hair)})))))

(defmethod transitions/transition-state events/control-product-detail-picker-option-length-select
  [_ event {:keys [selection value index]} app-state]
  (let [color        (-> app-state
                         (get-in catalog.keypaths/detailed-product-multiple-lengths-selections)
                         first
                         :hair/color)
        selected-sku (if (= 0 index)
                       (determine-sku-from-selections app-state {selection #{value}})
                       (get-in app-state catalog.keypaths/detailed-product-selected-sku))]
    (if (empty? value)
      (-> app-state
          (assoc-in catalog.keypaths/detailed-product-selected-sku selected-sku)
          (assoc-in catalog.keypaths/detailed-product-picker-visible? false)
          (update-in  (conj catalog.keypaths/detailed-product-multiple-lengths-selections index) empty))
      (-> app-state
          (assoc-in catalog.keypaths/detailed-product-selected-sku selected-sku)
          (assoc-in catalog.keypaths/detailed-product-picker-visible? false)
          (update-in (conj catalog.keypaths/detailed-product-multiple-lengths-selections index) merge {selection   value
                                                                                                       :hair/color color})))))

(defmethod effects/perform-effects events/control-product-detail-picker-option-length-select
  [_ event {:keys [navigation-event]} _ app-state]
  (let [sku-id-for-selection (-> app-state
                                 (get-in catalog.keypaths/detailed-product-selected-sku)
                                 :catalog/sku-id)
        params-with-sku-id   (cond-> (select-keys (products/current-product app-state)
                                                  [:catalog/product-id :page/slug])
                               (some? sku-id-for-selection)
                               (assoc :query-params {:SKU sku-id-for-selection}))]
    (effects/redirect navigation-event params-with-sku-id :sku-option-select)
    #?(:cljs (scroll/enable-body-scrolling))))

(defn ^:private sku->data-sku-reference
  [s]
  (select-keys s [:catalog/sku-id :legacy/variant-id]) )

(defn ^:private selections->product-selections
  [selections]
  (mapv
   #(merge (select-keys selections [:hair/color :hair/length])
           (select-keys % [:hair/color :hair/length]))
   selections))

(defn ^:private product-selections->skus
  [availability product-selections]
  (mapv
   (fn [{:hair/keys [color length]}]
     (get-in availability [color length]))
   product-selections))

(defn ^:private ->data-event-format
  [selections availability]
  (let [product-selections (selections->product-selections selections)]
    {:products           (->> product-selections
                              (product-selections->skus availability)
                              (mapv sku->data-sku-reference)
                              (map #(when (seq %) %)))
     :product-selections product-selections}))

#?(:cljs
   (defmethod trackings/perform-track events/control-product-detail-picker-option-length-select
     [_ event {:keys [selection value index] :as options} app-state]
     (stringer/track-event "look_facet-changed"
                           (merge {:position        index
                                   :selected-length value
                                   :facet-selected  selection}
                                  (->data-event-format
                                   (get-in app-state catalog.keypaths/detailed-product-multiple-lengths-selections)
                                   (get-in app-state catalog.keypaths/detailed-product-availability))))))

(defmethod transitions/transition-state events/control-product-detail-picker-add-length
  [_ _ {} state]
  (-> state
      (update-in catalog.keypaths/detailed-product-multiple-lengths-selections conj {})))

#?(:cljs
   (defmethod storefront.trackings/perform-track events/control-product-detail-picker-open
     [_ _ {:keys [facet-slug length-index] :as args} state]
     (when (and (-> (products/current-product state) :hair/family first (= "bundles"))
                (experiments/multiple-lengths-pdp? state))
       (let [picker-name (last facet-slug)]
         (stringer/track-event "look_facet-clicked" (merge {:facet-selected picker-name}
                                                           (when (= "length" picker-name)
                                                             {:position length-index})))))))
