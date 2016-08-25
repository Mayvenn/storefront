(ns storefront.utils.combinators)

(defn map-values
  "Applys a function to every value in a map, i.e. the functor instance for
  key-value pairs."
  [f m]
  (into {} (map (fn [[k v]] [k (f v)])) m))

(defn key-by
  "Creates a map where the key is the result of a function over each
  element in a collection with the value being the original
  collection. The key function should be injective/one-to-one."
  [f coll]
  (map-values first (group-by f coll)))

(defn filter-nil
  "Removes keys from m that have nil values."
  [m]
  (if (map? m)
    (into {} (filter (comp not nil? val) m))
    m))
