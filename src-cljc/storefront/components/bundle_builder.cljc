(ns storefront.components.bundle-builder
  (:require [storefront.accessors.bundle-builder :as bundle-builder]
            [storefront.platform.component-utils :as utils]
            [storefront.components.product :as product]
            [storefront.components.money-formatters :refer [as-money-without-cents]]
            [storefront.accessors.promos :as promos]
            [storefront.accessors.taxons :as taxons]
            [storefront.accessors.bundle-builder :as bundle-builder]
            [storefront.platform.reviews :as reviews]
            [storefront.components.ui :as ui]
            [clojure.string :as string]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.components.stylist-kit :as stylist-kit]
            [storefront.platform.carousel :as carousel]))

(defn option-html [later-step?
                   {:keys [option-name price-delta checked? sold-out? selections]}]
  [:label.btn.border-silver.p1.flex.flex-column.justify-center.light
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
   [:div.f2.titleize option-name]
   [:div.f4.line-height-2
    (if sold-out?
      "Sold Out"
      [:span {:class (when-not checked? "navy")}
       "+" (as-money-without-cents price-delta)])]])

(defn step-html [{:keys [step-name later-step? options]}]
  [:div.my2 {:key step-name}
   [:div.navy.f3.medium.shout (name step-name)]
   [:div.flex.flex-wrap.content-stretch.mxnp3
    (for [{:keys [option-name] :as option} options]
      [:div.flex.flex-column.justify-center.pp3
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
   [:div.navy desc]
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
  (component/html [:div.center.p2.navy promos/bundle-discount-description]))

(defn taxon-description [{:keys [colors weights materials commentary]}]
  (product/description-structure
   [:div
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
          [:div.h4.navy.medium.pr2 value]]))]
    [:div.h5.dark-gray.line-height-2 (first commentary)]]))

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

(defn taxon-title [taxon]
  (product/title (str (:name taxon) (when-not (taxons/is-closure? taxon) " Hair"))))

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
                 owner opts]
  (component/create
   (when taxon
     (ui/container
      (product/page
       (carousel carousel-images taxon)
       [:div
        [:div.center
         (taxon-title taxon)
         (reviews-summary reviews opts)
         [:meta {:item-prop "image" :content (first carousel-images)}]
         (product/full-bleed-narrow (carousel carousel-images taxon))
         (when-not fetching-variants? (starting-at variants))]
        (if fetching-variants?
          [:div.h1.mb2 ui/spinner]
          [:div
           (for [step (bundle-builder/steps flow
                                            (:product_facets taxon)
                                            selected-options
                                            variants)]
             (step-html step))
           [:div.py2.border-top.border-dark-white.border-width-2
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
      (component/build reviews/reviews-component reviews opts)))))

(defn images-from-variants [data]
  (let [taxon (taxons/current-taxon data)
        variants (bundle-builder/selected-variants data)]
    (if (and (#{"blonde" "closures" "frontals"} (:name taxon)) (seq variants))
      (vec (set (map #(get-in % [:images 0 :large_url]) variants)))
      (:images taxon))))

(defn query [data]
  (let [taxon (taxons/current-taxon data)]
    {:taxon              taxon
     :variants           (bundle-builder/current-taxon-variants data)
     :fetching-variants? (utils/requesting? data (conj request-keys/get-products (:slug taxon)))
     :selected-options   (get-in data keypaths/bundle-builder-selected-options)
     :flow               (bundle-builder/selection-flow taxon)
     :selected-product   (bundle-builder/selected-product data)
     :selected-variant   (bundle-builder/selected-variant data)
     :variant-quantity   (get-in data keypaths/browse-variant-quantity)
     :adding-to-bag?     (utils/requesting? data request-keys/add-to-bag)
     :bagged-variants    (get-in data keypaths/browse-recently-added-variants)
     :reviews            (reviews/query data)
     :carousel-images    (images-from-variants data)}))

(defn built-component [data opts]
  (component/build component (query data) opts))

(defn category-component [data owner opts]
  (component/create
   (if (bundle-builder/included-taxon? (taxons/current-taxon data))
     (built-component data opts)
     (stylist-kit/built-component data opts))))
