(ns storefront.platform.numbers)

(defn parse-float [s]
  (Float/parseFloat (str s)))

(defn parse-int [s]
  (Math/round (Math/floor (parse-float (str s)))))

(def to-float float)

(defn abs [x]
  (Math/abs x))

(defn round [x]
  (Math/round x))
