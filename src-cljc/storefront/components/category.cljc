(ns storefront.components.category
  (:require [storefront.platform.component-utils :as utils]
            [storefront.components.money-formatters :refer [as-money-without-cents]]
            [storefront.accessors.promos :as promos]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.accessors.products :as products]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.bundle-builder :as bundle-builder]
            [storefront.platform.reviews :as reviews]
            [storefront.platform.ugc :as ugc]
            [storefront.components.ui :as ui]
            [clojure.string :as string]
            [clojure.set :as set]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.assets :as assets]
            [storefront.request-keys :as request-keys]
            [storefront.platform.carousel :as carousel]))

(defn page [wide-left wide-right-and-narrow]
  [:div.clearfix.mxn2 {:item-type "http://schema.org/Product"}
   [:div.md-up-col.md-up-col-7.px2 [:div.to-md-hide wide-left]]
   [:div.md-up-col.md-up-col-5.px2 wide-right-and-narrow]])

(defn title [name]
  [:h2.medium.titleize.navy.h3.line-height-2 {:item-prop "name"} name])

(defn full-bleed-narrow [body]
  ;; The mxn2 pairs with the p2 of the ui/container, to make the body full width
  ;; on mobile.
  [:div.md-up-hide.mxn2.my2 body])

(def schema-org-offer-props
  {:item-prop "offers"
   :item-scope ""
   :item-type "http://schema.org/Offer"})

(defn quantity-and-price-structure [quantity price]
  [:div
   [:div.right-align.gray.h6 "PRICE"]
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

(defn add-to-bag-button [adding-to-bag? variant quantity]
  (ui/large-navy-button {:on-click  (utils/send-event-callback events/control-add-to-bag
                                                               {:variant  variant
                                                                :quantity quantity})
                         :data-test "add-to-bag"
                         :spinning? adding-to-bag?}
                        "Add to bag"))

(defn ^:private number->words [n]
  (let [mapping ["Zero" "One" "Two" "Three" "Four" "Five" "Six" "Seven" "Eight" "Nine" "Ten" "Eleven" "Twelve" "Thirteen" "Fourteen" "Fifteen"]]
    (get mapping n (str "(x " n ")"))))

(defn display-bagged-variant [idx {:keys [quantity variant]}]
  [:div.h6.line-height-3.my1.p1.py2.caps.gray.bg-silver.medium.center
   {:key idx
    :data-test "items-added"}
   "Added to bag: "
   (number->words quantity)
   " "
   ;; TODO keys need to be renamed in cellar at some point
   (products/product-title (set/rename-keys variant {:variant_attrs :variant-attrs :name :product-name}))])

(def checkout-button
  (component/html
   [:div
    {:data-test "cart-button"
     :data-ref "cart-button"}
    (ui/large-teal-button (utils/route-to events/navigate-cart) "Check out")]))

(defn bagged-variants-and-checkout [bagged-variants]
  (when (seq bagged-variants)
    [:div
     (map-indexed display-bagged-variant bagged-variants)
     checkout-button]))

(defn option-html [later-step?
                   {:keys [name image price-delta checked? sold-out? selections]}]
  [:label.btn.border-light-gray.p1.flex.flex-column.justify-center.items-center.col-12
   {:data-test (str "option-" (string/replace name #"\W+" ""))
    :style {:height "100%"}
    :class (cond
             sold-out?   "bg-light-gray gray light"
             later-step? "bg-dark-silver muted light"
             checked?    "bg-teal white regular"
             true        "bg-white gray light")}
   [:input.hide {:type      "radio"
                 :disabled  (or later-step? sold-out?)
                 :checked   checked?
                 :on-change (utils/send-event-callback events/control-bundle-option-select
                                                       {:selected-options selections})}]
   (if image
     [:img.mbp4.content-box.circle.border-light-silver
      {:src image :alt name
       :width 30 :height 30
       :class (cond checked? "border" sold-out? "muted")}]
     [:div.f3.titleize name])
   [:div.f5.line-height-2
    (if sold-out?
      "Sold Out"
      [:span {:class (when-not checked? "navy")}
       "+" (as-money-without-cents price-delta)])]])

(defn step-html [{:keys [step-name selected-option later-step? options]}]
  [:div.my2 {:key step-name}
   [:h3.clearfix.f4
    [:div.left.navy.medium.shout
     (name step-name)
     (when selected-option [:span.inline-block.mxp2.gray " - "])]
    (when selected-option
      [:div.overflow-hidden.gray.f4.regular
       (or (:long-name selected-option)
           [:span.titleize (:name selected-option)])])]
   [:radiogroup.flex.flex-wrap.content-stretch.mxnp3
    (for [{:keys [name] :as option} options]
      [:div.flex.flex-column.justify-center.pp3
       {:key   (string/replace (str name step-name) #"\W+" "-")
        :style {:height "72px"}
        :class (if (#{:length :color :style} step-name) "col-4" "col-6")}
       (option-html later-step? option)])]])

(defn indefinite-articalize [word]
  (let [vowel? (set "AEIOUaeiou")]
    (str (if (vowel? (first word)) "an " "a ")
         word)))

(defn variant-name [variant flow]
  (let [flow (if (some #{:style} flow)
               (conj (vec flow) :category)
               (conj (vec flow) :style))]
    (->> flow
         (map variant)
         (string/join " ")
         string/upper-case)))

(defn summary-structure [desc quantity-and-price]
  [:div
   (when (seq desc)
     [:div
      [:h3.light "Summary"]
      [:div.navy desc]])
   quantity-and-price])

(defn no-variant-summary [next-step]
  (summary-structure
   (str "Select " (-> next-step name string/capitalize indefinite-articalize) "!")
   (quantity-and-price-structure ui/nbsp "$--.--")))

(defn variant-summary [{:keys [flow
                               variant
                               variant-quantity]}]
  (let [{:keys [can_supply? price]} variant]
    (summary-structure
     (variant-name variant flow)
     (quantity-and-price-structure
      (counter-or-out-of-stock can_supply? variant-quantity)
      [:span {:item-prop "price"} (as-money-without-cents price)]))))

(def triple-bundle-upsell
  (component/html [:p.center.p2.navy promos/bundle-discount-description]))

(def shipping-and-guarantee
  (component/html
   [:div.border-top.border-bottom.border-dark-silver.p2.my2.center.navy.shout.medium.h6
    "Free shipping & 30 day guarantee"]))

(defn named-search-description [{:keys [colors weights materials summary commentary]}]
  [:div.border.border-gray.mt2.p2.rounded
   [:h3.h4.medium.navy.shout "Description"]
   [:div {:item-prop "description"}
    (when (or colors weights materials)
      [:div.clearfix.my2
       (let [attrs (->> [["Color" colors]
                         ["Weight" weights]
                         ["Material" materials]]
                        (filter second))
             size (str "multi-cols-" (count attrs))]
         (into [:dl {:class size}]
               (mapcat (fn [[title value]]
                         [[:dt.break-before.gray.shout.h6 title]
                          [:dd.ml0.h5.navy.medium value]])
                       attrs)))])
    (when (seq summary)
      [:div.my2
       [:h4.mbp3.h6 "Includes:"]
       [:ul.list-reset.navy.h6.medium
        (for [[idx item] (map-indexed vector summary)]
          [:li.mbp3 {:key idx} item])]])
    [:div.h6.gray.line-height-2
     (for [[idx item] (map-indexed vector commentary)]
       [:p.mt2 {:key idx} item])]]])

(defn carousel-image [image]
  [:div.bg-cover.bg-no-repeat.bg-top.col-12
   {:style {:background-image (assets/css-url image)
            :height "31rem"}}])

(defn carousel [images {:keys [slug]}]
  (let [items (->> images
                   (map-indexed (fn [idx image]
                                  {:id   (subs image (max 0 (- (count image) 50)))
                                   :body (carousel-image image)}))
                   vec)]
    (component/build carousel/swipe-component
                     {:items      items
                      :continuous true}
                     {:react-key (apply str "category-swiper-" slug (interpose "-" (map :id items)))
                      :opts      {:dot-location :left}})))

(defn starting-at [variants]
  (when-let [cheapest-price (bundle-builder/min-price variants)]
    [:div.center
     [:div.light-gray.f6 "Starting at"]
     [:div.gray.f2.light
      {:item-prop "price"}
      (as-money-without-cents cheapest-price)]]))

(defn reviews-summary [reviews opts]
  [:div.h6
   (component/build reviews/reviews-summary-component reviews opts)])

(defn named-search-uses-product-images [named-search-slug]
  (#{"closures" "frontals" "straight" "loose-wave" "deep-wave"} named-search-slug))

(def image-types ["model" "product" "social"])

(defn sort-images [images]
  (for [image-type image-types
        {:keys [type large_url]} images
        :when (and (= type image-type) large_url)]
    large_url))

(defn distinct-variant-images [selected-variants]
  (->> (sort-by #(-> % :variant_attrs :style) selected-variants)
       reverse ;;List straight styles first
       (map :images)
       (mapcat sort-images)
       distinct
       vec))

(defn ^:private images-from-variants
  "For some named-searches, when a selection has been made, show detailed product images"
  [named-search {:keys [selected-options selected-variants]}]
  (if (and (named-search-uses-product-images (:slug named-search))
           (seq selected-options))
    (distinct-variant-images selected-variants)
    (:images named-search)))

(defn component [{:keys [named-search
                         bundle-builder
                         fetching-variants?
                         variant-quantity
                         reviews
                         adding-to-bag?
                         bagged-variants
                         ugc]}
                 owner opts]
  (let [selected-variant  (bundle-builder/selected-variant bundle-builder)
        carousel-images   (images-from-variants named-search bundle-builder)
        needs-selections? (< 1 (count (:initial-variants bundle-builder)))
        review?           (named-searches/eligible-for-reviews? named-search)]
    (component/create
     (when named-search
       (ui/container
        (page
         [:div
          (carousel carousel-images named-search)
          [:div.to-md-hide (component/build ugc/component (assoc ugc :container-id "ugcDesktop") opts)]]
         [:div
          [:div.center
           (title (:long-name named-search))
           (when review? (reviews-summary reviews opts))
           [:meta {:item-prop "image" :content (first carousel-images)}]
           (full-bleed-narrow (carousel carousel-images named-search))
           (when (and (not fetching-variants?)
                      needs-selections?)
             (starting-at (:initial-variants bundle-builder)))]
          (if fetching-variants?
            [:div.h2.mb2 ui/spinner]
            [:div
             (when needs-selections?
               [:div.border-bottom.border-silver.border-width-2
                (for [step (bundle-builder/steps bundle-builder)]
                  (step-html step))])
             [:div schema-org-offer-props
              [:div.my2
               (if selected-variant
                 (variant-summary {:flow             (:flow bundle-builder)
                                   :variant          selected-variant
                                   :variant-quantity variant-quantity})
                 (no-variant-summary (bundle-builder/next-step bundle-builder)))]
              (when (named-searches/eligible-for-triple-bundle-discount? named-search)
                triple-bundle-upsell)
              (when selected-variant
                (add-to-bag-button adding-to-bag? selected-variant variant-quantity))
              (bagged-variants-and-checkout bagged-variants)
              (when (named-searches/is-stylist-product? named-search) shipping-and-guarantee)]])
          (named-search-description (:description named-search))
          [:div.md-up-hide.mxn2.mb3 (component/build ugc/component (assoc ugc :container-id "ugcMobile") opts)]])
        (when review? (component/build reviews/reviews-component reviews opts)))))))

(defn query [data]
  (let [named-search   (named-searches/current-named-search data)
        bundle-builder (get-in data keypaths/bundle-builder)]
    {:named-search       named-search
     :bundle-builder     bundle-builder
     :fetching-variants? (not (named-searches/products-loaded? data named-search))
     :variant-quantity   (get-in data keypaths/browse-variant-quantity)
     :adding-to-bag?     (utils/requesting? data request-keys/add-to-bag)
     :bagged-variants    (get-in data keypaths/browse-recently-added-variants)
     :reviews            (reviews/query data)
     :ugc                (ugc/query data)}))

(defn built-component [data opts]
  (component/build component (query data) opts))
