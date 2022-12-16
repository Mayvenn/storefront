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
                       [storefront.hooks.exception-handler :as exception-handler]
                       [storefront.hooks.facebook-analytics :as facebook-analytics]
                       [storefront.hooks.quadpay :as zip]
                       [storefront.hooks.reviews :as review-hooks]
                       [storefront.hooks.stringer :as stringer]
                       [storefront.hooks.seo :as seo]
                       [storefront.trackings :as trackings]])
            [api.catalog :refer [select ?wig]]
            api.current
            api.orders
            api.products
            [catalog.cms-dynamic-content :as cms-dynamic-content]
            [catalog.facets :as facets]
            catalog.keypaths
            [catalog.product-details-ugc :as ugc]
            [catalog.products :as products]
            [catalog.selector.sku :as sku-selector]
            [catalog.ui.add-to-cart :as add-to-cart]
            [catalog.ui.molecules :as catalog.M]
            [clojure.string]
            [homepage.ui.faq :as faq]
            [mayvenn.visual.tools :refer [with]]
            [mayvenn.visual.ui.titles :as titles]
            [spice.selector :as selector]
            [storefront.accessors.contentful :as contentful]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.images :as images]
            [storefront.accessors.products :as accessors.products]
            [storefront.accessors.sites :as sites]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.accordions.product-info :as accordions.product-info]
            [storefront.components.carousel :as carousel-neue]
            [storefront.components.money-formatters :as mf]
            [storefront.components.picker.picker :as picker]
            [storefront.components.tabbed-information :as tabbed-information]
            [storefront.components.accordion-v2022-10 :as accordion-neue]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages
             :as messages
             :refer [handle-message] :rename {handle-message publish}]
            [storefront.platform.reviews :as review-component]
            [storefront.request-keys :as request-keys]
            [storefront.transitions :as transitions]
            storefront.ugc
            [spice.core :as spice]
            [spice.maps]))

(defn two-column-layout [wide-left wide-right-and-narrow]
  [:div.clearfix.mxn2
   [:div.col-on-tb-dt.col-7-on-tb-dt.px2
    wide-left]
   [:div.col-on-tb-dt.col-5-on-tb-dt.px2 wide-right-and-narrow]])

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

(defn ^:private get-selected-options [selections options]
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
   [:h1.flex-auto
    (titles/proxima-left (with :title data))]
   [:div.flex.justify-between.my2
    (catalog.M/yotpo-reviews-summary data)
    [:div.col-3 (catalog.M/price-block data)]]
   #?(:cljs
      (component/build zip/pdp-component data _))])

(defn diamond-swatch [ucare-id facet-slug option-slug option-name selected? target size]
  (let [container-width #?(:clj (.hypot java.lang.Math size size)
                           :cljs (js/Math.hypot size size))]
    [(if target :a :div)
     (merge {:style {:width (str container-width "px")
                     :height (str container-width "px")}
             :class (str "flex items-center justify-center"
                         (when target " inherit-color pointer"))
             :key   option-slug}
            (when target
              {:data-test (str "picker-" facet-slug "-" option-slug)})
            (when target
              (apply utils/fake-href target)))
     [:div.overflow-hidden.flex.items-center.justify-center
      {:style {:transform "rotate(45deg)"
               :width     (str size "px")
               :height    (str size "px")
               :padding   "0"}
       :class (when selected? "border border-width-2 border-s-color")}
      [:img
       {:key   (str "product-details-" option-name "-" option-slug)
        :style {:transform "rotate(-45deg)"
                :width     (str (inc container-width) "px")
                :height    (str (inc container-width) "px")}
        :alt   option-name
        :src   (str "https://ucarecdn.com/" (ui/ucare-img-id ucare-id) "/-/format/auto/")}]]]))

(defcomponent picker-accordion-face-open [{:keys [facet-name facet-slug swatch option-slug option-name]} _ _]
  [:div.grid.ml2.py3.items-center
   {:data-test (str "picker-" facet-slug "-open")
    :style {:grid-template-columns "4rem auto"}}
   [:div.shout.content-3.bold facet-name]
   [:div.flex.items-center.gap-2
    {:data-test (str "picker-selected-" facet-slug "-" option-slug)}
    (when swatch
      (diamond-swatch swatch facet-slug option-slug option-name false nil 20))
    option-name]])
(defcomponent picker-accordion-face-closed [{:keys [facet-name facet-slug swatch option-slug option-name]} _ _]
  [:div.grid.ml2.py3.items-center
   {:data-test (str "picker-" facet-slug "-closed")
    :style {:grid-template-columns "4rem auto"}}
   [:div.shout.content-3 facet-name]
   [:div.flex.items-center.gap-2
    {:data-test (str "picker-selected-" facet-slug "-" option-slug)}
    (when swatch
      (diamond-swatch swatch facet-slug option-slug option-name false nil 20))
    option-name]])

(component/defcomponent picker-accordion-contents
  [{:keys [facet swatches? options] :as picker-contents} _ _]
  [:div.p2
   {:key (str "picker-contents-" facet)
    :data-test (str "picker-contents-" facet)}
   [:div.flex.flex-wrap.gap-2
    (if swatches?
      (for [{:keys [option-slug selected? option-name rectangle-swatch target]} options]
        (diamond-swatch rectangle-swatch facet option-slug option-name selected? target 30))
      (for [{:keys [option-slug copy selected? target]} options]
        [(if target :a :div)
         (merge {:key   option-slug
                 :style {:width  "2.5rem"
                         :height "2.5rem"}
                 :class (str "border flex items-center justify-center"
                             (if selected?
                               " border-s-color border-width-2 bg-refresh-gray"
                               " border-gray")
                             (when target
                               " inherit-color pointer"))}
                (when target
                  {:data-test (str "picker-" facet "-" option-slug)})
                (when target
                  (apply utils/fake-href target)))
         copy]))]
   (when-let [{:keys [target id content]} (not-empty (with :link picker-contents))]
     [:div.right-align
      (ui/button-small-underline-primary
       (assoc (apply utils/fake-href target) :data-test id)
       content)])])

(component/defcomponent template
  [{:keys [carousel-images
           product
           reviews
           selected-sku
           picker-data
           ugc
           faq-section
           add-to-cart
           accordion-v2?] :as data}
   _
   opts]
  (let [unavailable? (not (seq selected-sku))
        sold-out?    (not (:inventory/in-stock? selected-sku))]
    (component/html
     (if-not product
       [:div.flex.h2.p1.m1.items-center.justify-center
        {:style {:height "25em"}}
        (ui/large-spinner {:style {:height "4em"}})]
       [:div
        [:div.container.pdp-on-tb
         (when (:offset ugc)
           [:div.absolute.overlay.z4.overflow-auto
            {:key "popup-ugc"}
            (component/build ugc/popup-component (assoc ugc :id "popup-ugc") opts)])
         [:div
          {:key "page"}
          (two-column-layout
           (component/html
            (if (seq (with :product-carousel data))
              (component/build carousel-neue/component
                               (with :product-carousel data)
                               {:opts {:carousel/exhibit-thumbnail-component carousel-neue/product-carousel-thumbnail
                                       :carousel/exhibit-highlight-component carousel-neue/product-carousel-highlight
                                       :carousel/id                          :product-carousel}})
              [:div ^:inline
               (carousel carousel-images product)
               (component/build ugc/component (assoc ugc :id "ugc-dt") opts)]))
           (component/html
            [:div
             (component/build product-summary-organism data)
             [:div.px2
              (component/build picker/component picker-data opts)]
             (component/build accordion-neue/component
                              (with :pdp-picker data)
                              {:opts {:accordion.drawer.open/face-component   picker-accordion-face-open
                                      :accordion.drawer.closed/face-component picker-accordion-face-closed
                                      :accordion.drawer/contents-component    picker-accordion-contents}})
             [:div.mt4
              (cond
                unavailable? unavailable-button
                sold-out?    sold-out-button
                :else        (component/build add-to-cart/organism add-to-cart))]
             (when (products/stylist-only? product)
               shipping-and-guarantee)
             (if accordion-v2?
               (component/build accordion-neue/component
                                (with :product-details-accordion data)
                                {:opts {:accordion.drawer.open/face-component   accordions.product-info/face-open
                                        :accordion.drawer.closed/face-component accordions.product-info/face-closed
                                        :accordion.drawer/contents-component    accordions.product-info/contents}})
               (component/build tabbed-information/component data))
             (component/build catalog.M/non-hair-product-description data opts)
             [:div.hide-on-tb-dt.m3
              [:div.mxn2.mb3 (component/build ugc/component (assoc ugc :id "ugc-mb") opts)]]]))]]
        (when (seq reviews)
          [:div.container.col-7-on-tb-dt.px2
           (component/build review-component/reviews-component reviews opts)])
        (when faq-section
          [:div.container
           (component/build faq/organism faq-section opts)])]))))

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
  (let [selected-sku    (get-in app-state catalog.keypaths/detailed-product-selected-sku)]
    (merge
     {:cta/id    "add-to-cart"
      :cta/label "Add to Bag"

      ;; Fork here to use bulk add to cart
      :cta/target                [events/control-add-sku-to-bag
                                  {:sku      selected-sku
                                   :quantity (get-in app-state keypaths/browse-sku-quantity 1)}]
      :cta/spinning?             (utils/requesting? app-state (conj request-keys/add-to-bag (:catalog/sku-id selected-sku)))
      :cta/disabled?             (not (:inventory/in-stock? selected-sku))
      :sub-cta/promises          [{:icon :svg/shield
                                   :copy "30 day guarantee"}
                                  {:icon :svg/ship-truck
                                   :copy "Free standard shipping"}
                                  {:icon :svg/market
                                   :copy "Come visit our Texas locations"}]
      :sub-cta/learn-more-copy   "Find my store"
      :sub-cta/learn-more-target [events/navigate-retail-walmart {}]})))

(defn ^:private tab-section<
  [data
   {:keys [content-path fallback-content-path]
    :as   section}]
  (when-let [content (or (and content-path
                              (get-in data content-path))
                         (and fallback-content-path
                              (get-in data fallback-content-path)))]
    (-> section
        (select-keys [:heading :link/content :link/target :link/id])
        (assoc :content content))))

(def details-render-slots
  [{:pdp.details/hair-info
    [:pdp.details.hair-info/model-wearing
     ;; TODO: length guide
     :pdp.details.hair-info/unit-weight
     :pdp.details.hair-info/hair-quality
     :pdp.details.hair-info/hair-origin
     :pdp.details.hair-info/hair-weft-type
     :pdp.details.hair-info/part-design
     :pdp.details.hair-info/features
     :pdp.details.hair-info/available-materials
     :pdp.details.hair-info/lace-size
     :pdp.details.hair-info/silk-size
     :pdp.details.hair-info/cap-size
     :pdp.details.hair-info/density
     :pdp.details.hair-info/tape--in-glue-information]}
   {:pdp.details/description
    [:pdp.details.description/description
     :pdp.details.description/hair-type
     :pdp.details.description/what's-included]}
   {:pdp.details/care
    [:pdp.details.care/maintenance-level
     :pdp.details.care/can-it-be-colored?]}])

(defn legacy-content-from-cellar
  "Converts cellar SKU and cellar Product to template-slots"
  [current-product selected-sku model-image]
  (->> [:pdp.details.description/description             nil                        (->> current-product :copy/description)
        :pdp.details.description/whats-included          "Hair Type"                (->> current-product :copy/hair-type)
        :pdp.details.hair-info/model-wearing             "Model Wearing"            (or (:copy/model-wearing model-image)
                                                                                        (:copy/model-wearing current-product))
        :pdp.details.hair-info/unit-weight               "Unit Weight"              (or (->> selected-sku :hair/weight)
                                                                                        (->> current-product :copy/weights))
        :pdp.details.hair-info/hair-quality              "Hair Quality"             (->> current-product :copy/quality)
        :pdp.details.hair-info/density                   "Density"                  (->> current-product :copy/density)
        :pdp.details.hair-info/hair-origin               "Hair Origin"              (->> current-product :copy/origin)
        :pdp.details.hair-info/hair-weft-type            "Hair Weft Type"           (->> current-product :copy/weft-type)
        :pdp.details.hair-info/part-design               "Part Design"              (->> current-product :copy/part-design)
        :pdp.details.hair-info/features                  "Features"                 (->> current-product :copy/features)
        :pdp.details.hair-info/available-materials       "Available Materials"      (->> current-product :copy/materials)
        :pdp.details.hair-info/lace-size                 "Lace Size"                (->> current-product :copy/lace-size)
        :pdp.details.hair-info/silk-size                 "Silk Size"                (->> current-product :copy/silk-size)
        :pdp.details.hair-info/cap-size                  "Cap Size"                 (->> current-product :copy/cap-size)
        :pdp.details.hair-info/tape--in-glue-information "Tape-in Glue Information" (->> current-product :copy/tape-in-glue)
        :pdp.details.description/hair-type               "Hair Type"                (->> current-product :copy/hair-type)
        :pdp.details.care/maintenance-level              "Maintenance Level"        (->> current-product :copy/maintenance-level)]
       (partition 3)
       (keep (fn [[k heading content]]
               (when content
                 [k [:div (when heading [:h3 heading])
                     [:p content]]])))
       (into {})))

(defn content-slots<
  [current-product selected-sku model-image cms-pdp-content fake-cms-content]
  (merge (legacy-content-from-cellar current-product selected-sku model-image)
         (cms-dynamic-content/derive-product-details fake-cms-content selected-sku)
         ;; TODO: Uncomment this out to allow Contentful data
         #_(cms-dynamic-content/derive-product-details cms-pdp-content selected-sku)))

(defn content-slots->accordion-slots
  [content-slot-data product length-guide-image open-drawers]
  (merge
   ;; TODO drive initial-open-drawers off of Contentful Data?
   (if (contains? (:catalog/department product) "stylist-exclusives")
     {:allow-all-closed?    false
      :initial-open-drawers #{:pdp.details/description}}
     {:allow-all-closed?    true
      :initial-open-drawers #{:pdp.details/hair-info}})
   {:allow-multi-open?    false
    :drawers (->> details-render-slots
                  (keep (fn [drawer]
                          (let [[drawer-id slot-ids] (first drawer)
                                sections             (keep (fn [slot-id]
                                                             (merge
                                                              (when-let [content (get content-slot-data slot-id)]
                                                                {:content content})
                                                              (when (and (= slot-id :pdp.details.hair-info/model-wearing)
                                                                         length-guide-image)
                                                                {:link/content "Length Guide"
                                                                 :link/target  [events/popup-show-length-guide
                                                                                {:length-guide-image length-guide-image
                                                                                 :location           "hair-info-tab"}]
                                                                 :link/id      "hair-info-tab-length-guide"})))
                                                           slot-ids)]
                           (when (seq sections)
                             {:contents {:sections sections}
                              :id       drawer-id
                              ;; TODO: drive drawer face copy from Contentful data?
                              :face     {:copy (-> drawer-id
                                                   str
                                                   (clojure.string/split #"/")
                                                   last
                                                   (clojure.string/replace #"-" " "))}}))))
                  vec)
    :id                   "product-details-accordion"
    :open-drawers         open-drawers}))

;; TODO: Change content-value to markup. cms-dynamic-content/derive-product-details
;; will need to include a markup->hiccup layer
(def ^:private fake-contentful-product-details-data
  {:ctf-id-1
   {:content-slot-id "pdp/colorable"
    :selector        {"hair/color" #{"black" "1b-soft-black"}}
    :content-value   [:div [:h3 "Can It Be Colored?"]
                      [:p "Yes - This virgin human hair can be lifted (bleached) and colored with professional products."]]}
   :ctf-id-2
   {:content-slot-id "pdp/colorable"
    :selector        {"hair/color" #{"blonde" "blonde-dark-roots" "dark-blonde" "dark-blonde-dark-roots" "1c-mocha-brown" "#2-chocolate-brown"
                                     "#4-caramel-brown" "6-hazelnut-brown" "18-chestnut-blonde" "60-golden-ash-blonde" "613-bleach-blonde"}}
    :content-value   [:div [:h3 "Can It Be Colored?"]
                      [:p "Yes - Keep in mind pre-lightened blonde should not be lifted (bleached) any further, but can be professionally colored with deposit-only products or toners."]]}
   :ctf-id-3
   {:content-slot-id "pdp/colorable"
    :selector        {"hair/color" #{"#1-jet-black" "vibrant-burgundy"}}
    :content-value   [:div [:h3 "Can It Be Colored?"]
                      [:p "No - Since this hair has already been professionally processed, we don't recommend any lifting (bleaching) or coloring."]]}})

(defn query [data]
  (let [selections                (get-in data catalog.keypaths/detailed-product-selections)
        product                   (products/current-product data)
        product-skus              (products/extract-product-skus data product)
        images-catalog            (get-in data keypaths/v2-images)
        facets                    (facets/by-slug data)
        selected-sku              (get-in data catalog.keypaths/detailed-product-selected-sku)
        carousel-images           (find-carousel-images product product-skus images-catalog
                                                        ;;TODO These selection election keys should not be hard coded
                                                        (select-keys selections [:hair/color
                                                                                 :hair/base-material])
                                                        selected-sku)
        length-guide-image        (->> product
                                       (images/for-skuer images-catalog)
                                       (select {:use-case #{"length-guide"}})
                                       first)
        product-options           (get-in data catalog.keypaths/detailed-product-options)
        ugc                       (ugc-query product selected-sku data)
        sku-price                 (or (:product/essential-price selected-sku)
                                      (:sku/price selected-sku))
        review-data               (review-component/query data)
        shop?                     (or (= "shop" (get-in data keypaths/store-slug))
                                      (= "retail-location" (get-in data keypaths/store-experience)))
        hair?                     (accessors.products/hair? product)
        faq                       (when-let [pdp-faq-id (accessors.products/product->faq-id product)]
                                    (-> data
                                        (get-in (conj keypaths/cms-faq pdp-faq-id))
                                        (assoc :open-drawers (:accordion/open-drawers (accordion-neue/<- data :pdp-faq)))))
        selected-picker           (get-in data catalog.keypaths/detailed-product-selected-picker)
        model-image               (first (filter :copy/model-wearing carousel-images))
        pdp-accordion-picker?     (experiments/pdp-accordion-picker? data)
        accordion-v2?             (experiments/accordion-v2? data)
        picker-data               (picker/query data length-guide-image)
        bf-2022-sale?             (and (experiments/bf-2022-sale? data)
                                       (:promo.clearance/eligible selected-sku))
        content-slot-data         (content-slots< product
                                                  selected-sku
                                                  model-image
                                                  (get-in data keypaths/cms-pdp-content)
                                                  fake-contentful-product-details-data)]
    (merge
     {:reviews                            review-data
      :yotpo-reviews-summary/product-name (some-> review-data :yotpo-data-attributes :data-name)
      :yotpo-reviews-summary/product-id   (some-> review-data :yotpo-data-attributes :data-product-id)
      :yotpo-reviews-summary/data-url     (some-> review-data :yotpo-data-attributes :data-url)
      :zip-payments/sku-price             (if bf-2022-sale? (* 0.7 (:sku/price selected-sku)) (:sku/price selected-sku))
      :zip-payments/loaded?               (get-in data keypaths/loaded-quadpay)
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
      :accordion-v2?                      accordion-v2?}
     (when sku-price
       (if bf-2022-sale?
         {:price-block/primary-struck (mf/as-money sku-price)
          :price-block/new-primary    (mf/as-money (* 0.7 sku-price))
          :price-block/secondary      "each"}
         {:price-block/primary   (mf/as-money sku-price)
          :price-block/secondary "each"}))

     (when-not pdp-accordion-picker?
       {:picker-data picker-data})

     (when pdp-accordion-picker?
       (accordion-neue/accordion-query
        {:id                :pdp-picker
         :allow-all-closed? true
         :allow-multi-open? true
         :open-drawers      (-> data (accordion-neue/<- :pdp-picker) :accordion/open-drawers)
         :drawers           (let [{:keys [sku-quantity selected-length selected-color options]} picker-data]
                              [(let [color-options (->> options :hair/color (sort-by :filter/order))]
                                 {:id           "color"
                                  :face         {:facet-name  "Color"
                                                 :facet-slug  "color"
                                                 :option-name (:option/name selected-color)
                                                 :option-slug (facets/hacky-fix-of-bad-slugs-on-facets (:option/slug selected-color))
                                                 :swatch      (:option/rectangle-swatch selected-color)}
                                  :open-message [events/pdp|picker-options|viewed {:facet   "color"
                                                                                   :options (map :option/slug color-options)}]
                                  :contents
                                  {:swatches? true
                                   :facet     "color"
                                   :options   (->> color-options
                                                   (map (fn [{:keys [option/slug option/name option/rectangle-swatch stocked?]}]
                                                          (merge {:option-slug      (facets/hacky-fix-of-bad-slugs-on-facets slug)
                                                                  :option-name      name
                                                                  :rectangle-swatch rectangle-swatch
                                                                  :selected?        (= (:option/slug selected-color) slug)}
                                                                 (when (-> selected-color :option/slug (not= slug))
                                                                   {:target
                                                                    [events/pdp|picker-options|selected
                                                                     {:data             {:facet           "color"
                                                                                         :options         (map :option/slug color-options)
                                                                                         :selected-option slug}
                                                                      :callback-message [events/control-product-detail-picker-option-select
                                                                                         {:navigation-event events/navigate-product-details
                                                                                          :selection        :hair/color
                                                                                          :value            slug}]}]})))))}})
                               (let [length-options (->> options
                                                         :hair/length
                                                         (sort-by :filter/order))]
                                 {:id           "length"
                                  :face         {:facet-name  "Length"
                                                 :facet-slug  "length"
                                                 :option-name (:option/name selected-length)
                                                 :option-slug (:option/slug selected-length)}
                                  :open-message [events/pdp|picker-options|viewed {:facet   "length"
                                                                                   :options (map :option/slug length-options)}]
                                  :contents
                                  {:facet   "length"
                                   :options (->> length-options
                                                 (map (fn [{:keys [option/name option/slug stocked?]}]
                                                        (merge {:copy        name
                                                                :option-slug slug
                                                                :selected?   (= (:option/slug selected-length) slug)}
                                                               (when (-> selected-length :option/slug (not= slug))
                                                                 {:target [events/pdp|picker-options|selected
                                                                           {:data             {:facet           "length"
                                                                                               :options         (map :option/slug length-options)
                                                                                               :selected-option slug}
                                                                            :callback-message [events/control-product-detail-picker-option-select
                                                                                               {:navigation-event events/navigate-product-details
                                                                                                :selection        :hair/length
                                                                                                :value            slug}]}]})))))}})
                               (let [qty-options (->> (range)
                                                      (take 10)
                                                      (map inc))]
                                 {:id           "quantity"
                                  :face         {:facet-name  "Qty"
                                                 :facet-slug  "quantity"
                                                 :option-name sku-quantity
                                                 :option-slug sku-quantity}
                                  :open-message [events/pdp|picker-options|viewed {:facet   "quantity"
                                                                                   :options (map str qty-options)}]
                                  :contents
                                  {:facet   "quantity"
                                   :options (->> qty-options
                                                 (map (fn [qty]
                                                        (merge
                                                         {:copy        (str qty)
                                                          :option-slug (str qty)
                                                          :selected?   (= sku-quantity qty)}
                                                         (when (not (= sku-quantity qty))
                                                           {:target [events/pdp|picker-options|selected
                                                                     {:data             {:facet           "quantity"
                                                                                         :options         (->> (range) (take 10) (map inc) (map str))
                                                                                         :selected-option (str qty)}
                                                                      :callback-message [events/control-product-detail-picker-option-quantity-select
                                                                                         {:value qty}]}]})))))}})])}))

     (cond
       (and product accordion-v2?)
       (accordion-neue/accordion-query (let [{:accordion/keys [open-drawers]} (accordion-neue/<- data "product-details-accordion")]
                                         (cond-> (content-slots->accordion-slots content-slot-data product length-guide-image open-drawers)
                                           ;; In a perfect world the FAQ would be modeled with Filled Content Slots.
                                           ;; Instead, we shoehorn it into the accordion if we can find the data.
                                           (and (experiments/pdp-faq-in-accordion? data) faq)
                                           (update :drawers conj (let [{:keys [question-answers slug]} faq]
                                                                   {:id       "pdp-faq-drawer"
                                                                    :face     {:copy "FAQs"}
                                                                    :contents {:faq (accordion-neue/accordion-query
                                                                                     {:id                   :pdp-faq
                                                                                      :allow-all-closed?    true
                                                                                      :allow-multi-open?    false
                                                                                      :open-drawers         (:open-drawers faq)
                                                                                      :initial-open-drawers #{}
                                                                                      :drawers
                                                                                      (map-indexed (fn [ix {:keys [question answer]}]
                                                                                                     {:id       (str "pdp-faq-" ix)
                                                                                                      :face     {:copy (:text question)}
                                                                                                      :contents {:answer answer}})
                                                                                                   question-answers)})}})))))

       ;; Old accordion
       hair?
       (let [active-tab-name  (get-in data keypaths/product-details-information-tab)
             description-data {:product         product
                               :model-image     model-image
                               :selected-sku    selected-sku
                               :dynamic-content (if (experiments/pdp-content-slots? data) ; true = using contentful data, false = faked out content
                                                  (->> selected-sku
                                                       (cms-dynamic-content/derive-product-details (get-in data keypaths/cms-pdp-content)))
                                                  (->> selected-sku
                                                       (cms-dynamic-content/derive-product-details fake-contentful-product-details-data)
                                                       ;; The old accordion expects strings (to which it attaches hard-coded titles)
                                                       ;; The new accordion expects hiccup data containing the title
                                                       ;; The "fake" data must be useable by both old and new accordions, so we must
                                                       ;; update the content to consist only of the string.
                                                       (spice.maps/map-values #(-> % first (nth 2) (nth 1)))))}]
         #:tabbed-information{:id      "product-description-tabs"
                              :keypath keypaths/product-details-information-tab
                              :tabs    [{:title    "Hair Info"
                                         :id       :hair-info
                                         :active?  (= active-tab-name :hair-info)
                                         :icon     {:opts {:height "20px"
                                                           :width  "20px"}
                                                    :id   "info-color-circle"}
                                         :sections (keep (partial tab-section< description-data)
                                                         [(merge
                                                           (when (seq (or (:copy/model-wearing model-image)
                                                                          (:copy/model-wearing product)))
                                                             {:heading               "Model Wearing"
                                                              :content-path          [:model-image :copy/model-wearing]
                                                              :fallback-content-path [:product :copy/model-wearing]})
                                                           (when length-guide-image
                                                             {:link/content "Length Guide"
                                                              :link/target  [events/popup-show-length-guide
                                                                             {:length-guide-image length-guide-image
                                                                              :location           "hair-info-tab"}]
                                                              :link/id      "hair-info-tab-length-guide"}))
                                                          {:heading               "Unit Weight"
                                                           :content-path          [:selected-sku :hair/weight]
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
                                         :sections (keep (partial tab-section< description-data)
                                                         [{:heading      "Hair Type"
                                                           :content-path [:product :copy/hair-type]}
                                                          {:heading      "What's Included"
                                                           :content-path [:product :copy/whats-included]}])}
                                        {:title    "Care"
                                         :id       :care
                                         :active?  (= active-tab-name :care)
                                         :icon     {:opts {:height "20px"
                                                           :width  "20px"}
                                                    :id   "heart"}
                                         :sections (keep (partial tab-section< description-data)
                                                         [{:heading      "Maintenance Level"
                                                           :content-path [:product :copy/maintenance-level]}
                                                          {:heading      "Can it be Colored?"
                                                           :content-path [:dynamic-content :pdp.details.care/can-it-be-colored?]}])}]})
       :else (let [{:keys [copy/description
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

;; A requirement of the product carousel is that it needs to have the media
;; for a detailed product. That should probably be included on the product
;; itself, but for now we'll just use the product carousel query to get the media
(defn- detailed-product-media
  [images-catalog product]
  (->> product
       (images/for-skuer images-catalog)
       (selector/match-all {:selector/strict? true}
                           {:use-case #{"carousel"}
                            :image/of #{"model" "product"}})
       (sort-by :order)))

(defn ^:private product-carousel<-
  [images-catalog product-carousel detailed-product carousel-redesign?]
  (when (and carousel-redesign?
             (select ?wig [detailed-product]))
    (let [index    (:idx product-carousel)
          exhibits (->> detailed-product
                        (detailed-product-media images-catalog)
                        (map (fn [{:keys [alt url]}]
                               {:src url
                                :alt alt})))]
      #:product-carousel
      {:selected-exhibit-idx (if (< -1 index (count exhibits))
                               index 0)
       :exhibits             exhibits})))

(defn ^:export built-component
  [state opts]
  (let [;; Databases
        images-db          (get-in state keypaths/v2-images)
        product-carousel   (carousel-neue/<- state :product-carousel)
        ;; Flags
        carousel-redesign? (experiments/carousel-redesign? state)
        ;; Focus
        detailed-product   (products/current-product state)]
    (component/build template
                     (merge (query state)
                            {:add-to-cart (add-to-cart-query state)}
                            (product-carousel<- images-db product-carousel detailed-product carousel-redesign?))
                     opts)))

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
                           (messages/handle-message events/viewed-sku {:sku selected-sku}))))))

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

(defmethod transitions/transition-state events/control-product-detail-picker-option-select
  [_ event {:keys [selection value]} app-state]
  ;; HACK [#179260091]
  (let [switching-to-short-hd-lace? (and (= [selection value] [:hair/base-material "hd-lace"])
                                         (-> app-state
                                             (get-in catalog.keypaths/detailed-product-selections)
                                             :hair/length
                                             spice/parse-int
                                             (< 16)))
        selected-sku                (->> (if switching-to-short-hd-lace?
                                           {selection #{value} :hair/length #{"16"}}
                                           {selection #{value}})
                                         (determine-sku-from-selections app-state))
        options                     (generate-product-options (get-in app-state catalog.keypaths/detailed-product-id)
                                                              app-state)]
    (-> app-state
        (assoc-in catalog.keypaths/detailed-product-picker-visible? false)
        (assoc-in catalog.keypaths/detailed-product-selected-sku selected-sku)
        (update-in catalog.keypaths/detailed-product-selections merge (if switching-to-short-hd-lace?
                                                                        {selection value :hair/length "16"}
                                                                        {selection value}))
        (assoc-in catalog.keypaths/detailed-product-options options))))

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

(defmethod transitions/transition-state events/control-product-detail-picker-open
  [_ event {:keys [facet-slug length-index]} app-state]
  (-> app-state
      (assoc-in catalog.keypaths/detailed-product-selected-picker facet-slug)
      (assoc-in catalog.keypaths/detailed-product-picker-visible? true)
      (assoc-in catalog.keypaths/detailed-product-lengths-index length-index)))

(defmethod transitions/transition-state events/control-product-detail-picker-option-quantity-select
  [_ event {:keys [value]} app-state]
  (-> app-state
      (assoc-in keypaths/browse-sku-quantity value)
      (assoc-in catalog.keypaths/detailed-product-picker-visible? false)))

(defmethod transitions/transition-state events/control-product-detail-picker-close
  [_ event _ app-state]
  (-> app-state
      (assoc-in catalog.keypaths/detailed-product-picker-visible? false)
      (assoc-in catalog.keypaths/detailed-product-lengths-index nil)
      (assoc-in catalog.keypaths/detailed-product-selected-picker nil)))

#?(:cljs
   (defmethod effects/perform-effects events/control-product-detail-picker-open
     [_ _ _ _ _]
     (scroll/disable-body-scrolling)))

#?(:cljs
   (defmethod effects/perform-effects events/control-product-detail-picker-close
     [_ _ _ _ _]
     (scroll/enable-body-scrolling)))

#?(:cljs
   (defmethod effects/perform-effects events/navigate-product-details
     [_ event args prev-app-state app-state]
     (let [[prev-event prev-args] (get-in prev-app-state keypaths/navigation-message)
           product-id (:catalog/product-id args)
           product (get-in app-state (conj keypaths/v2-products product-id))
           just-arrived? (or (not= events/navigate-product-details prev-event)
                             (not= (:catalog/product-id args)
                                   (:catalog/product-id prev-args))
                             (= :first-nav (:navigate/caused-by args)))]
       (when (experiments/pdp-content-slots? app-state)
         (effects/fetch-cms2 app-state [:filledContentSlot]))
       (when (nil? product)
         (fetch-product-details app-state product-id))
       (when just-arrived?
         (messages/handle-message events/initialize-product-details (assoc args :origin-nav-event event))))))

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

(defmethod transitions/transition-state events/initialize-product-details
  [_ event {:as args :keys [catalog/product-id query-params]} app-state]
  (let [product-options   (generate-product-options product-id app-state)
        product           (products/product-by-id app-state product-id)
        product-skus      (products/extract-product-skus app-state product)
        sku               (or (->> (:SKU query-params)
                                   (conj keypaths/v2-skus)
                                   (get-in app-state))
                              (get-in app-state catalog.keypaths/detailed-product-selected-sku)
                              (first product-skus))
        availability      (catalog.products/index-by-selectors
                           [:hair/color :hair/length]
                           product-skus)]
    (-> app-state
        (assoc-in catalog.keypaths/detailed-product-id product-id)
        (assoc-in catalog.keypaths/detailed-product-selected-sku sku)
        (assoc-in keypaths/browse-sku-quantity 1)
        (assoc-in catalog.keypaths/detailed-product-selected-picker nil)
        (assoc-in catalog.keypaths/detailed-product-picker-visible? nil)
        (assoc-in catalog.keypaths/detailed-product-availability availability)
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
           (if (auth/permitted-product? app-state product)
             (review-hooks/insert-reviews)
             (effects/redirect events/navigate-home))
           (review-hooks/start)
           (seo/set-tags app-state)
           (when-let [album-keyword (storefront.ugc/product->album-keyword shop? product)]
             (effects/fetch-cms-keypath app-state [:ugc-collection album-keyword]))
           (when-let [pdp-faq-id (accessors.products/product->faq-id product)]
             (effects/fetch-cms-keypath app-state [:faq pdp-faq-id])))))))


(defmethod effects/perform-effects events/control-add-sku-to-bag
  [_ _ {:keys [sku quantity]} _ _]
  (messages/handle-message events/add-sku-to-bag
                           {:sku           sku
                            :stay-on-page? false
                            :quantity      quantity}))

;; TODO(corey) Move this to cart
(defmethod effects/perform-effects events/add-sku-to-bag
  [_ _ {:keys [sku quantity stay-on-page?]} _ state]
  #?(:cljs
     (let [nav-event          (get-in state keypaths/navigation-event)]
       (api/add-sku-to-bag
        (get-in state keypaths/session-id)
        {:sku                sku
         :quantity           quantity
         :stylist-id         (get-in state keypaths/store-stylist-id)
         :token              (get-in state keypaths/order-token)
         :number             (get-in state keypaths/order-number)
         :user-id            (get-in state keypaths/user-id)
         :user-token         (get-in state keypaths/user-token)
         :heat-feature-flags (keys (filter second (get-in state keypaths/features)))}
        #(do
           (messages/handle-message events/api-success-add-sku-to-bag
                                    {:order    %
                                     :quantity quantity
                                     :sku      sku})
           (when (not (or (= events/navigate-cart nav-event) stay-on-page?))
             (history/enqueue-navigate events/navigate-cart)))))))

(defmethod transitions/transition-state events/api-success-add-sku-to-bag
  [_ event {:keys [quantity sku]} app-state]
  (assoc-in app-state keypaths/browse-sku-quantity 1))

#?(:cljs
   (defmethod trackings/perform-track events/pdp|picker-options|viewed
     [_ event data app-state]
     (stringer/track-event "pdp.picker-options/viewed" data)))

#?(:cljs
   (defmethod effects/perform-effects events/pdp|picker-options|selected
     [_ _ {:keys [data callback-message]} _ _]
     (when (= (:facet data) "color")
       (publish events/pdp|carousel|color-synced
                {:color (:selected-option data)}))
     (apply messages/handle-message callback-message)))

#?(:cljs
   (defmethod trackings/perform-track events/pdp|picker-options|selected
     [_ event {:keys [data]} app-state]
     (stringer/track-event "pdp.picker-options/selected" data)))

;; Syncs the carousel to the color selected in the picker
(defmethod effects/perform-effects events/pdp|carousel|color-synced
  [_ _ {:keys [color]} _ state]
  (let [images-db        (get-in state keypaths/v2-images)
        {:keys [idx]}    (carousel-neue/<- state :product-carousel)
        detailed-product (products/current-product state)]
    (let [idx-synced? (-> (detailed-product-media images-db detailed-product)
                          (nth idx)
                          :hair/color
                          (= color))
          target-idx  (->> (detailed-product-media images-db detailed-product)
                           (map-indexed (fn [i item] [i item]))
                           (filter (fn [[_ item]] (= (:hair/color item) color)))
                           ffirst)]
      (when (and (not idx-synced?)
                 (int? target-idx))
        (publish events/carousel|jumped {:id  :product-carousel
                                         :idx target-idx})))))
