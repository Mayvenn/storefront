(ns storefront.components.bundle-builder
  (:require [storefront.components.utils :as utils]
            [storefront.components.product :refer [redesigned-display-bagged-variant]]
            [storefront.components.formatters :refer [as-money-without-cents as-money]]
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
            [storefront.messages :as messages]
            [storefront.components.carousel :as carousel]
            [storefront.hooks.experiments :as experiments]))

(defn option-html [later-step?
                   {:keys [option-name price-delta checked? sold-out? selections]}]
  [:label.btn.border-silver.p1.flex.flex-column.justify-center.light
   {:style {:width "100%"
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

(defn summary-format [variant flow]
  (let [flow (conj (vec flow) :category)]
    (->> flow
         (map variant)
         (string/join " ")
         string/upper-case)))

(defn summary-structure [desc counter price]
  [:div
   [:h3.regular.h2.light "Summary"]
   [:.navy desc]
   [:.right-align.light-gray.h5 "PRICE"]
   [:.flex.h1 {:style {:min-height "1.5em"}} ; prevent slight changes to size depending on content of counter
    [:.flex-auto counter]
    [:.navy price]]
   [:.center.p2.navy promos/bundle-discount-description]])

(defn no-variant-summary [next-step]
  (summary-structure
   (str "Select " (-> next-step name string/capitalize indefinite-articalize) "!")
   ui/nbsp
   "$--.--"))

(defn variant-summary [{:keys [flow
                               variant
                               variant-quantity]}]
  (summary-structure
   (summary-format variant flow)
   (ui/counter variant-quantity
               false
               (utils/send-event-callback events/control-counter-dec
                                          {:path keypaths/browse-variant-quantity})
               (utils/send-event-callback events/control-counter-inc
                                          {:path keypaths/browse-variant-quantity}))
   (as-money-without-cents (:price variant))))

(defn taxon-description [{:keys [colors weights materials commentary]}]
  [:.border.border-light-gray.p2.rounded-1
   [:.h3.medium.navy.shout "Description"]
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
   [:.h5.dark-gray.line-height-2 (first commentary)]])

(def checkout-button
  (html
   [:.cart-button ; for scrolling
    (ui/button "Check out" (utils/route-to events/navigate-cart))]))

(defn add-to-bag-button [adding-to-bag?]
  (ui/button
   "Add to bag"
   {:on-click      (utils/send-event-callback events/control-build-add-to-bag)
    :show-spinner? adding-to-bag?
    :color         "bg-navy"}))

(defn css-url [url] (str "url(" url ")"))

(defn carousel-image [image]
  [:.bg-cover.bg-no-repeat.bg-center.col-12
   {:style {:background-image (css-url image)
            :height "31rem"}}])

(defn carousel [carousel-images {:keys [slug]}]
  (let [items (->> carousel-images
                   (map-indexed (fn [idx image]
                                  {:id   (.substring image (max 0 (- (.-length image) 50)))
                                   :body (carousel-image image)}))
                   vec)]
    ;; The mxn2 pairs with the p2 of the ui/container, to make the carousel full
    ;; width on mobile. On desktop, we don't need the full-width effect, hence
    ;; the md-m0.
    [:.mxn2.md-m0
     (om/build carousel/swipe-component
               {:items      items
                :continuous true}
               {:react-key (apply str "category-swiper-" slug (interpose "-" (map :id items)))
                :opts      {:dot-location :left}})]))

(defn taxon-title [taxon]
  [:h1.medium.titleize.navy.h2.line-height-2
   (:name taxon)
   ;; TODO: if experiments/product-page-redesign? succeeds, put the word "Hair"
   ;; into cellar.
   (when-not (taxons/is-closure? taxon) " Hair")])

(defn starting-at [variants]
  (when-let [cheapest-price (bundle-builder/min-price variants)]
    [:.center
     [:.silver.f5 "Starting at"]
     [:.dark-gray.f1.light
      (as-money-without-cents cheapest-price)]]))

(defn reviews-summary [reviews]
  [:.inline-block.h5
   (om/build reviews/reviews-summary-component reviews)])

(defn component [{:keys [taxon
                         variants
                         fetching-variants?
                         selected-options
                         flow
                         variant
                         variant-quantity
                         reviews
                         adding-to-bag?
                         bagged-variants
                         carousel-images]}
                 owner]
  (om/component
   (html
    (when taxon
      (ui/container
       [:.clearfix.mxn2.md-mx0
        [:.md-col.md-col-7.px2
         [:.to-md-hide (carousel carousel-images taxon)]]
        [:.md-col.md-col-5.px2
         [:.center
          (taxon-title taxon)
          (reviews-summary reviews)
          [:.md-up-hide.my2 (carousel carousel-images taxon)]
          (when-not fetching-variants?
            (starting-at variants))]
         (if fetching-variants?
           [:.h1.mb2 ui/spinner]
           [:div
            (for [step (bundle-builder/steps flow
                                             (:product_facets taxon)
                                             selected-options
                                             variants)]
              (step-html step))
            [:.py2.border-top.border-dark-white.border-width-2
             (if variant
               (variant-summary {:flow             flow
                                 :variant          variant
                                 :variant-quantity variant-quantity})
               (no-variant-summary (bundle-builder/next-step flow selected-options)))
             (when variant
               (add-to-bag-button adding-to-bag?))
             (when (seq bagged-variants)
               [:div
                (map-indexed redesigned-display-bagged-variant bagged-variants)
                checkout-button])]])
         (taxon-description (:description taxon))]]
       (om/build reviews/reviews-component reviews))))))

(defn images-from-variants [data]
  (let [taxon (taxons/current-taxon data)
        variants (products/selected-variants data)]
    (if (and (#{"blonde" "closures" "frontals"} (:name taxon)) (seq variants))
      (vec (set (map #(get-in % [:images 0 :large_url]) variants)))
      (get-in data (conj keypaths/taxon-images (keyword (:name taxon)))))))

(defn query [data]
  (let [taxon (taxons/current-taxon data)]
    {:taxon              taxon
     :variants           (products/current-taxon-variants data)
     :fetching-variants? (utils/requesting? data (conj request-keys/get-products (:slug taxon)))
     :selected-options   (get-in data keypaths/bundle-builder-selected-options)
     :flow               (bundle-builder/selection-flow taxon)
     :variant            (products/selected-variant data)
     :variant-quantity   (get-in data keypaths/browse-variant-quantity)
     :adding-to-bag?     (utils/requesting? data request-keys/add-to-bag)
     :bagged-variants    (get-in data keypaths/browse-recently-added-variants)
     :reviews            (reviews/query data)
     :carousel-images    (images-from-variants data)}))

(defn built-component [data]
  (om/build component (query data)))
