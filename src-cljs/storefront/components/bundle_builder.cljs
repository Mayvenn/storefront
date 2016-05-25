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
            [storefront.components.carousel :as carousel]))

(defn option-html [later-step?
                   {:keys [option-name price-delta checked? sold-out? selections]}]
  [:label.border.border-silver.p1.block.center
   {:style {:height "150px"}
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
   [:.h3.titleize option-name]
   [:.h6.line-height-2
    (if sold-out?
      "Sold Out"
      [:span {:class (when-not checked? "navy")}
       "+" (as-money-without-cents price-delta)])]])

(defn step-html [{:keys [step-name later-step? options]}]
  [:.my2 {:key step-name}
   [:.navy.h4.medium.shout (name step-name)]
   [:.clearfix.mxnp3
    (for [{:keys [option-name] :as option} options]
      [:.col.pp3 {:key   (string/replace (str option-name step-name) #"\W+" "-")
                  :class (if (#{:length} step-name) "col-4" "col-6")}
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
   [:h3.regular.h2.extra-light "Summary"]
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
   (as-money (:price variant))))

(defn taxon-description [{:keys [colors weights materials commentary]}]
  [:.border.border-light-gray.p2
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

(defn starting-at-price [variants]
  (when-let [cheapest-price (bundle-builder/min-price variants)]
    [:.center.mt1
     [:.silver.h5 "Starting at"]
     [:.dark-gray.h1.extra-light
      (as-money-without-cents cheapest-price)]]))

(def checkout-button
  (html
   [:.cart-button ; for scrolling
    (ui/button "Check out" events/navigate-cart)]))

(defn add-to-bag-button [adding-to-bag?]
  (ui/button
   "Add to bag"
   events/control-build-add-to-bag
   {:show-spinner? adding-to-bag? :color "bg-navy"}))

(defn css-url [url] (str "url(" url ")"))

(defn carousel-image [image]
  [:.bg-cover.bg-no-repeat.bg-center.col-12
   {:style {:background-image (css-url image)
            :height "31rem"}}])

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
      (ui/narrow-container
       [:.px1
        [:.center
         [:h1.regular.titleize.navy.mbp4.h2 (:name taxon)]
         [:.inline-block
          (om/build reviews/reviews-summary-component reviews)]]
        (if fetching-variants?
          [:.h1 ui/spinner]
          [:div
           [:.mxn3
            (let [items (->> carousel-images
                             (map-indexed (fn [idx image]
                                            {:id   idx
                                             :body (carousel-image image)}))
                             vec)]
              [:div
               (om/build carousel/swipe-component
                         {:items      items
                          :continuous true}
                         {:react-key (str "category-swiper-" (:slug taxon))
                          :opts {:dot-location :left}})

               [:.clearfix
                [:.col-4.pt2.m-auto (starting-at-price variants)]]])]
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
               checkout-button])]
           (taxon-description (:description taxon))])]
       (om/build reviews/reviews-component reviews))))))

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
     :carousel-images    (get-in data (conj keypaths/taxon-images (keyword (:name taxon))))}))

(defn built-component [data]
  (om/build component (query data)))
