(ns storefront.platform.numbers)

(defn parse-float [s]
  (Float/parseFloat s))

(def to-float float)

(defn abs [x]
  (Math/abs x))

(defn round [x]
  (Math/round x))
