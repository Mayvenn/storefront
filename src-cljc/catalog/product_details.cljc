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
            [storefront.accessors.facets :as facets]
            [storefront.components.money-formatters :refer [as-money-without-cents as-money]]
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
            [catalog.product-details-ugc :as ugc]
            [storefront.request-keys :as request-keys]
            [storefront.transitions :as transitions]
            [storefront.platform.component-utils :as utils]
            #?@(:clj [[storefront.component-shim :as component]]
                :cljs [[storefront.hooks.pixlee :as pixlee-hooks]
                       [storefront.component :as component]
                       [storefront.hooks.reviews :as review-hooks]
                       [storefront.api :as api]
                       [storefront.history :as history]])
            [storefront.components.affirm :as affirm]))

(defn item-price [price]
  (when price
    [:span {:item-prop "price"} (as-money-without-cents price)]))

(defn starting-at [cheapest-price]
  [:div.center.dark-gray
   [:div.h6 "Starting at"]
   [:div.h2 (item-price cheapest-price)]])

(defn page [wide-left wide-right-and-narrow]
  [:div.clearfix.mxn2
   {:item-scope :itemscope :item-type "http://schema.org/Product"}
   [:div.col-on-tb-dt.col-7-on-tb-dt.px2
    [:div.hide-on-mb wide-left]]
   [:div.col-on-tb-dt.col-5-on-tb-dt.px2 wide-right-and-narrow]])

(defn title [title]
  [:h1.h2.medium.titleize.navy {:item-prop "name"} title])

(defn full-bleed-narrow [body]
  ;; The mxn2 pairs with the p2 of the container, to make the body full width
  ;; on mobile.
  [:div.hide-on-tb-dt.mxn2.my2 body])

(def schema-org-offer-props
  {:item-prop "offers"
   :item-scope ""
   :item-type "http://schema.org/Offer"})

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

(defn add-to-bag-button [adding-to-bag? sku quantity]
  (ui/navy-button {:on-click
                   (utils/send-event-callback events/control-add-sku-to-bag
                                              {:sku sku
                                               :quantity quantity})
                   :data-test "add-to-bag"
                   :disabled? (nil? sku)
                   :spinning? adding-to-bag?}
                  "Add to bag"))

(defn display-bagged-sku [idx {:keys [quantity sku]}]
  [:div.h6.my1.p1.py2.caps.dark-gray.bg-light-gray.medium.center
   {:key (str "bagged-sku-" idx)
    :data-test "items-added"}
   "Added to bag: "
   (spice/number->word quantity)
   " "
   (:sku/title sku)])

(def checkout-button
  (component/html
   [:div
    {:data-test "cart-button"
     :data-ref "cart-button"}
    (ui/teal-button (utils/route-to events/navigate-cart) "Check out")]))

(defn bagged-skus-and-checkout [bagged-skus]
  (when (seq bagged-skus)
    [:div
     (map-indexed display-bagged-sku bagged-skus)
     checkout-button]))

(defn- hacky-fix-of-bad-slugs-on-facets [slug]
  (string/replace (str slug) #"#" ""))

(defn option-html
  [selector {:keys [option/name option/slug image price-delta checked? stocked?] :as thing}]
  [:label.btn.p1.flex.flex-column.justify-center.items-center.container-size.letter-spacing-0
   {:data-test (str "option-" (hacky-fix-of-bad-slugs-on-facets slug))
    :class     (cond
                 checked? "border-gray bg-teal  white     medium"
                 stocked? "border-gray bg-white dark-gray light"
                 :else    "border-gray bg-gray  dark-gray light")
    :style     {:font-size "14px" :line-height "18px"}}
   (ui/hidden-field {:type      "radio"
                     :keypath   events/control-bundle-option-select
                     :disabled? (not stocked?)
                     :checked?  checked?
                     :selection selector
                     :value     slug})
   (if image
     [:img.mbp4.content-box.circle.border-light-gray
      {:src   image :alt    name
       :width 30    :height 30
       :class (cond checked? "border" (not stocked?) "muted")}]
     [:span.block.titleize name])
   [:span.block
    (if stocked?
      [:span (when-not checked? {:class "navy"})
       "+" (as-money-without-cents price-delta)]
      "Sold Out")]])

(defn selector-html
  [{:keys [selector options]}]
  [:div.my2
   {:key (str "selector-" selector)}
   [:h2.h3.clearfix.h5
    [:span.block.left.navy.medium.shout
     (name selector)
     [:span.inline-block.mxp2.dark-gray " - "]]
    [:span.block.overflow-hidden.dark-gray.h5.regular
     (:option/name (first (filter :checked? options)))]]
   [:div.flex.flex-wrap.content-stretch.mxnp3
    (for [option (sort-by :option/order options)]
      [:div.flex.flex-column.justify-center.pp3.col-4
       {:key   (string/replace (str "option-" (hash option)) #"\W+" "-")
        :style {:height "72px"}}
       (option-html selector option)])]])

(defn indefinite-articalize [word]
  (let [vowel? (set "AEIOUaeiou")]
    (str (if (vowel? (first word)) "an " "a ")
         word)))

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
  (component/html [:p.center.h5.p2.navy promos/bundle-discount-description]))

(def shipping-and-guarantee
  (component/html
   [:div.border-top.border-bottom.border-gray.p2.my2.center.navy.shout.medium.h6
    "Free shipping & 30 day guarantee"]))

(defn product-description
  [{:keys [copy/description copy/colors copy/weights copy/materials copy/summary hair/family]}
   human-hair?]
  (when (seq description)
    [:div.border.border-dark-gray.mt2.p2.rounded
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
       (when (and human-hair?
                  (not (or (contains? family "seamless-clip-ins")
                           (contains? family "tape-ins"))))
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
   (component/build review-component/reviews-summary-component reviews opts)])

(defn component
  [{:keys [adding-to-bag?
           cheapest-price
           bagged-skus
           carousel-images
           human-hair?
           options
           product
           reviews
           selected-sku
           sku-quantity
           ugc]}
   owner
   opts]
  (let [review? (:review? reviews)]
    (component/create
     (if-not product
       [:div.flex.h2.p1.m1.items-center.justify-center
        {:style {:height "25em"}}
        (ui/large-spinner {:style {:height "4em"}})]
       [:div
        (when (:offset ugc)
          [:div.absolute.overlay.z4.overflow-auto
           (component/build ugc/popup-component ugc opts)])
        [:div.container.p2
         (page
          [:div
           (carousel carousel-images product)
           [:div.hide-on-mb (component/build ugc/component ugc opts)]]
          [:div
           [:div.center
            (title (:copy/title product))
            (when review? (reviews-summary reviews opts))
            [:meta {:item-prop "image"
                    :content   (:url (first carousel-images))}]
            (full-bleed-narrow (carousel carousel-images product))
            (starting-at cheapest-price)]
           [:div
            [:div schema-org-offer-props
             [:div.my2
              [:div
               (when (contains? (:catalog/department product) "hair")
                 (for [facet (:selector/electives product)]
                   (selector-html {:selector facet
                                   :options  (get options facet)})))]
              (sku-summary {:sku          selected-sku
                            :sku-quantity sku-quantity})]
             (when (products/eligible-for-triple-bundle-discount? product)
               triple-bundle-upsell)
             (affirm/as-low-as-box {:amount      (:sku/price selected-sku)
                                    :middle-copy "Just select Affirm at check out."})
             (add-to-bag-button adding-to-bag?
                                selected-sku
                                sku-quantity)
             (bagged-skus-and-checkout bagged-skus)
             (when (products/stylist-only? product) shipping-and-guarantee)]]
           (product-description product human-hair?)
           [:div.hide-on-tb-dt.mxn2.mb3 (component/build ugc/component ugc opts)]])
         (when review?
           (component/build review-component/reviews-component reviews opts))]]))))

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

(defn ^:private construct-option
  [option-kw facets sku-skuer cheapest-for-option-kw cheapest-price options-for-option-kw sku]
  (let [option-name  (first (option-kw sku))
        facet-option (get-in facets [option-kw :facet/options option-name])
        image        (:option/image facet-option)]
    (update options-for-option-kw option-name
            (fn [existing]
              {:option/name  (:option/name facet-option)
               :option/slug  (:option/slug facet-option)
               :option/order (:filter/order facet-option)
               :stocked?     (or (:inventory/in-stock? sku)
                                 (:stocked? existing false))
               :image        image
               :price        (:sku/price sku)
               :price-delta  (- (get cheapest-for-option-kw option-name) cheapest-price)
               :checked?     (= (option-kw sku-skuer)
                                (option-kw sku))}))))

(defn skuer->selectors [{:keys [selector/essentials selector/electives]}]
  (set/union (set essentials) (set electives)))

(defn determine-relevant-skus
  [skus selected-sku product product-options]
  (let [electives      (skuers/electives product selected-sku)
        step-choosable (set/difference (set (:selector/electives product))
                                       (set (keys product-options)))]
    (selector/match-all {}
                        (apply dissoc
                               electives
                               step-choosable)
                        skus)))

(defn skus->options
  "Reduces this product's skus down to options for selection
   for a certain selector. e.g. options for :hair/color."
  [product selected-sku facets skus product-options option-kw]
  (let [relevant-skus          (determine-relevant-skus skus selected-sku product product-options)
        cheapest-for-option-kw (lowest-sku-price-for-option-kw relevant-skus option-kw)
        cheapest-price         (lowest-sku-price relevant-skus)
        sku->option            (partial construct-option option-kw facets selected-sku cheapest-for-option-kw cheapest-price)]
    (merge product-options
           {option-kw (->> (reduce sku->option {} relevant-skus)
                           vals
                           (sort-by :sku/price))})))

(defn ugc-query [product sku data]
  (when-let [ugc (get-in data keypaths/ugc)]
    (when-let [images (pixlee/images-in-album ugc (:legacy/named-search-slug product))]
      {:carousel-data {:product-id   (:catalog/product-id product)
                       :product-name (:copy/title product)
                       :page-slug    (:page/slug product)
                       :sku-id       (:catalog/sku-id sku)
                       :album        images}
       :offset (get-in data keypaths/ui-ugc-category-popup-offset)
       :back   (first (get-in data keypaths/navigation-undo-stack))})))

(defn generate-options [facets product product-skus selected-sku]
  (reduce (partial skus->options product selected-sku facets product-skus)
          {}
          (:selector/electives product)))

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

(defn query [data]
  (let [selected-sku    (get-in data catalog.keypaths/detailed-product-selected-sku)
        product         (products/current-product data)
        product-skus    (extract-product-skus data product)
        facets          (->> (get-in data keypaths/v2-facets)
                             (map #(update % :facet/options (partial maps/index-by :option/slug)))
                             (maps/index-by :facet/slug))
        carousel-images (find-carousel-images product product-skus selected-sku)
        ugc             (ugc-query product selected-sku data)]
    {:reviews           (add-review-eligibility (review-component/query data) product)
     :ugc               ugc
     :fetching-product? (utils/requesting? data (conj request-keys/search-v2-products
                                                      (:catalog/product-id product)))
     :adding-to-bag?    (utils/requesting? data (conj request-keys/add-to-bag (:catalog/sku-id selected-sku)))
     :bagged-skus       (get-in data keypaths/browse-recently-added-skus)
     :sku-quantity      (get-in data keypaths/browse-sku-quantity 1)
     :options           (generate-options facets product product-skus selected-sku)
     :product           product
     :selected-sku      selected-sku
     :cheapest-price    (lowest-sku-price product-skus)
     :carousel-images   carousel-images
     :human-hair? (experiments/human-hair? data)}))

(defn built-component [data opts]
  (component/build component (query data) opts))

(defn fetch-product-album
  [{:keys [legacy/named-search-slug]}]
  (when named-search-slug
    (when-let [album-id (get-in config/pixlee [:albums named-search-slug])]
      #?(:cljs (pixlee-hooks/fetch-album album-id named-search-slug)
         :clj nil))))

(defn get-valid-product-skus [product all-skus]
  (->> product
       :selector/skus
       (select-keys all-skus)
       vals
       (filter :inventory/in-stock?)))

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

(defmethod transitions/transition-state events/navigate-product-details
  [_ event {:keys [catalog/product-id query-params]} app-state]
  (let [product (products/product-by-id app-state product-id)
        sku-id  (determine-sku-id app-state product (:SKU query-params))
        sku     (get-in app-state (conj keypaths/v2-skus sku-id))]
    (-> app-state
        (assoc-in keypaths/ui-ugc-category-popup-offset (:offset query-params))
        (assoc-in catalog.keypaths/detailed-product-selected-sku sku)
        (assoc-in catalog.keypaths/detailed-product-id product-id)
        (assoc-in keypaths/browse-recently-added-skus [])
        (assoc-in keypaths/browse-sku-quantity 1))))

#?(:cljs
   (defmethod effects/perform-effects events/navigate-product-details
     [_ _ {:keys [catalog/product-id]} _ app-state]
     (api/search-v2-products (get-in app-state keypaths/api-cache)
                             {:catalog/product-id product-id}
                             (partial messages/handle-message
                                      events/api-success-v2-products-for-details))

     (if-let [current-product (products/current-product app-state)]
       (if (auth/permitted-product? app-state current-product)
         (do
           (fetch-product-album current-product)
           (review-hooks/insert-reviews))
         (effects/redirect events/navigate-home)))))

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
        product-skus (extract-product-skus app-state product)]
    (-> app-state
        (assoc-in catalog.keypaths/detailed-product-product-skus product-skus)
        (assoc-in catalog.keypaths/detailed-product-selected-sku sku))))

(defn first-when-only [coll]
  (when (= 1 (count coll))
    (first coll)))

(defn determine-sku-from-selections [app-state]
  (let [product      (products/current-product app-state)
        skus         (get-in app-state catalog.keypaths/detailed-product-product-skus)
        selections   (skuers/electives product (get-in app-state catalog.keypaths/detailed-product-selected-sku))
        selected-sku (selector/match-all {} selections skus)]
    (first-when-only selected-sku)))

(defn assoc-sku-from-selections [app-state]
  (assoc-in app-state catalog.keypaths/detailed-product-selected-sku
            (determine-sku-from-selections app-state)))

(defn determine-cheapest-length [skus]
  (-> (sort-by :sku/price skus)
      first
      :hair/length))

(defn determine-selected-length [app-state selected-option]
  (let [product    (products/current-product app-state)
        skus       (get-in app-state catalog.keypaths/detailed-product-product-skus)
        selections (skuers/electives product
                                     (cond-> (get-in app-state catalog.keypaths/detailed-product-selected-sku)
                                       (= selected-option :hair/color)
                                       (dissoc :hair/length)))]
    (determine-cheapest-length (selector/match-all {}
                                                   (merge selections {:inventory/in-stock? #{true}})
                                                   skus))))

(defn assoc-default-length [app-state selected-option]
  (assoc-in app-state (conj catalog.keypaths/detailed-product-selected-sku :hair/length)
            (determine-selected-length app-state selected-option)))

(defmethod transitions/transition-state events/control-bundle-option-select
  [_ event {:keys [selection value]} app-state]
  (-> app-state
      (assoc-in (conj catalog.keypaths/detailed-product-selected-sku selection) value)
      (assoc-default-length selection)
      assoc-sku-from-selections))

#?(:cljs
   (defmethod effects/perform-effects events/control-bundle-option-select
     [_ event {:keys [selection value]} _ app-state]
     (let [sku-id                                 (get-in app-state catalog.keypaths/detailed-product-selected-sku-id)
           {:keys [catalog/product-id page/slug]} (products/current-product app-state)]
       (history/enqueue-redirect events/navigate-product-details
                                 {:catalog/product-id product-id
                                  :page/slug          slug
                                  :query-params       {:SKU sku-id}}))))

(defmethod effects/perform-effects events/control-add-sku-to-bag
  [dispatch event {:keys [sku quantity] :as args} _ app-state]
  #?(:cljs (api/add-sku-to-bag
            (get-in app-state keypaths/session-id)
            {:sku        sku
             :quantity   quantity
             :stylist-id (get-in app-state keypaths/store-stylist-id)
             :token      (get-in app-state keypaths/order-token)
             :number     (get-in app-state keypaths/order-number)
             :user-id    (get-in app-state keypaths/user-id)
             :user-token (get-in app-state keypaths/user-token)}
            #(messages/handle-message events/api-success-add-sku-to-bag
                                      {:order    %
                                       :quantity quantity
                                       :sku      sku}))))

(defmethod transitions/transition-state events/api-success-add-sku-to-bag
  [_ event {:keys [order quantity sku]} app-state]
  (-> app-state
      (update-in keypaths/browse-recently-added-skus
                 conj
                 {:quantity quantity :sku sku})
      (assoc-in keypaths/browse-sku-quantity 1)
      (update-in keypaths/order merge order)))

