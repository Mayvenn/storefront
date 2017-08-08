(ns storefront.utils.maps)

(defn map-values
  "Applys a function to every value in a map, i.e. the functor instance for
  key-value pairs."
  [f m]
  (into {} (map (fn [[k v]] [k (f v)])) m))

(defn key-by
  "Returns a map containing the elements (e) of coll indexed by the result of (f e)

  (key-by :a [{:a 2 :b 5} {:a 3 :b 6}]) ->
  {2 {:a 2 :b 5}
   3 {:a 3 :b 6}} "
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
