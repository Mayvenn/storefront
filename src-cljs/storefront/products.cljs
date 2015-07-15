(ns storefront.products
  (:require [clojure.string :as string]))

(defn- str-contains? [substr str]
  (not= -1 (.indexOf str substr)))

(def ^:private malaysian? (partial str-contains? "malaysian"))
(def ^:private peruvian? (partial str-contains? "peruvian"))
(def ^:private brazilian? (partial str-contains? "brazilian"))
(def ^:private indian? (partial str-contains? "indian"))
(def ^:private russian? (partial str-contains? "russian"))

(defn collection->grade [collection-name product-name]
  (let [downcased-name (string/lower-case product-name)]
    (or ({"ultra" "8A Peruvian"
          "deluxe" "7A Peruvian"} collection-name)
        (cond
          (malaysian? downcased-name)
          "6A Malaysian"

          (peruvian? downcased-name)
          "6A Peruvian"

          (brazilian? downcased-name)
          "5A Brazilian"

          (indian? downcased-name)
          "6A Indian"

          (russian? downcased-name)
          "6A Russian"))))

(defn strip-origin-and-collection [product-name]
  (string/replace
   product-name
   #"(?:Peruvian|Malaysian|Peruvian|Brazilian|Indian|Russian|Ultra|Deluxe)\s"
   ""))
