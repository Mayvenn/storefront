(ns storefront.components.bundle-builder
  (:require [storefront.components.utils :as utils]
            [storefront.components.product :as product]
            [storefront.components.formatters :refer [as-money-without-cents]]
            [storefront.accessors.products :as products]
            [storefront.accessors.promos :as promos]
            [storefront.accessors.taxons :as taxons]
            [storefront.components.reviews :as reviews]
            [storefront.components.ui :as ui]
            [clojure.string :as string]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.accessors.bundle-builder :as bundle-builder]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.components.stylist-kit :as stylist-kit]
            [storefront.components.carousel :as carousel]))

(defn option-html [later-step?
                   {:keys [option-name price-delta checked? sold-out? selections]}]
  [:label.btn.btn-outline.border-silver.p1.flex.flex-column.justify-center.light
   {:data-test (str "option-" (string/replace option-name #"\W+" ""))
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
   [:.f2.titleize option-name]
   [:.f4.line-height-2
    (if sold-out?
      "Sold Out"
      [:span {:class (when-not checked? "navy")}
       "+" (as-money-without-cents price-delta)])]])

(defn step-html [{:keys [step-name later-step? options]}]
  [:.my2 {:key step-name}
   [:.navy.f3.medium.shout (name step-name)]
   [:.flex.flex-wrap.content-stretch.mxnp3
    (for [{:keys [option-name] :as option} options]
      [:.flex.flex-column.justify-center.pp3
       {:key   (string/replace (str option-name step-name) #"\W+" "-")
        :style {:height "72px"}
        :class (case step-name
                 :length "col-4"
                 "col-6")}
       (option-html later-step? option)])]])

(defn indefinite-articalize [word]
  (let [vowel? (set "AEIOUaeiou")]
    (str (if (vowel? (first word)) "an " "a ")
         word)))

(defn variant-name [variant flow]
  (let [flow (conj (vec flow) :category)]
    (->> flow
         (map variant)
         (string/join " ")
         string/upper-case)))

(defn summary-structure [desc quantity-and-price]
  [:div
   [:h3.regular.h2.light "Summary"]
   [:.navy desc]
   quantity-and-price])

(defn no-variant-summary [next-step]
  (summary-structure
   (str "Select " (-> next-step name string/capitalize indefinite-articalize) "!")
   (product/quantity-and-price-structure ui/nbsp "$--.--")))

(defn variant-summary [{:keys [flow
                               variant
                               variant-quantity]}]
  (summary-structure
   (variant-name variant flow)
   (product/counter-and-price variant variant-quantity)))

(def triple-bundle-upsell
  (html [:.center.p2.navy promos/bundle-discount-description]))

(defn taxon-description [{:keys [colors weights materials commentary]}]
  (product/description-structure
   [:div
    [:.clearfix.my2
     (let [attrs (->> [["Color" colors]
                       ["Weight" weights]
                       ["Material" materials]]
                      (filter second))
           size (str "col-" (/ 12 (count attrs)))]
       (for [[title value] attrs]
         [:.col {:class size
                 :key title}
          [:.dark-gray.shout.h5 title]
          [:.h4.navy.medium.pr2 value]]))]
    [:.h5.dark-gray.line-height-2 (first commentary)]]))

(defn css-url [url] (str "url(" url ")"))

(defn carousel-image [image]
  [:.bg-cover.bg-no-repeat.bg-center.col-12
   {:style {:background-image (css-url image)
            :height "31rem"}}])

(defn carousel [images {:keys [slug]}]
  (let [items (->> images
                   (map-indexed (fn [idx image]
                                  {:id   (.substring image (max 0 (- (.-length image) 50)))
                                   :body (carousel-image image)}))
                   vec)]
    (om/build carousel/swipe-component
              {:items      items
               :continuous true}
              {:react-key (apply str "category-swiper-" slug (interpose "-" (map :id items)))
               :opts      {:dot-location :left}})))

(defn taxon-title [taxon]
  (product/title
   (str
    (:name taxon)
    ;; TODO: if experiments/product-page-redesign? succeeds, put the word "Hair"
    ;; into cellar.
    (when-not (taxons/is-closure? taxon) " Hair"))))

(defn starting-at [variants]
  (when-let [cheapest-price (bundle-builder/min-price variants)]
    [:.center
     [:.silver.f5 "Starting at"]
     [:.dark-gray.f1.light
      {:item-prop "price"}
      (as-money-without-cents cheapest-price)]]))

(defn reviews-summary [reviews]
  [:.inline-block.h5
   (om/build reviews/reviews-summary-component reviews)])

(defn component [{:keys [taxon
                         variants
                         fetching-variants?
                         selected-options
                         flow
                         selected-product
                         selected-variant
                         variant-quantity
                         reviews
                         adding-to-bag?
                         carousel-images
                         bagged-variants]}
                 owner]
  (om/component
   (html
    (when taxon
      (ui/container
       (product/page
        (carousel carousel-images taxon)
        [:div
         [:.center
          (taxon-title taxon)
          (reviews-summary reviews)
          [:meta {:item-prop "image" :content (first carousel-images)}]
          (product/full-bleed-narrow (carousel carousel-images taxon))
          (when-not fetching-variants? (starting-at variants))]
         (if fetching-variants?
           [:.h1.mb2 ui/spinner]
           [:div
            (for [step (bundle-builder/steps flow
                                             (:product_facets taxon)
                                             selected-options
                                             variants)]
              (step-html step))
            [:.py2.border-top.border-dark-white.border-width-2
             product/schema-org-offer-props
             (if selected-variant
               (variant-summary {:flow             flow
                                 :variant          selected-variant
                                 :variant-quantity variant-quantity})
               (no-variant-summary (bundle-builder/next-step flow selected-options)))
             triple-bundle-upsell
             (when selected-variant
               (product/add-to-bag-button adding-to-bag? selected-product selected-variant variant-quantity))
             (product/bagged-variants-and-checkout bagged-variants)]])
         (taxon-description (:description taxon))])
       (om/build reviews/reviews-component reviews))))))

(defn images-from-variants [data]
  (let [taxon (taxons/current-taxon data)
        variants (products/selected-variants data)]
    (if (and (#{"blonde" "closures" "frontals"} (:name taxon)) (seq variants))
      (vec (set (map #(get-in % [:images 0 :large_url]) variants)))
      (:images taxon))))

(defn query [data]
  (let [taxon (taxons/current-taxon data)]
    {:taxon              taxon
     :variants           (products/current-taxon-variants data)
     :fetching-variants? (utils/requesting? data (conj request-keys/get-products (:slug taxon)))
     :selected-options   (get-in data keypaths/bundle-builder-selected-options)
     :flow               (bundle-builder/selection-flow taxon)
     :selected-product   (products/selected-product data)
     :selected-variant   (products/selected-variant data)
     :variant-quantity   (get-in data keypaths/browse-variant-quantity)
     :adding-to-bag?     (utils/requesting? data request-keys/add-to-bag)
     :bagged-variants    (get-in data keypaths/browse-recently-added-variants)
     :reviews            (reviews/query data)
     :carousel-images    (images-from-variants data)}))

(defn built-component [data]
  (om/build component (query data)))

(defn category-component [data owner]
  (om/component
   (html
    (if (bundle-builder/included-taxon? (taxons/current-taxon data))
      (built-component data)
      (stylist-kit/built-component data)))))
