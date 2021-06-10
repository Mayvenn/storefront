(ns storefront.utils.query
  (:refer-clojure :exclude [get]))

(defn matches? [query object]
  ;; note, key can be a function
  (every? (fn [[key val]] (= (key object) val)) query))

(defn all [query objects]
  (filter (partial matches? query) objects))

(defn get [query objects]
  (first (all query objects)))

(defn starts-with [query object]
  ;; note, key can be a function
  (every? (fn [[qkey qval]]
            (let [obj-val (qkey object)]
              (assert (coll? qval))
              (assert (coll? obj-val))
              (= (take (count qval) obj-val)
                 qval)))
          query))

(defn all-starting-with [query objects]
  (filter (partial starts-with query) objects))

(defn first-starting-with [query objects]
  (first (all-starting-with query objects)))
