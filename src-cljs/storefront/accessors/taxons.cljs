(ns storefront.accessors.taxons
  (:require [clojure.string :as string]
            [storefront.keypaths :as keypaths]))

(defn taxon-path-for [taxon]
  (string/replace (:name taxon) #" " "-"))

(defn taxon-name-from [taxon-path]
  (string/replace taxon-path #"-" " "))

(defn default-taxon-path [app-state]
  (when-let [default-taxon (first (get-in app-state keypaths/taxons))]
    (taxon-path-for default-taxon)))

(defn taxon-class-name [taxon]
  (string/replace (:permalink taxon) #"/" "-"))
