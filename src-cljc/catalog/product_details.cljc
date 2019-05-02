(ns catalog.product-details
  (:require [clojure.set :as set]
            [clojure.string :as string]
            [catalog.products :as products]
            [catalog.skuers :as skuers]
            [catalog.keypaths]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.accessors.promos :as promos]
            [storefront.accessors.skus :as skus]
            [catalog.facets :as facets]
            [storefront.accessors.orders :as orders]
            [storefront.components.money-formatters :refer [as-money-without-cents as-money]]
            [storefront.components.picker.picker :as picker]
            [storefront.components.ui :as ui]
            [spice.maps :as maps]
            [spice.core :as spice]
            [spice.selector :as selector]
            [storefront.config :as config]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.messages :as messages]
            [storefront.platform.reviews :as review-component]
            [catalog.selector.sku :as sku-selector]
            [catalog.product-details-ugc :as ugc]
            [storefront.request-keys :as request-keys]
            [storefront.transitions :as transitions]
            [storefront.platform.component-utils :as utils]
            [storefront.component :as component]
            #?@(:cljs [[storefront.hooks.pixlee :as pixlee-hooks]
                       [storefront.hooks.reviews :as review-hooks]
                       [storefront.api :as api]
                       [storefront.history :as history]
                       [storefront.hooks.quadpay :as quadpay]
                       [storefront.browser.scroll :as scroll]
                       [goog.dom]
                       [goog.events.EventType :as EventType]
                       [goog.events]
                       [goog.style]
                       [om.core :as om]])
            [spice.date :as date]
            [storefront.components.svg :as svg]
            [storefront.components.v2 :as v2]
            [storefront.components.marquee :as marquee]))

(defn item-price [price]
  (when price
    [:span {:item-prop "price"} (as-money-without-cents price)]))

(defn page [wide-left wide-right-and-narrow]
  [:div.clearfix.mxn2
   {:item-scope :itemscope :item-type "http://schema.org/Product"}
   [:div.col-on-tb-dt.col-7-on-tb-dt.px2
    [:div.hide-on-mb wide-left]]
   [:div.col-on-tb-dt.col-5-on-tb-dt.px2 wide-right-and-narrow]])

(defn full-bleed-narrow [body]
  ;; The mxn2 pairs with the p2 of the container, to make the body full width
  ;; on mobile.
  [:div.hide-on-tb-dt.mxn2.mt2 body])

(defn quantity-and-price-structure [quantity price]
  [:div
   [:div.right-align.dark-gray.h6 "PRICE"]
   [:div.flex.h2 {:style {:min-height "1.5em"}} ; prevent slight changes to size depending on content of counter
    [:div.flex-auto quantity]
    [:div.navy price]]])

(defn counter-or-out-of-stock [can-supply? quantity]
  (if can-supply?
    [:div
     [:link {:item-prop "availability" :href "http://schema.org/InStock"}]
     (ui/counter {:spinning? false
                  :data-test "pdp"}
                 quantity
                 (utils/send-event-callback events/control-counter-dec
                                            {:path keypaths/browse-sku-quantity})
                 (utils/send-event-callback events/control-counter-inc
                                            {:path keypaths/browse-sku-quantity}))]
    [:span.h4 "Currently out of stock"]))

(def sold-out-button
  (ui/navy-button {:on-click       utils/noop-callback
                   :data-test      "sold-out"
                   :disabled?      true
                   :disabled-class "bg-gray"}
                  "Sold Out"))

(def unavailable-button
  (ui/navy-button {:on-click       utils/noop-callback
                   :data-test      "unavailable"
                   :disabled?      true
                   :disabled-class "bg-gray"}
                  "Unavailable"))

(defn add-to-bag-button
  [adding-to-bag? sku quantity]
  (ui/teal-button {:on-click
                   (utils/send-event-callback events/control-add-sku-to-bag
                                              {:sku sku
                                               :quantity quantity})
                   :data-test "add-to-bag"
                   :disabled? (not (:inventory/in-stock? sku))
                   :spinning? adding-to-bag?}
                  "Add to bag"))

;; TODO(jeff, heather): this should be refactored to a sticky-component to be shared with PDP
(defn sticky-add-component
  [{:keys [selected-options sold-out? unavailable? adding-to-bag? sku quantity image]} owner opts]
  (let [unpurchasable? (or sold-out? unavailable?)
        text-style     (if unpurchasable? {:class "gray"} {})]
    #?(:clj (component/create [:div])
       :cljs
       (letfn [(handle-scroll [e] (om/set-state! owner :show? (< 866 (.-y (goog.dom/getDocumentScroll)))))
               (set-height [] (om/set-state! owner :add-button-height (some-> owner
                                                                              (om/get-node "add-button")
                                                                              goog.style/getSize
                                                                              .-height)))]
         (reify
           om/IInitState
           (init-state [this]
             ;; Treat show? as a trinary state:
             ;; nil = we need to hide the element, but still be available for height calculations
             ;; true = show the element (set margin-bottom to 0)
             ;; false = hide the element (set margin-bottom to computed height calculations)
             {:show? nil})
           om/IDidMount
           (did-mount [this]
             (set-height)
             (handle-scroll nil) ;; manually fire once on load incase the page already scrolled
             (goog.events/listen js/window EventType/SCROLL handle-scroll))
           om/IWillUnmount
           (will-unmount [this]
             (goog.events/unlisten js/window EventType/SCROLL handle-scroll))
           om/IWillReceiveProps
           (will-receive-props [this next-props]
             (set-height))
           om/IRenderState
           (render-state [this {:keys [show? add-button-height]}]
             (component/html
              [:div.fixed.z4.bottom-0.left-0.right-0.transition-2
               (cond
                 (nil? show?) {:style {:visibility "hidden"}}
                 show?        {:style {:margin-bottom "0"}}
                 :else        {:style {:margin-bottom (str "-" add-button-height "px")}})
               [:div {:ref "add-button"}
                [:div.p3.flex.justify-center.items-center.bg-white.border-top.border-light-gray
                 [:div.col-8
                  [:a.inherit-color
                   {:on-click #(scroll/scroll-selector-to-top "body")}
                   [:div.flex.items-center
                    [:img.border.border-gray.rounded-0
                     {:height "33px"
                      :width  "65px"
                      :src    (:option/rectangle-swatch (:hair/color selected-options))}]
                    [:span.ml2 "Length: " [:span text-style (:option/name (:hair/length selected-options))]]
                    [:span.ml2 "Qty: " [:span text-style quantity]]]]]
                 [:div.col-4
                  (ui/teal-button {:on-click
                                   (utils/send-event-callback events/control-add-sku-to-bag
                                                              {:sku      sku
                                                               :quantity quantity})
                                   :data-test      "add-to-bag"
                                   :disabled?      unpurchasable?
                                   :disabled-class "bg-gray"
                                   :spinning?      adding-to-bag?}
                                  (cond
                                    unavailable? "Unavailable"
                                    sold-out?    "Sold Out"
                                    :default     "Add"))]]]])))))))

(def checkout-button
  (component/html
    [:div
     {:data-test "cart-button"
      :data-ref "cart-button"}
     (ui/teal-button (utils/route-to events/navigate-cart) "Check out")]))

(defn- hacky-fix-of-bad-slugs-on-facets [slug]
  (string/replace (str slug) #"#" ""))

(defn summary-structure [desc quantity-and-price]
  [:div
   (when (seq desc)
     [:div
      [:h2.h3.light "Summary"]
      [:div.navy desc]])
   quantity-and-price])

(defn sku-summary [{:keys [sku sku-quantity]}]
  (let [{:keys [inventory/in-stock? sku/price]} sku]
    (summary-structure
      (some-> sku :sku/title string/upper-case)
      (quantity-and-price-structure
        (counter-or-out-of-stock in-stock? sku-quantity)
        (item-price price)))))

(def triple-bundle-upsell
  [:p.center.h6.flex.items-center.justify-center
   (svg/discount-tag {:class  "mxnp6"
                      :height "4em"
                      :width  "4em"})
   [:span.medium.shout "10% off "] [:span.bold.h5.mx1 "·"]
   "Buy 3 bundles or more"])

(def shipping-and-guarantee
  (component/html
    [:div.border-top.border-bottom.border-gray.p2.my2.center.navy.shout.medium.h6
     "Free shipping & 30 day guarantee"]))

(defn product-description
  [{:keys [copy/description copy/colors copy/weights copy/materials copy/summary hair/family] :as product}]
  (when (seq description)
    [:div.border.border-dark-gray.mt7.p2.rounded
     [:h2.h3.medium.navy.shout "Description"]
     [:div {:item-prop "description"}
      (when (or colors weights materials)
        (let [attrs (->> [["Color" colors]
                          ["Weight" weights]
                          ["Material" materials]]
                         (filter second))
              ;;This won't work if we have 5 possible attrs
              size (str "col-" (/ 12 (count attrs)))]
          (into [:div.clearfix.mxn1.my2]
                (for [[title value] attrs]
                  [:dl.col.m0.inline-block {:class size}
                   [:dt.mx1.dark-gray.shout.h6 title]
                   [:dd.mx1.ml0.h5.navy.medium value]]))))
      (when (seq summary)
        [:div.my2
         [:h3.mbp3.h5 "Includes:"]
         [:ul.list-reset.navy.h5.medium
          (for [[idx item] (map-indexed vector summary)]
            [:li.mbp3 {:key (str "item-" idx)} item])]])
      [:div.h5.dark-gray
       (for [[idx item] (map-indexed vector description)]
         [:p.mt2 {:key (str "product-description-" idx)} item])
       (when (not (or (contains? family "seamless-clip-ins")
                      (contains? family "tape-ins")
                      (contains? (:stylist-exclusives/family product) "kits")))
         [:p [:a.teal.underline (utils/route-to events/navigate-content-our-hair)
              "Learn more about our hair."]])]]]))

(defn image-body [{:keys [filename url alt]}]
  (ui/aspect-ratio
    640 580
    [:img.col-12
     {:src (str url "-/format/auto/-/resize/640x/" filename)
      :alt alt}]))

(defn carousel [images {:keys [slug]}]
  (let [items (mapv (fn [image]
                      {:id   (str (hash (or (:large_url image)
                                            (:url image))))
                       :body (image-body image)})
                    images)]
    (component/build carousel/component
                     {:slides (map :body items)
                      :settings {:dots true}}
                     {:react-key (apply str "category-swiper-" slug (interpose "-" (map :id items)))})))

(defn reviews-summary [reviews opts]
  [:div.h6
   {:style {:min-height "18px"}}
   (component/build review-component/reviews-summary-dropdown-experiment-component reviews opts)])

(defn get-selected-options [selections options]
  (reduce
   (fn [acc selection]
     (assoc acc selection
            (first (filter #(= (selection selections) (:option/slug %))
                           (selection options)))))
   {}
   (keys selections)))

(defn component
  [{:keys [adding-to-bag?
           carousel-images
           product
           reviews
           selected-sku
           sku-quantity
           selected-options
           get-a-free-install-section-data
           selections
           options
           picker-data
           loaded-quadpay?
           aladdin-or-phoenix?
           ugc] :as data} owner opts]
  (let [review?      (seq reviews)
        unavailable? (not (seq selected-sku))
        sold-out?    (not (:inventory/in-stock? selected-sku))]
    (component/create
      (if-not product
        [:div.flex.h2.p1.m1.items-center.justify-center
         {:style {:height "25em"}}
         (ui/large-spinner {:style {:height "4em"}})]
        [:div
         [:div.container
          (when (:offset ugc)
            [:div.absolute.overlay.z4.overflow-auto
             (component/build ugc/popup-component ugc opts)])
          [:div.p2
           (page
            [:div
             (carousel carousel-images product)
             [:div.hide-on-mb (component/build ugc/component ugc opts)]]
            [:div
             [:div
              [:div.mx2
               [:h1.h2.medium.titleize {:item-prop "name"}
                (:copy/title product)]
               (when review? (reviews-summary reviews opts))]
              [:meta {:item-prop "image"
                      :content   (:url (first carousel-images))}]
              (full-bleed-narrow (carousel carousel-images product))]
             [:div {:item-prop  "offers"
                    :item-scope ""
                    :item-type  "http://schema.org/Offer"}
              [:div.pb2 (component/build picker/component picker-data opts)]
              (when (products/eligible-for-triple-bundle-discount? product)
                [:div triple-bundle-upsell])
              [:div.center.mb6.pt4
               [:div.h6.navy "Price Per Item"]
               [:div.medium (item-price (:sku/price selected-sku))]]
              [:div
               [:div.mt1.mx3
                (cond
                  unavailable? unavailable-button
                  sold-out?    sold-out-button
                  :else        (add-to-bag-button adding-to-bag? selected-sku sku-quantity))]]
              #?(:cljs [:div.mbn4
                         (component/build quadpay/component
                                          {:show?       loaded-quadpay?
                                           :order-total (:sku/price selected-sku)
                                           :directive   [:div.flex.justify-center.items-center
                                                         "Just select"
                                                         [:div.mx1 {:style {:width "70px" :height "14px"}}
                                                          svg/quadpay-logo]
                                                         "at check out."]}
                                          nil)])
              (when (products/stylist-only? product)
                shipping-and-guarantee)]
             (product-description product)
             [:div.hide-on-tb-dt.mxn2.mb3 (component/build ugc/component ugc opts)]])]]
         (when aladdin-or-phoenix?
           [:div.py10.bg-transparent-teal.col-on-tb-dt.mt4
            (v2/get-a-free-install get-a-free-install-section-data)])
         (when review?
           [:div.container.col-7-on-tb-dt.px2
            (component/build review-component/reviews-component reviews opts)])
         (when (and (nil? (:offset ugc))
                    (not (products/stylist-only? product)))
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
                              :quantity         sku-quantity} {})])]))))

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
  (when-let [ugc (get-in data keypaths/ugc)]
    (when-let [images (pixlee/images-in-album ugc (keyword (:legacy/named-search-slug product)))]
      {:carousel-data {:product-id   (:catalog/product-id product)
                       :product-name (:copy/title product)
                       :page-slug    (:page/slug product)
                       :sku-id       (:catalog/sku-id sku)
                       :album        images}
       :show-cta?     true
       :offset        (get-in data keypaths/ui-ugc-category-popup-offset)
       :close-message [events/navigate-product-details
                       {:catalog/product-id (:catalog/product-id product)
                        :page/slug          (:page/slug product)
                        :query-params       {:SKU (:catalog/sku-id sku)}}]
       ;;TODO GROT:
       ;; This is to force UGC to re-render after Slick's initial render
       ;; Slick has a bug when before 485px width where it shows a sliver
       ;; of the next image on the right.
       ;; This ugly terrible hack gets it to re-evaluate its width
       ;; The correct solution is to get rid of/fix slick
       :now           (date/now)})))

(defn find-carousel-images [product product-skus selected-sku]
  (->> (selector/match-all {}
                           (or selected-sku (first product-skus))
                           (:selector/images product))
       (selector/match-all {:selector/strict? true}
                           {:use-case #{"carousel"}
                            :image/of #{"model" "product"}})
       (sort-by :order)))

(defn default-selections
  "Using map of current selections (which can be empty)
  for un-selected values picks first option from the available options.
  e.g.: if there's no `hair/color` in `selections` map - it sets it to whatever the first in the list, e.g.: \"black\""
  [facets product product-skus]
  (let [options (sku-selector/product-options facets product product-skus)]
    (reduce
     (fn [a k]
       (if (get a k)
         a
         (assoc a k (-> options k first :option/slug))))
     {}
     (keys options))))

(defn query [data]
  (let [selected-sku      (get-in data catalog.keypaths/detailed-product-selected-sku)
        selections        (get-in data catalog.keypaths/detailed-product-selections)
        product           (products/current-product data)
        product-skus      (products/extract-product-skus data product)
        facets            (facets/by-slug data)
        carousel-images   (find-carousel-images product product-skus selected-sku)
        options           (get-in data catalog.keypaths/detailed-product-options)
        ugc               (ugc-query product selected-sku data)
        store             (marquee/query data)
        gallery-ucare-ids (->> store
                               :gallery
                               :images
                               (filter (comp (partial = "approved") :status))
                               (map (comp v2/get-ucare-id-from-url :resizable-url)))]
    {:reviews                         (review-component/query data)
     :ugc                             ugc
     :aladdin-or-phoenix?             (experiments/v2-experience? data)
     :fetching-product?               (utils/requesting? data (conj request-keys/search-v2-products
                                                                    (:catalog/product-id product)))
     :adding-to-bag?                  (utils/requesting? data (conj request-keys/add-to-bag (:catalog/sku-id selected-sku)))
     :sku-quantity                    (get-in data keypaths/browse-sku-quantity 1)
     :options                         options
     :product                         product
     :selections                      selections
     :selected-options                (get-selected-options selections options)
     :selected-sku                    selected-sku
     :facets                          facets
     :selected-picker                 (get-in data catalog.keypaths/detailed-product-selected-picker)
     :picker-data                     (picker/query data)
     :carousel-images                 carousel-images
     :get-a-free-install-section-data {:store                 store
                                       :gallery-ucare-ids     gallery-ucare-ids
                                       :stylist-portrait      (:portrait store)
                                       :stylist-name          (:store-nickname store)
                                       :stylist-gallery-open? (get-in data keypaths/carousel-stylist-gallery-open?)}
     :loaded-quadpay?            (get-in data keypaths/loaded-quadpay)}))

(defn built-component [data opts]
  (component/build component (query data) opts))

(defn fetch-product-album
  [{:keys [legacy/named-search-slug]}]
  (let [named-search-kw (keyword named-search-slug)
        album-id        (get-in config/pixlee [:albums named-search-kw])]
    (when album-id
      #?(:cljs (pixlee-hooks/fetch-album album-id named-search-kw)
         :clj nil))))

(defn get-valid-product-skus [product all-skus]
  (->> product
       :selector/skus
       (select-keys all-skus)
       vals))

(defn determine-sku-id
  ([app-state product]
   (determine-sku-id app-state product nil))
  ([app-state product new-sku-id]
   (let [prev-sku-id        (get-in app-state catalog.keypaths/detailed-product-selected-sku-id)
         valid-product-skus (get-valid-product-skus product (get-in app-state keypaths/v2-skus))
         facets             (get-in app-state storefront.keypaths/v2-facets)
         epitome            (skus/determine-epitome (facets/color-order-map facets)
                                                    valid-product-skus)
         valid-sku-ids      (set (map :catalog/sku-id valid-product-skus))]
     (or (when (seq new-sku-id)
           (valid-sku-ids new-sku-id))
         (valid-sku-ids prev-sku-id)
         (:catalog/sku-id epitome)))))

(defn url-points-to-invalid-sku? [selected-sku query-params]
  (and (:catalog/sku-id selected-sku)
       (not= (:catalog/sku-id selected-sku)
             (:SKU query-params))))

#?(:cljs
   (defn fetch-product-details [app-state product-id]
     (api/search-v2-products (get-in app-state keypaths/api-cache)
                             {:catalog/product-id product-id}
                             (fn [response]
                               (messages/handle-message events/api-success-v2-products-for-details response)
                               (messages/handle-message events/viewed-sku {:sku (get-in app-state catalog.keypaths/detailed-product-selected-sku)})))

     (if-let [current-product (products/current-product app-state)]
       (if (auth/permitted-product? app-state current-product)
         (do
           (fetch-product-album current-product)
           (review-hooks/insert-reviews))
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
        product-skus (products/extract-product-skus app-state product)]
    (sku-selector/product-options facets
                                  product
                                  product-skus)))

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
        product-skus (products/extract-product-skus app-state product)]
    (-> app-state
        (assoc-in catalog.keypaths/detailed-product-selections
                  (merge
                   (default-selections facets product product-skus)
                   (get-in app-state catalog.keypaths/detailed-product-selections)
                   (reduce-kv #(assoc %1 %2 (first %3))
                              {}
                              (select-keys sku (:selector/electives product))))))))

;; TODO change effect handler to check for this:
;; (and sku-id (contains? (set skus) sku-id))
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
        (assoc-selections sku)
        (assoc-in catalog.keypaths/detailed-product-options options))))

(defmethod transitions/transition-state events/control-product-detail-picker-option-select
  [_ event {:keys [selection value]} app-state]
  (let [selected-sku (->> {selection #{value}}
                          (determine-sku-from-selections app-state))
        options      (generate-product-options app-state)]
    (-> app-state
        (assoc-in catalog.keypaths/detailed-product-selected-sku
                  selected-sku)
        (update-in catalog.keypaths/detailed-product-selections
                   merge {selection value})
        (assoc-in catalog.keypaths/detailed-product-options options))))

(defmethod effects/perform-effects events/control-product-detail-picker-option-select
  [_ event {:keys [selection value navigation-event]} _ app-state]
  (let [sku-id-for-selection (-> app-state
                                 (get-in catalog.keypaths/detailed-product-selected-sku)
                                  :catalog/sku-id)
        params-with-sku-id   (-> app-state
                                 products/current-product
                                 (select-keys [:catalog/product-id :page/slug])
                                 (assoc :query-params {:SKU sku-id-for-selection}))]
    (effects/redirect navigation-event params-with-sku-id)
    #?(:cljs (scroll/enable-body-scrolling))))

(defmethod transitions/transition-state events/control-product-detail-picker-open
  [_ event {:keys [facet-slug]} app-state]
  (assoc-in app-state catalog.keypaths/detailed-product-selected-picker facet-slug))

(defmethod transitions/transition-state events/control-product-detail-picker-option-quantity-select
  [_ event {:keys [value]} app-state]
  (-> app-state
      (assoc-in keypaths/browse-sku-quantity value)
      (assoc-in catalog.keypaths/detailed-product-selected-picker nil)))

(defmethod transitions/transition-state events/control-product-detail-picker-close
  [_ event _ app-state]
  (assoc-in app-state catalog.keypaths/detailed-product-selected-picker nil))

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
                           {:catalog/product-id product-id
                            :page/slug          slug
                            :query-params       {:SKU (:catalog/sku-id selected-sku)}})
         (fetch-product-details app-state product-id)))))

#?(:cljs
   (defmethod effects/perform-effects events/navigate-product-details
     [_ event args _ app-state]
     (messages/handle-message events/initialize-product-details (assoc args :origin-nav-event event))))

(defmethod effects/perform-effects events/api-success-v2-products-for-details
  [_ _ _ _ app-state]
  (fetch-product-album (products/current-product app-state)))

(defmethod transitions/transition-state events/api-success-v2-products-for-details
  ;; for pre-selecting skus by url
  [_ event {:keys [skus]} app-state]
  (let [product      (products/current-product app-state)
        skus         (products/index-skus skus)
        sku-id       (determine-sku-id app-state product)
        sku          (get skus sku-id)
        product-skus (products/extract-product-skus app-state product)
        options      (generate-product-options app-state)]
    (-> app-state
        (assoc-in catalog.keypaths/detailed-product-product-skus product-skus)
        (assoc-in catalog.keypaths/detailed-product-selected-sku sku)
        (assoc-in catalog.keypaths/detailed-product-options options))))

(defmethod effects/perform-effects events/control-add-sku-to-bag
  [dispatch event {:keys [sku quantity] :as args} _ app-state]
  #?(:cljs
     (let [nav-event (get-in app-state keypaths/navigation-event)]
       (api/add-sku-to-bag
        (get-in app-state keypaths/session-id)
        {:sku        sku
         :quantity   quantity
         :stylist-id (get-in app-state keypaths/store-stylist-id)
         :token      (get-in app-state keypaths/order-token)
         :number     (get-in app-state keypaths/order-number)
         :user-id    (get-in app-state keypaths/user-id)
         :user-token (get-in app-state keypaths/user-token)}
        #(do
           (when (not= events/navigate-cart nav-event)
             (history/enqueue-navigate events/navigate-cart))
           (messages/handle-later events/api-success-add-sku-to-bag
                                  {:order    %
                                   :quantity quantity
                                   :sku      sku}))))))

(defmethod transitions/transition-state events/api-success-add-sku-to-bag
  [_ event {:keys [order quantity sku]} app-state]
  (let [previous-order (get-in app-state keypaths/order)]
    (-> app-state
        (update-in keypaths/browse-recently-added-skus
                   conj
                   {:quantity quantity :sku sku})
        (assoc-in keypaths/browse-sku-quantity 1))))
