(ns catalog.cms-dynamic-content
  (:require [spice.selector :as selector]
            [spice.maps]))

(defn derive-product-details
  [cms-dynamic-content-data sku]
  (->> cms-dynamic-content-data
       (filter #(seq (selector/match-all {} (-> %
                                                :selector
                                                clojure.walk/keywordize-keys) [sku])))
       (map (juxt (comp keyword :content-slot-id) :content-value))
       (group-by first)
       (spice.maps/map-values (partial map second))
       (into {})))
