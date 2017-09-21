(ns catalog.product-details
  (:require [clojure.set :as set]
            [clojure.string :as string]
            [catalog.selector :as selector]
            [catalog.products :as products]
            [catalog.keypaths]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.accessors.promos :as promos]
            [storefront.assets :as assets]
            [storefront.components.money-formatters :refer [as-money-without-cents as-money]]
            [storefront.components.ui :as ui]
            [spice.maps :as maps]
            [spice.core :as spice]
            [storefront.config :as config]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.messages :as messages]
            [storefront.platform.reviews :as review-component]
            [storefront.platform.ugc :as ugc]
            [storefront.request-keys :as request-keys]
            [storefront.transitions :as transitions]
            [storefront.platform.component-utils :as utils]
            #?@(:clj [[storefront.component-shim :as component]]
                :cljs [[storefront.hooks.pixlee :as pixlee-hooks]
                       [storefront.component :as component]
                       [storefront.hooks.reviews :as review-hooks]
                       [storefront.api :as api]
                       [storefront.history :as history]])))

(defn item-price [price]
  [:span {:item-prop "price"} (as-money-without-cents price)])

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

(defn title [name]
  [:h1.h2.medium.titleize.navy {:item-prop "name"} name])

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
     (ui/counter quantity
                 false
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
                   :spinning? adding-to-bag?}
                  "Add to bag"))

(defn display-bagged-sku [idx {:keys [quantity sku]}]
  [:div.h6.my1.p1.py2.caps.dark-gray.bg-light-gray.medium.center
   {:key (str "bagged-sku-" idx)
    :data-test "items-added"}
   "Added to bag: "
   (spice/number->word quantity)
   " "
   (:sku/name sku)])

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

(defn option-html
  [selector {:keys [option/name option/slug image price-delta checked? stocked? selected-criteria]}]
  [:label.btn.p1.flex.flex-column.justify-center.items-center.container-size.letter-spacing-0
   {:data-test (str "option-" (string/replace name #"\W+" ""))
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
    (for [{option-name :name :as option} options]
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
  (let [{:keys [in-stock? price]} sku]
    (summary-structure
     (some-> sku :sku/name string/upper-case)
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
  [{{:keys [description colors weights materials summary]} :sku-set/copy}]
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
         [:p.mt2 {:key (str "product-description-" idx)} item])]]]))

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
           bagged-skus
           carousel-images
           fetching-product?
           options
           product
           reviews
           selected-sku
           sku-quantity
           ugc]}
   owner
   opts]
  (let [review?        (:review? reviews)
        cheapest-price (->> options vals flatten (map :price) sort first)]
    (component/create
     [:div.container.p2
      (page
       [:div
        (carousel carousel-images product)
        [:div.hide-on-mb (component/build ugc/component ugc opts)]]
       [:div
        [:div.center
         (title (:sku-set/name product))
         (when review? (reviews-summary reviews opts))
         [:meta {:item-prop "image" :content (first carousel-images)}]
         (full-bleed-narrow (carousel carousel-images product))
         (starting-at cheapest-price)]
        (if fetching-product?
          [:div.h2.mb2 ui/spinner]
          [:div
           [:div schema-org-offer-props
            [:div.my2
             [:div
              (when (= (:product/department product) "hair")
                (for [facet (:selector/electives product)]
                  (selector-html {:selector facet
                                  :options  (get options facet)})))]
             (sku-summary {:sku          selected-sku
                           :sku-quantity sku-quantity})]
            (when (products/eligible-for-triple-bundle-discount? product)
              triple-bundle-upsell)
            (add-to-bag-button adding-to-bag?
                               selected-sku
                               sku-quantity)
            (bagged-skus-and-checkout bagged-skus)
            (when (products/stylist-only? product) shipping-and-guarantee)]])
        (product-description product)
        [:div.hide-on-tb-dt.mxn2.mb3 (component/build ugc/component ugc opts)]])
      (when review?
        (component/build review-component/reviews-component reviews opts))])))

(defn min-of-maps
  ([k] {})
  ([k a b]
   (if (< (get a k) (get b k))
     a b)))

(defn lowest [k ms]
  (reduce (partial min-of-maps k) ms))

(defn lowest-sku-price [skus]
  (:price (lowest :price skus)))

(defn lowest-sku-price-for-option-kw [skus option-kw]
  (into {}
        (map (fn [[option-kw skus]]
               {option-kw
                (lowest-sku-price skus)})
             (group-by option-kw skus))))

(defn ^:private construct-option [option-kw facets criteria cheapest-for-option-kw cheapest-price options-for-option-kw sku]
  (let [option-name  (option-kw sku)
        facet-option (get-in facets [option-kw :facet/options option-name])
        image        (:option/image facet-option)]
    (update options-for-option-kw option-name
            (fn [existing]
              {:option/name (:option/name facet-option)
               :option/slug (:option/slug facet-option)
               :stocked?    (or (:in-stock? sku)
                                (:stocked? existing))
               :image       image
               :price       (:price sku)
               :price-delta (- (get cheapest-for-option-kw option-name) cheapest-price)
               :checked?    (= (option-kw criteria)
                               (option-kw sku))}))))
(defn determine-relevant-skus
  [skus criteria electives product-options]
  (selector/query skus
                  (apply dissoc criteria
                         (set/difference (set electives)
                                         (set (keys product-options))))))

(defn skus->options
  "Reduces this product's skus down to options for selection
   for a certain selector. e.g. options for :hair/color."
  [electives criteria facets skus product-options option-kw]
  (let [relevant-skus          (determine-relevant-skus skus criteria electives product-options)
        cheapest-for-option-kw (lowest-sku-price-for-option-kw relevant-skus option-kw)
        cheapest-price         (lowest-sku-price relevant-skus)
        sku->option (partial construct-option option-kw facets criteria cheapest-for-option-kw cheapest-price)]
    (merge product-options
           {option-kw (->> (reduce sku->option {} relevant-skus)
                           vals
                           (sort-by :price))})))

(defn ugc-query [{:keys [product-id] long-name :sku-set/name} data]
  (let [slug   (products/id->named-search product-id)
        images (pixlee/images-in-album (get-in data keypaths/ugc) slug)]
    {:slug      slug
     :long-name long-name
     :album     images}))
;; finding a sku from a product

(defn generate-options [facets product product-skus criteria]
  (reduce (partial skus->options (:selector/electives product) criteria facets product-skus)
          {}
          (:selector/electives product)))

(defn add-review-eligibility [review-data product]
  (assoc review-data :review? (products/eligible-for-reviews? product)))


(defn query [data]
  (let [selected-sku (get-in data catalog.keypaths/detailed-product-selected-sku)
        criteria     (get-in data keypaths/bundle-builder-selections)
        product      (products/->skuer-schema (products/current-sku-set data))
        product-skus (->> (selector/query (get-in data keypaths/db-skus) product)
                          (sort-by :price))
        facets       (->> (get-in data keypaths/facets)
                          (map #(update % :facet/options (partial maps/index-by :option/slug)))
                          (maps/index-by :facet/slug))

        image-selector  (selector/map->Selector
                         {:skuer      product
                          :identifier :image-id
                          :space      (get-in data keypaths/db-images)})
        carousel-images (->> criteria
                             (merge {:use-case "carousel"
                                     :image/of #{"model" "product"}})
                             (selector/select image-selector)
                             (sort-by :order))]

    {:reviews           (add-review-eligibility (review-component/query data) product)
     :ugc               (ugc-query product data)
     :fetching-product? (utils/requesting? data (conj request-keys/search-sku-sets
                                                      (:catalog/product-id product)))
     :adding-to-bag?    (utils/requesting? data request-keys/add-to-bag)
     :bagged-skus       (get-in data keypaths/browse-recently-added-skus)
     :sku-quantity      (get-in data keypaths/browse-sku-quantity 1)
     :options           (generate-options facets product product-skus criteria)
     :product           product
     :selected-sku      selected-sku
     :carousel-images   carousel-images}))

(defn built-component [data opts]
  (component/build component (query data) opts))

(defn fetch-current-sku-set-album [app-state sku-set-id]
  (when-let [slug (->> sku-set-id
                       (products/sku-set-by-id app-state)
                       :sku-set/id
                       products/id->named-search)]
    (when-let [album-id (get-in config/pixlee [:albums slug])]
      #?(:cljs (pixlee-hooks/fetch-album album-id slug)
         :clj nil))))

(defmethod transitions/transition-state events/navigate-product-details
  [_ event {:keys [catalog/product-id page/slug catalog/sku-id]} app-state]
  (let [product      (products/->skuer-schema (products/current-sku-set app-state))
        product-skus (vals (select-keys (get-in app-state keypaths/skus) (:selector/skus product)))
        sku          (get-in app-state (conj keypaths/skus (or sku-id (:sku (lowest :price product-skus)))))]
    (-> app-state
        (assoc-in catalog.keypaths/detailed-product-selected-sku-id sku-id)
        (assoc-in catalog.keypaths/detailed-product-id product-id)
        (assoc-in keypaths/browse-recently-added-skus [])
        (assoc-in keypaths/bundle-builder-selections (merge product (:attributes sku)))
        (assoc-in keypaths/browse-sku-quantity 1))))

#?(:cljs
   (defmethod effects/perform-effects events/navigate-product-details
     [_ event {:keys [catalog/product-id]} _ app-state]
     (let [product (products/sku-set-by-id app-state product-id)]
       (if (auth/permitted-product? app-state product)
         (do
           (api/search-sku-sets (get-in app-state keypaths/api-cache)
                                product-id
                                (partial messages/handle-message
                                         events/api-success-sku-sets-for-details))
           (api/fetch-facets (get-in app-state keypaths/api-cache))
           (review-hooks/insert-reviews)
           (fetch-current-sku-set-album app-state product-id))
         (effects/redirect events/navigate-home)))))

(defmethod effects/perform-effects events/api-success-sku-sets-for-details
  [_ event {:keys [sku-sets] :as response} _ app-state]
  (fetch-current-sku-set-album app-state
                               (get-in app-state catalog.keypaths/detailed-product-id)))

(defmethod transitions/transition-state events/api-success-sku-sets-for-details
  ;; for pre-selecting skus by url
  [_ event {:keys [skus] :as response} app-state]
  (let [sku-id (get-in app-state catalog.keypaths/detailed-product-selected-sku-id)
        sku    (get-in app-state (conj keypaths/skus sku-id))]
    (if sku
      (->> [:hair/color :hair/length]
           (select-keys (:attributes sku))
           ;;TODO this needs a new keypath
           (update-in app-state keypaths/bundle-builder-selections merge))
      app-state)))

(defn first-when-only [coll]
  (when (= 1 (count coll))
    (first coll)))

(defn determine-sku-from-selections [app-state]
  (let [skus       (get-in app-state keypaths/db-skus)
        selections (get-in app-state keypaths/bundle-builder-selections)
        selected-sku (selector/query skus selections)]
    (first-when-only selected-sku)))

(defn assoc-sku-from-selections [app-state]
  (assoc-in app-state catalog.keypaths/detailed-product-selected-sku
            (determine-sku-from-selections app-state)))

(defn determine-cheapest-length [skus]
  (-> :price
      (sort-by skus)
      first
      :hair/length))

(defn determine-selected-length [app-state selected-option]
  (let [skus            (get-in app-state keypaths/db-skus)
        selections      (cond-> (get-in app-state keypaths/bundle-builder-selections)
                          (= selected-option :hair/color)
                          (dissoc :hair/length))]
    (determine-cheapest-length (selector/query skus selections))))

(defn assoc-default-length [app-state selected-option]
  (assoc-in app-state (conj keypaths/bundle-builder-selections :hair/length)
            (determine-selected-length app-state selected-option)))

(defmethod transitions/transition-state events/control-bundle-option-select
  [_ event {:keys [selection value]} app-state]
  (-> app-state
    (assoc-in (conj keypaths/bundle-builder-selections selection) value)
    (assoc-default-length selection)
    assoc-sku-from-selections))

#?(:cljs
   (defmethod effects/perform-effects events/control-bundle-option-select
     [_ event {:keys [selection value]} _ app-state]
     (let [sku-id (get-in app-state catalog.keypaths/detailed-product-selected-sku-id)
           prod (products/current-sku-set app-state)]
       (history/enqueue-redirect events/navigate-product-details-sku
                                 {:catalog/product-id (:sku-set/id prod)
                                  :page/slug (:sku-set/slug prod)
                                  :catalog/sku-id sku-id}))))

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

