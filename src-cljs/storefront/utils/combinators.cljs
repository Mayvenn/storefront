(ns storefront.utils.combinators)

(defn map-values
  "Applys a function to every value in a map, i.e. the functor instance for
  key-value pairs."
  [f m]
  (into {} (map (fn [[k v]] [k (f v)])) m))
