(ns storefront.accessors.taxons
  (:require [clojure.string :as string]
            [storefront.utils.query :as query]
            [storefront.keypaths :as keypaths]))

(def filter-nav-taxons
  (partial remove :stylist_only?))

(def filter-stylist-taxons
  (partial filter :stylist_only?))

(defn default-stylist-taxon-slug [app-state]
  (->> (get-in app-state keypaths/taxons)
       filter-stylist-taxons
       first
       :slug))

(defn current-taxon [app-state]
  (query/get (get-in app-state keypaths/browse-taxon-query)
             (get-in app-state keypaths/taxons)))
