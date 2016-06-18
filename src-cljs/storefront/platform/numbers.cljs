(ns storefront.platform.numbers)

(defn parse-float [s]
  (js/parseFloat s))

(def to-float parse-float)
