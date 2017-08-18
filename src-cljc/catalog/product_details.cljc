(ns catalog.product-details
  (:require [clojure.set :as set]
            [clojure.string :as string]
            [catalog.bundle-builder :as bundle-builder]
            [catalog.selector :as selector]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.accessors.products :as products]
            [storefront.accessors.promos :as promos]
            [storefront.accessors.sku-sets :as sku-sets]
            [storefront.assets :as assets]
            [storefront.components.money-formatters :refer [as-money-without-cents as-money]]
            [storefront.components.ui :as ui]
            [storefront.utils.maps :as maps]
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
                       [storefront.api :as api]])))

(def steps [:hair/color :hair/length])

(defn item-price [price]
  [:span {:item-prop "price"} (as-money-without-cents price)])

(defn starting-at [cheapest-price]
  [:div.center.dark-gray
   [:div.h6 "Starting at"]
   [:div.h2 (item-price cheapest-price)]])

(defn facet->option-name [facets facet-slug option-slug]
  (-> facets
      facet-slug
      :facet/options
      (get option-slug)
      :option/name))

(defn sku-name [facets {:keys [hair/family] :as sku}]
  (let [slug-keys (if (= family "bundles")
                    [:hair/color :hair/origin :hair/length :hair/texture]
                    [:hair/color :hair/texture :hair/base-material :hair/origin :hair/length :hair/family])
        slugs     ((apply juxt slug-keys) sku)]
    (some->> (map (partial facet->option-name facets) slug-keys slugs)
             (clojure.string/join " ")
             string/upper-case)))

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

(defn ^:private number->words [n]
  (let [mapping ["Zero" "One" "Two" "Three" "Four" "Five" "Six" "Seven" "Eight" "Nine" "Ten" "Eleven" "Twelve" "Thirteen" "Fourteen" "Fifteen"]]
    (get mapping n (str "(x " n ")"))))

(defn display-bagged-sku [facets idx {:keys [quantity sku]}]
  [:div.h6.my1.p1.py2.caps.dark-gray.bg-light-gray.medium.center
   {:key (str "bagged-sku-" idx)
    :data-test "items-added"}
   "Added to bag: "
   (number->words quantity)
   " "
   (sku-name facets sku)])

(def checkout-button
  (component/html
   [:div
    {:data-test "cart-button"
     :data-ref "cart-button"}
    (ui/teal-button (utils/route-to events/navigate-cart) "Check out")]))

(defn bagged-skus-and-checkout [facets bagged-skus]
  (when (seq bagged-skus)
    [:div
     (map-indexed (partial display-bagged-sku facets) bagged-skus)
     checkout-button]))

(defn option-html
  [step-name {:keys [option/name option/slug image price-delta checked? stocked? selected-criteria]}]
  [:label.btn.p1.flex.flex-column.justify-center.items-center.container-size.letter-spacing-0
   {:data-test (str "option-" (string/replace name #"\W+" ""))
    :class     (cond
                 checked? "border-gray bg-teal  white     medium"
                 stocked? "border-gray bg-white dark-gray light"
                 :else    "border-gray bg-gray  dark-gray light")
    :style     {:font-size "14px" :line-height "18px"}}
   [:input.hide {:type      "radio"
                 :disabled  (not stocked?)
                 :checked   checked?
                 :on-change (utils/send-event-callback events/control-bundle-option-select
                                                       {:selection step-name
                                                        :value     slug})}]
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

(defn step-html
  [{:keys [step-name selected-option options]}]
  [:div.my2
   {:key (str "step-" step-name)}
   [:h2.h3.clearfix.h5
    [:span.block.left.navy.medium.shout
     (name step-name)
     (when selected-option
       [:span.inline-block.mxp2.dark-gray " - "])]
    (when selected-option
      [:span.block.overflow-hidden.dark-gray.h5.regular
       (:option/name (first (filter :checked? options)))])]
   [:div.flex.flex-wrap.content-stretch.mxnp3
    (for [{option-name :name :as option} options]
      [:div.flex.flex-column.justify-center.pp3.col-4
       {:key   (string/replace (str "option-" (hash option)) #"\W+" "-")
        :style {:height "72px"}}
       (option-html step-name option)])]])

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

(defn no-sku-summary [facets selected-criteria steps]
  (let [next-step (some (fn [step] (when-not (contains? selected-criteria step) step)) steps)]
    (summary-structure
     (str "Select " (some-> next-step facets :facet/name string/capitalize indefinite-articalize) "!")
     (quantity-and-price-structure ui/nbsp "$--.--"))))

(defn sku-summary [{:keys [facets sku sku-quantity]}]
  (let [{:keys [in-stock? price]} sku]
    (summary-structure
     (sku-name facets sku)
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
  [{{:keys [included-items description]} :copy}]
  (when (seq description)
    [:div.border.border-dark-gray.mt2.p2.rounded
     [:h2.h3.medium.navy.shout "Description"]
     [:div {:item-prop "description"}
      (when (seq included-items)
        [:div.my2
         [:h3.mbp3.h5 "Includes:"]
         [:ul.list-reset.navy.h5.medium
          (for [[idx item] (map-indexed vector included-items)]
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

(defn product->options
  "Reduces product skus down to options for selection
   for a certain selector. e.g. options for :hair/color."
  [facets skus selector]
  (let [sku->option
        (fn [options sku]
          (let [option-name  (selector sku)
                facet-option (get-in facets [selector :facet/options option-name])
                image        (:option/image facet-option)]
            (update options option-name
                    (fn [existing]
                      {:option/name (:option/name facet-option)
                       :option/slug (:option/slug facet-option)
                       :stocked?    (or (:in-stock? sku)
                                        (:in-stock? existing))
                       :image       image
                       :price       (:price sku)}))))]
    {selector (->> skus
                   (reduce sku->option {})
                   vals
                   (sort-by :price))}))

(defn check-options [selected-criteria initial-skus options-by-selector]
  (into {} (for [[option-selector options] options-by-selector]
             (let [min-price               (:price (first options))
                   color-matches-selected? #(= (:hair/color selected-criteria)
                                               (:hair/color %))]
               [option-selector (->> options
                                     (mapv
                                      (fn [{:keys [option/slug price] :as option}]
                                        (-> option
                                            (assoc :checked? (= slug (option-selector selected-criteria)))
                                            (assoc :price-delta (- price min-price)))))
                                     (filterv
                                      (fn [{:keys [option/slug]}]
                                        ; Special case hair/length to be removed if they don't
                                        ; exist for a color. In lieu of a general step/flow concept
                                        (or (not= option-selector :hair/length)
                                            ; skus with hair/lengths that exist for hair/color
                                            (some #(and (color-matches-selected? %)
                                                        (= slug (get % option-selector)))
                                                  initial-skus))) ))]))))

(defn component
  [{:keys [adding-to-bag?
           bagged-skus
           carousel-images
           facets
           fetching-product?
           options
           product
           reviews
           selected-sku
           selected-criteria
           sku-quantity
           steps
           ugc
           cheapest-price]}
   owner
   opts]
  (let [review? (:review? reviews)]
    (component/create
     [:div.container.p2
      (page
       [:div
        (carousel carousel-images product)
        [:div.hide-on-mb (component/build ugc/component ugc opts)]]
       [:div
        [:div.center
         (title (:name product))
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
              (when (contains? (-> product :criteria :product/department set) "hair")
                (for [step-name steps]
                  (step-html {:step-name       step-name
                              :selected-option (step-name selected-criteria)
                              :options         (step-name options)})))]
             (sku-summary {:sku          selected-sku
                           :sku-quantity sku-quantity
                           :facets       facets})]
            (when (sku-sets/eligible-for-triple-bundle-discount? product)
              triple-bundle-upsell)
            (add-to-bag-button adding-to-bag?
                               selected-sku
                               sku-quantity)
            (bagged-skus-and-checkout facets bagged-skus)
            (when (sku-sets/stylist-only? product) shipping-and-guarantee)]])
        (product-description product)
        [:div.hide-on-tb-dt.mxn2.mb3 (component/build ugc/component ugc opts)]])
      (when review?
        (component/build review-component/reviews-component reviews opts))])))

(defn ugc-query [product data]
  (let [images (pixlee/images-in-album (get-in data keypaths/ugc)
                                       (sku-sets/id->named-search (:id product)))]
    {:named-search product
     :album        images}))
;; finding a sku from a product

(defn make-selected-criteria [options existing-selected-criteria selector]
  (let [existing-selection (selector existing-selected-criteria)
        minimal-option     (:option/slug (first (selector options)))]
    (assoc existing-selected-criteria selector (or existing-selection minimal-option))))

(defn query [data]
  (let [product (sku-sets/current-sku-set data)

        facets (->> (get-in data keypaths/facets)
                    (map #(update % :facet/options (partial maps/key-by :option/slug)))
                    (maps/key-by :facet/slug))

        reviews (assoc (review-component/query data)
                       :review?
                       (sku-sets/eligible-for-reviews? product))

        skus-db (->> (:skus product)
                     (select-keys (get-in data keypaths/skus))
                     vals
                     (map #(merge (:attributes %) %))
                     (mapv #(dissoc % :attributes))
                     selector/skus-db)

        ;; Essential

        essential-criteria (-> product :criteria (maps/update-vals first))

        initial-skus (->> (selector/query skus-db essential-criteria)
                          (sort-by :price))

        initial-options (->> steps
                             (map (partial product->options facets initial-skus))
                             (apply merge))

        ;; Applied selections

        selected-criteria (reduce (partial make-selected-criteria initial-options)
                                  ;;TODO This needs a new keypath
                                  (or (get-in data keypaths/bundle-builder-selections) {})
                                  steps)

        selection-options (check-options selected-criteria initial-skus initial-options)

        again-criteria (reduce (partial make-selected-criteria selection-options)
                               ;;TODO This needs a new keypath
                               (or (get-in data keypaths/bundle-builder-selections) {})
                               steps)

        again-options (check-options again-criteria initial-skus initial-options)


        selected-skus (->> (selector/query skus-db
                                           essential-criteria
                                           again-criteria)
                           (sort-by :price))

        selected-sku (first selected-skus)]
    {:adding-to-bag?    (utils/requesting? data request-keys/add-to-bag)
     :bagged-skus       (get-in data keypaths/browse-recently-added-skus)
     :carousel-images   (set (filter (comp #{"carousel"} :use-case) (:images selected-sku)))
     :facets            facets
     :fetching-product? (utils/requesting? data (conj request-keys/search-sku-sets (:id product)))
     :options           again-options
     :product           product
     :reviews           reviews
     :selected-sku      selected-sku
     :selected-criteria selected-criteria
     :sku-quantity      (get-in data keypaths/browse-sku-quantity 1)
     :steps             steps
     :ugc               (ugc-query product data)
     :cheapest-price    (-> initial-skus first :price)}))

(defn built-component [data opts]
  (component/build component (query data) opts))

(defn fetch-current-sku-set-album [app-state sku-set-id]
  (when-let [slug (->> sku-set-id
                       (sku-sets/sku-set-by-id app-state)
                       :id
                       sku-sets/id->named-search)]
    (when-let [album-id (get-in config/pixlee [:albums slug])]
      #?(:cljs (pixlee-hooks/fetch-album album-id slug)
         :clj nil))))

(defn handle-sku-set-response [response]
  (messages/handle-message events/api-success-sku-sets-for-details
                           response))

(defmethod effects/perform-effects events/navigate-product-details
  [_ event {:keys [id]} _ app-state]
  #?(:cljs (do (api/search-sku-sets id handle-sku-set-response)
               (api/fetch-facets (get-in app-state keypaths/api-cache))
               (review-hooks/insert-reviews)))
  (fetch-current-sku-set-album app-state id))

(defmethod effects/perform-effects events/api-success-sku-sets-for-details
  [_ event {:keys [sku-sets] :as response} _ app-state]
  (fetch-current-sku-set-album app-state (get-in app-state keypaths/product-details-sku-set-id)))

(defmethod transitions/transition-state events/api-success-sku-sets-for-details
  [_ event {:keys [sku-sets skus] :as response} app-state]
  (let [sku-code (get-in app-state keypaths/product-details-url-sku-code)
        sku (get-in app-state (conj keypaths/skus sku-code))]
    (if sku
      (->> steps
           (select-keys (:attributes sku))
           ;;TODO this needs a new keypath
           (assoc-in app-state keypaths/bundle-builder-selections))
      app-state)))

(defmethod transitions/transition-state events/navigate-product-details
  [_ event {:keys [id slug sku-code]} app-state]
  (-> app-state
      (assoc-in keypaths/product-details-url-sku-code sku-code)
      (assoc-in keypaths/product-details-sku-set-id id)
      (assoc-in keypaths/browse-recently-added-skus [])
      (assoc-in keypaths/browse-sku-quantity 1)))

(defmethod transitions/transition-state events/control-bundle-option-select
  [_ event {:keys [selection value]} app-state]
  ;;TODO this needs a new keypath
  (cond-> (assoc-in app-state
                    (conj keypaths/bundle-builder-selections selection) value)
    (= :hair/color selection)
    (update-in keypaths/bundle-builder-selections dissoc :hair/length)))

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
                                       :sku            sku}))))

(defmethod transitions/transition-state events/api-success-add-sku-to-bag
  [_ event {:keys [order quantity sku]} app-state]
  (-> app-state
      (update-in keypaths/browse-recently-added-skus
                 conj
                 {:quantity quantity :sku sku})
      (assoc-in keypaths/browse-sku-quantity 1)
      (update-in keypaths/order merge order)))

