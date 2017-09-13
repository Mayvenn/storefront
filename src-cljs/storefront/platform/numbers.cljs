(ns storefront.platform.numbers
  (:require [clojure.string :as string]))

(def digits (into #{} (map str (range 0 10))))

(defn digits-only [value]
  (when value (string/replace value #"[^0-9]" "")))

(defn parse-float [s]
  (js/parseFloat s))

(defn parse-int [s]
  (int s))

(def to-float parse-float)

(defn abs [x]
  (js/Math.abs x))

(defn round [x]
  (js/Math.round x))
