(ns storefront.platform.numbers)

(def digits (into #{} (map str (range 0 10))))

(defn digits-only [value]
  (clojure.string/replace value #"[^0-9]" ""))

(defn parse-float [s]
  (Float/parseFloat (str s)))

(defn parse-int [s]
  (Math/round (Math/floor (parse-float (str s)))))

(def to-float float)

(defn abs [x]
  (Math/abs x))

(defn round [x]
  (Math/round x))
