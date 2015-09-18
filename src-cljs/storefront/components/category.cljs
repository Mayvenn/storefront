(ns storefront.components.category
  (:require [storefront.components.utils :as utils]
            [storefront.components.product :refer [display-bagged-variant]]
            [storefront.components.formatters :refer [as-money-without-cents as-money]]
            [storefront.accessors.products :as products]
            [storefront.accessors.taxons :refer [filter-nav-taxons taxon-path-for taxon-class-name]]
            [storefront.components.counter :refer [counter-component]]
            [clojure.string :as string]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.hooks.experiments :as experiments]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.utils.query :as query]))

(defn index [xs]
  (map vector (range 0 (count xs)) xs))

(defn display-taxon [data selected-taxon taxon]
  (let [taxon-path (taxon-path-for taxon)
        selected-class (if (= selected-taxon taxon) "selected" nil)
        taxon-classes (string/join " " (conj [taxon-path] selected-class))]
    [:div.hair-taxon.decorated.small-width {:class taxon-classes}
     [:a.taxon-link (utils/route-to data events/navigate-category {:taxon-path taxon-path})
      [:p.hair-taxon-name (:name taxon)]]]))

(defn display-product [data taxon product]
  (let [collection-name (:collection_name product)]
   [:a {:href (utils/href-to data events/navigate-product {:product-path (:slug product) :query-params {:taxon_id (taxon :id)}})
        :on-click (utils/click-to data events/control-click-category-product {:target product :taxon taxon})}
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
      [:div.taxon-product-info-container
       [:div.taxon-product-description-container
        [:div.taxon-product-collection
         (when (products/graded? product)
           [:div.taxon-product-collection-indicator
            {:class collection-name}])
         collection-name]
        [:div.taxon-product-title
         (:name product)]]
       [:div.taxon-from-price
        [:span "From: "]
        [:br]
        (let [variant (-> product products/all-variants first)]
          (if (= (variant :price) (variant :original_price))
            (as-money-without-cents (variant :price))
            (list
             [:span.original-price
              (as-money-without-cents (variant :original_price))]
             [:span.current-price
              (as-money-without-cents (variant :price))])))]]]]))

(defn original-category-component [data owner]
  (om/component
   (html
    (if-let [taxon (query/get (get-in data keypaths/browse-taxon-query)
                              (get-in data keypaths/taxons))]
      [:div
       [:div.taxon-products-banner {:class (taxon-class-name taxon)}]
       [:div.taxon-products-container
        (when-not (:stylist_only? taxon)
          [:div.taxon-nav
           (map (partial display-taxon data taxon)
                (filter-nav-taxons (get-in data keypaths/taxons)))
           [:div {:style {:clear "both"}}]])
        [:div.taxon-products-list-container
         (let [products (->> (get-in data keypaths/products)
                             vals
                             (sort-by :index)
                             (filter #(contains? (set (:taxon_ids %)) (:id taxon))))]
           (if (query/get {:request-key (concat request-keys/get-products
                                                [(taxon-path-for taxon)])}
                          (get-in data keypaths/api-requests))
             [:.spinner]
             (map (partial display-product data taxon) products)))]]

       [:div.gold-features
        [:figure.guarantee-feature]
        [:figure.free-shipping-feature]
        [:figure.triple-bundle-feature]
        [:feature.fs-feature]]]))))

;; Bundle builder below

(defn selection-flow [data]
  (let [taxon-name (:name (query/get (get-in data keypaths/browse-taxon-query)
                                     (get-in data keypaths/taxons)))]
    (condp = taxon-name
      "closures" '(:style :material :origin :length)
      "blonde" '(:color :grade :origin :length)
      '(:grade :origin :length))))

(defn index-of [xs x]
  (or (reduce (fn [acc [idx elem]]
                (or acc
                    (when (= elem x) idx)))
              nil
              (index xs))
      -1))

(defn format-step-name [step-name]
  (let [step-name (name step-name)
        vowel? (set "AEIOUaeiou")]
    (str (if (vowel? (first step-name)) "an " "a ")
         (string/capitalize step-name))))

(defn next-step [data step-name]
  (let [flow (selection-flow data)]
    (get (->> flow
              (partition 2 1)
              (map vec)
              (into {nil (first flow)}))
         step-name)))

(defn option-selection-event [data step-name dependent-steps selected-variants option-name]
  (utils/send-event-callback data
                             events/control-bundle-option-select
                             {:step-name step-name
                              :selected-steps dependent-steps
                              :selected-variants selected-variants
                              :option-name option-name}))

(defn min-strs [new old]
  (let [numeric-new (js/parseFloat new)]
    (if (and old (> old 0))
      (min old numeric-new)
      numeric-new)))

(defn minimums-for-products [label-fn all-products]
  (reduce (fn [acc product]
              (update-in acc [(label-fn product)]
                         (partial min-strs (:from_price product))))
            {}
            all-products))

(defn price-differences [label-fn all-products]
  (let [min-prices (minimums-for-products label-fn all-products)
        minnest-price (apply min (vals min-prices))]
    (into {} (map (fn [[k v]] [k (- v minnest-price)]) min-prices))))

(defn price-for-option [step-name option-min-price option-price-diff]
  (case step-name
    :grade    [:min-price option-min-price]
    :material [:diff-price option-price-diff]
    :origin   [:diff-price option-price-diff]
    :style    []
    :length   []
    :color    []))

(defn format-price [[type price]]
  (str ({:min-price "From " :diff-price "+ "} type) (as-money price)))

(defn filter-variants-by-selections [selections variants]
  (filter (fn [variant]
            (every? (fn [[step-name option-name]]
                      (= (step-name variant) option-name))
                    selections))
          variants))

(defn build-options-for-step [data variants {:keys [step-name option-names dependent-steps]}]
  (let [all-selections          (get-in data keypaths/bundle-selected-options) ;; e.g. {:grade "6a nsd"}
        prior-selections        (select-keys all-selections dependent-steps)
        step-disabled?          (> (count dependent-steps) (count all-selections))
        selected-variants       (filter-variants-by-selections prior-selections variants)
        selectable-option-names (set (map step-name selected-variants))
        minimums                (minimums-for-products step-name variants)
        differences             (price-differences step-name variants)]
    (for [option-name option-names]
      (let [variants-for-option (partial filter-variants-by-selections
                                         {step-name option-name})
            sold-out? (every? (comp not :can_supply?)
                              (variants-for-option variants))]
        {:option-name option-name
         :price (price-for-option step-name (minimums option-name) (differences option-name))
         :disabled (or step-disabled?
                       sold-out?
                       (not (selectable-option-names option-name)))
         :checked (= (get all-selections step-name nil) option-name)
         :sold-out sold-out?
         :on-change (option-selection-event data
                                            step-name
                                            dependent-steps
                                            (variants-for-option selected-variants)
                                            option-name)}))))

(defn step-html [step-name idx options]
    [:.step
     [:h2 (str (inc idx)) ". Choose " (format-step-name step-name)]
     [:.options
      (for [[idx {:keys [option-name price disabled checked sold-out on-change]}] (index options)]
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
            [:label {:for option-id}]])))]])

(defn bundle-builder-steps [data variants steps]
  (map-indexed (fn [idx {:keys [step-name] :as step}]
                 (step-html step-name
                            idx
                            (build-options-for-step data variants step)))
               steps))

(def summary-option-mapping
  {"6a premier collection" "6a premier"
   "7a deluxe collection" "7a deluxe"
   "8a ultra collection" "8a ultra"
   "closures" "closure"})

(defn summary-format [data]
  (let [taxon (query/get (get-in data keypaths/browse-taxon-query)
                         (get-in data keypaths/taxons))]
    (->> (get-in data keypaths/bundle-selected-options)
         vals
         (#(concat % [(:name taxon)]))
         (map #(get summary-option-mapping % %))
         (string/join " ")
         string/upper-case)))

(defn price-preview [data variant]
  (as-money (:price variant)))

(defn add-to-bag-button [data]
  (if (query/get {:request-key request-keys/add-to-bag}
                 (get-in data keypaths/api-requests))
    [:button.large.primary#add-to-cart-button.saving]
    [:button.large.primary#add-to-cart-button
     {:on-click (utils/send-event-callback data events/control-browse-add-to-bag)}
     "ADD TO BAG"]))

(defn summary-section [data]
  (if-let [variant (products/selected-variant data)]
    [:.selected
     [:.line-item-summary (summary-format data)]
     (om/build counter-component data {:opts {:path keypaths/browse-variant-quantity
                                              :inc-event events/control-counter-inc
                                              :dec-event events/control-counter-dec
                                              :set-event events/control-counter-set}})
     [:.price (price-preview data variant)]
     (add-to-bag-button data)]
    [:.selected
     [:div (str "Select " (format-step-name (next-step data (get-in data keypaths/bundle-previous-step))) "!")]
     [:.price "$--.--"]]))

;; FIXME: Move to utils or something
(defn map-kv [f m]
  (reduce-kv #(assoc %1 %2 (f %3)) {} m))

(defn build-variants
  "We wish the API gave us a list of variants.  Instead, variants are nested
  inside products.

  So, this explodes them out into the data structure we'd prefer."
  [product]
  (let [product-attrs (map-kv (comp :name first) (:product_attrs product))
        variants (:variants product)]
    (map (fn [variant]
           (-> variant
               (merge product-attrs)
               ;; Variants have one specific length, stored in option_values.
               ;; We need to overwrite the product length, which includes all
               ;; possible lengths.
               (assoc :length (some-> variant :option_values first :name (str "\""))
                      :from_price (:from_price product))
               (dissoc :option_values)))
         variants)))

(defn build-steps [flow redundant-attributes]
  (let [options (->> redundant-attributes
                     (apply merge-with concat)
                     (map-kv set)
                     (map-kv (fn [opts] (->> opts
                                             (sort-by :position)
                                             (map :name)))))
        dependent-steps (vec (reductions #(conj %1 %2) [] flow))]
    (map (fn [step step-dependencies]
           {:step-name step
            :option-names (step options)
            :dependent-steps step-dependencies})
         flow
         dependent-steps)))

(defn bundle-builder-category-component [data owner]
  (om/component
   (html
    (let [taxon (query/get (get-in data keypaths/browse-taxon-query)
                           (get-in data keypaths/taxons))
          products (products/for-taxon data taxon)]
      [:.bundle-builder
       [:header
        [:h1
         [:div
          "Select Your "
          (:name taxon)
          " Hair"]
         [:div.buy-now "Buy now and get FREE SHIPPING"]]]

       (if (seq products)
         [:div
          [:.reviews]
          [:.carousel
           [:.hair-category-image {:class (taxon-path-for taxon)}]]
          (let [variants (mapcat build-variants products)
                steps (build-steps (selection-flow data) (map :product_attrs products))]
            (bundle-builder-steps data variants steps))
          [:#summary
           [:h3 "Summary"]
           (summary-section data)
           [:div [:em.bundle-discount-callout "Save 5% - Purchase 3 or more bundles"]]
           (when-let [bagged-variants (seq (get-in data keypaths/browse-recently-added-variants))]
             [:div#after-add {:style {:display "block"}}
              [:div.added-to-bag-container
               (map (partial display-bagged-variant data) bagged-variants)]
              [:div.go-to-checkout
               [:a.cart-button (utils/route-to data events/navigate-cart) "Checkout"]]])]]
         [:.spinner])

       [:div.gold-features
        [:figure.guarantee-feature]
        [:figure.free-shipping-feature]
        [:figure.triple-bundle-feature]]]))))

(defn category-component [data owner]
  (apply (if (experiments/display-variation data "bundle-builder")
           bundle-builder-category-component
           original-category-component)
         [data owner]))
