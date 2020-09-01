(ns storefront.accessors.categories
  (:require [storefront.accessors.products :as products]
            [catalog.keypaths]
            [catalog.skuers :as skuers]
            [cemerick.url :as cemerick-url]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.string :as string]
            clojure.set
            [spice.maps :as maps]
            [spice.selector :as selector]
            [storefront.keypaths :as keypaths]))

(def query-param-separator "~")

(def query-params->facet-slugs
  {:grade         :hair/grade
   :family        :hair/family
   :origin        :hair/origin
   :weight        :hair/weight
   :texture       :hair/texture
   :base-material :hair/base-material
   :color         :hair/color
   :length        :hair/length
   :color.process :hair/color.process
   :style         :wig/trait})

(defn sort-query-params
  [params]
  (let [ordering {"origin"        0
                  "texture"       1
                  "style"         2
                  "color"         3
                  "base-material" 4
                  "weight"        5
                  "family"        6}]
    (into (sorted-map-by
           (fn [key1 key2]
             (compare (get ordering key1 100)
                      (get ordering key2 100))))
          params)))

(def ^:private facet-slugs->query-params
  (clojure.set/map-invert query-params->facet-slugs))

(defn category-selections->query-params
  [category-selections]
  (->> category-selections
       (maps/map-values (fn [s] (string/join query-param-separator s)))
       (maps/map-keys (comp (fnil name "") facet-slugs->query-params))
       sort-query-params))

(defn query-params->selector-electives [query-params]
  (->> (maps/select-rename-keys query-params query-params->facet-slugs)
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

(defn canonical-category-data
  "With ICPs, the 'canonical category id' may be different from the ICP category
  id. E.g. 13-wigs with a selected family of 'lace-front-wigs' will have a
  canonical cateogry id of 24, or in other words, lace-front-wigs' category id."
  [categories requested-category current-nav-url]
  (let [family-selection (some-> current-nav-url
                                 :query
                                 #?(:clj cemerick-url/query->map
                                    :cljs identity)
                                 (get "family")
                                 (string/split #"~"))]
    (cond
      (and family-selection (= (count family-selection) 1))
      (let [canonical-category (->> categories
                                    (filter #(and (= 1 (count (:hair/family %)))
                                                  (some (:hair/family %) family-selection)))
                                    first)
            canonical-texture  (some-> requested-category
                                       :hair/texture
                                       first)]
        (merge {:category-id   (:catalog/category-id canonical-category)
                :category-slug (:page/slug canonical-category)}
               (when canonical-texture
                 {:selections {:texture canonical-texture}})))

      :else {:category-id   (:catalog/category-id requested-category)
             :category-slug (:page/slug requested-category)})))

(defn wig-category? [category]
  (-> category
      :hair/family
      first
      products/wig-families))
