(ns storefront.accessors.category-filters
  (:require [clojure.set :as set]))

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
  (let [new-filtered-sku-sets (matches-any? new-criteria :derived-criteria initial-sku-sets)
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
  {:initial-sku-sets  sku-sets
   :filtered-sku-sets sku-sets
   :criteria          {}
   :facets            (map (fn [facet-slug]
                             {:slug      facet-slug
                              :selected? false
                              :title     (facet-slug->name facet-slug)
                              :options   (->> facets
                                              (filter (fn [{:keys [step options]}]
                                                        (= step facet-slug)))
                                              first
                                              :options
                                              (map (fn [{:keys [long-name name :option/slug] :as option}]
                                                     {:slug      slug
                                                      :label     (or long-name name)
                                                      :selected? false})))})
                           (:unconstrained-facets category))})
