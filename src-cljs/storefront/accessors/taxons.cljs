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

(defn- default-taxon-path [filter-fn app-state]
  (when-let [default-taxon (-> (get-in app-state keypaths/taxons)
                               filter-fn
                               first)]
    (taxon-path-for default-taxon)))

(def default-nav-taxon-path (partial default-taxon-path filter-nav-taxons))
(def default-stylist-taxon-path (partial default-taxon-path filter-stylist-taxons))

(defn taxon-class-name [taxon]
  (string/replace (:permalink taxon) #"/" "-"))
