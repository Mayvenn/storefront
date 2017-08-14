(ns catalog.product-details
  (:require [clojure.set :as set]
            [clojure.string :as string]
            [catalog.bundle-builder :as bundle-builder]
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
            [datascript.core :as d]
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
            #?@(:clj [[storefront.backend-api :as api]
                      [storefront.component-shim :as component]]
                :cljs [[storefront.hooks.pixlee :as pixlee-hooks]
                       [storefront.component :as component]
                       [storefront.hooks.reviews :as review-hooks]
                       [storefront.api :as api]])))


(defn page [wide-left wide-right-and-narrow]
  [:div.clearfix.mxn2 {:item-scope :itemscope :item-type "http://schema.org/Product"} [:div.col-on-tb-dt.col-7-on-tb-dt.px2 [:div.hide-on-mb wide-left]]
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
                                            {:path keypaths/browse-variant-quantity})
                 (utils/send-event-callback events/control-counter-inc
                                            {:path keypaths/browse-variant-quantity}))]
    [:span.h4 "Currently out of stock"]))

(defn add-to-bag-button [adding-to-bag? sku quantity]
  (ui/navy-button {:on-click
                   (utils/send-event-callback events/control-add-to-bag
                                              {:sku sku :quantity quantity})
                   :data-test "add-to-bag"
                   :spinning? adding-to-bag?}
                  "Add to bag"))

(defn ^:private number->words [n]
  (let [mapping ["Zero" "One" "Two" "Three" "Four" "Five" "Six" "Seven" "Eight" "Nine" "Ten" "Eleven" "Twelve" "Thirteen" "Fourteen" "Fifteen"]]
    (get mapping n (str "(x " n ")"))))

(defn display-bagged-sku [idx {:keys [quantity sku]}]
  [:div.h6.my1.p1.py2.caps.dark-gray.bg-light-gray.medium.center
   {:key (str "bagged-sku-" idx)
    :data-test "items-added"}
   "Added to bag: "
   (number->words quantity)
   " "
   (products/product-title sku)])

(def checkout-button
  (component/html
   [:div
    {:data-test "cart-button"
     :data-ref "cart-button"}
    (ui/teal-button (utils/route-to events/navigate-cart) "Check out")]))

(defn skus-variants-and-checkout [bagged-skus]
  (when (seq bagged-skus)
    [:div
     (map-indexed display-bagged-sku bagged-skus)
     checkout-button]))

(defn option-html [step-name
                   {:keys [option/name option/slug image price-delta checked? sold-out? selections]}]
  [:label.btn.p1.flex.flex-column.justify-center.items-center.container-size.letter-spacing-0
   {:data-test (str "option-" (string/replace name #"\W+" ""))
    :class     (cond
                 sold-out? "border-gray bg-gray  dark-gray light"
                 checked?  "border-gray bg-teal  white     medium"
                 true      "border-gray bg-white dark-gray light")
    :style     {:font-size "14px" :line-height "18px"}}
   [:input.hide {:type      "radio"
                 :disabled  sold-out?
                 :checked   checked?
                 :on-change (utils/send-event-callback events/control-bundle-option-select
                                                       {:selection step-name
                                                        :value     slug})}]
   (if image
     [:img.mbp4.content-box.circle.border-light-gray
      {:src   image :alt    name
       :width 30    :height 30
       :class (cond checked? "border" sold-out? "muted")}]
     [:span.block.titleize name])
   [:span.block
    (when sold-out?
      "Sold Out"
      #_ [:span (when-not checked? {:class "navy"})
       "+" (as-money-without-cents price-delta)])]])

(defn step-html [{:keys [step-name selected-option options]}]
  [:div.my2 {:key (str "step-" step-name)}
   [:h2.h3.clearfix.h5
    [:span.block.left.navy.medium.shout
     (name step-name)
     (when selected-option [:span.inline-block.mxp2.dark-gray " - "])]
    (when selected-option
      [:span.block.overflow-hidden.dark-gray.h5.regular
       (or (:long-name selected-option)
           [:span.titleize (:name selected-option)])])]
   [:div.flex.flex-wrap.content-stretch.mxnp3
    (for [{option-name :name :as option} options]
      [:div.flex.flex-column.justify-center.pp3
       {:key   (string/replace (str "option-" (hash option)) #"\W+" "-")
        :style {:height "72px"}
        :class (if (#{:length :color :style} step-name) "col-4" "col-6")}
       (option-html step-name option)])]])

(defn indefinite-articalize [word]
  (let [vowel? (set "AEIOUaeiou")]
    (str (if (vowel? (first word)) "an " "a ")
         word)))

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
    (->> (map (partial facet->option-name facets) slug-keys slugs)
         (clojure.string/join " "))))

(defn summary-structure [desc quantity-and-price]
  [:div
   (when (seq desc)
     [:div
      [:h2.h3.light "Summary"]
      [:div.navy.shout desc]])
   quantity-and-price])

(defn no-sku-summary [next-step]
#_  (summary-structure
   (str "Select " (-> next-step name string/capitalize indefinite-articalize) "!")
   (quantity-and-price-structure ui/nbsp "$--.--")))

(defn item-price [price]
  [:span {:item-prop "price"} (as-money-without-cents price)])

(defn sku-summary [{:keys [facets sku quantity]}]
  (let [{:keys [in-stock? price]} sku]
    (summary-structure
     (sku-name facets sku)
     (quantity-and-price-structure
      (counter-or-out-of-stock in-stock? quantity)
      (item-price price)))))

(def triple-bundle-upsell
  (component/html [:p.center.h5.p2.navy promos/bundle-discount-description]))

(def shipping-and-guarantee
  (component/html
   [:div.border-top.border-bottom.border-gray.p2.my2.center.navy.shout.medium.h6
    "Free shipping & 30 day guarantee"]))

(defn sku-set-description [{{:keys [included-items description]} :copy}]
  [:div.border.border-dark-gray.mt2.p2.rounded
   [:h2.h3.medium.navy.shout "Description"]
   [:div {:item-prop "description"}
    #_
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
    (when (seq included-items)
      [:div.my2
       [:h3.mbp3.h5 "Includes:"]
       [:ul.list-reset.navy.h5.medium
        (for [[idx item] (map-indexed vector included-items)]
          [:li.mbp3 {:key (str "item-" idx)} item])]])
    [:div.h5.dark-gray
     (for [[idx item] (map-indexed vector description)]
       [:p.mt2 {:key (str "sku-set-description-" idx)} item])]]])

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
  [facets skus selections selector]
  (let [sku->option
        (fn [options sku]
          (let [option-name   (selector sku)
                selected-name (selector selections)
                facet-option  (get-in facets [selector :facet/options option-name])
                image         (:option/image facet-option)]
            (update options option-name
                    (fn [existing]
                      {:option/name        (:option/name facet-option)
                       :option/slug        (:option/slug facet-option)
                       :can-supply? 10
                       :image       image
                       :checked?    (if (seq existing)
                                      (= option-name selected-name)
                                      (seq selected-name))
                       :sold-out?   (not (or (:in-stock? sku)
                                             (:in-stock? existing)))}))))]

    {selector (->> skus
                   (reduce sku->option {})
                   vals)}))

(defn component
  [{:keys [initial-skus
           selected-skus
           steps
           skus
           selections
           options
           selectors
           product
           fetching-sku-set?
           carousel-images
           reviews
           ugc
           selected-sku
           sku-quantity
           facets
           bundle-builder]}
   owner
   opts]
  (let [review? (:review? reviews)
        selected-sku
        (when (= 1 (count selected-skus)) (first selected-skus))]
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
         #_(when (and (not fetching-sku-set?)
                    needs-selections?)
           (starting-at (:initial-variants bundle-builder)))]
        (if fetching-sku-set?
          [:div.h2.mb2 ui/spinner]
          [:div
           [:div schema-org-offer-props
            [:div.my2
             [:small
              [:div
               [:div.h4.bold "Skus Initial: " (count initial-skus)]
               [:code (prn-str (:criteria product))]]
              [:div
               [:div.h4.bold "Skus Selected: " (count selected-skus)]
               [:code (prn-str selections)]]]
             [:div
              (when (contains? (-> product :criteria :product/department set) "hair")
                (for [step-name steps]
                  (step-html {:step-name       step-name
                              :selected-option (step-name selections)
                              :options         (step-name options)})))]
             (if selected-sku
               (sku-summary {:sku      selected-sku
                             :quantity 1 #_ quantity
                             :facets   facets})
               [:div "todo"] ;; (no-variant-summary (bundle-builder/next-step bundle-builder))
               )]
            (when (sku-sets/eligible-for-triple-bundle-discount? product)
              triple-bundle-upsell)
            (when selected-sku
              (add-to-bag-button false #_ adding-to-bag? selected-sku sku-quantity))
            #_(bagged-variants-and-checkout bagged-variants)
            (when (sku-sets/stylist-only? product) shipping-and-guarantee)]])
        (sku-set-description product)
        [:div.hide-on-tb-dt.mxn2.mb3 (component/build ugc/component ugc opts)]])
      (when review?
        (component/build review-component/reviews-component reviews opts))])))

(defn ugc-query [sku-set data]
  (let [images (pixlee/images-in-album (get-in data keypaths/ugc)
                                       (sku-sets/id->named-search (:id sku-set)))]
    {:named-search sku-set
     :album        images}))
;; finding a sku from a product

(defn ->clauses [m] (mapv (fn [[k v]] ['?s k v]) m))

(defn ^:private update-vals [m f & args]
  (reduce (fn [r [k v]] (assoc r k (apply f v args))) {} m))

(defn query [data]
  (let [sku-code->sku      (get-in data keypaths/skus)
        product            (sku-sets/current-sku-set data)
        skus               (map sku-code->sku
                                (:skus product))
        ;; TODO this is a mess. probably unneeded
        images             (mapcat :images skus)
        reviews            (assoc (review-component/query data)
                                  :review?
                                  (sku-sets/eligible-for-reviews? product))
        skus-db            (-> (d/empty-db)
                               (d/db-with (->> skus
                                               (map #(merge (:attributes %) %))
                                               (mapv #(dissoc % :attributes)))))
        selections         (get-in data keypaths/bundle-builder-selections)
        selections-clauses (->clauses selections)
        criteria           (-> product :criteria (update-vals first) ->clauses)
        initial-query      (when (seq criteria)
                             (concat [:find '(pull ?s [*])
                                      :where]
                                     criteria))
        selected-query     (when (seq criteria)
                             (concat [:find '(pull ?s [*])
                                      :where]
                                     criteria
                                     selections-clauses))
        initial-skus       (when (seq initial-query)
                             (->> skus-db
                                  (d/q initial-query)
                                  (map first)
                                  (sort-by :price)))
        selected-skus      (when (seq initial-query)
                             (->> skus-db
                                  (d/q selected-query)
                                  (map first)
                                  (sort-by :price)))
        steps              [:hair/color :hair/length]
        facets             (->> (get-in data keypaths/facets)
                                (map #(update % :facet/options (partial maps/key-by :option/slug)))
                                (maps/key-by :facet/slug))
        options            (->> steps
                                (map (partial product->options
                                        facets
                                        initial-skus
                                        selections))
                                (apply merge))]
    {:product           product
     :skus              skus
     :selections        selections
     :steps             steps
     :options           options
     :initial-skus      initial-skus
     :selected-skus     selected-skus
     :carousel-images   (set (filter (comp #{"carousel"} :use-case)
                                     images))
     :fetching-sku-set? false
     :reviews           reviews
     :ugc               (ugc-query product data)
     :bundle-builder    (get-in data keypaths/bundle-builder)
     :facets            facets}))

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

(defmethod effects/perform-effects events/navigate-product-details
  [_ event {:keys [id slug]} _ app-state]
  (api/search-sku-sets id (fn [response] (messages/handle-message events/api-success-sku-sets-for-details response)))
  (api/fetch-facets (get-in app-state keypaths/api-cache))
  #?(:cljs (review-hooks/insert-reviews))
  (fetch-current-sku-set-album app-state id))

(defmethod effects/perform-effects events/api-success-sku-sets-for-details
  [_ event {:keys [sku-sets] :as response} _ app-state]
  (fetch-current-sku-set-album app-state (get-in app-state keypaths/product-details-sku-set-id)))

(defmethod transitions/transition-state events/navigate-product-details
  [_ event {:keys [id slug]} app-state]
  (let [bundle-builder-selections (-> (get-in app-state keypaths/bundle-builder)
                                      bundle-builder/expanded-selections
                                      (dissoc :length))]
    (-> app-state
        (assoc-in keypaths/product-details-sku-set-id id)
        (assoc-in keypaths/saved-bundle-builder-options bundle-builder-selections)
        (assoc-in keypaths/browse-recently-added-skus [])
        (assoc-in keypaths/browse-sku-quantity 1))))

(defmethod transitions/transition-state events/control-bundle-option-select
  [_ event {:keys [selection value]} app-state]
  (update-in app-state
             keypaths/bundle-builder-selections
             (fn [selections]
               (if (and (= (get selections selection) value)
                        (not= 1 (count selections)))
                 (dissoc selections selection)
                 (assoc selections selection value)))))
