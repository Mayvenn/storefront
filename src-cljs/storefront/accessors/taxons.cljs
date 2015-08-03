(ns storefront.accessors.taxons
  (:require [clojure.string :as string]
            [storefront.keypaths :as keypaths]))

(def filter-nav-taxons
  (partial filter (complement :stylist_only?)))

(def filter-stylist-taxons
  (partial filter :stylist_only?))

(defn taxon-path-for [taxon]
  (string/replace (:name taxon) #" " "-"))

(defn taxon-name-from [taxon-path]
  (string/replace taxon-path #"-" " "))

(defn default-taxon-path [app-state]
  (when-let [default-taxon (-> (get-in app-state keypaths/taxons)
                               filter-nav-taxons
                               first)]
    (taxon-path-for default-taxon)))

(defn taxon-class-name [taxon]
  (string/replace (:permalink taxon) #"/" "-"))
