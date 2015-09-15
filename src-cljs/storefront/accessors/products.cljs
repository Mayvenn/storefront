(ns storefront.accessors.products
  (:require [storefront.keypaths :as keypaths]))

(defn graded? [product]
  (-> product
      :collection_name
      #{"premier" "deluxe" "ultra"}
      boolean))

(defn all-variants [product]
  (conj (:variants product) (:master product)))

(defn for-taxon [data taxon]
  (->> (get-in data keypaths/products)
       vals
       (sort-by :index)
       (filter #(contains? (set (:taxon_ids %)) (:id taxon)))))

(defn selected-variant [data]
  (let [variants (mapcat :variants (get-in data [:ui :bundle :filtered-products]))
        matches (filter
                 (fn [variant] (= (-> variant :option_values first :name (#(str % "\"")))
                                  (get-in data keypaths/bundle-builder-length)))
                 variants)]
    (when (= 1 (count matches))
      (first matches))))
