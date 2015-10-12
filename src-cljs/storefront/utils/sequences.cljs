(ns storefront.utils.sequences)

(defn update-vals [f m]
   (reduce-kv #(assoc %1 %2 (f %3)) {} m))

(defn index-sequence [s]
  (map vector (iterate inc 0) s))
