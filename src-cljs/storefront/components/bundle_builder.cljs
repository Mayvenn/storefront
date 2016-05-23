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

(defn format-step-name [step-name]
  (when step-name
    (let [step-name (name step-name)
          vowel? (set "AEIOUaeiou")]
      (str (if (vowel? (first step-name)) "an " "a ")
           (string/capitalize step-name)))))

(defn option-selection-event [step-name selected-options selected-variants]
  ;; FIXME: events/control-bundle-option-select really only needs
  ;; selected-options. Denormalizing the rest of this is convenient, but means
  ;; threading extraneous data. From selected-options it's easy to calculate the
  ;; selected-variants, and with the flow, one could calculate the "but-last"
  ;; options.
  (utils/send-event-callback events/control-bundle-option-select
                             {:step-name step-name
                              :selected-options selected-options
                              :selected-variants selected-variants}))

(defn option-html [{:keys [step-name step-variants later-step?]}
                   {:keys [option-name price-delta checked? sold-out? selections]}]
  [:label.border.border-silver.p1.block.center
   {:class (cond
             sold-out?   "bg-silver gray"
             later-step? "bg-light-silver muted"
             checked?    "bg-green white"
             true        "bg-white gray")}
   [:input.hide {:type      "radio"
                 :disabled  (or later-step? sold-out?)
                 :checked   checked?
                 :on-change (option-selection-event step-name
                                                    selections
                                                    (products/filter-variants-by-selections selections step-variants))}]
   [:.h3.titleize option-name]
   [:.h6.line-height-2
    (if sold-out?
      "Sold Out"
      [:span {:class (when-not checked? "navy")}
       "+" (as-money-without-cents price-delta)])]])

(defn step-html [{:keys [step-name options] :as step}]
  [:.my2 {:key step-name}
   [:h2.regular.navy.center.h4.shout (name step-name)]
   [:.clearfix.mxnp3
    (for [{:keys [option-name] :as option} options]
      [:.col.pp3 {:key   (string/replace (str option-name step-name) #"\W+" "-")
                  :class (if (#{:length} step-name) "col-4" "col-6")}
       (option-html step option)])]])

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

(defn no-variant-summary [{:keys [flow selected-options]}]
  (summary-structure
   (str "Select " (format-step-name (bundle-builder/next-step flow selected-options)) "!")
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
                      ["Base" materials]]
                     (filter second))
          size (str "col-" (/ 12 (count attrs)))]
      (for [[title value] attrs]
        [:.col {:class size
                :key title}
         [:.dark-gray.shout.h5 title]
         [:.h3.navy.medium value]]))]
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

(defn carousel-circles [items selected handler]
  (for [item items]
    [:.p1.col.pointer {:key (:id item) :on-click (fn [_] (handler item))}
     [:.border.border-light-gray.circle
      {:class (when (= selected (:id item)) "bg-light-gray")
       :style {:width "8px" :height "8px"}}]]))

(defn css-url [url] (str "url(" url ")"))

(defn carousel-image [image]
  [:.bg-cover.bg-no-repeat.bg-center.col-12
   {:style {:background-image (css-url image)
            :height "200px"}}])

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
                         carousel-images
                         carousel-index]}
                 owner]
  (om/component
   (html
    (when taxon
      (ui/narrow-container
       [:.px1
        [:.center
         [:h1.regular.titleize.navy.mt1.h2 (:name taxon)]
         [:.inline-block {:key (:slug taxon)}
          (om/build reviews/reviews-summary-component reviews)]]
        (if fetching-variants?
          [:.h1 ui/spinner]
          [:div
           (let [items (->> carousel-images
                            (map-indexed (fn [idx image]
                                           {:id idx
                                            :body (carousel-image image)}))
                            vec)
                 selected (or carousel-index 0)
                 handler (fn [item]
                           (messages/handle-message events/control-carousel-move
                                                    {:index (:id item)}))]
             [:div
              (om/build carousel/swipe-component
                        {:selected-index selected
                         :items items}
                        {:opts {:handler handler}})

              [:.clearfix
               [:.col.col-4 (carousel-circles items selected handler)]
               [:.col.col-4 (starting-at-price variants)]]])
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
              (no-variant-summary {:flow             flow
                                   :selected-options selected-options}))
            (when variant
              (add-to-bag-button adding-to-bag?))
            (when-let [bagged-variants (seq bagged-variants)]
              [:div
               (map-indexed redesigned-display-bagged-variant bagged-variants)
               checkout-button])]
           (taxon-description (:description taxon))])]
       [:div {:key (:slug taxon)}
        (om/build reviews/reviews-component reviews)])))))

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
     :carousel-images    (get-in data (conj keypaths/taxon-images (keyword (:name taxon))))
     :carousel-index     (get-in data keypaths/bundle-builder-carousel-index)}))

(defn built-component [data]
  (om/build component (query data)))
