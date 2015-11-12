(ns storefront.components.category
  (:require [storefront.components.utils :as utils]
            [storefront.components.product :refer [display-bagged-variant]]
            [storefront.components.formatters :refer [as-money-without-cents as-money]]
            [storefront.accessors.products :as products]
            [storefront.accessors.taxons :refer [filter-nav-taxons taxon-path-for taxon-class-name] :as taxons]
            [storefront.components.reviews :refer [reviews-component reviews-summary-component]]
            [storefront.components.counter :refer [counter-component]]
            [storefront.components.carousel :refer [carousel-component]]
            [clojure.string :as string]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.hooks.experiments :as experiments]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.utils.query :as query]
            [storefront.utils.sequences :refer [update-vals]]))

(defn display-taxon [data selected-taxon taxon]
  (let [taxon-path (taxon-path-for taxon)
        selected-class (if (= selected-taxon taxon) "selected" nil)
        taxon-classes (string/join " " (conj [taxon-path] selected-class))]
    [:div.hair-taxon.decorated.small-width {:class taxon-classes}
     [:a.taxon-link (utils/route-to data events/navigate-category {:taxon-path taxon-path})
      [:p.hair-taxon-name (:name taxon)]]]))

(defn display-product [data taxon-id product]
  (let [collection-name (:collection_name product)]
    [:a (utils/route-to data events/navigate-product
                        {:product-path (:slug product)
                         :query-params {:taxon_id taxon-id}})
     [:div.taxon-product-container
      (when-let [first-image (->> product
                                  :master
                                  :images
                                  first
                                  :product_url)]
        [:div.taxon-product-image-container
         {:style {:background-image (str "url('" first-image "')")}}
         (when (#{"ultra" "deluxe"} collection-name)
           [:div.corner-ribbon {:class collection-name}
            collection-name])
         [:img {:src first-image}]])
      [:div.taxon-product-title-container
       [:div.taxon-product-title
        (:name product)]]]]))

(defn original-category-component [data owner]
  (om/component
   (html
    (if-let [taxon (taxons/current-taxon data)]
      [:div
       [:div.taxon-products-banner {:class (taxon-class-name taxon)}]
       [:div.taxon-products-container
        (when-not (:stylist_only? taxon)
          [:div.taxon-nav
           (map (partial display-taxon data taxon)
                (filter-nav-taxons (get-in data keypaths/taxons)))
           [:div {:style {:clear "both"}}]])
        [:div.taxon-products-list-container
         (let [products (products/ordered-products-for-category data taxon)]
           (if (query/get {:request-key (concat request-keys/get-products
                                                [(taxon-path-for taxon)])}
                          (get-in data keypaths/api-requests))
             [:.spinner]
             (map (partial display-product data (:id taxon)) products)))]]

       [:div.gold-features
        [:figure.guarantee-feature]
        [:figure.free-shipping-feature]
        [:figure.triple-bundle-feature]
        [:feature.fs-feature]]]))))

;; Bundle builder below

(def display-product-images-for-taxons #{"blonde" "closures"})

(defn selection-flow [data]
  (let [taxon-name (:name (taxons/current-taxon data))]
    (case taxon-name
      "closures" '(:style :material :origin :length)
      "blonde" '(:color :origin :length)
      '(:origin :length))))

(defn format-step-name [step-name]
  (when step-name
    (let [step-name (name step-name)
          vowel? (set "AEIOUaeiou")]
      (str (if (vowel? (first step-name)) "an " "a ")
           (string/capitalize step-name)))))

(defn next-step [selection-flow selected-options]
  (let [selected-set (set (keys selected-options))]
    (first (drop-while selected-set selection-flow))))

(defn option-selection-event [data step-name selected-options selected-variants]
  (utils/send-event-callback data
                             events/control-bundle-option-select
                             {:step-name step-name
                              :selected-options selected-options
                              :selected-variants selected-variants}))

(defn min-price [variants]
  (when (seq variants)
    (->> variants
         (map :price)
         (apply min))))

(defn price-for-option [step-name option-min-price option-price-diff]
  (case step-name
    :grade    [:min-price option-min-price]
    :material [:diff-price option-price-diff]
    :origin   [:diff-price option-price-diff]
    :style    [:diff-price option-price-diff]
    :length   [:diff-price option-price-diff]
    :color    [:diff-price option-price-diff]))

(defn format-price [[type price]]
  (str ({:min-price "From " :diff-price "+ "} type) (as-money price)))

(defn build-options-for-step [data variants {:keys [step-name option-names dependent-steps]}]
  (let [all-selections   (get-in data keypaths/bundle-builder-selected-options) ;; e.g. {:grade "6a" :source "malaysia"}
        prior-selections (select-keys all-selections dependent-steps)
        step-disabled?   (> (count dependent-steps) (count all-selections))
        step-variants    (products/filter-variants-by-selections prior-selections variants)
        step-min-price   (min-price step-variants)]
    (for [option-name option-names]
      (let [option-variants  (products/filter-variants-by-selections {step-name option-name} step-variants)
            option-min-price (min-price option-variants)
            represented?     (not (empty? option-variants))
            sold-out?        (and represented?
                                  (every? :sold-out? option-variants))]
        {:option-name option-name
         :price (when (and (not step-disabled?) represented?)
                  (price-for-option step-name option-min-price (- option-min-price step-min-price)))
         :disabled (or step-disabled?
                       sold-out?
                       (not represented?))
         :represented represented?
         :checked (= (get all-selections step-name nil) option-name)
         :sold-out sold-out?
         :on-change (option-selection-event data
                                            step-name
                                            (assoc prior-selections step-name option-name)
                                            option-variants)}))))

(defn step-html [step-name idx options]
  [:.step
   [:h2 (str (inc idx)) ". Choose " (format-step-name step-name)]
   [:.options
    (for [{:keys [option-name price represented disabled checked sold-out on-change]} options]
      (when represented
        (let [option-id (string/replace (str option-name step-name) #"\W+" "-")]
          (list
           [:input {:type "radio"
                    :id option-id
                    :disabled disabled
                    :checked checked
                    :on-change on-change}]
           [:.option {:class [step-name (when sold-out "sold-out")]}
            [:.option-name option-name]
            (cond
              sold-out [:.subtext "Sold Out"]
              (seq price) [:.subtext (format-price price)])
            [:label {:for option-id}]]))))]])

(defn bundle-builder-steps [data variants steps]
  (map-indexed (fn [idx {:keys [step-name] :as step}]
                 (step-html step-name
                            idx
                            (build-options-for-step data variants step)))
               steps))

(defn summary-format [data]
  (let [variant (products/selected-variant data)
        flow (conj (vec (selection-flow data)) :category)]
    (->> flow
         (map variant)
         (string/join " ")
         string/upper-case)))

(defn add-to-bag-button [data variants]
  (let [saving (query/get {:request-key request-keys/add-to-bag}
                          (get-in data keypaths/api-requests))]
    [:button.large.primary#add-to-cart-button
     {:on-click (when-not saving
                  (utils/send-event-callback data events/control-build-add-to-bag))
      :class [(when saving "saving") (when (experiments/simplify-funnel? data) "bright")]}
     (if (experiments/simplify-funnel? data)
       "ADD TO CART"
       "ADD TO BAG")]))

(def bundle-promotion-notice [:div [:em.bundle-discount-callout "Save 10% - Purchase 3 or more bundles"]])

(defn summary-section [data variants]
  (if-let [variant (products/selected-variant data)]
    [:.selected
     [:.line-item-summary (summary-format data)]
     (om/build counter-component data {:opts {:path keypaths/browse-variant-quantity
                                              :inc-event events/control-counter-inc
                                              :dec-event events/control-counter-dec
                                              :set-event events/control-counter-set}})
     [:.price (as-money (:price variant))]
     bundle-promotion-notice
     (add-to-bag-button data variants)]
    [:.selected
     [:div (str "Select " (format-step-name (next-step (selection-flow data) (get-in data keypaths/bundle-builder-selected-options))) "!")]
     [:.price "$--.--"]
     bundle-promotion-notice]))

(defn build-steps [flow redundant-attributes]
  (let [options (->> redundant-attributes
                     (apply merge-with concat)
                     (update-vals set)
                     (update-vals (fn [opts] (->> opts
                                                  (sort-by :position)
                                                  (map :name)))))
        dependent-steps (vec (reductions #(conj %1 %2) [] flow))]
    (map (fn [step step-dependencies]
           {:step-name step
            :option-names (step options)
            :dependent-steps step-dependencies})
         flow
         dependent-steps)))

(defn css-url [url] (str "url(" url ")"))

(defn product-image-url [data taxon]
  (when-let [product (products/selected-product data)]
    (when (contains? display-product-images-for-taxons (:name taxon))
      (get-in product [:master :images 0 :large_url]))))

(defn category-descriptions [taxon]
  (case (:name taxon)
    "closures"
    '("100% Human Virgin Hair"
      "Silk and Lace Materials"
      "Colors: 1B and #613 Blonde"
      "14\" and 18\" Length Bundles"
      "3.5 ounces")

    "blonde"
    '("100% Human Virgin Hair"
      "Colors: #27 and #613 Blonde"
      "14\" - 26\" Length Bundles"
      "3.5 ounces")

    '("100% Human Virgin Hair"
      "Color 1B"
      "12\" - 28\" Length Bundles"
      "3.5 ounces")))

(defn representative-product-id-for-taxon
  "The bundle-builder shows data for many products, but yotpo can show
  reviews for only one product at a time.  For now, we just pick one product that represents the whole taxon."
  [taxon]
  (let [taxon-path (keyword (taxons/taxon-path-for taxon))]
    (get {:straight 13
          :loose-wave 1
          :body-wave 3
          :deep-wave 11
          :curly 9
          :blonde 15
          :closures 28}
         taxon-path)))

(defn starting-at-price [variants]
  (let [cheapest-price (apply min (map :price variants))]
    [:div.starting-at (str "Starting at " (as-money cheapest-price))]))

(defn bundle-builder-category-component [data owner]
  (om/component
   (html
    (when-let [taxon (taxons/current-taxon data)]
      (let [products (products/current-taxon-whitelisted-products data)]
        [:.bundle-builder
         [:header
          [:h1
           [:div
            "Select Your "
            (:name taxon)
            " Hair"]
           [:.category-sub-header "Buy now and get FREE SHIPPING"]]]

         (if (query/get {:request-key (concat request-keys/get-products
                                              [(taxon-path-for taxon)])}
                        (get-in data keypaths/api-requests))
           [:.spinner]
           [:div
            [:.reviews-wrapper
             [:.reviews-inner-wrapper
              (when (get-in data keypaths/reviews-loaded)
                (om/build reviews-summary-component data
                          {:opts {:product-id (representative-product-id-for-taxon taxon)}}))]]
            [:.carousel
             (if-let [product-url (product-image-url data taxon)]
               [:.hair-category-image {:style {:background-image (css-url product-url)}}]
               (om/build carousel-component data {:opts {:index-path keypaths/bundle-builder-carousel-index
                                                         :images-path (conj keypaths/taxon-images
                                                                            (keyword (:name taxon)))}}))]
            (let [variants (products/current-taxon-variants data)
                  steps (build-steps (selection-flow data) (map :product_attrs products))]
              (list
               (starting-at-price variants)
               (bundle-builder-steps data variants steps)
               [:#summary
                [:h3 "Summary"]
                (summary-section data variants)
                (when-let [bagged-variants (seq (get-in data keypaths/browse-recently-added-variants))]
                  [:div#after-add {:style {:display "block"}}
                   [:div.added-to-bag-container
                    (map (partial display-bagged-variant data) bagged-variants)]
                   [:div.go-to-checkout
                    [:a.cart-button (utils/route-to data events/navigate-cart) "Checkout"]]])]))
            [:ul.category-description
             (for [description (category-descriptions taxon)]
               [:li description])]
            [:.reviews-wrapper
             (when (get-in data keypaths/reviews-loaded)
               (om/build reviews-component data
                         {:opts {:product-id (representative-product-id-for-taxon taxon)}}))]])
         [:div.gold-features
          [:figure.guarantee-feature]
          [:figure.free-shipping-feature]
          [:figure.triple-bundle-feature]]])))))

(defn category-component [data owner]
  (apply (if (experiments/bundle-builder-included-taxon? data (taxons/current-taxon data))
           bundle-builder-category-component
           original-category-component)
         [data owner]))
