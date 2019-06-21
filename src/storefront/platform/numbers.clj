(ns storefront.platform.numbers)

(def digits (into #{} (map str (range 0 10))))

(defn digits-only [value]
  (clojure.string/replace value #"[^0-9]" ""))

(defn abs [x]
  (Math/abs x))

(defn round [x]
  (Math/round x))
