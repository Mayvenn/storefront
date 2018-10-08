(ns storefront.accessors.promos)

(def bundle-discount-description "Save 10% - Purchase 3 or more bundles")
(def freeinstall-description "GET A FREE INSTALL - USE CODE: FREEINSTALL")

(defn promotion-lookup-map [promotions]
  (->> promotions
       (filter :code)
       (map (juxt :code identity))
       (into {})))

(defn find-promotion-by-code [promotions code]
  ((promotion-lookup-map promotions) code))

(defn default-advertised-promotion [promotions]
  (first (filter :advertised promotions)))
