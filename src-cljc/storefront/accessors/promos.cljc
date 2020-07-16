(ns storefront.accessors.promos)

(defn promotion-lookup-map [promotions]
  (->> promotions
       (filter :code)
       (map (juxt :code identity))
       (into {})))

(defn find-promotion-by-code [promotions code]
  ((promotion-lookup-map promotions) code))

(defn default-advertised-promotion [promotions]
  (first (filter :advertised promotions)))
