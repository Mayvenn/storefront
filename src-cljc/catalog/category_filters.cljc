(ns catalog.category-filters
  (:require [storefront.utils.query :as query]
            [spice.maps :as maps]
            [catalog.selector :as selector]
            [catalog.skuers :as skuers]
            [catalog.products :as products]
            [spice.core :as spice]))

(comment
  "schema of category-filters"
  {:initial-sku-sets  []
   :initial-skus      []
   :filtered-sku-sets []
   :criteria          {:hair/color #{"black"}}
   :facets            [{:slug      ""
                        :selected? false
                        :title     ""
                        :options   [{:label     ""
                                     :selected? false
                                     :slug      ""}]}]})

(defn ^:private attributes->criteria [attributes]
  (maps/into-multimap [attributes]))

(defn ^:private by-launched-at-price-name [x y]
  ;; launched-at is desc
  (compare [(:launched-at y) (:price (:representative-sku x)) (:name x)]
           [(:launched-at x) (:price (:representative-sku y)) (:name y)]))

(defn ^:private apply-criteria
  [{:keys [initial-sku-sets facets] :as filters} new-criteria]
  (let [new-filtered-sku-sets
        (->> initial-sku-sets
             (keep (fn [sku-set]
                     (let [matching-skus    (seq (selector/strict-query (:sku-set/full-skus sku-set)
                                                                        new-criteria))]
                       (when matching-skus
                         (assoc sku-set
                                :matching-skus matching-skus
                                :representative-sku (apply min-key :price matching-skus))))))
             (sort by-launched-at-price-name))
        new-facets (mapv (fn [{:keys [slug options] :as facet}]
                           (let [criteria-options (get new-criteria slug)
                                 new-options      (mapv (fn [option]
                                                          (assoc option
                                                                 :selected?
                                                                 (contains? criteria-options (:slug option))))
                                                        options)]
                             (assoc facet :options new-options)))
                         facets)]
    (assoc filters
           :criteria new-criteria
           :filtered-sku-sets new-filtered-sku-sets
           :facets new-facets)))

(defn deselect-criterion [{:keys [criteria] :as filters} facet-slug option-slug]
  (let [selected-criteria (disj (facet-slug criteria) option-slug)
        new-criteria      (if (seq selected-criteria)
                            (assoc criteria facet-slug selected-criteria)
                            (dissoc criteria facet-slug))]
    (apply-criteria filters new-criteria)))

(defn select-criterion [{:keys [criteria] :as filters} facet-slug option-slug]
  (let [new-criteria (update criteria facet-slug (fnil conj #{}) option-slug)]
    (apply-criteria filters new-criteria)))

(defn clear-criteria [filters]
  (apply-criteria filters {}))

(defn close [filters]
  (update filters :facets (fn [facets]
                            (map #(assoc % :selected? false)
                                 facets))))

(defn open [filters facet-slug]
  (update filters :facets (fn [facets]
                            (map #(assoc % :selected? (= (:slug %) facet-slug))
                                 facets))))

(defn init [category sku-sets skus facets]
  (let [category-electives   (:selector/electives category)
        init-sku-sets (->> (selector/query (vals sku-sets)
                                           (skuers/essentials category))
                           (map (fn [sku-set]
                                  (assoc sku-set
                                         :sku-set/full-skus
                                         (vals (select-keys skus (:selector/skus sku-set)))))))
        represented-criteria (->> (selector/query (vals skus)
                                                  (skuers/essentials category))
                                  (map (fn [sku]
                                         (select-keys sku category-electives)))
                                  (maps/into-multimap))]
    (-> {:initial-sku-sets init-sku-sets
         :facets           (->> (:selector/electives category)
                                (map (fn [tab] (query/get {:facet/slug tab} facets)))
                                (map (fn [{:keys [:facet/slug :facet/name :facet/options]}]
                                       (let [represented-options (get represented-criteria slug)]
                                         {:slug    slug
                                          :title   name
                                          :options (->> options
                                                        (sort-by :filter/order)
                                                        (map (fn [{:keys [:option/name
                                                                          :option/slug
                                                                          :option/sku-set-ids]}]
                                                               {:slug         slug
                                                                :sku-set-ids  (set sku-set-ids)
                                                                :represented? (contains? represented-options slug)
                                                                :label        name})))}))))}
        clear-criteria
        (apply-criteria (select-keys category (:selector/essentials category)))
        close)))
