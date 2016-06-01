(ns storefront.components.category
  (:require [storefront.components.utils :as utils]
            [storefront.components.product :refer [display-bagged-variant]]
            [storefront.components.formatters :refer [as-money-without-cents as-money]]
            [storefront.accessors.products :as products]
            [storefront.accessors.promos :as promos]
            [storefront.accessors.taxons :as taxons]
            [storefront.components.reviews :as reviews]
            [storefront.components.counter :refer [counter-component]]
            [storefront.components.carousel :refer [carousel-component]]
            [storefront.components.bundle-builder :as bundle-builder]
            [storefront.hooks.experiments :as experiments]
            [clojure.string :as string]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.accessors.bundle-builder :as accessors.bundle-builder]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]))

(defn display-product [product]
  [:a (utils/route-to events/navigate-product
                      {:product-slug (:slug product)})
   [:div.taxon-product-container
    (when-let [medium-image (->> product
                                 :master
                                 :images
                                 first
                                 :product_url)]
      [:div.taxon-product-image-container
       {:style {:background-image (str "url('" medium-image "')")}}
       [:img {:src medium-image}]])
    [:div.taxon-product-title-container
     [:div.taxon-product-title
      (:name product)]]]])

(defn kits-component [data owner]
  (om/component
   (html
    (when-let [taxon (taxons/current-taxon data)]
      [:div
       [:div.taxon-products-banner.stylist-products]
       [:div.taxon-products-container
        [:div.taxon-products-list-container
         (let [products (products/ordered-products-for-category data taxon)]
           (if (utils/requesting? data (conj request-keys/get-products
                                             (:slug taxon)))
             [:.spinner]
             (map display-product products)))]]]))))

;; Bundle builder below

(def display-product-images-for-taxon? #{"blonde" "closures" "frontals"})

(defn selection-flow [data]
  (let [taxon-name (:name (taxons/current-taxon data))]
    (case taxon-name
      "frontals" '(:style :material :origin :length)
      "closures" '(:style :material :origin :length)
      "blonde" '(:color :origin :length)
      '(:origin :length))))

(defn format-step-name [step-name]
  (when step-name
    (let [step-name (name step-name)
          vowel? (set "AEIOUaeiou")]
      (str (if (vowel? (first step-name)) "an " "a ")
           (string/capitalize step-name)))))

(defn next-step [flow selected-options]
  (let [selected-set (set (keys selected-options))]
    (first (drop-while selected-set flow))))

(defn option-selection-event [selected-options]
  (utils/send-event-callback events/control-bundle-option-select
                             {:selected-options selected-options}))

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
         :on-change   (option-selection-event (assoc prior-selections step-name option-name))}))))

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

(defn step-html [{:keys [name index options]}]
  [:.step {:key name}
   [:h2 (str (inc index)) ". Choose " (format-step-name name)]
   [:.options
    (for [{:keys [option-id option-name price-delta disabled? checked? sold-out? on-change]} options]
      [:.option-container {:key option-id}
       [:input {:type      "radio"
                :id        option-id
                :disabled  disabled?
                :checked   checked?
                :on-change on-change}]
       [:label.option {:for option-id :class [name (when sold-out? "sold-out")]}
        [:.option-name option-name]
        [:.subtext (cond
                     sold-out?       "Sold Out"
                     (not disabled?) [:span "+ " (as-money price-delta)])]]])]])

(defn summary-format [variant flow]
  (let [flow (conj (vec flow) :category)]
    (->> flow
         (map variant)
         (string/join " ")
         string/upper-case)))

(defn add-to-bag-button [data]
  (let [saving (utils/requesting? data request-keys/add-to-bag)]
    [:button.large.primary.alternate#add-to-cart-button
     {:on-click (when-not saving
                  (utils/send-event-callback events/control-build-add-to-bag))
      :class (when saving "saving")}
     "ADD TO CART"]))

(def bundle-promotion-notice [:em.bundle-discount-callout promos/bundle-discount-description])

(defn summary-section [data]
  (let [flow (selection-flow data)]
    (if-let [variant (products/selected-variant data)]
      [:.selected
       [:.line-item-summary (summary-format variant flow)]
       (om/build counter-component data {:opts {:path keypaths/browse-variant-quantity
                                                :inc-event events/control-counter-inc
                                                :dec-event events/control-counter-dec
                                                :set-event events/control-counter-set}})
       [:.price (as-money (:price variant))]
       bundle-promotion-notice
       (add-to-bag-button data)]
      [:.selected
       "Select "
       (format-step-name (next-step flow (get-in data keypaths/bundle-builder-selected-options)))
       "!"
       [:.price "$--.--"]
       bundle-promotion-notice])))

(defn css-url [url] (str "url(" url ")"))

(defn representative-image-url [data taxon]
  (when (display-product-images-for-taxon? (:name taxon))
    (let [products (vals (products/selected-products data))
          images (->> products
                      (map #(get-in % [:master :images 0 :large_url]))
                      set)]
      (when (= 1 (count images))
        (first images)))))

(defn category-descriptions [taxon]
  (case (:name taxon)
    "frontals"
    '("100% Human Virgin Hair"
      "13\" x 4\" size"
      "Lace Material"
      "Color 1B"
      "14\" and 18\" Length Bundles"
      "2.5 ounces")

    "closures"
    '("100% Human Virgin Hair"
      "4\" x 4\" size"
      "Silk and Lace Materials"
      "Colors: 1B and #613 Blonde"
      "14\" and 18\" Length Bundles"
      "1.2 ounces")

    "blonde"
    '("100% Human Virgin Hair"
      "Colors: #27 and #613 Blonde"
      "14\" - 26\" Length Bundles"
      "3.5 ounces")

    '("100% Human Virgin Hair"
      "Color 1B"
      "12\" - 28\" Length Bundles"
      "3.5 ounces")))

(defn starting-at-price [variants]
  (when-let [cheapest-price (apply min (map :price variants))]
    (str "Starting at " (as-money cheapest-price))))

(defn taxon-reviews-summary [data]
  [:.reviews-wrapper
   [:.reviews-inner-wrapper.mb1
    (om/build reviews/reviews-summary-component (reviews/query data))]])

(defn taxon-review-full [data]
  [:.reviews-wrapper
   (om/build reviews/reviews-component (reviews/query data))])

(defn bundle-builder-category-component [data owner]
  (om/component
   (html
    (when-let [taxon (taxons/current-taxon data)]
      (let [variants (products/current-taxon-variants data)]
        [:.bundle-builder
         [:.bundle-builder-info
          [:header.single-column.relative
           [:h1
            [:div "Select Your " (:name taxon) " Hair"]
            [:.category-header-sub "Buy now and get FREE SHIPPING"]]
           (taxon-reviews-summary data)]
          [:header.two-column.relative
           [:div.starting-at.floated (starting-at-price variants)]
           [:h1
            [:div (:name taxon) " Hair"]]
           (taxon-reviews-summary data)
           [:.category-header-sub "Buy now and get FREE SHIPPING"]]
          (if (utils/requesting? data (conj request-keys/get-products (:slug taxon)))
            [:.spinner]
            [:div
             [:.carousel
              (if-let [image-url (representative-image-url data taxon)]
                [:.hair-category-image {:style {:background-image (css-url image-url)}}]
                (om/build carousel-component data {:opts {:index-path keypaths/bundle-builder-carousel-index
                                                          :images (:images taxon)}}))]
             [:div.starting-at.centered (starting-at-price variants)]
             (for [step (build-steps (selection-flow data)
                                     (:product_facets taxon)
                                     (get-in data keypaths/bundle-builder-selected-options)
                                     variants)]
               (step-html step))
             [:#summary
              [:h3 "Summary"]
              (summary-section data)
              (when-let [bagged-variants (seq (get-in data keypaths/browse-recently-added-variants))]
                [:div#after-add {:style {:display "block"}}
                 [:div.added-to-bag-container
                  (map display-bagged-variant bagged-variants)]
                 [:div.go-to-checkout
                  [:a.cart-button (utils/route-to events/navigate-cart) "Checkout"]]])]
             (into [:ul.category-description]
                   (for [description (category-descriptions taxon)]
                     [:li description]))])]
         (taxon-review-full data)])))))

(defn category-component [data owner]
  (om/component
   (html
    (let [redesigned? (experiments/product-page-redesign? data)]
      [:div
       (if (accessors.bundle-builder/included-taxon? (taxons/current-taxon data))
         (if redesigned?
           (bundle-builder/built-component data)
           (om/build bundle-builder-category-component data))
         (om/build kits-component data))
       (when-not redesigned?
         [:div.gold-features
          [:figure.guarantee-feature]
          [:figure.free-shipping-feature]
          [:figure.triple-bundle-feature]])]))))
