(ns storefront.accessors.products
  (:require [storefront.keypaths :as keypaths]
            [storefront.accessors.taxons :as taxons]))

(defn all-variants [product]
  (conj (:variants product) (:master product)))

(defn selected-variants [data]
  (get-in data keypaths/bundle-builder-selected-variants))

(defn selected-variant [data]
  (let [variants (selected-variants data)]
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

(defn build-variants [product]
  (map (fn [variant]
         (-> variant
             (merge (:variant_attrs variant))
             (assoc :price (js/parseFloat (:price variant))
                    :sold-out? (not (:can_supply? variant)))))
       (:variants product)))

(defn ordered-products-for-category [app-state {:keys [product-ids]}]
  (remove nil? (map (get-in app-state keypaths/products) product-ids)))

(defn current-taxon-variants [data]
  (->> (taxons/current-taxon data)
       (ordered-products-for-category data)
       (mapcat build-variants)))

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
