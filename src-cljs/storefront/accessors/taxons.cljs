(ns storefront.accessors.taxons
  (:require [clojure.string :as string]
            [storefront.utils.query :as query]
            [storefront.keypaths :as keypaths]))

(def filter-nav-taxons
  (partial remove :stylist_only?))

(def filter-stylist-taxons
  (partial filter :stylist_only?))

(defn current-taxon [app-state]
  (query/get (get-in app-state keypaths/browse-taxon-query)
             (get-in app-state keypaths/taxons)))

(def new-taxon? #{"frontals"})

(def slug->name
  {"stylist-products" "kits"})

(def is-closure? (comp #{"frontals" "closures"} :slug))
(def is-stylist-product? (comp #{"stylist-products"} :slug))
(defn is-extension? [taxon]
  (not (or (is-closure? taxon)
           (is-stylist-product? taxon))))
