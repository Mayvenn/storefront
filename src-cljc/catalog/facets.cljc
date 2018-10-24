(ns catalog.facets
  "Facets are possible shopping attributes like color, texture, length, etc."
  (:require [spice.maps :as maps]
            [storefront.keypaths :as keypaths]))

;; TODO spec out what a facet looks like

(defn by-slug
  [data]
  (->> (get-in data keypaths/v2-facets)
       (map #(update % :facet/options (partial maps/index-by :option/slug)))
       (maps/index-by :facet/slug)))

(defn color-order-map
  "FIXME what kind of 'facets' does this deal with, not sure"
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
  "FIXME what kind of 'facets' does this deal with, not sure"
  [color-slug facets]
  (->> facets
       (filter (fn [facet] (= (:facet/slug facet) :hair/color)))
       first
       :facet/options
       (filter (fn [color] (= (:option/slug color) color-slug)))
       first))
