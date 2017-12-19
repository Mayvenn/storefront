(ns storefront.accessors.facets)

(defn color-order-map [facets]
  (->> facets
       (filter #(= (:facet/slug %) :hair/color))
       first
       :facet/options
       (sort-by :filter/order)
       (map :option/slug)
       (map-indexed (fn [idx slug] [slug idx]))
       (into {})))
