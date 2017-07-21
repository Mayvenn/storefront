(ns storefront.accessors.category-filters
  (:require [clojure.set :as set]
            [storefront.utils.maps :as maps]))

(comment
  "schema of category-filters"
  {:initial-sku-sets  []
   :filtered-sku-sets []
   :criteria          {:color #{"black"}}
   :facets            [{:slug      ""
                        :selected? false
                        :title     ""
                        :options   [{:label     ""
                                     :selected? false
                                     :slug      ""}]}]})

(defn attributes->criteria [attributes]
  (maps/into-multimap [attributes]))

(defn matches-any? [search-criteria item-criteria-fn coll]
  (filter (fn [item]
            (let [item-criteria (item-criteria-fn item)]
              (every? (fn [[facet allowed-vals]]
                        (let [item-vals (get item-criteria facet)]
                          (or (nil? item-vals) ; e.g., this sku doesn't have material
                              (some allowed-vals item-vals))))
                      search-criteria)))
          coll))

(defn ^:private apply-criteria [{:keys [initial-sku-sets facets] :as filters} new-criteria]
  (let [new-filtered-sku-sets (->> initial-sku-sets
                                   (map (fn [{:keys [skus] :as sku-set}]
                                          (assoc sku-set
                                                 :matching-skus (matches-any? new-criteria
                                                                              (comp attributes->criteria :attributes)
                                                                              skus))))
                                   (filterv (comp seq :matching-skus)))
        new-facets            (mapv (fn [{:keys [slug options] :as facet}]
                                      (let [[facet-in-criteria criteria-options] (find new-criteria slug)
                                            new-options (mapv (fn [option]
                                                                (assoc option :selected? (contains? criteria-options (:slug option))))
                                                              options)]
                                        (assoc facet :options new-options)))
                                    facets)]
    (assoc filters
           :criteria new-criteria
           :filtered-sku-sets new-filtered-sku-sets
           :facets new-facets)))

(defn deselect-criteria [{:keys [criteria] :as filters} facet-slug option-slug]
  (let [selected-criteria (disj (facet-slug criteria) option-slug)
        new-criteria      (if (seq selected-criteria)
                            (assoc criteria facet-slug selected-criteria)
                            (dissoc criteria facet-slug))]
    (apply-criteria filters new-criteria)))

(defn select-criteria [{:keys [criteria] :as filters} facet-slug option-slug]
  (let [new-criteria (update criteria facet-slug (fnil conj #{}) option-slug)]
    (apply-criteria filters new-criteria)))

(defn clear-criteria [filters]
  (apply-criteria filters {}))

(defn close [filters]
  (update filters :facets (fn [filters]
                            (map #(assoc % :selected? false)
                                 filters))))

(defn open [filters facet-slug]
  (update filters :facets (fn [filters]
                            (map #(assoc % :selected? (= (:slug %) facet-slug))
                                 filters))))

(def facet-slug->name
  {:family   "Category"
   :style    "Texture"
   :origin   "Origin"
   :material "Material"
   :color    "Color"})

(defn init [category sku-sets facets]
  (-> {:initial-sku-sets  sku-sets
       :facets            (map (fn [facet-slug]
                                 {:slug      facet-slug
                                  :title     (facet-slug->name facet-slug)
                                  :options   (->> facets
                                                  (filter #(= (:step %) facet-slug))
                                                  first
                                                  :options
                                                  (map (fn [{:keys [long-name name :option/slug]}]
                                                         {:slug  slug
                                                          :label (or long-name name)})))})
                               (:unconstrained-facets category))}
      clear-criteria
      close))
