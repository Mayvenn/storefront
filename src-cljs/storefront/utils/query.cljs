(ns storefront.utils.query
  (:refer-clojure :exclude [get]))

(defn matches? [query object]
  ;; note, key can be a function
  (every? (fn [[key val]] (= (key object) val)) query))

(defn all [query objects]
  (filter (partial matches? query) objects))

(defn get [query objects]
  (first (all query objects)))
