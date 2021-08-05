(ns catalog.product-details
  (:require #?@(:cljs [[goog.dom]
                       [goog.events]
                       [goog.style]
                       [storefront.accessors.auth :as auth]
                       [storefront.api :as api]
                       [storefront.browser.scroll :as scroll]
                       [storefront.components.popup :as popup]
                       [storefront.components.svg :as svg]
                       [storefront.history :as history]
                       [storefront.hooks.facebook-analytics :as facebook-analytics]
                       [storefront.hooks.reviews :as review-hooks]
                       [storefront.hooks.stringer :as stringer]
                       [storefront.hooks.seo :as seo]
                       [storefront.trackings :as trackings]])
            [api.catalog :refer [select ?cart-product-image]]
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
            [checkout.cart.swap :as swap]
            [homepage.ui.faq :as faq]
            [mayvenn.live-help.core :as live-help]
            [mayvenn.visual.lib.call-out-box :as call-out-box]
            [mayvenn.visual.tools :refer [with]]
            [mayvenn.visual.ui.titles :as titles]
            [spice.selector :as selector]
            [storefront.accessors.contentful :as contentful]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.images :as images]
            [storefront.accessors.products :as accessors.products]
            [storefront.accessors.sites :as sites]
            [storefront.accessors.skus :as skus]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.money-formatters :as mf]
            [storefront.components.picker.picker :as old-picker]
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
            storefront.ugc

            [spice.maps :as maps]
            [storefront.components.picker.picker-two :as picker-two]))

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
    [:div.col-2
     (catalog.M/price-block data)]]
   (catalog.M/yotpo-reviews-summary data)])

(defcomponent component
  [{:keys [carousel-images
           product
           reviews
           selected-sku
           old-picker-data
           ugc
           faq-section
           add-to-cart
           live-help
           picker-modal
           multiple-lengths-pdp?] :as data} _ opts]
  (let [unavailable? (not (seq selected-sku))
        sold-out?    (not (:inventory/in-stock? selected-sku))]
    (if-not product
      [:div.flex.h2.p1.m1.items-center.justify-center
       {:style {:height "25em"}}
       (ui/large-spinner {:style {:height "4em"}})]
      [:div
       (when multiple-lengths-pdp?
         (component/build picker-two/modal picker-modal))
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
            [:div.my5
             (when live-help
               (component/build call-out-box/variation-2 (update live-help :action/id str "-desktop")))]
            (component/build ugc/component ugc opts)])
          (component/html
           [:div
            [:div
             (full-bleed-narrow
              [:div (carousel carousel-images product)])]
            (component/build product-summary-organism data)
            [:div.px2
             (if multiple-lengths-pdp?
               (component/build picker-two/picker-one-combo-face (:pickers data) opts)
               (component/build old-picker/component old-picker-data opts))]
            [:div
             (cond
               unavailable? unavailable-button
               sold-out?    sold-out-button
               :else        (component/build add-to-cart/organism add-to-cart))]
            (when (products/stylist-only? product)
              shipping-and-guarantee)
            (component/build tabbed-information/component data)
            (component/build catalog.M/non-hair-product-description data opts)
            [:div.hide-on-tb-dt.m3
             (when live-help (component/build call-out-box/variation-1 live-help))
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
       :offset        (get-in data keypaths/ui-ugc-category-popup-offset)
       :close-message [events/navigate-product-details
                       {:catalog/product-id (:catalog/product-id product)
                        :page/slug          (:page/slug product)
                        :query-params       {:SKU (:catalog/sku-id sku)}}]})))

(defn find-carousel-images [product product-skus images-catalog selections selected-sku]
  (->> (selector/match-all {}
                           (or selected-sku
                               selections
                               (first product-skus))
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

(defmethod transitions/transition-state events/popup-show-length-guide
  [_ _ {:keys [length-guide-image]} state]
  (-> state
      (assoc-in keypaths/popup :length-guide)
      (assoc-in catalog.keypaths/length-guide-image length-guide-image)))

#?(:cljs
   (defmethod trackings/perform-track events/popup-show-length-guide
     [_ event {:keys [location] :as args} state]
     (stringer/track-event "length_guide_link_pressed"
                           {:location location})))

(defn add-to-cart-query
  [app-state
   selected-sku]
  (let [shop?                              (or (= "shop" (get-in app-state keypaths/store-slug))
                                               (= "retail-location" (get-in app-state keypaths/store-experience)))
        sku-price                          (:sku/price selected-sku)
        quadpay-loaded?                    (get-in app-state keypaths/loaded-quadpay)
        sku-family                         (-> selected-sku :hair/family first)
        mayvenn-install-incentive-families #{"bundles" "closures" "frontals" "360-frontals"}]
    (merge
     {:cta/id                      "add-to-cart"
      :cta/label                   "Add to Bag"
      :cta/target                  [events/control-add-sku-to-bag
                                    {:sku      selected-sku
                                     :quantity (get-in app-state (if (experiments/multiple-lengths-pdp? app-state)
                                                                   (conj catalog.keypaths/detailed-pdp-selections :quantity)
                                                                   keypaths/browse-sku-quantity) 1)}]
      :cta/spinning?               (utils/requesting? app-state (conj request-keys/add-to-bag (:catalog/sku-id selected-sku)))
      :cta/disabled?               (not (:inventory/in-stock? selected-sku))
      :add-to-cart.quadpay/price   sku-price
      :add-to-cart.quadpay/loaded? quadpay-loaded?}
     (when (and shop?
                (mayvenn-install-incentive-families sku-family))
       {:add-to-cart.incentive-block/id          "add-to-cart-incentive-block"
        :add-to-cart.incentive-block/footnote    "*Mayvenn Services cannot be combined with other promotions"
        :add-to-cart.incentive-block/link-id     "learn-more-mayvenn-install"
        :add-to-cart.incentive-block/link-label  "Learn more"
        :add-to-cart.incentive-block/link-target [events/popup-show-consolidated-cart-free-install]
        :add-to-cart.incentive-block/callout     "✋Don't miss out on free Mayvenn Service"
        :add-to-cart.incentive-block/message     (str "Get a free Mayvenn Service by a licensed "
                                                      "stylist with qualifying purchases.* ")}))))

(defn ^:private tab-section<
  [product
   {:keys [heading content-key]
    :as   section}]
  (when-let [content (get product content-key)]
    (merge
     {:heading heading
      :content content}
     (select-keys section [:link/content :link/target :link/id]))))

;; NEW
(defn ^:private pickers<
  [facets-db
   skus-db
   selections
   picker-options
   availability]
  {:quantity-picker (let [quantity-options (:quantity picker-options)
                          selected-option  (->> quantity-options
                                                (filter :option/checked?)
                                                first)]
                      {:id               "picker-quantity"
                       :value-id         "picker-selected-quantity-1"
                       :options          quantity-options
                       :primary          (:option/label selected-option)
                       :selected-value   (:option/value selected-option)
                       :selection-target (:option/selection-target selected-option)
                       :open-target      [events/control-pdp-picker-open
                                          {:picker-id [:quantity]}]})

   :color-picker (let [{:option/keys [rectangle-swatch name slug]}
                       (get-in facets-db [:hair/color
                                          :facet/options
                                          (get-in selections [:hair/color])])
                       color-option    (:hair/color picker-options)
                       selected-option (->> color-option
                                            (filter :option/checked?)
                                            first)]
                   {:id               "picker-color"
                    :value-id         (str "picker-selected-color-" (facets/hacky-fix-of-bad-slugs-on-facets slug))
                    :image-src        rectangle-swatch
                    :primary          name
                    :options          (:hair/color picker-options)
                    :selected-value   slug
                    :selection-target (:option/selection-target selected-option)
                    :open-target      [events/control-pdp-picker-open {:picker-id [:hair/color]}]})

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

            hair-length-facet-option
            (get-in facets-db [:hair/length
                               :facet/options
                               selected-hair-length])

            picker-data {:id               (str "picker-length-" index)
                         :value-id         (str "picker-selected-length-" index "-" (:option/slug hair-length-facet-option))
                         :image-src        (->> hair-family-and-color-skus first :selector/images (select ?cart-product-image) first :url)
                         :options          length-options
                         :primary          (:option/name hair-length-facet-option)
                         :selected-value   (:option/slug hair-length-facet-option)
                         :selection-target [events/control-pdp-picker-option-select {:selection [:per-item index :hair/length]}]
                         :open-target      [events/control-pdp-picker-open {:picker-id [:per-item index :hair/length]}]}]
        (cond
          (not (boolean
                (get-in availability [item-hair-family
                                      (:hair/color selections)
                                      selected-hair-length])))
          (-> picker-data
              (update :primary str " - Unavailable")
              (assoc :primary-attrs {:class "red"}  ;; TODO: too low contrast
                     :image-attrs {:style {:opacity "50%"}}))

          (not (boolean
                (get-in availability [item-hair-family
                                      (:hair/color selections)
                                      selected-hair-length
                                      :inventory/in-stock?])))
          (-> picker-data
              (update :primary str " - Sold Out")
              (assoc :primary-attrs {:class "red"}  ;; TODO: too low contrast
                     :image-attrs {:style {:opacity "50%"}}))

          :else picker-data)))
    (->> picker-options :per-item (mapv :hair/length)))})

(defn ^:private picker-modal<
  [picker-options picker-visible? selected-picker length-guide-image]
  (let [picker-type (last selected-picker)
        options     (get-in picker-options selected-picker)]
    {:picker-modal/title        (case picker-type
                                  :hair/color  "Color"
                                  :hair/length "Length"
                                  :quantity    "Quantity"
                                  nil)
     :picker-modal/type         picker-type
     :picker-modal/options      options
     ;; NOTE: There is a difference between selected and visible. We toggle
     ;; picker visibility to signal that the modal should close but we don't remove
     ;; the options so the close animation isn't stopped prematurely due to the
     ;; child options re-rendering.
     :picker-modal/visible?     (and picker-visible? options selected-picker)
     :picker-modal/close-target [events/control-pdp-picker-close]
     :picker-modal/length-guide-image length-guide-image}))

(defn query [data selected-sku]
  (let [selections (get-in data catalog.keypaths/detailed-product-selections)
        product    (products/current-product data)

        product-skus       (products/extract-product-skus data product)
        images-catalog     (get-in data keypaths/v2-images)
        facets             (facets/by-slug data)
        carousel-images    (find-carousel-images product product-skus images-catalog
                                                 (select-keys selections [:hair/color])
                                                 selected-sku)
        length-guide-image (->> product
                                (images/for-skuer images-catalog)
                                (select {:use-case #{"length-guide"}})
                                first)
        options            (get-in data catalog.keypaths/detailed-product-options)
        ugc                (ugc-query product selected-sku data)
        sku-price          (or (:product/essential-price selected-sku)
                               (:sku/price selected-sku))
        review-data        (review-component/query data)
        shop?              (or (= "shop" (get-in data keypaths/store-slug))
                               (= "retail-location" (get-in data keypaths/store-experience)))
        hair?              (accessors.products/hair? product)
        faq                (when-let [pdp-faq-id (accessors.products/product->faq-id product)]
                             (get-in data (conj keypaths/cms-faq pdp-faq-id)))]
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
      :options                            options
      :product                            product
      :selections                         selections
      :selected-options                   (get-selected-options selections options)
      :selected-sku                       selected-sku
      :facets                             facets
      :faq-section                        (when (and shop? faq)
                                            (let [{:keys [question-answers]} faq]
                                              {:faq/expanded-index (get-in data keypaths/faq-expanded-section)
                                               :list/sections      (for [{:keys [question answer]} question-answers]
                                                                     {:faq/title   (:text question)
                                                                      :faq/content answer})}))
      :carousel-images                    carousel-images
      :selected-picker                    (get-in data catalog.keypaths/detailed-product-selected-picker)
      :old-picker-data                    (old-picker/query data length-guide-image)
      :picker-modal                       (picker-modal< (get-in data catalog.keypaths/detailed-pdp-options)
                                                         (get-in data catalog.keypaths/detailed-pdp-picker-visible?)
                                                         (get-in data catalog.keypaths/detailed-pdp-selected-picker)
                                                         length-guide-image)
      :pickers                            (pickers< facets
                                                    product-skus
                                                    (get-in data catalog.keypaths/detailed-pdp-selections)
                                                    (get-in data catalog.keypaths/detailed-pdp-options)
                                                    (get-in data catalog.keypaths/detailed-pdp-availability))
      :multiple-lengths-pdp?              (experiments/multiple-lengths-pdp? data)}


     (when sku-price
       {:price-block/primary   (mf/as-money sku-price)
        :price-block/secondary "each"})

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
                                         :sections (keep (partial tab-section< product)
                                                         [(merge
                                                           {:heading     "Model Wearing"
                                                            :content-key :copy/model-wearing}
                                                           (when length-guide-image
                                                             {:link/content "Length Guide"
                                                              :link/target  [events/popup-show-length-guide
                                                                             {:length-guide-image length-guide-image
                                                                              :location           "hair-info-tab"}]
                                                              :link/id      "hair-info-tab-length-guide"}))
                                                          {:heading     "Unit Weight"
                                                           :content-key :copy/weights}
                                                          {:heading     "Hair Quality"
                                                           :content-key :copy/quality}
                                                          {:heading     "Hair Origin"
                                                           :content-key :copy/origin}
                                                          {:heading     "Hair Weft Type"
                                                           :content-key :copy/weft-type}
                                                          {:heading     "Part Design"
                                                           :content-key :copy/part-design}
                                                          {:heading     "Features"
                                                           :content-key :copy/features}
                                                          {:heading     "Available Materials"
                                                           :content-key :copy/materials}
                                                          {:heading     "Lace Size"
                                                           :content-key :copy/lace-size}
                                                          {:heading     "Lace Color"
                                                           :content-key :copy/lace-color}
                                                          {:heading     "Silk Size"
                                                           :content-key :copy/silk-size}
                                                          {:heading     "Silk Color"
                                                           :content-key :copy/silk-color}
                                                          {:heading     "Cap Size"
                                                           :content-key :copy/cap-size}
                                                          {:heading     "Wig Density"
                                                           :content-key :copy/density}
                                                          {:heading     "Tape-in Glue Information"
                                                           :content-key :copy/tape-in-glue}])}
                                        {:title    "Description"
                                         :id       :description
                                         :active?  (= active-tab-name :description)
                                         :icon     {:opts {:height "18px"
                                                           :width  "18px"}
                                                    :id   "description"}
                                         :primary  (:copy/description product)
                                         :sections (keep (partial tab-section< product)
                                                         [{:heading     "Hair Type"
                                                           :content-key :copy/hair-type}
                                                          {:heading     "What's Included"
                                                           :content-key :copy/whats-included}
                                                          {:heading     "Available Services"
                                                           :content-key :copy/available-services}])}
                                        {:title    "Care"
                                         :id       :care
                                         :active?  (= active-tab-name :care)
                                         :icon     {:opts {:height "20px"
                                                           :width  "20px"}
                                                    :id   "heart"}
                                         :sections (keep (partial tab-section< product)
                                                         [{:heading     "Maintenance Level"
                                                           :content-key :copy/maintenance-level}
                                                          {:heading     "Can it be Dyed?"
                                                           :content-key :copy/dyeable?}])}]})
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

(defn ^:export built-component
  [state opts]
  (let [selected-sku (get-in state catalog.keypaths/detailed-product-selected-sku)]
    (component/build component
                     (merge (query state selected-sku)
                            {:add-to-cart (add-to-cart-query state
                                                             selected-sku)
                             :live-help   (when (live-help/kustomer-started? state)
                                            (live-help/banner-query "product-detail-page-banner"))})
                     opts)))

;; GROT
(defn determine-sku-id
  [app-state product]
  (let [selected-sku-id    (get-in app-state catalog.keypaths/detailed-product-selected-sku-id)
        valid-product-skus (products/extract-product-skus app-state product)
        valid-sku-ids      (set (map :catalog/sku-id valid-product-skus))
        direct-load?       (zero? (count (get-in app-state keypaths/navigation-undo-stack)))
        direct-to-details? (contains? (->> (get-in app-state keypaths/categories)
                                           (keep :direct-to-details/sku-id)
                                           set)
                                      (or (:SKU (:query-params (get-in app-state storefront.keypaths/navigation-args)))
                                          selected-sku-id))]
    ;; When given a sku-id use it
    ;; Else on direct load use the epitome
    ;; otherwise return nil to indicate an unavailable combination of items
    (or (valid-sku-ids selected-sku-id)
        (when (or direct-load?
                  direct-to-details?
                  ;; HACK: Handle the case with services that the product and sku are 1-1
                  (:SKU (:query-params (get-in app-state storefront.keypaths/navigation-args))))
          (:catalog/sku-id
           (skus/determine-epitome
            (facets/color-order-map (get-in app-state storefront.keypaths/v2-facets))
            valid-product-skus))))))

(defn url-points-to-invalid-sku? [selected-sku query-params]
  (boolean
   (and (:catalog/sku-id selected-sku)
        (not= (:catalog/sku-id selected-sku)
              (:SKU query-params)))))

#?(:cljs
   (defn fetch-product-details [app-state product-id]
     (api/get-products (get-in app-state keypaths/api-cache)
                       {:catalog/product-id product-id}
                       (fn [response]
                         (messages/handle-message events/api-success-v3-products-for-details response)
                         (when-let [selected-sku (get-in app-state catalog.keypaths/detailed-product-selected-sku)]
                           (messages/handle-message events/viewed-sku {:sku selected-sku}))))

     (when-let [current-product (products/current-product app-state)]
       (if (auth/permitted-product? app-state current-product)
         (review-hooks/insert-reviews)
         (effects/redirect events/navigate-home)))))

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

(defn generate-product-options
  [product-id app-state]
  (let [product      (products/product-by-id app-state product-id)
        facets       (facets/by-slug app-state)
        product-skus (products/extract-product-skus app-state product)
        images       (get-in app-state keypaths/v2-images)]
    (sku-selector/product-options facets product product-skus images)))

(defn ^:private assoc-selections
  "Selections are ultimately a function of three inputs:
   1. options of the cheapest sku
   2. prior selections on another product, but validated for this product
   3. options of the sku from the URI

   They are merged together each (possibly partially) overriding the
   previous selection map of options.

   NB: User clicks for selections are indirectly used by changing the URI."
  [app-state sku]
  (let [product-id   (get-in app-state catalog.keypaths/detailed-product-id)
        product      (products/product-by-id app-state product-id)
        facets       (facets/by-slug app-state)
        product-skus (products/extract-product-skus app-state product)
        images       (get-in app-state keypaths/v2-images)]
    (-> app-state
        (assoc-in catalog.keypaths/detailed-product-selections
                  (merge
                   (default-selections facets product product-skus images)
                   (let [{:catalog/keys [department]
                          :product?essential-service/keys [electives]}
                         (api.products/product<- app-state product)]
                     (when (= #{"service"} department)
                       electives))
                   (get-in app-state catalog.keypaths/detailed-product-selections)
                   (reduce-kv #(assoc %1 %2 (first %3))
                              {}
                              (select-keys sku (:selector/electives product))))))))

(defmethod transitions/transition-state events/control-pdp-picker-open
  [_ event {:keys [picker-id]} app-state]
  (-> app-state
      (assoc-in catalog.keypaths/detailed-pdp-selected-picker picker-id)
      (assoc-in catalog.keypaths/detailed-pdp-picker-visible? true)))

#?(:cljs
   (defmethod storefront.trackings/perform-track events/control-pdp-picker-open
     [_ _ {:keys [picker-id] :as args} _]
     ;; TODO
     #_
     (let [picker-name (name (last picker-id))]
       (stringer/track-event "look_facet-clicked" (merge {:facet-selected picker-name}
                                                         (when (= "length" picker-name)
                                                           {:position (second picker-id)}))))))

(defmethod transitions/transition-state events/control-pdp-picker-close
  [_ event _ app-state]
  (assoc-in app-state catalog.keypaths/detailed-pdp-picker-visible? false))

;; NEW
#?(:cljs
   (defmethod storefront.trackings/perform-track events/control-pdp-picker-option-select
     [_ _ {:keys [selection value]} app-state]
     (let [picker-name (name (last selection))]
       ;; TODO: make sure option tracking is working on PDP picker v1
       #_(stringer/track-event "look_facet-changed"
                             (merge (case picker-name
                                      "color"
                                      {:selected-color value}

                                      "length"
                                      {:position        (second selection)
                                       :selected-length value}
                                      nil)
                                    {:facet-selected picker-name}
                                    (->data-event-format
                                     (get-in app-state catalog.keypaths/detailed-pdp-selections)
                                     (get-in app-state catalog.keypaths/detailed-pdp-availability)
                                     (get-in app-state catalog.keypaths/detailed-pdp-services)))))))

;; NEW
(defmethod transitions/transition-state events/control-pdp-picker-option-select
  [_ event {:keys [selection value]} app-state]
  (let [new-selections (-> app-state
                           (get-in catalog.keypaths/detailed-pdp-selections)
                           (assoc-in selection value))]
    (cond->
        (-> app-state
            (assoc-in catalog.keypaths/detailed-pdp-selections new-selections)
            (update-in (concat catalog.keypaths/detailed-pdp-options selection)
                       (partial mapv (fn [option] (assoc option :option/checked? (= (:option/value option) value))))))
      (= [:hair/color] selection)
      ;; TODO(jjh) What does this part do???
      (update-in (conj catalog.keypaths/detailed-pdp-options :per-item)
                 (fn [per-item-options]
                   (->> per-item-options
                        (map-indexed
                         (fn [index per-item-options]
                           (update per-item-options
                                   :hair/length
                                   (partial mapv
                                            identity
                                            #_(comp (partial product-option->length-picker-option
                                                           availability
                                                           new-selections
                                                           index)
                                                    :product/option)))))
                        vec))))))

;; NEW
(defmethod effects/perform-effects events/control-pdp-picker-option-select
  [_ event _args _ app-state]
  (let [[nav-event nav-args] (get-in app-state keypaths/navigation-message)
        availability         (get-in app-state catalog.keypaths/detailed-pdp-availability)
        selections           (get-in app-state catalog.keypaths/detailed-pdp-selections)
        color                (:hair/color selections)
        {:hair/keys
         [family length]}    (-> selections :per-item first)
        new-sku-id           (get-in availability [family color length :catalog/sku-id])]
    (if (-> nav-args
            :query-params
            :SKU
            (= new-sku-id))
      (messages/handle-message events/control-pdp-picker-close)
      (effects/redirect nav-event
                       (assoc-in nav-args [:query-params :SKU] new-sku-id)
                       :sku-option-select))
    #?(:cljs (scroll/enable-body-scrolling)) ; TODO(jjh): cargo cult
    ))

;; OLD
(defmethod transitions/transition-state events/control-product-detail-picker-option-select
  [_ event {:keys [selection value]} app-state]
  (let [selected-sku (->> {selection #{value}}
                          (determine-sku-from-selections app-state))
        options      (generate-product-options (get-in app-state catalog.keypaths/detailed-product-id)
                      app-state)]
    (-> app-state
        (assoc-in catalog.keypaths/detailed-product-selected-sku selected-sku)
        (update-in catalog.keypaths/detailed-product-selections merge {selection value})
        (assoc-in catalog.keypaths/detailed-product-options options))))

;; OLD
(defmethod effects/perform-effects events/control-product-detail-picker-option-select
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

;; OLD
(defmethod transitions/transition-state events/control-product-detail-picker-open
  [_ event {:keys [facet-slug]} app-state]
  (-> app-state
      (assoc-in catalog.keypaths/detailed-product-selected-picker facet-slug)
      (assoc-in catalog.keypaths/detailed-product-picker-visible? true)))

;; OLD
(defmethod transitions/transition-state events/control-product-detail-picker-option-quantity-select
  [_ event {:keys [value]} app-state]
  (-> app-state
      (assoc-in keypaths/browse-sku-quantity value)
      (assoc-in catalog.keypaths/detailed-product-picker-visible? false)))

;; OLD
(defmethod transitions/transition-state events/control-product-detail-picker-close
  [_ event _ app-state]
  (assoc-in app-state catalog.keypaths/detailed-product-picker-visible? false))

;; OLD
#?(:cljs
   (defmethod effects/perform-effects events/control-product-detail-picker-open
     [_ _ _ _ _]
     (scroll/disable-body-scrolling)))

;; OLD
#?(:cljs
   (defmethod effects/perform-effects events/control-product-detail-picker-close
     [_ _ _ _ _]
     (scroll/enable-body-scrolling)))

#?(:cljs
   (defmethod effects/perform-effects events/navigate-product-details
     [_ event args _ app-state]
     (let [[prev-event prev-args] (get-in app-state (conj keypaths/navigation-undo-stack 0))
           product-id (:catalog/product-id args)
           product (get-in app-state (conj keypaths/v2-products product-id))
           navigating-to-self? (and (= events/navigate-product-details prev-event)
                                    (apply = (map :catalog/product-id [args prev-args])))]
       (when (nil? product)
         (fetch-product-details app-state product-id))
       (when-not navigating-to-self?
         (messages/handle-message events/initialize-product-details (assoc args :origin-nav-event event)))
       (messages/handle-message events/control-pdp-picker-close))))


#?(:cljs
   (defmethod trackings/perform-track events/navigate-product-details
     [_ event {:keys [catalog/product-id]} app-state]
     (when (-> product-id
               ((get-in app-state keypaths/v2-products))
               accessors.products/wig-product?)
       (facebook-analytics/track-event "wig_content_fired"))))

#?(:cljs
   (defmethod effects/perform-effects events/api-success-v3-products-for-details
     [_ event _ _ app-state]
     (messages/handle-message events/initialize-product-details (get-in app-state keypaths/navigation-args))))

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

(defn color-option<
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

(defn product-option->length-picker-option
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

(defn hair-length-option<
  [selection-event selections availability index per-item]
  (update per-item :hair/length
          (partial mapv
                   (partial product-option->length-picker-option
                            selection-event
                            availability
                            selections
                            index))))

(defn initialize-picker-options
  [selection-event
   selections
   availability
   options]
  {:hair/color (->> options
                    (mapcat :hair/color)
                    (map #(dissoc % :price :stocked?))
                    (distinct-by :option/slug)
                    (sort-by :filter/order)
                    (mapv (partial color-option< selection-event selections)))
   :per-item   (->> options
                    (map #(select-keys % [:hair/length]))
                    (map-indexed (partial hair-length-option< selection-event selections availability))
                    vec)
   :quantity   (map-indexed (fn [index quantity]
                              {:option/available?       true
                               :option/checked?         (zero? index)
                               :option/selection-target [selection-event {:selection [:quantity]
                                                                          :value quantity}]
                               :option/label            (str quantity)
                               :option/value            quantity
                               :option/id               (str "quantity-" quantity)})
                            (range 1 11))})

(defmethod transitions/transition-state events/initialize-product-details
  [_ event {:as args :keys [catalog/product-id query-params]} app-state]
  (let [ugc-offset      (:offset query-params)
        sku             (or (->> (:SKU query-params)
                                 (conj keypaths/v2-skus)
                                 (get-in app-state))
                            (get-in app-state catalog.keypaths/detailed-product-selected-sku))
        product-options (generate-product-options product-id app-state)

        ;; Picker Two Refactor
        product-electives  [:hair/family :hair/color :hair/length]
        initial-selections (let [{:hair/keys [length color family]}
                                 (maps/map-values first (select-keys sku product-electives))]
                             {:hair/color color
                              :per-item   [{:hair/length length
                                            :hair/family family}]})
        product            (products/product-by-id app-state product-id)
        product-skus       (products/extract-product-skus app-state product)
        availability       (catalog.products/index-by-selectors
                            product-electives
                            product-skus)

        picker-two-options (initialize-picker-options
                            events/control-pdp-picker-option-select
                            initial-selections
                            availability
                            [product-options])]
    (-> app-state

        ;; START refactor (temp)
        (assoc-in catalog.keypaths/detailed-pdp-selected-picker nil)
        (assoc-in catalog.keypaths/detailed-pdp-selections initial-selections)
        (assoc-in catalog.keypaths/detailed-pdp-options picker-two-options)
        (assoc-in catalog.keypaths/detailed-pdp-skus-db product-skus)
        (assoc-in catalog.keypaths/detailed-pdp-availability availability)
        ;; END Refactor

        (assoc-in catalog.keypaths/detailed-product-id product-id)
        (assoc-in catalog.keypaths/detailed-product-selected-sku sku)
        (assoc-in keypaths/ui-ugc-category-popup-offset ugc-offset)
        (assoc-in keypaths/browse-sku-quantity 1)
        (assoc-in catalog.keypaths/detailed-product-selected-picker nil)
        (assoc-in catalog.keypaths/detailed-product-picker-visible? nil)
        (assoc-selections sku)
        (assoc-in catalog.keypaths/detailed-product-options product-options))))

#?(:cljs
   (defmethod effects/perform-effects events/initialize-product-details
     [_ _ {:keys [catalog/product-id page/slug query-params origin-nav-event]} _ app-state]
     (let [selected-sku (get-in app-state catalog.keypaths/detailed-product-selected-sku)
           shop?        (= :shop (sites/determine-site app-state))]
       (if (url-points-to-invalid-sku? selected-sku query-params)
         (effects/redirect origin-nav-event
                           (merge
                            {:catalog/product-id product-id
                             :page/slug          slug}
                            (when selected-sku
                              {:query-params {:SKU (:catalog/sku-id selected-sku)}})))
         (let [product (get-in app-state (conj keypaths/v2-products product-id))]
           (seo/set-tags app-state)
           (when-let [album-keyword (storefront.ugc/product->album-keyword shop? product)]
             (effects/fetch-cms-keypath app-state [:ugc-collection album-keyword]))
           (when-let [pdp-faq-id (accessors.products/product->faq-id product)]
             (effects/fetch-cms-keypath app-state [:faq pdp-faq-id])))))))


(defmethod effects/perform-effects events/control-add-sku-to-bag
  [_ _ {:keys [sku quantity]} _ state]
  (let [cart-swap (swap/cart-swap<- state {:service/intended sku})]
    (if (:service/swap? cart-swap)
      (messages/handle-message events/cart-swap-popup-show
                               cart-swap)
      (messages/handle-message events/add-sku-to-bag
                               {:sku           sku
                                :stay-on-page? false
                                :service-swap? false
                                :quantity      quantity}))))

;; TODO(corey) Move this to cart
(defmethod effects/perform-effects events/add-sku-to-bag
  [dispatch event {:keys [sku quantity stay-on-page? service-swap?] :as args} _ app-state]
  #?(:cljs
     (let [nav-event          (get-in app-state keypaths/navigation-event)
           cart-interstitial? (and
                               (not service-swap?)
                               (= :shop (sites/determine-site app-state)))]
       (api/add-sku-to-bag
        (get-in app-state keypaths/session-id)
        {:sku                sku
         :quantity           quantity
         :stylist-id         (get-in app-state keypaths/store-stylist-id)
         :token              (get-in app-state keypaths/order-token)
         :number             (get-in app-state keypaths/order-number)
         :user-id            (get-in app-state keypaths/user-id)
         :user-token         (get-in app-state keypaths/user-token)
         :heat-feature-flags (get-in app-state keypaths/features)}
        #(do
           (messages/handle-message events/api-success-add-sku-to-bag
                                    {:order         %
                                     :quantity      quantity
                                     :sku           sku})
           (when (not (or (= events/navigate-cart nav-event) stay-on-page?))
             (history/enqueue-navigate (if cart-interstitial?
                                         events/navigate-added-to-cart
                                         events/navigate-cart))))))))

(defmethod effects/perform-effects events/add-servicing-stylist-and-sku
  [_ _ {:keys [sku quantity servicing-stylist]} _ state]
  (let [token  (get-in state keypaths/order-token)
        number (get-in state keypaths/order-number)]
    #?(:cljs
       (api/add-servicing-stylist-and-sku
        (get-in state keypaths/session-id)
        (cond-> {:sku                sku
                 :servicing-stylist  servicing-stylist
                 :quantity           quantity
                 :stylist-id         (get-in state keypaths/store-stylist-id)
                 :user-id            (get-in state keypaths/user-id)
                 :user-token         (get-in state keypaths/user-token)
                 :heat-feature-flags (get-in state keypaths/features)}
          (and token number)
          (merge {:token  token
                  :number number}))
        #(messages/handle-message events/api-success-add-sku-to-bag
                                  {:order    %
                                   :quantity quantity
                                   :sku      sku})))))

(defmethod transitions/transition-state events/api-success-add-sku-to-bag
  [_ event {:keys [quantity sku]} app-state]
  (assoc-in app-state keypaths/browse-sku-quantity 1))
