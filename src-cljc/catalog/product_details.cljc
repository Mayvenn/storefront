(ns catalog.product-details
  (:require #?@(:cljs [[goog.dom]
                       [goog.events]
                       [goog.style]
                       [storefront.accessors.auth :as auth]
                       [storefront.api :as api]
                       [storefront.hooks.seo :as seo]
                       [storefront.browser.scroll :as scroll]
                       [storefront.history :as history]
                       [storefront.hooks.facebook-analytics :as facebook-analytics]
                       [storefront.hooks.reviews :as review-hooks]
                       [storefront.trackings :as trackings]])
            [api.catalog :refer [select ?addons ?discountable]]
            api.current
            api.orders
            api.products
            [catalog.facets :as facets]
            [catalog.keypaths :as catalog.keypaths]
            [catalog.product-details-ugc :as ugc]
            [catalog.products :as products]
            [catalog.selector.sku :as sku-selector]
            [catalog.ui.add-to-cart :as add-to-cart]
            [catalog.ui.molecules :as catalog.M]
            [checkout.cart.swap :as swap]
            [homepage.ui.faq :as faq]
            mayvenn.live-help.core
            [mayvenn.visual.lib.call-out-box :as call-out-box]
            [mayvenn.visual.tools :refer [with]]
            [mayvenn.visual.ui.titles :as titles]
            [spice.selector :as selector]
            [storefront.accessors.contentful :as contentful]
            [storefront.accessors.images :as images]
            [storefront.accessors.products :as accessors.products]
            [storefront.accessors.sites :as sites]
            [storefront.accessors.skus :as skus]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.money-formatters :as mf]
            [storefront.components.picker.picker :as picker]
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
            [storefront.transitions :as transitions]
            storefront.ugc
            [storefront.accessors.experiments :as experiments]
            [mayvenn.live-help.core :as live-help]))

(defn page [wide-left wide-right-and-narrow]
  [:div.clearfix.mxn2
   [:div.col-on-tb-dt.col-7-on-tb-dt.px2
    [:div.hide-on-mb wide-left]]
   [:div.col-on-tb-dt.col-5-on-tb-dt.px2 wide-right-and-narrow]])

(defn full-bleed-narrow [body]
  [:div.hide-on-tb-dt body])

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
           browse-stylists-banner
           live-help] :as data} owner opts]
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
            [:div.my5 (component/build call-out-box/variation-1 browse-stylists-banner)]
            (component/build ugc/component ugc opts)])
          (component/html
           [:div
            [:div
             (full-bleed-narrow
              [:div (carousel carousel-images product)])]
            (component/build product-summary-organism data)
            [:div.px2
             (component/build picker/component picker-data opts)]

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
             (when live-help (component/build live-help/banner live-help))
             [:div.mt3 (component/build call-out-box/variation-1 browse-stylists-banner)]
             [:div.mxn2.mb3 (component/build ugc/component ugc opts)]]]))]]

       (when (seq reviews)
         [:div.container.col-7-on-tb-dt.px2
          (component/build review-component/reviews-component reviews opts)])
       (when faq-section
         [:div.container
          (component/build faq/organism faq-section opts)])])))


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
                                     :quantity (get-in app-state keypaths/browse-sku-quantity 1)}]
      :cta/spinning?               (utils/requesting? app-state (conj request-keys/add-to-bag (:catalog/sku-id selected-sku)))
      :cta/disabled?               (not (:inventory/in-stock? selected-sku))
      :add-to-cart.quadpay/price   sku-price
      :add-to-cart.quadpay/loaded? quadpay-loaded?}
     (when shop?
       (mayvenn-install-incentive-families sku-family)
       {:add-to-cart.incentive-block/id          "add-to-cart-incentive-block"
        :add-to-cart.incentive-block/footnote    "*Mayvenn Services cannot be combined with other promotions"
        :add-to-cart.incentive-block/link-id     "learn-more-mayvenn-install"
        :add-to-cart.incentive-block/link-label  "Learn more"
        :add-to-cart.incentive-block/link-target [events/popup-show-consolidated-cart-free-install]
        :add-to-cart.incentive-block/callout     "✋Don't miss out on free Mayvenn Service"
        :add-to-cart.incentive-block/message     (str "Get a free Mayvenn Service by a licensed "
                                                      "stylist with qualifying purchases.* ")}))))

(defn query [data selected-sku]
  (let [selections (get-in data catalog.keypaths/detailed-product-selections)
        product    (products/current-product data)

        product-skus               (products/extract-product-skus data product)
        images-catalog             (get-in data keypaths/v2-images)
        facets                     (facets/by-slug data)
        carousel-images            (find-carousel-images product product-skus images-catalog
                                                         (select-keys selections [:hair/color])
                                                         selected-sku)
        options                    (get-in data catalog.keypaths/detailed-product-options)
        ugc                        (ugc-query product selected-sku data)
        sku-price                  (or (:product/essential-price selected-sku)
                                       (:sku/price selected-sku))
        review-data                (review-component/query data)
        shop?                      (or (= "shop" (get-in data keypaths/store-slug))
                                       (= "retail-location" (get-in data keypaths/store-experience)))
        hair?                      (accessors.products/hair? product)
        wig?                       (accessors.products/wig-product? product)
        tape-in-or-seamless-clips? (some #{"seamless-clip-ins" "tape-ins"} (:hair/family product))
        faq                        (when-let [pdp-faq-id (accessors.products/product->faq-id product)]
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
      :selected-picker                    (get-in data catalog.keypaths/detailed-product-selected-picker)
      :picker-data                        (picker/query data)
      :faq-section                        (when (and shop? faq)
                                            (let [{:keys [question-answers]} faq]
                                              {:faq/expanded-index (get-in data keypaths/faq-expanded-section)
                                               :list/sections      (for [{:keys [question answer]} question-answers]
                                                                     {:faq/title   (:text question)
                                                                      :faq/content answer})}))
      :carousel-images                    carousel-images}

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
                                        {:title    "Description"
                                         :id       :description
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
                                        {:title    "Care"
                                         :id       :care
                                         :active?  (= active-tab-name :care)
                                         :icon     {:opts {:height "20px"
                                                           :width  "20px"}
                                                    :id   "heart"}
                                         :sections (keep (fn [[heading content-key]]
                                                           (when-let [content (get product content-key)]
                                                             {:heading heading
                                                              :content content}))
                                                         [["Maintenance Level" :copy/maintenance-level]
                                                          ["Can it be Dyed?" :copy/dyeable?]])}]})
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
                                                        events/navigate-content-our-hair)}))

     (when (and shop?
                (not (or
                      tape-in-or-seamless-clips?
                      wig?)))
       {:browse-stylists-banner {:title/primary   "Buy 3 items and we'll pay for your hair install"
                                 :title/secondary "Choose any Mayvenn stylist in your area"
                                 :action/label    "browse stylists"
                                 :action/target   [events/navigate-adventure-find-your-stylist]
                                 :class           "bg-pale-purple"
                                 :action/id       "browse-stylists-banner-cta"}}))))

(defn ^:export built-component
  [state opts]
  (let [selected-sku (get-in state catalog.keypaths/detailed-product-selected-sku)
        live-help?   (experiments/live-help? state)]
    (component/build component
                     (merge (query state selected-sku)
                            {:add-to-cart (add-to-cart-query state
                                                             selected-sku)
                             :live-help   (when live-help?
                                            {:live-help/location "product-detail-page-banner"})})
                     opts)))


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

     (if-let [current-product (products/current-product app-state)]
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
                   (let [{:catalog/keys [department]
                          :product?essential-service/keys [electives]}
                         (api.products/product<- app-state product)]
                     (when (= #{"service"} department)
                       electives))
                   (get-in app-state catalog.keypaths/detailed-product-selections)
                   (reduce-kv #(assoc %1 %2 (first %3))
                              {}
                              (select-keys sku (:selector/electives product))))))))

(defmethod transitions/transition-state events/initialize-product-details
  [_ event {:as args :keys [catalog/product-id query-params]} app-state]
  (let [ugc-offset (:offset query-params)
        sku        (or (->> (:SKU query-params)
                            (conj keypaths/v2-skus)
                            (get-in app-state))
                       (get-in app-state catalog.keypaths/detailed-product-selected-sku))
        options    (generate-product-options app-state)]
    (-> app-state
        (assoc-in catalog.keypaths/detailed-product-id product-id)
        (assoc-in catalog.keypaths/detailed-product-selected-sku sku)
        (assoc-in keypaths/ui-ugc-category-popup-offset ugc-offset)
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
        params-with-sku-id   (cond-> (select-keys (products/current-product app-state)
                                                  [:catalog/product-id :page/slug])
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
           (when-let [album-keyword (storefront.ugc/product->album-keyword shop? product)]
             (effects/fetch-cms-keypath app-state [:ugc-collection album-keyword]))
           (when-let [pdp-faq-id (accessors.products/product->faq-id product)]
             (effects/fetch-cms-keypath app-state [:faq pdp-faq-id]))
           (fetch-product-details app-state product-id))))))

#?(:cljs
   (defmethod effects/perform-effects events/navigate-product-details
     [_ event args _ state]
     (messages/handle-message events/initialize-product-details
                              (assoc args :origin-nav-event event))))

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
  [_ event {:keys [quantity sku]} app-state] ;; TODO: why does this have a quantity always set as 1?
  (assoc-in app-state keypaths/browse-sku-quantity 1))
