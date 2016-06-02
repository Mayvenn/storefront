(ns storefront.utils.query
  (:refer-clojure :exclude [get]))

(defn matches? [query object]
  ;; note, key can be a function
  (every? (fn [[key val]] (= (key object) val)) query))

(defn all [query objects]
  (filter (partial matches? query) objects))

(defn get [query objects]
  (first (all query objects)))

(defn ^:private filter-update-step
  "Single step for the update-where function."
  [filter-pred update-fn args]
  (fn [[matched acc] x]
    (if (filter-pred x)
      [true (conj acc (apply update-fn x args))]
      [matched (conj acc x)])))

(defn update-where
  "Update-in analog for sequences that no-ops if no matches were found."
  [s filter-pred update-fn & args]
  (let [[matched out-seq] (reduce (filter-update-step filter-pred update-fn args)
                                  [false []]
                                  s)]
    out-seq))
