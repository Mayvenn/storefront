(ns storefront.components.category
  (:require [storefront.platform.component-utils :as utils]
            [storefront.components.money-formatters :refer [as-money-without-cents]]
            [storefront.accessors.promos :as promos]
            [storefront.accessors.taxons :as taxons]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.bundle-builder :as bundle-builder]
            [storefront.platform.reviews :as reviews]
            [storefront.components.ui :as ui]
            [clojure.string :as string]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.platform.carousel :as carousel]))

(defn page [wide-left wide-right-and-narrow]
  [:div.clearfix.mxn2 {:item-type "http://schema.org/Product"}
   [:div.md-up-col.md-up-col-7.px2 [:div.to-md-hide wide-left]]
   [:div.md-up-col.md-up-col-5.px2 wide-right-and-narrow]])

(defn title [name]
  [:h1.medium.titleize.navy.h2.line-height-2 {:item-prop "name"} name])

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
   [:div.right-align.light-gray.h5 "PRICE"]
   [:div.flex.h1 {:style {:min-height "1.5em"}} ; prevent slight changes to size depending on content of counter
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
    [:span.h3 "Currently out of stock"]) )

(defn add-to-bag-button [adding-to-bag? product variant quantity]
  (ui/button
   "Add to bag"
   {:on-click      (utils/send-event-callback events/control-add-to-bag
                                              {:product  product
                                               :variant  variant
                                               :quantity quantity})
    :data-test     "add-to-bag"
    :show-spinner? adding-to-bag?
    :color         "bg-navy"}))

(defn ^:private number->words [n]
  (let [mapping ["Zero" "One" "Two" "Three" "Four" "Five" "Six" "Seven" "Eight" "Nine" "Ten" "Eleven" "Twelve" "Thirteen" "Fourteen" "Fifteen"]]
    (get mapping n (str "(x " n ")"))))

(defn display-bagged-variant [idx {:keys [quantity product variant]}]
  [:div.h6.line-height-3.my1.p1.caps.gray.bg-dark-white.medium.center
   {:key idx
    :data-test "items-added"}
   "Added to bag: "
   (number->words quantity)
   " "
   (some-> variant :variant_attrs :length)
   " "
   ;; TODO: could this be products/summary? (it only needs a variant)
   (:name product)])

(def checkout-button
  (component/html
   [:div
    {:data-test "cart-button"
     :data-ref "cart-button"}
    (ui/button "Check out" (utils/route-to events/navigate-cart))]))

(defn bagged-variants-and-checkout [bagged-variants]
  (when (seq bagged-variants)
    [:div
     (map-indexed display-bagged-variant bagged-variants)
     checkout-button]))

(defn option-html [later-step?
                   {:keys [name image price-delta checked? sold-out? selections]}
                   color-option?]
  [:label.btn.border-silver.p1.flex.flex-column.justify-center.items-center.light
   {:data-test (str "option-" (string/replace name #"\W+" ""))
    :style {:width "100%"
            :height "100%"}
    :class (cond
             sold-out?   "bg-silver gray"
             later-step? "bg-light-silver muted"
             checked?    "bg-green white"
             true        "bg-white gray")}
   [:input.hide {:type      "radio"
                 :disabled  (or later-step? sold-out?)
                 :checked   checked?
                 :on-change (utils/send-event-callback events/control-bundle-option-select
                                                       {:selected-options selections})}]
   (if (and color-option? image)
     [:img.mbp4.content-box.circle.border-white
      {:src image :width 30 :height 30 :class (cond checked? "border"
                                                    sold-out? "muted")}]
     [:div.f2.titleize name])
   [:div.f4.line-height-2
    (if sold-out?
      "Sold Out"
      [:span {:class (when-not checked? "navy")}
       "+" (as-money-without-cents price-delta)])]])

(defn step-html [{:keys [step-name selected-option later-step? options]} color-option?]
  (let [selected? (and color-option? selected-option)]
    [:div.my2 {:key step-name}
     [:div.clearfix.f3
      [:div.left.navy.medium.shout
       (name step-name)
       (when selected? [:span.inline-block.mxp2.light-gray " - "])]
      (when selected?
        [:div.overflow-hidden.light-gray
         (or (:long-name selected-option)
             [:span.titleize (:name selected-option)])])]
     [:div.flex.flex-wrap.content-stretch.mxnp3
      (for [{:keys [name] :as option} options]
        [:div.flex.flex-column.justify-center.pp3
         {:key   (string/replace (str name step-name) #"\W+" "-")
          :style {:height "72px"}
          :class (cond
                   (= :length step-name) "col-4"
                   (and color-option? (= :color step-name)) "col-4"
                   :else "col-6")}
         (option-html later-step? option color-option?)])]]))

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
      [:h3.regular.h2.light "Summary"]
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
  (component/html [:div.center.p2.navy promos/bundle-discount-description]))

(def shipping-and-guarantee
  (component/html
   [:div.border-top.border-bottom.border-light-silver.p2.my2.center.navy.shout.medium.h5
    "Free shipping & 30 day guarantee"]))

(defn taxon-description [{:keys [colors weights materials summary commentary]}]
  [:div.border.border-light-gray.mt2.p2.rounded
   [:div.h3.medium.navy.shout "Description"]
   [:div {:item-prop "description"}
    (when (or colors weights materials)
      [:div.clearfix.my2
       (let [attrs (->> [["Color" colors]
                         ["Weight" weights]
                         ["Material" materials]]
                        (filter second))
             size (str "col-" (/ 12 (count attrs)))]
         (for [[title value] attrs]
           [:div.col {:class size
                      :key title}
            [:div.dark-gray.shout.h5 title]
            [:div.h4.navy.medium.pr2 value]]))])
    (when (seq summary)
      [:div.my2
       [:div.mbp3.h6 "Includes:"]
       [:ul.list-reset.m0.mb2.navy.h5.medium
        (for [[idx item] (map-indexed vector summary)]
          [:li.mbp3 {:key idx} item])]])
    [:div.h5.dark-gray.line-height-2
     (for [[idx item] (map-indexed vector commentary)]
       [:p.mt2 {:key idx} item])]]])

(defn css-url [url] (str "url(" url ")"))

(defn carousel-image [image]
  [:div.bg-cover.bg-no-repeat.bg-center.col-12
   {:style {:background-image (css-url image)
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
     [:div.silver.f5 "Starting at"]
     [:div.dark-gray.f1.light
      {:item-prop "price"}
      (as-money-without-cents cheapest-price)]]))

(defn reviews-summary [reviews opts]
  [:div.inline-block.h5
   (component/build reviews/reviews-summary-component reviews opts)])

(defn images-from-variants
  "For some taxons, when a selection has been made, show detailed product images"
  [taxon {:keys [selected-options selected-variants]}]
  (if (and (#{"blonde" "closures" "frontals"} (:slug taxon))
           (seq selected-options))
    (let [sorted-variants (reverse (sort-by #(-> % :variant_attrs :style) selected-variants))]
      (vec (distinct (map #(get-in % [:images 0 :large_url]) sorted-variants))))
    (:images taxon)))

(defn component [{:keys [taxon
                         bundle-builder
                         fetching-variants?
                         selected-product
                         variant-quantity
                         reviews
                         adding-to-bag?
                         color-option?
                         bagged-variants]}
                 owner opts]
  (let [selected-variant  (bundle-builder/selected-variant bundle-builder)
        carousel-images   (images-from-variants taxon bundle-builder)
        needs-selections? (< 1 (count (:initial-variants bundle-builder)))
        review?           (taxons/eligible-for-reviews? taxon)]
    (component/create
     (when taxon
       (ui/container
        (page
         (carousel carousel-images taxon)
         [:div
          [:div.center
           (title (:long-name taxon))
           (when review? (reviews-summary reviews opts))
           [:meta {:item-prop "image" :content (first carousel-images)}]
           (full-bleed-narrow (carousel carousel-images taxon))
           (when (and (not fetching-variants?)
                      needs-selections?)
             (starting-at (:initial-variants bundle-builder)))]
          (if fetching-variants?
            [:div.h1.mb2 ui/spinner]
            [:div
             (when needs-selections?
               [:div.border-bottom.border-dark-white.border-width-2
                (for [step (bundle-builder/steps bundle-builder)]
                  (step-html step color-option?))])
             [:div schema-org-offer-props
              [:div.my2
               (if selected-variant
                 (variant-summary {:flow             (:flow bundle-builder)
                                   :variant          selected-variant
                                   :variant-quantity variant-quantity})
                 (no-variant-summary (bundle-builder/next-step bundle-builder)))]
              (when (taxons/eligible-for-triple-bundle-discount? taxon)
                triple-bundle-upsell)
              (when selected-variant
                (add-to-bag-button adding-to-bag? selected-product selected-variant variant-quantity))
              (bagged-variants-and-checkout bagged-variants)
              (when (taxons/is-stylist-product? taxon) shipping-and-guarantee)]])
          (taxon-description (:description taxon))])
        (when review? (component/build reviews/reviews-component reviews opts)))))))

(defn query [data]
  (let [taxon          (taxons/current-taxon data)
        bundle-builder (get-in data keypaths/bundle-builder)]
    {:taxon              taxon
     :bundle-builder     bundle-builder
     :fetching-variants? (not (taxons/products-loaded? data taxon))
     ;; TODO: can the add-to-bag button work without a selected-product?
     :selected-product   (bundle-builder/selected-product bundle-builder (get-in data keypaths/products))
     :variant-quantity   (get-in data keypaths/browse-variant-quantity)
     :adding-to-bag?     (utils/requesting? data request-keys/add-to-bag)
     :color-option?      (experiments/color-option? data)
     :bagged-variants    (get-in data keypaths/browse-recently-added-variants)
     :reviews            (reviews/query data)}))

(defn built-component [data opts]
  (component/build component (query data) opts))

(defn category-component [data owner opts]
  (component/create (built-component data opts)))
