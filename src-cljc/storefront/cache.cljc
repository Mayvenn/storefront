(ns storefront.cache
  (:require [clojure.walk :refer [postwalk]]))

;;  Neccessary for a frustrating bug in Clojurescript that doesn't seem to be
;;  able to hash a deep map correctly. Feel free to delete this when
;;  ClojureScript fixes this error.
(defn unique-serialize
  "Walks a collection and converts every map within into a sorted map, then
  serializes the entirety of it into a string."
  [coll]
  (let [sort-if-map
        #(if (map? %) (into (sorted-map) %) %)]
    (pr-str
     (postwalk sort-if-map coll))))

(defn cache-key [v]
  (pr-str (unique-serialize v)))
