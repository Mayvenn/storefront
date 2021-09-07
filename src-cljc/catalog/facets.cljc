(ns catalog.facets
  "Facets are possible shopping attributes like color, texture, length, etc."
  (:require [spice.maps :as maps]
            [storefront.keypaths :as keypaths]
            [clojure.set :as set]
            [clojure.string :as string]))

;; TODO spec out what a facet looks like

(def query-param>slug
  {:grade         :hair/grade
   :family        :hair/family
   :origin        :hair/origin
   :weight        :hair/weight
   :texture       :hair/texture
   :base-material :hair/base-material
   :color         :hair/color
   :length        :hair/length
   :color.process :hair/color.process
   :style         :wig/trait
   :lace-size     :hair.closure/area})

(def slug>query-param
  (set/map-invert query-param>slug))

(defn by-slug
  [data]
  (->> (get-in data keypaths/v2-facets)
       (map #(update % :facet/options (partial maps/index-by :option/slug)))
       (maps/index-by :facet/slug)))

(defn color-order-map
  [facets]
  (->> facets
       (filter #(= (:facet/slug %) :hair/color))
       first
       :facet/options
       (sort-by :filter/order)
       (map :option/slug)
       (map-indexed (fn [idx slug] [slug idx]))
       (into {})))

(defn get-color
  [color-slug facets]
  (->> facets
       (filter (fn [facet] (= (:facet/slug facet) :hair/color)))
       first
       :facet/options
       (filter (fn [color] (= (:option/slug color) color-slug)))
       first))

(defn hacky-fix-of-bad-slugs-on-facets [slug]
  (string/replace (str slug) #"#" ""))
