(ns storefront.components.category
  (:require [storefront.components.utils :as utils]
            [storefront.components.formatters :refer [as-money-without-cents as-money]]
            [storefront.accessors.products :as products]
            [storefront.accessors.taxons :refer [filter-nav-taxons taxon-path-for taxon-class-name]]
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


;;(defn bundle-builder-chooser [data all-choices valid-choices step-filters path]
;;  [:.chooser
;;   (map (fn [{choice :name position :position}]
;;          [:div
;;           [:input {:type "radio"
;;                    :id choice
;;                    :disabled (not (get valid-choices choice))
;;                    :checked (= (get-in data path) choice)
;;                    :on-change (utils/send-event-callback data
;;                                                          events/control-chooser-select
;;                                                          {:path path
;;                                                           :step-filters step-filters
;;                                                           :choice choice})}]
;;           [:.choice
;;            [:.choice-name choice]
;;            [:.from-price "position " position]
;;            [:label {:for choice}]]])
;;        (sort-by :position all-choices))])

(defn choice-matches? [product [choice value]]
  (some (partial = value)
        (->> product :product_attrs choice (map :name))))

(defn filter-products-by-choices [selected-choices products]
  (filter (fn [product]
            (every? (partial choice-matches? product)
                    selected-choices))
          products))

(defn min-price [products]
  (mapcat (comp (partial map :price) products/all-variants) products))

(defn gen-choices [products choice-type]
  (->> products
       (mapcat (comp choice-type :product_attrs))
       (set)
       (sort-by :position)))

(defn filter-selected [data filters-to-apply]
  (select-keys (get-in data keypaths/bundle-builder) filters-to-apply))

(defn valid-choices [products choice-type]
  (->> products
       (mapcat (comp choice-type :product_attrs))
       (set)))

(defn mk-choice-disabled [valid-choices]
  (fn [choice]  (not (valid-choices choice))))

(defn choice-type-path [choice-type]
  (conj keypaths/bundle-builder choice-type))

(defn choice-checked? [data choice choice-type]
  (= (:name choice) (get-in data (choice-type-path choice-type))))

(def selection-flow '(:grade :origin :length))

(defn index-of [xs x]
  (or (reduce (fn [acc [idx elem]]
                (or acc
                    (when (= elem x) idx)))
              nil
              (index xs))
      -1))

(defn choose-string [choice-name]
  (str (if ((set "AEIOUaeiou") (first choice-name))
         "an " "a ")
       (string/capitalize choice-name)))

(defn next-choice [data]
  (prn (last  (keys  (get-in data keypaths/bundle-builder))))
  (->> (get-in data keypaths/bundle-builder)
       keys
       last
       (index-of selection-flow)
       inc
       (nth selection-flow)
       name
       choose-string))

(defn choice-selection-event [data choice-type filters-to-apply filtered-products choice]
  (utils/send-event-callback
   data
   events/control-chooser-select
   {:path (choice-type-path choice-type)
    :applied-filters filters-to-apply
    :filtered-products filtered-products
    :choice (:name choice)}))


(defn min-strs [new old]
  (let [numeric-new (js/parseFloat new)]
    (if (and old (> old 0))
      (min old numeric-new)
      numeric-new)))

(defn grade-minimums [all-products]
  (let [grade-name #(-> % :product_attrs :grade first :name keyword)
        low-price #(-> % :from_price)]
    (reduce (fn [acc product]
              (update-in acc [(grade-name product)]
                         (partial min-strs (low-price product))))
            {}
            all-products)))

(defn origin-minimums [all-products]
  (let [origin-name #(-> % :product_attrs :origin first :name keyword)
        low-price   #(-> % :from_price)
        min-prices (reduce (fn [acc product]
                             (update-in acc [(origin-name product)]
                                        (partial min-strs (low-price product))))
                           {}
                           all-products)
        minnest-price (apply min (vals min-prices))] ;; blame Corey for naming --Alex
    (into {} (map (fn [[k v]] [k (- v minnest-price)]) min-prices))))

(defn format-subtext [choice-type subtext]
  (case choice-type
    :grade (str "From " (as-money subtext))
    :origin  (str "+ " (as-money subtext))
    :length ""))

(defn build-choices [data products choice-type filters-to-apply]
  (let [all-choices (gen-choices products choice-type)
        filtered-products (filter-products-by-choices
                           (filter-selected data filters-to-apply)
                           products)
        choice-disabled? (mk-choice-disabled
                          (valid-choices filtered-products choice-type))
        choice-subtexts (merge (grade-minimums products)
                               (origin-minimums products))
        ]
    (for [choice all-choices]
      {:id (:name choice)
       :subtext ((keyword (:name choice)) choice-subtexts)
       :disabled (or (choice-disabled? choice)
                     (> (count filters-to-apply) (count (get-in data keypaths/bundle-builder))))
       :checked (choice-checked? data choice choice-type)
       :on-change (choice-selection-event data
                                          choice-type
                                          filters-to-apply
                                          filtered-products
                                          choice)})))


(defn choices-html [choice-type idx choices]
    [:.choose.step
     [:h2 (str (inc idx)) ". Choose " (choose-string (name choice-type))]
     [:.chooser
      (for [[idx {:keys [id subtext disabled checked on-change]}] (index choices)]
        (list
         [:input {:type "radio"
                  :id id
                  :disabled disabled
                  :checked checked
                  :on-change on-change}]
         [:.choice {:class choice-type}
          [:.choice-name id]
          [:.from-price (format-subtext choice-type subtext)]
          [:label {:for id}]]))]])


(defn bundle-builder-steps [data products keys]
  (let [key-steps (vec (reductions #(conj %1 %2) [] keys))]
    (map-indexed (fn [idx choice-type]
                   (choices-html choice-type idx
                                 (build-choices data
                                                products
                                                choice-type
                                                (get key-steps idx)
                                                ))) keys)))


;; TODO: Fix this in the API because it's borderline incomprehensible
(defn selected-variant [data]
  (let [variants (mapcat :variants (get-in data [:ui :bundle :filtered-products]))
        matches (filter
                 (fn [variant] (= (-> variant :option_values first :name (#(str % "\"")))
                                  (get-in data keypaths/bundle-builder-length)))
                 variants)]
    (when (= 1 (count matches))
      (first matches))))

(defn summary-format [data]
  (map (fn [[k v]] [:li (name k) ": " v])
       (get-in data keypaths/bundle-builder)))

(defn summary-section [data]
  (let [variant (selected-variant data)]
    (if variant
      [:.selected
       [:ul (summary-format data)]
       [:.price (as-money (:price variant))]]
      [:.selected
       [:div (str "Select " (next-choice data) "!")]
       [:.price "$--.--"]])))

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
          (string/join
           " "
           (map
            string/capitalize
            (string/split (:name taxon) " ")))
          " Hair"]
         [:div.buy-now "Buy now and get FREE SHIPPING"]]]
       (if (seq products)
         [:div
          [:.reviews]
          [:.carousel
           [:.hair-category-image {:class (taxon-path-for taxon)}]]
          (bundle-builder-steps data products selection-flow)
          [:.summary
           [:h3 "Summary"]
           (summary-section data)
           [:div [:em.bundle-discount-callout "Save 5% - Purchase 3 or more bundles"]]
           ]]
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

