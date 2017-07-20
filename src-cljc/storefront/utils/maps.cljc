(ns storefront.utils.maps)

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
  "Removes keys from map m that have nil values."
  [m]
  (if (map? m)
    (into {} (filter (comp not nil? val) m))
    m))

(defn multimap-assoc
  "A multimap is a map where keys can have more than one value. Returns a new multimap with the key updated to include the value"
  [mm k v]
  (update mm k (fnil conj #{}) v))

(defn into-multimap
  "A multimap is a map where keys can have more than one value. Converts a sequence of maps into a multimap"
  [ms]
  (reduce (fn [mm m] (reduce-kv multimap-assoc mm m)) {} ms))
