(ns catalog.category-filters
  (:require [clojure.set :as set]
            [storefront.utils.query :as query]
            [storefront.utils.maps :as maps]))

(comment
  "schema of category-filters"
  {:initial-sku-sets  []
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

(defn ^:private matches-any? [search-criteria item-criteria-fn coll]
  (filter (fn [item]
            (let [item-criteria (item-criteria-fn item)]
              (every? (fn [[facet allowed-vals]]
                        (let [item-vals (get item-criteria facet)]
                          (some allowed-vals item-vals)))
                      search-criteria)))
          coll))

(defn ^:private by-launched-at-price-name [x y]
  ;; launched-at is desc
  (compare [(:launched-at y) (:price (:representative-sku x)) (:name x)]
           [(:launched-at x) (:price (:representative-sku y)) (:name y)]))

(defn ^:private apply-criteria
  [{:keys [initial-sku-sets facets] :as filters} new-criteria]
  (let [new-filtered-sku-sets (->> initial-sku-sets
                                   (keep (fn [{:keys [sku-set/full-skus] :as sku-set}]
                                           (when-let [matching-skus (seq (matches-any? new-criteria
                                                                                       (comp attributes->criteria :attributes)
                                                                                       full-skus))]
                                             (assoc sku-set
                                                    :matching-skus matching-skus
                                                    :representative-sku (apply min-key :price matching-skus)))))
                                   (sort by-launched-at-price-name))
        new-facets            (mapv (fn [{:keys [slug options] :as facet}]
                                      (let [criteria-options (get new-criteria slug)
                                            new-options      (mapv (fn [option]
                                                                     (assoc option :selected? (contains? criteria-options (:slug option))))
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

(defn undo-criterion [{:keys [criteria previous-criteria] :as filters}]
  (let [new-criteria      (last previous-criteria)]
    (-> filters
        (apply-criteria new-criteria)
        (update :previous-criteria pop))))

(defn replace-criterion [{:keys [criteria] :as filters} facet-slug option-slug]
  (let [new-criteria (assoc criteria facet-slug #{option-slug})]
    (->
     (apply-criteria filters new-criteria)
     (update :previous-criteria (fnil conj []) criteria))))

(defn clear-criteria [filters]
  (apply-criteria filters {}))

(defn close [filters]
  (update filters :facets (fn [facets]
                            (map #(assoc % :selected? false)
                                 facets))))

(defn step [filters {facet-slug :slug}]
  (update filters :facets
          (fn [facets]
            (let [selected-idx (count (take-while (comp not #{facet-slug} :slug) facets))
                  found (< selected-idx (count facets))]
              (if found
                (map-indexed (fn [idx facet]
                               (assoc facet :selected? (<= idx selected-idx)))
                             facets)
                facets)))))

(defn open [filters facet-slug]
  (update filters :facets (fn [facets]
                            (map #(assoc % :selected? (= (:slug %) facet-slug))
                                 facets))))

(defn init [category sku-sets facets]
  (let [represented-criteria (->> sku-sets
                                  (mapcat :sku-set/full-skus)
                                  (map :attributes)
                                  (maps/into-multimap))]
    (-> {:initial-sku-sets sku-sets
         :facets           (->> (:filter-tabs category)
                                (map (fn [tab] (query/get {:facet/slug tab} facets)))
                                (map (fn [{:keys [:facet/slug :facet/name :facet/options]}]
                                       (let [represented-options (get represented-criteria slug)]
                                         {:slug         slug
                                          :title        name
                                          :options      (->> options
                                                             (sort-by :filter/order)
                                                             (map (fn [{:keys [:option/name :option/slug :option/sku-set-ids]}]
                                                                    {:slug         slug
                                                                     :sku-set-ids  (set sku-set-ids)
                                                                     :represented? (contains? represented-options slug)
                                                                     :label        name})))}))))}
        clear-criteria
        (apply-criteria (:criteria category))
        close)))
