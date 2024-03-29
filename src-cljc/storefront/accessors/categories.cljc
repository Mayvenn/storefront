(ns storefront.accessors.categories
  (:require [storefront.accessors.products :as products]
            [catalog.keypaths]
            [cemerick.url :as cemerick-url]
            [clojure.string :as string]
            clojure.set
            [spice.maps :as maps]
            [catalog.facets :as facets]
            [storefront.keypaths :as keypaths]))

(def query-param-separator "~")

(defn sort-query-params
  [params]
  (let [ordering {"origin"          0
                  "texture"         1
                  "style"           2
                  "color"           3
                  "color-shorthand" 4
                  "color-feature"   5
                  "base-material"   6
                  "weight"          7
                  "family"          8}]
    (into (sorted-map-by
           (fn [key1 key2]
             (compare (get ordering key1 100)
                      (get ordering key2 100))))
          params)))

(defn category-selections->query-params
  [category-selections]
  (->> category-selections
       (maps/map-values (fn [s] (string/join query-param-separator s)))
       (maps/map-keys (comp (fnil name "") facets/slug>query-param))
       sort-query-params))

(defn query-params->selector-electives [query-params]
  (->> (maps/select-rename-keys query-params facets/query-param>slug)
       (maps/map-values #(set (.split (str %) query-param-separator)))))

(defn id->category [id categories]
  (->> categories
       (filter (comp #{(str id)} :catalog/category-id))
       first))

(defn named-search->category [named-search-slug categories]
  (->> categories
       (filter #(= named-search-slug
                   (:legacy/named-search-slug %)))
       first))

;; TODO: this should receive categories as first arg instead of app state
(defn current-traverse-nav [data]
  (id->category (get-in data keypaths/current-traverse-nav-id)
                (get-in data keypaths/categories)))

;; TODO: this should receive categories as first arg instead of app state
(defn current-category [data]
  (id->category (get-in data catalog.keypaths/category-id)
                (get-in data keypaths/categories)))

(defn query-map
 [query]
  #?(:clj (cemerick-url/query->map query)
     :cljs (identity query)))

(defn canonical-category-data
  "With ICPs, the 'canonical category id' may be different from the ICP category
  id. E.g. 13-wigs with a selected family of 'lace-front-wigs' will have a
  canonical cateogry id of 24, or in other words, lace-front-wigs' category id."
  [categories requested-category current-nav-url]
  (let [query-map         (some-> current-nav-url :query)
        category-27?      (=  "27" (:catalog/category-id requested-category))
        texture-selection (some-> query-map
                                  (get "texture")
                                  (string/split #"~"))
        family-selection  (some-> query-map
                                 (get "family")
                                 (string/split #"~"))]
    (cond
      ;; Are we on human hair bundles ICP w/ a texture chosen?
      ;; we have to check specifically for category 27 because multiple categories use texture as a filter
      (and category-27?
           texture-selection
           (= (count texture-selection) 1))
      (let [canonical-category (->> categories
                                    (filter #(and (= 1 (count (:hair/texture %)))
                                                  (some (:hair/texture %) texture-selection)))
                                    first)]
        {:category-id   (:catalog/category-id canonical-category)
         :category-slug (:page/slug canonical-category)
         :selections    query-map})

      ;; Are we on a bundle texture category page?
      (:seo/self-referencing-texture? requested-category)
      {:category-id   (:catalog/category-id requested-category)
       :category-slug (:page/slug requested-category)
       :selections    query-map}

      ;; Are we on the wigs ICP?
      ;; wigs icp is the only category that uses :hair/family as a filter.
      (and family-selection
           (= (count family-selection) 1))
      (let [canonical-category (->> categories
                                    (filter #(and (= 1 (count (:hair/family %)))
                                                  (some (:hair/family %) family-selection)))
                                    first)
            canonical-texture  (some-> requested-category
                                       :hair/texture
                                       first)]
        (merge {:category-id   (:catalog/category-id canonical-category)
                :category-slug (:page/slug canonical-category)}
               (if canonical-texture
                 {:selections (merge query-map {"texture" canonical-texture})}
                 {:selections query-map})))

      :else {:category-id   (:catalog/category-id requested-category)
             :category-slug (:page/slug requested-category)})))

(defn wig-category? [category]
  (-> category
      :hair/family
      first
      products/wig-families))
