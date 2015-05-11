(ns storefront.taxons
  (:require [clojure.string :as string]))

(defn taxon-path-for [taxon]
  (string/replace (:name taxon) #" " "-"))

(defn taxon-name-from [taxon-path]
  (string/replace taxon-path #"-" " "))
