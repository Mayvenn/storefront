(ns storefront.components.category
  (:require [storefront.platform.component-utils :as utils]
            [storefront.components.money-formatters :refer [as-money-without-cents as-money]]
            [storefront.accessors.promos :as promos]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.accessors.products :as products]
            [storefront.accessors.orders :as orders]
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
  [:div.clearfix.mxn2 {:item-scope :itemscope :item-type "http://schema.org/Product"}
   [:div.col-on-tb-dt.col-7-on-tb-dt.px2 wide-left]
   [:div.col-on-tb-dt.col-5-on-tb-dt.px2 wide-right-and-narrow]])

(defn title [name]
  [:h1.h2.medium.titleize.navy {:item-prop "name"} name])

(defn full-bleed-narrow [body]
  ;; The mxn2 pairs with the p2 of the container, to make the body full width
  ;; on mobile.
  [:div.mxn2.my2 body])

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

(defn add-to-bag-button [adding-to-bag? variant quantity]
  (ui/navy-button {:on-click  (utils/send-event-callback events/control-add-to-bag
                                                         {:variant  variant
                                                          :quantity quantity})
                   :data-test "add-to-bag"
                   :spinning? adding-to-bag?}
                  "Add to bag"))

(defn ^:private number->words [n]
  (let [mapping ["Zero" "One" "Two" "Three" "Four" "Five" "Six" "Seven" "Eight" "Nine" "Ten" "Eleven" "Twelve" "Thirteen" "Fourteen" "Fifteen"]]
    (get mapping n (str "(x " n ")"))))

(defn display-bagged-variant [idx {:keys [quantity variant]}]
  [:div.h6.my1.p1.py2.caps.dark-gray.bg-light-gray.medium.center
   {:key idx
    :data-test "items-added"}
   "Added to bag: "
   (number->words quantity)
   " "
   (products/product-title variant)])

(def checkout-button
  (component/html
   [:div
    {:data-test "cart-button"
     :data-ref "cart-button"}
    (ui/teal-button (utils/route-to events/navigate-cart) "Check out")]))

(defn bagged-variants-and-checkout [bagged-variants]
  (when (seq bagged-variants)
    [:div
     (map-indexed display-bagged-variant bagged-variants)
     checkout-button]))

(defn option-html [{:keys [name image checked? sold-out? selection]}]
  [:label.btn.border-gray.p1.flex.flex-column.justify-center.items-center.container-size.letter-spacing-0
   {:data-test (str "option-" (string/replace name #"\W+" ""))
    :class (cond
             sold-out?   "bg-gray dark-gray light"
             checked?    "bg-teal white medium"
             true        "bg-white dark-gray light")
    :style {:font-size "14px" :line-height "18px"}}
   [:input.hide {:type      "radio"
                 :disabled  sold-out?
                 :checked   checked?
                 :on-change (utils/send-event-callback events/control-bundle-option-select
                                                       {:selection selection})}]
   (if image
     [:img.mbp4.content-box.circle.border-light-gray
      {:src image :alt name
       :width 30 :height 30
       :class (cond checked? "border" sold-out? "muted")}]
     [:span.block.titleize name])
   [:span.block (when sold-out? "Sold Out")]])

(defn step-html [{:keys [step-name selected-option options]}]
  [:div.my2 {:key step-name}
   [:h2.h3.clearfix.h5
    [:span.block.left.navy.medium.shout
     (name step-name)
     (when selected-option [:span.inline-block.mxp2.dark-gray " - "])]
    (when selected-option
      [:span.block.overflow-hidden.dark-gray.h5.regular
       (or (:long-name selected-option)
           [:span.titleize (:name selected-option)])])]
   [:div.flex.flex-wrap.content-stretch.mxnp3
    (for [{:keys [name] :as option} options]
      [:div.flex.flex-column.justify-center.pp3
       {:key   (string/replace (str step-name name) #"\W+" "-")
        :style {:height "72px"}
        :class (if (#{:length :color :style} step-name) "col-4" "col-6")}
       (option-html option)])]])

(defn indefinite-articalize [word]
  (let [vowel? (set "AEIOUaeiou")]
    (str (if (vowel? (first word)) "an " "a ")
         word)))

(defn variant-name [variant flow]
  (when (seq flow)
    (string/upper-case (:variant-name variant))))

(defn summary-structure [desc quantity-and-price]
  [:div
   (when (seq desc)
     [:div
      [:h2.h3.light "Summary"]
      [:div.navy desc]])
   quantity-and-price])

(defn item-price [price]
  [:span {:item-prop "price"} (as-money-without-cents price)])

(defn variant-summary [{:keys [flow
                               variant
                               variant-quantity]}]
  (let [{:keys [can_supply? price]} variant]
    (summary-structure
     (variant-name variant flow)
     (quantity-and-price-structure
      (counter-or-out-of-stock can_supply? variant-quantity)
      (item-price price)))))

(def triple-bundle-upsell
  (component/html [:p.center.h5.p2.navy promos/bundle-discount-description]))

(def shipping-and-guarantee
  (component/html
   [:div.border-top.border-bottom.border-gray.p2.my2.center.navy.shout.medium.h6
    "Free shipping & 30 day guarantee"]))

(defn named-search-description [{:keys [colors weights materials summary commentary]}]
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
          [:li.mbp3 {:key idx} item])]])
    [:div.h5.dark-gray
     (for [[idx item] (map-indexed vector commentary)]
       [:p.mt2 {:key idx} item])]]])

(defn carousel-image [image]
  (ui/aspect-ratio
   640 580
   [:img.col-12 (ui/img-attrs image :large)]))

(defn carousel [images {:keys [slug]}]
  (let [items (mapv (fn [image]
                      {:id   (subs (:large_url image) (max 0 (- (count image) 50)))
                       :body (carousel-image image)})
                    images)]
    (component/build carousel/component
                     {:slides (map :body items)
                      :settings {:dots true}}
                     {:react-key (apply str "category-swiper-" slug (interpose "-" (map :id items)))})))

(defn reviews-summary [reviews opts]
  [:div.h6
   (component/build reviews/reviews-summary-component reviews opts)])

(defn named-search-uses-product-images [named-search-slug]
  (#{"closures" "frontals" "straight" "loose-wave" "deep-wave"} named-search-slug))

(def image-types ["model" "product" "social"])

(defn sort-images [images]
  (for [image-type image-types
        {:keys [type large_url] :as image} images
        :when (and (= type image-type) large_url)]
    image))

(defn ^:private images-from-variants
  "For some named-searches, when a selection has been made, show detailed product images"
  [named-search {:keys [selections selected-variant]}]
  (sort-images (if (named-search-uses-product-images (:slug named-search))
                 (:images selected-variant)
                 (:carousel-images named-search))))

(defn component [{:keys [named-search
                         bundle-builder
                         fetching-variants?
                         variant-quantity
                         reviews
                         adding-to-bag?
                         bagged-variants
                         ugc
                         yaki-and-waterwave?]}
                 owner opts]
  (let [carousel-images   (images-from-variants named-search bundle-builder)
        needs-selections? (< 1 (count (:initial-variants bundle-builder)))
        review?           (named-searches/eligible-for-reviews? named-search)]
    (component/create
     (when named-search
       [:div.container.p2
        (page
         (full-bleed-narrow (carousel carousel-images named-search))
         [:div
          [:div.center
           (title (:long-name named-search))
           (when review? (reviews-summary reviews opts))
           [:meta {:item-prop "image" :content (first carousel-images)}]]
          (if fetching-variants?
            [:div.h2.mb2 ui/spinner]
            [:div
             (when needs-selections?
               [:div.border-bottom.border-light-gray.border-width-2
                (for [step (bundle-builder/steps bundle-builder yaki-and-waterwave?)]
                  (step-html step))])
             [:div schema-org-offer-props
              [:div.my2
               (variant-summary {:flow                  (:flow bundle-builder)
                                 :variant               (:selected-variant bundle-builder)
                                 :variant-quantity      variant-quantity})]
              (when (named-searches/eligible-for-triple-bundle-discount? named-search)
                triple-bundle-upsell)
              (add-to-bag-button adding-to-bag? (:selected-variant bundle-builder) variant-quantity)
              (bagged-variants-and-checkout bagged-variants)
              (when (named-searches/is-stylist-product? named-search) shipping-and-guarantee)]])])
        (named-search-description (:description named-search))
        [:div.mxn2.mb3 (component/build ugc/component ugc opts)]
        (when review? (component/build reviews/reviews-component reviews opts))]))))

(defn query [data]
  (let [named-search     (named-searches/current-named-search data)
        bundle-builder   (get-in data keypaths/bundle-builder)
        variant-quantity (get-in data keypaths/browse-variant-quantity)]
    {:named-search        named-search
     :bundle-builder      bundle-builder
     :variant-quantity    variant-quantity
     :fetching-variants?  (not (named-searches/products-loaded? data named-search))
     :adding-to-bag?      (utils/requesting? data request-keys/add-to-bag)
     :bagged-variants     (get-in data keypaths/browse-recently-added-variants)
     :reviews             (reviews/query data)
     :ugc                 (ugc/query data)
     :yaki-and-waterwave? (experiments/yaki-and-waterwave? data)}))

(defn built-component [data opts]
  (component/build component (query data) opts))
