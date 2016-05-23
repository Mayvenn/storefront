(ns storefront.accessors.taxons
  (:require [storefront.utils.query :as query]
            [storefront.keypaths :as keypaths]))

(defn current-taxon [app-state]
  (query/get (get-in app-state keypaths/browse-taxon-query)
             (get-in app-state keypaths/taxons)))

(def new-taxon? #{"frontals"})

(def slug->name
  {"stylist-products" "kits"})

(def is-closure? (comp #{"frontals" "closures"} :slug))
(def is-stylist-product? :stylist_only?)
(defn is-extension? [taxon]
  (not (or (is-closure? taxon)
           (is-stylist-product? taxon))))
