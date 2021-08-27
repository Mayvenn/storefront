(ns catalog.product-details.multiple-lengths
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
            [checkout.cart.swap :as swap]
            [homepage.ui.faq :as faq]
            [mayvenn.live-help.core :as live-help]
            [mayvenn.visual.lib.call-out-box :as call-out-box]
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
            [storefront.components.picker.picker :as picker]
            [storefront.components.picker.picker-two :as picker-two]
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
            [spice.core :as spice]))

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
           picker-data
           ugc
           faq-section
           add-to-cart
           live-help
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
             (if (-> product :hair/family first (= "bundles"))
               [:div
                (component/build picker-two/modal picker-modal)
                [:div.bg-refresh-gray
                 [:div.px3.my4
                  [:div.proxima.title-3.shout "Color"]
                  (picker-two/component (with :color.picker data))

                  ;;TODO: material picker row?

                  [:div.proxima.title-3.shout "Lengths"]
                  [:div
                   (picker-two/component (with :length-main.picker data))
                   (for [auxiliary-data (:queries (with :length-auxiliary.picker data))]
                     (picker-two/component auxiliary-data))]]]]

                 (component/build picker/component picker-data opts))
             (let [{:keys [id event]} (with :add-auxiliary data)]
               (when id
                 [:div.center.py1 (ui/button-medium-underline-primary (merge
                                                                       (utils/fake-href event)
                                                                       {:data-test "auxiliary-add-length"}) "Add Another Length")]))]
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
  (let [shop?                              (or (= "shop" (get-in app-state keypaths/store-slug))
                                               (= "retail-location" (get-in app-state keypaths/store-experience)))
        selected-sku                       (get-in app-state catalog.keypaths/detailed-product-selected-sku)
        selections                         (get-in app-state catalog.keypaths/detailed-product-selections)
        quadpay-loaded?                    (get-in app-state keypaths/loaded-quadpay)
        sku-family                         (-> selected-sku :hair/family first)
        mayvenn-install-incentive-families #{"bundles" "closures" "frontals" "360-frontals"}
        selected-skus                      (->> (get-in app-state catalog.keypaths/detailed-product-auxiliary-selections)
                                                (filterv not-empty)
                                                (mapv
                                                 #(determine-sku-from-selections app-state (merge selections %)))
                                                (concat [selected-sku]))
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
                   "Add Product to Bag"
                   "Add to Bag")

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

(defn ^:private picker-query
  [{:keys [facets selections options auxiliary-selections]}]
  (let [selected-color             (get-in facets [:hair/color :facet/options (:hair/color selections)])
        selected-auxiliary-lengths (map #(get-in facets [:hair/length :facet/options (:hair/length %)]) auxiliary-selections)
        image-src                  (->> options
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
     (within :length-main.picker (let [{:option/keys [name slug]} (get-in facets [:hair/length :facet/options (get-in selections [:hair/length])])]
                                   {:id               "picker-length-main"
                                    :value-id         (str "picker-selected-length-main-" (facets/hacky-fix-of-bad-slugs-on-facets slug))
                                    :image-src        image-src
                                    :primary          name
                                    :options          (map (fn[option]
                                                             {:option/value      (:option/slug option)
                                                              :option/label      (:option/name option)
                                                              :option/available? (:stocked? option)})
                                                           (:hair/length options))
                                    :selected-value   slug
                                    :selection-target [events/control-product-detail-picker-option-select {:selection :hair/length}]
                                    :open-target      [events/control-product-detail-picker-open {:facet-slug [:hair/length]}]}))
     (within :length-auxiliary.picker {:queries (map-indexed (fn [idx selection]
                                                               {:id        (str "picker-length-" idx)
                                                                :value-id  (str "picker-length-" (:hair/length selection) "-" idx)
                                                                :image-src image-src
                                                                :primary   (if-let [picker-label (:option/name selection)]
                                                                             picker-label
                                                                             "Choose Length (optional)")
                                                                :options   (concat
                                                                            (if selection
                                                                              [{:option/value      ""
                                                                                :option/label      "Remove Length"
                                                                                :option/available? true}]
                                                                              [{:option/value      nil
                                                                                :option/label      nil
                                                                                :option/available? true}])

                                                                            (mapv (fn[option]
                                                                                    {:option/value      (:option/slug option)
                                                                                     :option/label      (:option/name option)
                                                                                     :option/available? (:stocked? option)})
                                                                                  (:hair/length options)))
                                                                :selected-value   (:hair/length selection)
                                                                :selection-target [events/control-product-detail-picker-option-auxiliary-select {:auxiliary-index idx
                                                                                                                                                 :selection       :hair/length}]
                                                                :open-target      [events/control-product-detail-picker-open {:facet-slug      [:hair/length]
                                                                                                                              :auxiliary-index idx}]})
                                                             selected-auxiliary-lengths)}))))

(defn ^:private picker-modal<
  [picker-options picker-visible? selected-picker auxiliary-index]
  (let [picker-type           (last selected-picker)
        options               (get-in picker-options selected-picker)
        options-for-auxiliary (when auxiliary-index
                                (mapv (fn[option]
                                        (assoc-in
                                         option
                                         [:option/selection-target]
                                         [events/control-product-detail-picker-option-auxiliary-select {:auxiliary-index auxiliary-index
                                                                                                        :selection       :hair/length
                                                                                                        :value           (:option/value option)}]))
                                      options))]
    {:picker-modal/title        (case picker-type
                                  :hair/color  "Color"
                                  :hair/length "Length"
                                  nil)
     :picker-modal/type         picker-type
     :picker-modal/options      (if auxiliary-index
                                  options-for-auxiliary
                                  options)
     ;; NOTE: There is a difference between selected and visible. We toggle
     ;; picker visibility to signal that the modal should close but we don't remove
     ;; the options so the close animation isn't stopped prematurely due to the
     ;; child options re-rendering.
     :picker-modal/visible?     (and picker-visible? options selected-picker)
     :picker-modal/close-target [events/control-product-detail-picker-close]}))

(defn query [data]
  (let [selections            (get-in data catalog.keypaths/detailed-product-selections)
        product               (products/current-product data)
        product-skus          (products/extract-product-skus data product)
        images-catalog        (get-in data keypaths/v2-images)
        facets                (facets/by-slug data)
        selected-sku          (get-in data catalog.keypaths/detailed-product-selected-sku)
        carousel-images       (find-carousel-images product product-skus images-catalog
                                                    ;;TODO These selection election keys should not be hard coded
                                                    (select-keys selections [:hair/color
                                                                             :hair/base-material])
                                                    selected-sku)
        length-guide-image    (->> product
                                   (images/for-skuer images-catalog)
                                   (select {:use-case #{"length-guide"}})
                                   first)
        picker-options        (get-in data catalog.keypaths/detailed-product-picker-options)
        product-options       (get-in data catalog.keypaths/detailed-product-options)
        ugc                   (ugc-query product selected-sku data)
        sku-price             (or (:product/essential-price selected-sku)
                                  (:sku/price selected-sku))
        review-data           (review-component/query data)
        shop?                 (or (= "shop" (get-in data keypaths/store-slug))
                                  (= "retail-location" (get-in data keypaths/store-experience)))
        hair?                 (accessors.products/hair? product)
        faq                   (when-let [pdp-faq-id (accessors.products/product->faq-id product)]
                                (get-in data (conj keypaths/cms-faq pdp-faq-id)))
        auxiliary-selections  (get-in data catalog.keypaths/detailed-product-auxiliary-selections)]
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
      :selected-picker                    (get-in data catalog.keypaths/detailed-product-selected-picker)
      :picker-data                        (picker/query data length-guide-image) ; non-multiple-lengths picker
      :picker-modal                       (picker-modal< picker-options
                                                         (get-in data catalog.keypaths/detailed-product-picker-visible?)
                                                         (get-in data catalog.keypaths/detailed-product-selected-picker)
                                                         (get-in data catalog.keypaths/detailed-product-auxiliary-index))}
     (picker-query {:facets               facets
                    :selections           selections
                    :options              product-options
                    :auxiliary-selections auxiliary-selections})
     (when (-> (get-in data catalog.keypaths/detailed-product-auxiliary-selections) count (< 4))
       #:add-auxiliary{:id    "add-auxiliary"
                       :event events/control-product-detail-picker-add})
     (when sku-price
       {:price-block/primary   "starting at"
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
