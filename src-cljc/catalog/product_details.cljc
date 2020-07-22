(ns catalog.product-details
  (:require #?@(:cljs [[goog.dom]
                       [goog.events.EventType :as EventType]
                       [goog.events]
                       [goog.style]
                       [storefront.accessors.auth :as auth]
                       [storefront.api :as api]
                       [storefront.hooks.seo :as seo]
                       [storefront.browser.scroll :as scroll]
                       [storefront.history :as history]
                       [storefront.hooks.facebook-analytics :as facebook-analytics]
                       [storefront.hooks.reviews :as review-hooks]
                       [storefront.platform.messages :as messages]
                       [storefront.trackings :as trackings]])
            [catalog.facets :as facets]
            [catalog.keypaths]
            [catalog.product-details-ugc :as ugc]
            [catalog.products :as products]
            [catalog.selector.sku :as sku-selector]
            [catalog.ui.molecules :as catalog.M]
            [catalog.ui.how-it-works :as how-it-works]
            [clojure.string :as string]
            [spice.date :as date]
            [spice.selector :as selector]
            [spice.maps :as maps]
            [storefront.accessors.contentful :as contentful]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.products :as accessors.products]
            [storefront.accessors.skus :as skus]
            [storefront.accessors.images :as images]
            [storefront.component :as component :refer [defcomponent defdynamic-component]]
            [storefront.components.marquee :as marquee]
            [storefront.components.money-formatters :as mf]
            [storefront.components.picker.picker :as picker]
            [storefront.components.svg :as svg]
            [storefront.components.tabbed-information :as tabbed-information]
            [storefront.components.ui :as ui]
            [storefront.components.v2 :as v2]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.reviews :as review-component]
            [storefront.request-keys :as request-keys]
            [storefront.transitions :as transitions]
            [catalog.ui.add-to-cart :as add-to-cart]
            [catalog.ui.freeinstall-banner :as freeinstall-banner]
            [catalog.keypaths :as catalog.keypaths]
            [spice.core :as core]))

(defn page [wide-left wide-right-and-narrow]
  [:div.clearfix.mxn2
   [:div.col-on-tb-dt.col-7-on-tb-dt.px2
    [:div.hide-on-mb wide-left]]
   [:div.col-on-tb-dt.col-5-on-tb-dt.px2 wide-right-and-narrow]])

(defn full-bleed-narrow [body]
  ;; The mxn2 pairs with the p2 of the container, to make the body full width
  ;; on mobile.
  [:div.hide-on-tb-dt.mxn2 body])

(defn counter-or-out-of-stock [can-supply? quantity]
  (if can-supply?
    [:div
     (ui/counter {:spinning? false
                  :data-test "pdp"}
                 quantity
                 (utils/send-event-callback events/control-counter-dec
                                            {:path keypaths/browse-sku-quantity})
                 (utils/send-event-callback events/control-counter-inc
                                            {:path keypaths/browse-sku-quantity}))]
    [:span.h4 "Currently out of stock"]))

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

(defn ^:private handle-scroll [component e]
  #?(:cljs (component/set-state! component :show? (< 866 (.-y (goog.dom/getDocumentScroll))))))

(defn ^:private set-height [component]
  #?(:cljs (component/set-state! component :add-button-height (some-> (component/get-ref component "add-button")
                                                                      goog.style/getSize
                                                                      .-height))))

(defdynamic-component ^:private sticky-add-component
  (constructor [c props]
               (component/create-ref! c "add-button")
               (set! (.-handle-scroll c) (partial handle-scroll c))
               (set! (.-set-height c) (partial set-height c))
               ;; Treat show? as a trinary state:
               ;; nil = we need to hide the element, but still be available for height calculations
               ;; true = show the element (set margin-bottom to 0)
               ;; false = hide the element (set margin-bottom to computed height calculations)
               {:show? nil})
  (did-mount [c]
             #?(:cljs
                (do
                  (.set-height c)
                  (.handle-scroll c nil) ;; manually fire once on load incase the page already scrolled
                  (goog.events/listen js/window EventType/SCROLL (.-handle-scroll c)))))
  (will-unmount [c]
                #?(:cljs
                   (goog.events/unlisten js/window EventType/SCROLL (.-handle-scroll c))))
  (render [c]
          (let [{:keys [show? add-button-height]}                                             (component/get-state c)
                {:keys [selected-options sold-out? unavailable? adding-to-bag? sku quantity]} (component/get-props c)
                unpurchasable?                                                                (or sold-out? unavailable?)
                text-style                                                                    (if unpurchasable? {:class "gray"} {})]
            (component/html
             [:div.fixed.z4.bottom-0.left-0.right-0.transition-2
              (cond
                (nil? show?) {:style {:visibility "hidden"}}
                show?        {:style {:margin-bottom "0"}}
                :else        {:style {:margin-bottom (str "-" add-button-height "px")}})
              [:div {:ref (component/use-ref c "add-button")}
               [:div.p3.flex.justify-center.items-center.bg-white.border-top.border-cool-gray
                [:div.col-8
                 [:a.inherit-color
                  #?(:cljs {:on-click #(scroll/scroll-selector-to-top "body")})
                  [:div.flex.items-center
                   [:img.border.border-gray.rounded-0
                    {:height "33px"
                     :width  "65px"
                     :src    (:option/rectangle-swatch (:hair/color selected-options))}]
                   [:span.ml2 "Length: " [:span text-style (:option/name (:hair/length selected-options))]]
                   [:span.ml2 "Qty: " [:span text-style quantity]]]]]
                [:div.col-4
                 (ui/button-large-primary {:on-click
                                           (utils/send-event-callback events/control-add-sku-to-bag
                                                                      {:sku      sku
                                                                       :quantity quantity})
                                           :data-test      "sticky-add-to-cart"
                                           :disabled?      unpurchasable?
                                           :disabled-class "bg-gray"
                                           :spinning?      adding-to-bag?}
                                          (cond
                                            unavailable? "Unavailable"
                                            sold-out?    "Sold Out"
                                            :default     "Add"))]]]]))))

(def checkout-button
  (component/html
   [:div
    {:data-test "cart-button"
     :data-ref "cart-button"}
    (ui/button-large-primary (utils/route-to events/navigate-cart) "Check out")]))

(def shipping-and-guarantee
  (component/html
   [:div.border-top.border-bottom.border-gray.p2.my2.center.shout.medium.h6
    "Free shipping & 30 day guarantee"]))

(defn image-body [i {:keys [filename url alt]}]
  (ui/aspect-ratio
   640 580
   (if (zero? i)
     [:img.col-12
      {:src (str url "-/format/auto/-/resize/640x/" filename)
       :alt alt}]
     (ui/defer-ucare-img
       {:class       "col-12"
        :alt         alt
        :width       640
        :placeholder (ui/large-spinner {:style {:height     "60px"
                                                :margin-top "130px"}})}
       url))))

(defn carousel [images _]
  (component/build carousel/component
                   {:images   images
                    :settings {:edgePadding 0
                               :items       1}}
                   {:opts {:slides (map-indexed image-body images)}}))

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
     (catalog.M/product-title data)]
    [:div.col-2
     (catalog.M/price-block data)]]
   (catalog.M/yotpo-reviews-summary data)])

(defcomponent component
  [{:keys [adding-to-bag?
           carousel-images
           product
           reviews
           selected-sku
           sku-quantity
           selected-options
           how-it-works
           get-a-free-install-section-data
           options
           picker-data
           aladdin?
           sticky-add-to-bag?
           ugc] :as data} owner opts]
  (let [unavailable? (not (seq selected-sku))
        sold-out?    (not (:inventory/in-stock? selected-sku))]
    (if-not product
      [:div.flex.h2.p1.m1.items-center.justify-center
       {:style {:height "25em"}}
       (ui/large-spinner {:style {:height "4em"}})]
      [:div
       [:div.container
        (when (:offset ugc)
          [:div.absolute.overlay.z4.overflow-auto
           {:key "popup-ugc"}
           (component/build ugc/popup-component ugc opts)])
        [:div
         {:key "page"}
         (page
          (component/html
           [:div
            ^:inline (carousel carousel-images product)
            [:div.hide-on-mb (component/build ugc/component ugc opts)]
            (when how-it-works
              [:div.container.mx-auto.mt4.px4.hide-on-mb
               (component/build how-it-works/organism how-it-works)])])
          (component/html
           [:div
            [:div
             (full-bleed-narrow (carousel carousel-images product))]
            (component/build product-summary-organism data)
            [:div.px2
             (component/build picker/component picker-data opts)]
            [:div
             (cond
               unavailable? unavailable-button
               sold-out?    sold-out-button
               :else        (component/build add-to-cart/organism data))]
            (when (products/stylist-only? product)
              shipping-and-guarantee)
            (component/build catalog.M/service-description data opts)
            (component/build tabbed-information/component data)
            (component/build catalog.M/non-hair-product-description data opts)
            (when how-it-works
              [:div.container.mx-auto.mt4.px4.hide-on-dt.hide-on-tb
               (component/build how-it-works/organism how-it-works)])
            (component/build freeinstall-banner/organism data opts)
            [:div.hide-on-tb-dt.mxn2.mb3 (component/build ugc/component ugc opts)]]))]]
       (when aladdin?
         [:div.py10.bg-pale-purple.col-on-tb-dt.mt4
          (component/build v2/get-a-free-install get-a-free-install-section-data)])
       (when (seq reviews)
         [:div.container.col-7-on-tb-dt.px2
          (component/build review-component/reviews-component reviews opts)])
       (when sticky-add-to-bag?
           ;; We use visibility:hidden rather than display:none so that this component has a height.
           ;; We use the height on mobile view to slide it on/off the bottom of the page.
         [:div.invisible-on-tb-dt
          (component/build sticky-add-component
                           {:image            (->> options
                                                   :hair/color
                                                   (filter #(= (first (:hair/color selected-sku))
                                                               (:option/slug %)))
                                                   first
                                                   :option/rectangle-swatch)
                            :adding-to-bag?   adding-to-bag?
                            :sku              selected-sku
                            :sold-out?        sold-out?
                            :unavailable?     (empty? selected-sku)
                            :selected-options selected-options
                            :quantity         sku-quantity} {})])])))

(defn min-of-maps
  ([k] {})
  ([k a b]
   (if (<= (get a k) (get b k))
     a b)))

(defn lowest [k ms]
  (reduce (partial min-of-maps k) ms))

(defn lowest-sku-price [skus]
  (:sku/price (lowest :sku/price skus)))

(defn lowest-sku-price-for-option-kw [skus option-kw]
  (into {}
        (map (fn [[option-kw skus]]
               {(first option-kw)
                (lowest-sku-price skus)})
             (group-by option-kw skus))))

(defn ugc-query [product sku data]
  (let [ugc                (get-in data keypaths/ugc)
        album-keyword      (-> product :legacy/named-search-slug keyword)
        cms-ugc-collection (get-in data (conj keypaths/cms-ugc-collection album-keyword))]
    (when-let [social-cards (when product
                              (->> cms-ugc-collection
                                   :looks
                                   (mapv (partial contentful/look->pdp-social-card
                                                  (get-in data keypaths/navigation-event)
                                                  album-keyword))
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

(defn add-to-cart-query
  [data selected-sku sku-price]
  (let [shop?                                (= "shop" (get-in data keypaths/store-slug))
        sku-family                           (-> selected-sku :hair/family first)
        mayvenn-install-incentive-families   #{"bundles" "closures" "frontals" "360-frontals"}
        wig-customization-incentive-families #{"360-wigs" "lace-front-wigs"}
        base-service-already-in-cart?        (boolean (some #(= (:catalog/sku-id selected-sku) (:sku %))
                                                            (orders/service-line-items (get-in data keypaths/order))))]
    (cond-> {:cta/id        "add-to-cart"
             :cta/label     (if base-service-already-in-cart? "Already In Cart" "Add to Cart")
             :cta/target    [events/control-add-sku-to-bag
                             {:sku      selected-sku
                              :quantity (get-in data keypaths/browse-sku-quantity 1)}]
             :cta/spinning? (utils/requesting? data (conj request-keys/add-to-bag (:catalog/sku-id selected-sku)))
             :cta/disabled? (or (not (:inventory/in-stock? selected-sku))
                                base-service-already-in-cart?)}

      (not (accessors.products/product-is-mayvenn-install-service? selected-sku))
      (merge
       {:add-to-cart.quadpay/loaded? (get-in data keypaths/loaded-quadpay)
        :add-to-cart.quadpay/price   sku-price})

      (and shop? (mayvenn-install-incentive-families sku-family))
      (merge
       {:add-to-cart.incentive-block/id          "add-to-cart-incentive-block"
        :add-to-cart.incentive-block/footnote    "*Mayvenn Services cannot be combined with other promotions"
        :add-to-cart.incentive-block/link-id     "learn-more-mayvenn-install"
        :add-to-cart.incentive-block/link-label  "Learn more"
        :add-to-cart.incentive-block/link-target [events/popup-show-consolidated-cart-free-install]
        :add-to-cart.incentive-block/message     (str "Get a free Mayvenn Service by a licensed "
                                                      "stylist with qualifying purchases.* ")})
      (and shop? (wig-customization-incentive-families sku-family))
      (merge
       {:add-to-cart.incentive-block/id       "add-to-cart-incentive-block"
        :add-to-cart.incentive-block/callout  "✋Don't miss out on free Wig Customization"
        :add-to-cart.incentive-block/footnote "*Wig Customization cannot be combined with other promotions"
        :add-to-cart.incentive-block/message  (str "Get a free Wig Customization by a licensed stylist when "
                                                   "you purchase a Virgin Lace Front Wig or Virgin 360 Wig.")}))))

(defn query [data]
  (let [selected-sku          (get-in data catalog.keypaths/detailed-product-selected-sku)
        selections            (get-in data catalog.keypaths/detailed-product-selections)
        product               (products/current-product data)
        product-skus          (products/extract-product-skus data product)
        images-catalog        (get-in data keypaths/v2-images)
        facets                (facets/by-slug data)
        carousel-images       (find-carousel-images product product-skus images-catalog
                                                    (select-keys selections [:hair/color])
                                                    selected-sku)
        options               (get-in data catalog.keypaths/detailed-product-options)
        ugc                   (ugc-query product selected-sku data)
        sku-price             (:sku/price selected-sku)
        review-data           (review-component/query data)
        shop?                 (= "shop" (get-in data keypaths/store-slug))
        free-mayvenn-service? (accessors.products/product-is-mayvenn-install-service? product)
        standalone-service?   (accessors.products/standalone-service? product)
        service?              (accessors.products/service? product)
        hair?                 (accessors.products/hair? product)
        wig?                  (accessors.products/wig-product? product)
        wig-customization?    (seq (spice.selector/match-all {} {:catalog/department "service"
                                                                 :service/category   "customization"} [product]))]
    (merge
     {:reviews                            review-data
      :yotpo-reviews-summary/product-name (some-> review-data :yotpo-data-attributes :data-name)
      :yotpo-reviews-summary/product-id   (some-> review-data :yotpo-data-attributes :data-product-id)
      :yotpo-reviews-summary/data-url     (some-> review-data :yotpo-data-attributes :data-url)
      :title/primary                      (:copy/title product)
      :ugc                                ugc
      :aladdin?                           (experiments/aladdin-experience? data)
      :fetching-product?                  (utils/requesting? data (conj request-keys/get-products
                                                                        (:catalog/product-id product)))
      :adding-to-bag?                     (utils/requesting? data (conj request-keys/add-to-bag (:catalog/sku-id selected-sku)))
      :sku-quantity                       (get-in data keypaths/browse-sku-quantity 1)
      :options                            options
      :product                            product
      :selections                         selections
      :selected-options                   (get-selected-options selections options)
      :selected-sku                       selected-sku
      :facets                             facets
      :selected-picker                    (get-in data catalog.keypaths/detailed-product-selected-picker)
      :picker-data                        (picker/query data)
      :carousel-images                    carousel-images
      :sticky-add-to-bag?                 (and (nil? (:offset ugc))
                                               (not (products/stylist-only? product))
                                               (not
                                                (accessors.products/product-is-mayvenn-install-service? product))
                                               (not standalone-service?)
                                               (not (experiments/promotion-helper? data)))}

     (when (and (not service?) sku-price)
       {:price-block/primary   (mf/as-money sku-price)
        :price-block/secondary "each"})

     (add-to-cart-query data selected-sku sku-price)

     (when hair?
       (let [active-tab-name (or (keyword (get-in data keypaths/product-details-tab)) :description)]
         #:tabbed-information{:id      "product-description-tabs"
                              :keypath keypaths/product-details-tab
                              :tabs    [{:title    "Description"
                                         :id       "description"
                                         :active?  (= active-tab-name :description)
                                         :icon     {:opts {:height "18px"
                                                           :width  "18px"}
                                                    :id   "description"}
                                         :primary  (:copy/description product)
                                         :sections (keep (fn [[heading content-key]]
                                                           (when-let [content (get product content-key)]
                                                             {:heading heading
                                                              :content content}))
                                                         [["Hair Type" :copy/hair-type]
                                                          ["What's Included" :copy/whats-included]
                                                          ["Model Wearing" :copy/model-wearing]
                                                          ["Available Services" :copy/available-services]])}
                                        {:title    "Hair Info"
                                         :id       "hair-info"
                                         :active?  (= active-tab-name :hair-info)
                                         :icon     {:opts {:height "20px"
                                                           :width  "20px"}
                                                    :id   "info-color-circle"}
                                         :sections (keep (fn [[heading content-key]]
                                                           (when-let [content (get product content-key)]
                                                             {:heading heading
                                                              :content content}))
                                                         [["Unit Weight" :copy/weights]
                                                          ["Hair Quality" :copy/quality]
                                                          ["Hair Origin" :copy/origin]
                                                          ["Hair Weft Type" :copy/weft-type]
                                                          ["Part Design" :copy/part-design]
                                                          ["Features" :copy/features]
                                                          ["Available Materials" :copy/materials]
                                                          ["Lace Size" :copy/lace-size]
                                                          ["Lace Color" :copy/lace-color]
                                                          ["Silk Size" :copy/silk-size]
                                                          ["Silk Color" :copy/silk-color]
                                                          ["Cap Size" :copy/cap-size]
                                                          ["Wig Density" :copy/density]
                                                          ["Tape-in Glue Information" :copy/tape-in-glue]])}
                                        {:title    "Care"
                                         :id       "care"
                                         :active?  (= active-tab-name :care)
                                         :icon     {:opts {:height "20px"
                                                           :width  "20px"}
                                                    :id   "heart"}
                                         :sections (keep (fn [[heading content-key]]
                                                           (when-let [content (get product content-key)]
                                                             {:heading heading
                                                              :content content}))
                                                         [["Maintenance Level" :copy/maintenance-level]
                                                          ["Can it be Dyed?" :copy/dyeable?]])}]}))

     (when-not hair?
       (let [{:keys [copy/description
                     copy/colors
                     copy/weights
                     copy/density
                     copy/materials
                     copy/whats-included
                     copy/summary
                     copy/duration]} product]
         #:product-description {:service?             (accessors.products/service? product)
                                :duration             duration
                                :summary              summary
                                :description          description
                                :materials            materials
                                :colors               colors
                                :density              density
                                :whats-included       whats-included
                                :weights              (when-not density
                                                        weights)
                                :learn-more-nav-event (when-not (or (contains? (:stylist-exclusives/family product) "kits")
                                                                    standalone-service?)
                                                        events/navigate-content-our-hair)}))

     (when (and shop? (not standalone-service?))
       #:freeinstall-banner {:title       "Buy 3 items and we'll pay for your hair install"
                             :subtitle    "Choose any Mayvenn stylist in your area"
                             :button-copy "browse stylists"
                             :nav-event   [events/navigate-adventure-match-stylist]
                             :class       "bg-pale-purple"
                             :id          "freeinstall-banner-cta"})

     (when free-mayvenn-service?
       {:price-block/primary-struck (mf/as-money sku-price)
        :price-block/secondary      [:span.teal "FREE"]
        :title/secondary            (:promo.mayvenn-install/requirement-copy product)
        :freeinstall-banner/title          "Amazing Stylists"
        :freeinstall-banner/icon           (svg/heart {:class  "fill-p-color"
                                                       :width  "32px"
                                                       :height "29px"})
        :freeinstall-banner/subtitle       (str "We’ve rounded up the best stylists in the country so you can be "
                                                "sure your hair is in really, really good hands.")
        :freeinstall-banner/button-copy    "browse stylists"
        :freeinstall-banner/nav-event      [events/navigate-adventure-match-stylist]
        :freeinstall-banner/image-ucare-id "f4c760b8-c240-4b31-b98d-b953d152eaa5"
        :freeinstall-banner/class          "bg-refresh-gray"
        :freeinstall-banner/id             "freeinstall-banner-cta"
        :how-it-works
        {:how-it-works/title-secondary "Here’s how it works."
         :how-it-works/step-elements
         [{:how-it-works.step.title/primary   "01"
           :how-it-works.step.title/secondary "Pick your service"
           :how-it-works.step.body/primary    "Choose the service you’d like to book from our full list of complimentary Mayvenn service offerings."}
          {:how-it-works.step.title/primary   "02"
           :how-it-works.step.title/secondary "Select a Mayvenn-Certified stylist"
           :how-it-works.step.body/primary    (str "We've hand-picked thousands of talented stylists around the country. "
                                                   "Browse the stylists in your area to find your perfect match.") }
          {:how-it-works.step.title/primary   "03"
           :how-it-works.step.title/secondary "Schedule your appointment"
           :how-it-works.step.body/primary    (str "We’ll connect you with your stylist to set up your service. "
                                                   "Then, we’ll send you a prepaid voucher to cover the cost. ")}]}})
     (when wig-customization?
       {:how-it-works
        {:how-it-works/title-secondary "Here’s how it works."
         :how-it-works/step-elements
         [{:how-it-works.step.title/primary   "01"
           :how-it-works.step.title/secondary "Select Your Wig"
           :how-it-works.step.body/primary    "Decide which wig you want and buy it from Mayvenn. Shop Lace Front & 360 Lace Wigs."}
          {:how-it-works.step.title/primary   "02"
           :how-it-works.step.title/secondary "Choose a Mayvenn Certified Stylist"
           :how-it-works.step.body/primary    "Browse our network of professional stylists in your area and make an appointment." }
          {:how-it-works.step.title/primary   "03"
           :how-it-works.step.title/secondary "Drop Off Your Wig"
           :how-it-works.step.body/primary    (str "Leave the wig with your stylist and talk about what you want. "
                                                   "Your stylist will bleach the knots, tint the lace, cut the lace, customize your hairline and make sure it fits perfectly.  ")}
          {:how-it-works.step.title/primary   "04"
           :how-it-works.step.title/secondary "Schedule Your Pickup"
           :how-it-works.step.body/primary    "Make an appointment to pick up your wig with your stylist in a week."}
          {:how-it-works.step.title/primary   "05"
           :how-it-works.step.title/secondary "Go Get Your Wig"
           :how-it-works.step.body/primary    "You pick up your wig. Let us pick up the tab. Let us cover the cost of your customization—we insist."}]}})

     (when standalone-service?
       {:price-block/primary               (mf/as-money sku-price)
        :freeinstall-banner/title          "Amazing Stylists"
        :freeinstall-banner/icon           (svg/heart {:class  "fill-p-color"
                                                       :width  "32px"
                                                       :height "29px"})
        :freeinstall-banner/subtitle       (str "We’ve rounded up the best stylists in the country so you can be "
                                                "sure your hair is in really, really good hands.")
        :freeinstall-banner/button-copy    "browse stylists"
        :freeinstall-banner/nav-event      [events/navigate-adventure-match-stylist]
        :freeinstall-banner/image-ucare-id "f4c760b8-c240-4b31-b98d-b953d152eaa5"
        :freeinstall-banner/class          "bg-refresh-gray"
        :freeinstall-banner/id             "freeinstall-banner-cta"
        :how-it-works
        {:how-it-works/title-secondary "Here’s how it works."
         :how-it-works/step-elements
         [{:how-it-works.step.title/primary   "01"
           :how-it-works.step.title/secondary "Pick your service"
           :how-it-works.step.body/primary    (str "Choose the service you’d like to book from our full list of salon service offerings. "
                                                   "Next, you’ll see which stylists are nearby and decide who you want to book.")}
          {:how-it-works.step.title/primary   "02"
           :how-it-works.step.title/secondary "Select a Mayvenn-Certified stylist"
           :how-it-works.step.body/primary    (str "We've hand-picked thousands of talented stylists around the country. "
                                                   "Browse the stylists in your area to find your perfect match.") }
          {:how-it-works.step.title/primary   "03"
           :how-it-works.step.title/secondary "Schedule your appointment"
           :how-it-works.step.body/primary    (str "We’ll connect you with your stylist to set up your service. "
                                                   "Then, we’ll send you a prepaid voucher to cover the cost. ")}]}})

     (when wig?
       {:freeinstall-banner/title "Buy any Lace Front or 360 Wig and we'll pay for your wig customization"}))))

(defn ^:export built-component [data opts]
  (component/build component (query data) opts))

(defn get-valid-product-skus [product all-skus]
  (->> product
       :selector/skus
       (select-keys all-skus)
       vals))

(defn determine-sku-id
  [app-state product]
  (let [selected-sku-id    (get-in app-state catalog.keypaths/detailed-product-selected-sku-id)
        valid-product-skus (get-valid-product-skus product (get-in app-state keypaths/v2-skus))
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
                   direct-to-details?)
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

     (if-let [current-product (products/current-product app-state)]
       (if (auth/permitted-product? app-state current-product)
         (review-hooks/insert-reviews)
         (effects/redirect events/navigate-home)))))

(defn first-when-only [coll]
  (when (= 1 (count coll))
    (first coll)))

(defn determine-sku-from-selections
  [app-state new-selections]
  (let [product-skus   (get-in app-state catalog.keypaths/detailed-product-product-skus)
        old-selections (get-in app-state catalog.keypaths/detailed-product-selections)]
    (->> product-skus
         (selector/match-all {} (merge old-selections new-selections))
         first-when-only)))

(defn generate-product-options
  [app-state]
  (let [product-id   (get-in app-state catalog.keypaths/detailed-product-id)
        product      (products/product-by-id app-state product-id)
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
                   (get-in app-state catalog.keypaths/detailed-product-selections)
                   (reduce-kv #(assoc %1 %2 (first %3))
                              {}
                              (select-keys sku (:selector/electives product))))))))

(defmethod transitions/transition-state events/initialize-product-details
  [_ event {:as args :keys [catalog/product-id query-params]} app-state]
  (let [ugc-offset (:offset query-params)
        sku        (->> (:SKU query-params)
                        (conj keypaths/v2-skus)
                        (get-in app-state))
        options    (generate-product-options app-state)]
    (-> app-state
        (assoc-in catalog.keypaths/detailed-product-id product-id)
        (assoc-in catalog.keypaths/detailed-product-selected-sku sku)
        (assoc-in keypaths/ui-ugc-category-popup-offset ugc-offset)
        (assoc-in keypaths/browse-recently-added-skus [])
        (assoc-in keypaths/browse-sku-quantity 1)
        (assoc-in catalog.keypaths/detailed-product-selected-picker nil)
        (assoc-in catalog.keypaths/detailed-product-picker-visible? nil)
        (cond-> sku (assoc-selections sku))  ; can be nil if not yet fetched
        (assoc-in catalog.keypaths/detailed-product-options options))))

(defmethod transitions/transition-state events/control-product-detail-picker-option-select
  [_ event {:keys [selection value]} app-state]
  (let [selected-sku (->> {selection #{value}}
                          (determine-sku-from-selections app-state))
        options      (generate-product-options app-state)]
    (-> app-state
        (assoc-in catalog.keypaths/detailed-product-selected-sku selected-sku)
        (update-in catalog.keypaths/detailed-product-selections merge {selection value})
        (assoc-in catalog.keypaths/detailed-product-options options))))

(defmethod effects/perform-effects events/control-product-detail-picker-option-select
  [_ event {:keys [navigation-event]} _ app-state]
  (let [sku-id-for-selection (-> app-state
                                 (get-in catalog.keypaths/detailed-product-selected-sku)
                                 :catalog/sku-id)
        params-with-sku-id   (cond-> app-state
                               :always
                               products/current-product
                               :always
                               (select-keys [:catalog/product-id :page/slug])
                               (some? sku-id-for-selection)
                               (assoc :query-params {:SKU sku-id-for-selection}))]
    (effects/redirect navigation-event params-with-sku-id :sku-option-select)
    #?(:cljs (scroll/enable-body-scrolling))))

(defmethod transitions/transition-state events/control-product-detail-picker-open
  [_ event {:keys [facet-slug]} app-state]
  (-> app-state
      (assoc-in catalog.keypaths/detailed-product-selected-picker facet-slug)
      (assoc-in catalog.keypaths/detailed-product-picker-visible? true)))

(defmethod transitions/transition-state events/control-product-detail-picker-option-quantity-select
  [_ event {:keys [value]} app-state]
  (-> app-state
      (assoc-in keypaths/browse-sku-quantity value)
      (assoc-in catalog.keypaths/detailed-product-picker-visible? false)))

(defmethod transitions/transition-state events/control-product-detail-picker-close
  [_ event _ app-state]
  (assoc-in app-state catalog.keypaths/detailed-product-picker-visible? false))

#?(:cljs
   (defmethod effects/perform-effects events/control-product-detail-picker-open
     [_ _ _ _ _]
     (scroll/disable-body-scrolling)))

#?(:cljs
   (defmethod effects/perform-effects events/control-product-detail-picker-close
     [_ _ _ _ _]
     (scroll/enable-body-scrolling)))

#?(:cljs
   (defmethod effects/perform-effects events/initialize-product-details
     [_ _ {:keys [catalog/product-id page/slug query-params origin-nav-event]} _ app-state]
     (let [selected-sku (get-in app-state catalog.keypaths/detailed-product-selected-sku)]
       (if (url-points-to-invalid-sku? selected-sku query-params)
         (effects/redirect origin-nav-event
                           (merge
                            {:catalog/product-id product-id
                             :page/slug          slug}
                            (when selected-sku
                              {:query-params {:SKU (:catalog/sku-id selected-sku)}})))
         (do
           (when-let [album-keyword (some->> (conj keypaths/v2-products
                                                   product-id
                                                   :legacy/named-search-slug)
                                             (get-in app-state)
                                             keyword)]
             (effects/fetch-cms-keypath app-state [:ugc-collection album-keyword]))
           (fetch-product-details app-state product-id))))))

#?(:cljs
   (defmethod effects/perform-effects events/navigate-product-details
     [_ event args _ app-state]
     (messages/handle-message events/initialize-product-details (assoc args :origin-nav-event event))))

#?(:cljs
   (defmethod trackings/perform-track events/navigate-product-details
     [_ event {:keys [catalog/product-id]} app-state]
     (when (-> product-id
               ((get-in app-state keypaths/v2-products))
               accessors.products/wig-product?)
       (facebook-analytics/track-event "wig_content_fired"))))

;; When a sku for combination is not found return nil sku -> 'Unavailable'
;; When no sku id is given in the query parameters we must find and use an epitome

(defmethod transitions/transition-state events/api-success-v3-products-for-details
  ;; for pre-selecting skus by url
  [_ event {:keys [skus]} app-state]
  (let [product      (products/current-product app-state)
        skus         skus
        sku-id       (determine-sku-id app-state product)
        sku          (get skus sku-id)
        product-skus (products/extract-product-skus app-state product)
        options      (generate-product-options app-state)]
    (-> app-state
        (assoc-selections sku)
        (assoc-in catalog.keypaths/detailed-product-product-skus product-skus)
        (assoc-in catalog.keypaths/detailed-product-selected-sku sku)
        (assoc-in catalog.keypaths/detailed-product-options options))))

#?(:cljs
   (defmethod effects/perform-effects events/api-success-v3-products-for-details
     [_ event _ _ app-state]
     ;; "Setting seo tags for the product detail page relies on the product
     ;; being available"
     (seo/set-tags app-state)))

(defmethod effects/perform-effects events/control-add-sku-to-bag
  [dispatch event {:keys [sku quantity] :as args} _ app-state]
  #?(:cljs
     (let [nav-event (get-in app-state keypaths/navigation-event)]
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
           (when (not= events/navigate-cart nav-event)
             (history/enqueue-navigate events/navigate-cart))
           (messages/handle-later events/api-success-add-sku-to-bag
                                  {:order    %
                                   :quantity quantity
                                   :sku      sku}))))))

(defmethod transitions/transition-state events/api-success-add-sku-to-bag
  [_ event {:keys [quantity sku]} app-state]
  (-> app-state
      (update-in keypaths/browse-recently-added-skus conj {:quantity quantity :sku sku})
      (assoc-in keypaths/browse-sku-quantity 1)))
