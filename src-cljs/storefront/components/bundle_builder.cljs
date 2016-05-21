(ns storefront.components.bundle-builder
  (:require [storefront.components.utils :as utils]
            [storefront.components.product :refer [redesigned-display-bagged-variant]]
            [storefront.components.formatters :refer [as-money-without-cents as-money]]
            [storefront.accessors.products :as products]
            [storefront.accessors.promos :as promos]
            [storefront.accessors.taxons :refer [filter-nav-taxons] :as taxons]
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
            [storefront.utils.sequences :refer [update-vals]]
            [swipe :as swipe]
            [storefront.components.carousel :as carousel]))

(defn format-step-name [step-name]
  (when step-name
    (let [step-name (name step-name)
          vowel? (set "AEIOUaeiou")]
      (str (if (vowel? (first step-name)) "an " "a ")
           (string/capitalize step-name)))))

(defn next-step [flow selected-options]
  (let [selected-set (set (keys selected-options))]
    (first (drop-while selected-set flow))))

(defn option-selection-event [step-name selected-options selected-variants]
  (utils/send-event-callback events/control-bundle-option-select
                             {:step-name step-name
                              :selected-options selected-options
                              :selected-variants selected-variants}))

(defn min-price [variants]
  (when (seq variants)
    (->> variants
         (map :price)
         (apply min))))

(defn build-options-for-step [{:keys [all-selections variants step-name dependent-steps option-names]}]
  (let [prior-selections (select-keys all-selections dependent-steps)
        later-step?      (> (count dependent-steps) (count all-selections))
        step-variants    (products/filter-variants-by-selections prior-selections variants)
        step-min-price   (min-price step-variants)]
    (for [option-name option-names
          :let        [option-variants (products/filter-variants-by-selections {step-name option-name} step-variants)]
          :when       (not (empty? option-variants))]
      (let [option-min-price (min-price option-variants)
            sold-out?        (every? :sold-out? option-variants)]
        {:option-id   (string/replace (str option-name step-name) #"\W+" "-")
         :option-name option-name
         :price-delta (- option-min-price step-min-price)
         :disabled?   (or later-step? sold-out?)
         :checked?    (= (get all-selections step-name nil) option-name)
         :sold-out?   sold-out?
         :on-change   (option-selection-event step-name
                                              (assoc prior-selections step-name option-name)
                                              option-variants)}))))

(defn build-steps
  "We are going to build the steps of the bundle builder. A step is an index,
  name and vector of options.
  E.g., 1, Material, Lace/Silk ->
  Step 2. Choose Material: Lace or Silk

  The options are hardest to generate because they have to take into
  consideration where in the flow the step appears, the list of variants in
  play, and what the user has selected so far."
  [flow step->option-names all-selections variants]
  (map (fn [idx step dependent-steps]
         {:name    step
          :index   idx
          :options (build-options-for-step {:all-selections  all-selections
                                            :variants        variants
                                            :step-name       step
                                            :dependent-steps dependent-steps
                                            :option-names    (step->option-names step)})})
       (range)
       flow
       (reductions conj [] flow)))

(defn step-html [{:keys [index options] step-name :name}]
  [:.my2 {:key step-name}
   [:h2.regular.navy.center.h4.shout (name step-name)]
   [:.clearfix.mxnp3
    (for [{:keys [option-id option-name price-delta disabled? checked? sold-out? on-change]} options]
      [:.col.pp3 {:key   option-id
                 :class (if (#{:length} step-name) "col-4" "col-6")}
       [:input.hide {:type      "radio"
                     :id        option-id
                     :disabled  disabled?
                     :checked   checked?
                     :on-change on-change}]
       [:label.border.border-silver.p1.block.center
        {:for   option-id
         :class (cond
                  sold-out? "bg-silver gray"
                  disabled? "bg-light-silver muted"
                  checked?  "bg-green white"
                  true      "bg-white gray")}
        [:.h3.titleize option-name]
        [:.h6.line-height-2
         (if sold-out?
           "Sold Out"
           [:span {:class (when-not checked? "navy")}
            "+" (as-money-without-cents price-delta)])]]])]])

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
   (str "Select " (format-step-name (next-step flow selected-options)) "!")
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

(defn css-url [url] (str "url(" url ")"))

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
  (when-let [cheapest-price (apply min (map :price variants))]
    [:.center
     [:.silver.h5 "Starting at"]
     [:.dark-gray.h1.extra-light
      (as-money-without-cents cheapest-price)]]))

(defn component [{:keys [taxon
                         variants
                         fetching-taxon?
                         image-url
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
        (if fetching-taxon?
          [:.h1 ui/spinner]
          [:div
           (let [items (vec (map-indexed (fn [idx image] {:id idx
                                                         :body [:.bg-cover.bg-no-repeat.bg-center.col-12 {:style {:background-image (css-url image)
                                                                                                                  :height "200px"}}]})
                                         carousel-images))
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
               [:.col.col-4
                (for [item items]
                  [:.p1.col.pointer {:key (:id item) :on-click (fn [_] (handler item))}
                   [:.border.border-light-gray.circle
                    {:class (when (= selected (:id item)) "bg-light-gray")
                     :style {:width "8px" :height "8px"}}]])]
               [:.col.col-4
                (starting-at-price variants)]]])
           (for [step (build-steps flow
                                   (:product_facets taxon)
                                   selected-options
                                   variants)]
             (step-html step))
           [:.py2.border-top.border-dark-white.border-width-2
            (if-not variant
              (no-variant-summary {:flow             flow
                                   :selected-options selected-options})
              [:div
               (variant-summary {:flow             flow
                                 :variant          variant
                                 :variant-quantity variant-quantity})
               (ui/button
                "Add to bag"
                events/control-build-add-to-bag
                {:show-spinner? adding-to-bag? :color "bg-navy"})])
            (when-let [bagged-variants (seq bagged-variants)]
              [:div
               (map-indexed redesigned-display-bagged-variant bagged-variants)
               [:.cart-button ; for scrolling
                (ui/button "Check out" events/navigate-cart)]])]
           (taxon-description (:description taxon))])]
       [:div {:key (:slug taxon)}
        (om/build reviews/reviews-component reviews)])))))

(def display-product-images-for-taxon? #{"blonde" "closures" "frontals"})

(defn representative-image-url [data taxon]
  (when (display-product-images-for-taxon? (:name taxon))
    (let [products (vals (products/selected-products data))
          images (->> products
                      (map #(get-in % [:master :images 0 :large_url]))
                      set)]
      (when (= 1 (count images))
        (first images)))))

(defn selection-flow [data]
  (let [taxon-name (:name (taxons/current-taxon data))]
    (case taxon-name
      "frontals" '(:style :material :origin :length)
      "closures" '(:style :material :origin :length)
      "blonde" '(:color :origin :length)
      '(:origin :length))))

(defn query [data]
  (let [taxon (taxons/current-taxon data)]
    {:taxon            taxon
     :variants         (products/current-taxon-variants data)
     :fetching-taxon?  (utils/requesting? data (conj request-keys/get-products (:slug taxon)))
     :image-url        (representative-image-url data taxon)
     :selected-options (get-in data keypaths/bundle-builder-selected-options)
     :flow             (selection-flow data)
     :variant          (products/selected-variant data)
     :variant-quantity (get-in data keypaths/browse-variant-quantity)
     :adding-to-bag?   (utils/requesting? data request-keys/add-to-bag)
     :bagged-variants  (get-in data keypaths/browse-recently-added-variants)
     :reviews          (reviews/query data)
     :carousel-images  (get-in data (conj keypaths/taxon-images (keyword (:name taxon))))
     :carousel-index   (get-in data keypaths/bundle-builder-carousel-index)}))

(defn built-component [data]
  (om/build component (query data)))
