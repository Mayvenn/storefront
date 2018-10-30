(ns catalog.product-details-dropdown
  (:require #?@(:cljs [[storefront.hooks.pixlee :as pixlee-hooks]
                       [storefront.hooks.reviews :as review-hooks]
                       [storefront.api :as api]
                       [storefront.history :as history]
                       [goog.dom]
                       [goog.events.EventType :as EventType]
                       [goog.events]
                       [goog.style]
                       [om.core :as om]])
            [catalog.facets :as facets]
            [lambdaisland.uri :as uri]
            [catalog.keypaths]
            [catalog.product-details-ugc :as ugc]
            [storefront.components.picker.picker :as picker]
            [catalog.products :as products]
            [catalog.skuers :as skuers]
            [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [spice.core :as spice]
            [spice.date :as date]
            [spice.maps :as maps]
            [spice.selector :as selector]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.accessors.promos :as promos]
            [storefront.accessors.skus :as skus]
            [storefront.component :as component]
            [storefront.components.affirm :as affirm]
            [storefront.components.money-formatters :refer [as-money-without-cents as-money]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.config :as config]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.platform.reviews :as review-component]
            [storefront.request-keys :as request-keys]
            [storefront.transitions :as transitions]))

(s/def ::keyword
  (s/conformer
   (fn [value]
     (cond (string? value)  (keyword value)
           (keyword? value) value
           :otherwise       ::s/invalid))))

(s/def ::ucare-url
  (s/conformer
   (s/nilable
    (fn [value]
      (or
       (and value
            (= "ucarecdn.com"
               (:host (uri/parse value))))
       ::s/invalid)))))

(s/def :option/name string?)
(s/def :option/slug string?)
(s/def :option/order int?)
(s/def :option/sku-swatch ::ucare-url)
(s/def :option/rectangular-swatch ::ucare-url)

(s/def ::stocked? boolean?)
(s/def ::image ::ucare-url)
(s/def ::price number?)
(s/def ::price-delta double?)
(s/def ::checked? boolean?)

(s/def ::selector-option
  (s/keys
   :req [:option/slug
         :option/name]
   :opt [:option/rectangular-swatch
         :option/sku-swatch
         :option/order]
   :req-un [::image
            ::price
            ::checked?]))

(defn conform! [spec value]
  (let [result (s/conform spec value)]
    (if (= ::s/invalid result)
      (do
        #?(:cljs (js/console.log (s/explain-str spec value)))
        (throw (ex-info "Failing spec!" {:explaination (s/explain-str spec value)
                                         :value value
                                         :spec spec})))
      result)))

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
  (ui/navy-button {:on-click  utils/noop-callback
                   :data-test "sold-out"
                   :class     "bg-gray"}
                  "Sold Out"))

(def unavailable-button
  (ui/navy-button {:on-click  utils/noop-callback
                   :data-test "unavailable"
                   :class     "bg-gray"}
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

(defn sticky-add-component
  [{:keys [purchasable? adding-to-bag? sku quantity image]} owner opts]
  #?(:clj (component/create [:div])
     :cljs
     (letfn [(handle-scroll [e] (om/set-state! owner :show? (and purchasable?
                                                                 (< 866 (.-y (goog.dom/getDocumentScroll))))))
             (set-height [] (om/set-state! owner :add-button-height (some-> owner
                                                                            (om/get-node "add-button")
                                                                            goog.style/getSize
                                                                            .-height)))]
       (reify
         om/IInitState
         (init-state [this]
           {:show? false})
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
             (if show?
               {:style {:margin-bottom "0"}}
               {:style {:margin-bottom (str "-" add-button-height "px")}})
             [:div {:ref "add-button"}
              [:div.p3.flex.justify-center.items-center.bg-white.border-top.border-light-gray
               [:div.col-8
                [:div.flex.items-center
                 [:img.border.border-gray.rounded-0
                  {:height "33px"
                   :width  "65px"
                   :src    image}]
                 [:span.ml2 "Length: " (:hair/length sku) "″"]
                 [:span.ml2 "Qty: " quantity]]]
               [:div.col-4
                (ui/teal-button {:on-click
                                 (utils/send-event-callback events/control-add-sku-to-bag
                                                            {:sku      sku
                                                             :quantity quantity})
                                 :data-test "add-to-bag"
                                 :disabled? (not (:inventory/in-stock? sku))
                                 :spinning? adding-to-bag?}
                                "Add")]]]]))))))

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

(defn component
  [{:keys [adding-to-bag?
           carousel-images
           product
           reviews
           selected-sku
           sku-quantity
           options
           picker-data
           ugc] :as data}
   owner
   opts]
  (let [review?      (:review? reviews)
        unavailable? (not (seq selected-sku))
        sold-out?    (not (:inventory/in-stock? selected-sku))]
    (component/create
      (if-not product
        [:div.flex.h2.p1.m1.items-center.justify-center
         {:style {:height "25em"}}
         (ui/large-spinner {:style {:height "4em"}})]
        [:div
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
            [:div
             [:div {:item-prop  "offers"
                    :item-scope ""
                    :item-type  "http://schema.org/Offer"}
              (component/build picker/component picker-data opts)
              (when (products/eligible-for-triple-bundle-discount? product)
                [:div.pt2.pb4 triple-bundle-upsell])
              [:div.center.mb6
               [:div.h6.navy "Price Per Bundle"]
               [:div.medium (item-price (:sku/price selected-sku))]]
              (affirm/pdp-dropdown-experiment-as-low-as-box
               {:amount      (:sku/price selected-sku)
                :middle-copy "Just select Affirm at check out."})
              [:div
               [:div.mt1.mx3
                (cond
                  unavailable? unavailable-button
                  sold-out?    sold-out-button
                  :else        (add-to-bag-button adding-to-bag? selected-sku sku-quantity))]]
              (when (products/stylist-only? product) shipping-and-guarantee)]]
            (product-description product)
            [:div.hide-on-tb-dt.mxn2.mb3 (component/build ugc/component ugc opts)]])
          (when review?
            (component/build review-component/reviews-component reviews opts))
          [:div.hide-on-tb-dt
           (component/build sticky-add-component
                            {:image          (->> options
                                                  :hair/color
                                                  (filter :checked?)
                                                  first
                                                  :option/rectangle-swatch)
                             :adding-to-bag? adding-to-bag?
                             :sku            selected-sku
                             :purchasable?   (and
                                              (seq selected-sku)
                                              (not sold-out?))
                             :quantity       sku-quantity} {})]]]))))

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

(defn find-swatch-sku-image [sku]
  (first (selector/match-all {:selector/strict? true}
                           {:use-case #{"cart"}}
                           (:selector/images sku))))

(defn ugc-query [product sku data]
  (when-let [ugc (get-in data keypaths/ugc)]
    (when-let [images (pixlee/images-in-album ugc (keyword (:legacy/named-search-slug product)))]
      {:carousel-data {:product-id   (:catalog/product-id product)
                       :product-name (:copy/title product)
                       :page-slug    (:page/slug product)
                       :sku-id       (:catalog/sku-id sku)
                       :album        images}
       :offset        (get-in data keypaths/ui-ugc-category-popup-offset)
       :back          (first (get-in data keypaths/navigation-undo-stack))
       ;;TODO GROT:
       ;; This is to force UGC to re-render after Slick's initial render
       ;; Slick has a bug when before 485px width where it shows a sliver
       ;; of the next image on the right.
       ;; This ugly terrible hack gets it to re-evaluate its width
       ;; The correct solution is to get rid of/fix slick
       :now           (date/now)})))

(defn generate-options
  "given these args we need to generate a map of the form:

  {:facet-slug  [[#{option-slug}] #{option-slug} #{option-slug}]
   :attr2 [{... option .. }]}"
  [facets {:as product :keys [selector/electives]} product-skus selections]
  (reduce (fn [acc-options [facet-slug {:as   facet
                                        :keys [facet/options]}]]
            (->> product-skus
                 (group-by facet-slug)
                 (map (fn [[option-slug option-skus]]
                        (let [cheapest-sku (apply min-key :sku/price option-skus)
                              option       (merge (dissoc (get options (first option-slug)) :sku/name)
                                                  {:price             (:sku/price cheapest-sku)
                                                   :checked?          (= (get selections facet-slug ::missing-selection)
                                                                         (first option-slug))
                                                   :stocked?          (when (seq option-skus)
                                                                        (some :inventory/in-stock? option-skus))
                                                   :option/sku-swatch (:url (find-swatch-sku-image cheapest-sku))
                                                   :image             (get-in options [option-slug :option/image])})]
                          (conform! ::selector-option option))))
                 (sort-by :filter/order)
                 (assoc acc-options facet-slug)))
          {}
          (select-keys facets electives)))

(defn add-review-eligibility [review-data product]
  (assoc review-data :review? (products/eligible-for-reviews? product)))

(defn find-carousel-images [product product-skus selected-sku]
  (->> (selector/match-all {}
                           (or selected-sku (first product-skus))
                           (:selector/images product))
       (selector/match-all {:selector/strict? true}
                           {:use-case #{"carousel"}
                            :image/of #{"model" "product"}})
       (sort-by :order)))

(defn extract-product-skus [app-state product]
  (->> (select-keys (get-in app-state keypaths/v2-skus)
                    (:selector/skus product))
       vals
       (sort-by :sku/price)))

(defn default-selections
  "Using map of current selections (which can be empty)
  for un-selected values picks first option from the available options.
  e.g.: if there's no `hair/color` in `selections` map - it sets it to whatever the first in the list, e.g.: \"black\""
  [selections facets product product-skus]
  (let [options (generate-options facets product product-skus {})]
    (reduce
     (fn [a k]
       (if (get a k)
         a
         (assoc a k (-> options k first :option/slug))))
     selections
     (keys options))))

(defn query [data]
  (let [selected-sku    (get-in data catalog.keypaths/detailed-product-selected-sku)
        selections      (get-in data catalog.keypaths/detailed-product-selections)
        product         (products/current-product data)
        product-skus    (extract-product-skus data product)
        facets          (facets/by-slug data)
        carousel-images (find-carousel-images product product-skus selected-sku)
        ugc             (ugc-query product selected-sku data)]
    {:reviews           (add-review-eligibility (review-component/query data) product)
     :ugc               ugc
     :fetching-product? (utils/requesting? data (conj request-keys/search-v2-products
                                                      (:catalog/product-id product)))
     :adding-to-bag?    (utils/requesting? data (conj request-keys/add-to-bag (:catalog/sku-id selected-sku)))
     :sku-quantity      (get-in data keypaths/browse-sku-quantity 1)
     :options           (get-in data catalog.keypaths/detailed-product-options)
     :product           product
     :selections        selections
     :selected-sku      selected-sku
     :facets            facets
     :selected-picker   (get-in data catalog.keypaths/detailed-product-selected-picker)
     :picker-data       (picker/query data)
     :carousel-images   carousel-images}))

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
  (let [skus (->> product
                  :selector/skus
                  (select-keys all-skus)
                  vals)]
    (or (not-empty (filter :inventory/in-stock? skus)) skus)))

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

(defn determine-sku-from-selections [app-state]
  (let [product      (products/current-product app-state)
        skus         (get-in app-state catalog.keypaths/detailed-product-product-skus)
        selections   (get-in app-state catalog.keypaths/detailed-product-selections)
        selected-sku (selector/match-all {} selections skus)]
    (first-when-only selected-sku)))

(defn assoc-sku-from-selections [app-state]
  (assoc-in app-state
            catalog.keypaths/detailed-product-selected-sku
            (determine-sku-from-selections app-state)))

(defn assoc-detailed-product-options
  "Sets default selections for product electives `:hair/color`, `:hair/length`"
  [app-state]
  (let [product-id   (get-in app-state catalog.keypaths/detailed-product-id)
        product      (products/product-by-id app-state product-id)
        facets       (facets/by-slug app-state)
        product-skus (extract-product-skus app-state product)
        selections   (get-in app-state catalog.keypaths/detailed-product-selections)]
    (cond-> app-state
      (experiments/pdp-dropdown? app-state)
      (assoc-in catalog.keypaths/detailed-product-options (generate-options facets product product-skus selections)))))

(defmethod transitions/transition-state events/control-product-detail-picker-option-select
  [_ event {:keys [selection value]} app-state]
  (-> app-state
      (update-in catalog.keypaths/detailed-product-selections merge {selection value})
      (assoc-in catalog.keypaths/detailed-product-selected-picker nil)
      assoc-sku-from-selections
      assoc-detailed-product-options))

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
