(ns storefront.accessors.products
  (:require [storefront.keypaths :as keypaths]
            [storefront.utils.sequences :refer [update-vals]]
            [storefront.accessors.taxons :as taxons]))

(defn all-variants [product]
  (conj (:variants product) (:master product)))

(defn for-taxon [data taxon]
  (->> (get-in data keypaths/products)
       vals
       (sort-by :index)
       (filter #(contains? (set (:taxon_ids %)) (:id taxon)))))

(defn selected-variant [data]
  (let [variants (get-in data keypaths/bundle-builder-selected-variants)]
    (when (= 1 (count variants))
      (first variants))))

(defn selected-products [data]
  (let [variants (get-in data keypaths/bundle-builder-selected-variants)
        product-ids (set (map :product_id variants))]
    (select-keys (get-in data keypaths/products) product-ids)))

(defn selected-product [data]
  (let [selected (selected-products data)]
    (when (= 1 (count selected))
      (last (first selected)))))

(defn build-variants
  "We wish the API gave us a list of variants.  Instead, variants are nested
  inside products.

  So, this explodes them out into the data structure we'd prefer."
  [product]
  (let [product-attrs (update-vals (comp :name first) (:product_attrs product))
        variants (:variants product)]
    (map (fn [variant]
           (-> variant
               (merge product-attrs)
               ;; Variants have one specific length, stored in variant_attrs.
               ;; We need to overwrite the product length, which includes all
               ;; possible lengths.
               (assoc :length (some-> variant :variant_attrs :length)
                      :price (js/parseFloat (:price variant))
                      :sold-out? (not (:can_supply? variant)))))
         variants)))

(defn current-taxon-whitelisted-products [app-state]
  (filter #(-> % :product_attrs :grade first :name #{"6a premier collection"})
          (for-taxon app-state (taxons/current-taxon app-state))))

(defn current-taxon-variants [app-state]
  (let [products (current-taxon-whitelisted-products app-state)]
    (mapcat build-variants products)))

(defn ordered-products-for-category [app-state {:keys [slug]}]
  (let [product-order-by-taxon-slug (get-in app-state keypaths/taxon-product-order)]
    (map (get-in app-state keypaths/products {})
         (product-order-by-taxon-slug slug))))

(defn filter-variants-by-selections [selections variants]
  (filter (fn [variant]
            (every? (fn [[step-name option-name]]
                      (= (step-name variant) option-name))
                    selections))
          variants))

(defn not-black-color [attr]
  (when (not= "black" (:color attr))
    (:color attr)))

(def ^:private frontal-summary [:style :material :origin :length (constantly "frontal")])
(def ^:private closure-summary [:style :material :origin :length (constantly "closure")])
(def ^:private bundle-summary [not-black-color :origin :length :style])

(defn closure? [variant]
  (= "closures" (get-in variant [:variant-attrs :category])))

(defn frontal? [variant]
  (= "frontals" (get-in variant [:variant-attrs :category])))

(defn bundle? [variant]
  (boolean (get-in variant [:variant-attrs :category])))

(defn summary [{:keys [variant-attrs product-name] :as variant}]
  (let [summary-fns (cond (closure? variant) closure-summary
                          (frontal? variant) frontal-summary
                          (bundle? variant)  bundle-summary
                          :else [(constantly product-name)])
        strs (filter identity ((apply juxt summary-fns) variant-attrs))]
    (clojure.string/join " " strs)))

(def ^:private frontal-product-title [:origin :style :material (constantly "frontal")])
(def ^:private closure-product-title [:origin :style :material (constantly "closure")])
(def ^:private bundle-product-title [not-black-color :origin :style])

(defn product-title [{:keys [variant-attrs product-name] :as variant}]
  (let [title-fns (cond (closure? variant) closure-product-title
                        (frontal? variant) frontal-product-title
                        (bundle? variant)  bundle-product-title
                        :else [(constantly product-name)])
        strs (filter identity ((apply juxt title-fns) variant-attrs))]
    (clojure.string/join " " strs)))

(defn thumbnail-url [products product-id]
  (get-in products [product-id :master :images 0 :small_url]))
