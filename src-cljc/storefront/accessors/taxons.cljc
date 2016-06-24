(ns storefront.accessors.taxons
  (:require [storefront.utils.query :as query]
            [storefront.accessors.products :as products]
            [storefront.request-keys :as request-keys]
            [storefront.keypaths :as keypaths]))

(defn current-taxon [app-state]
  (query/get (get-in app-state keypaths/browse-taxon-query)
             (get-in app-state keypaths/taxons)))

(defn current-taxons [app-state]
  (query/all (dissoc (get-in app-state keypaths/browse-taxon-query) :slug)
             (get-in app-state keypaths/taxons)))

(defn products-loaded? [app-state taxon]
  (every? (products/loaded-ids app-state) (:product-ids taxon)))

(def new-taxon? #{"frontals"})

(def is-closure? (comp #{"frontals" "closures"} :slug))
(def is-stylist-product? :stylist_only?)
(defn is-extension? [taxon]
  (not (or (is-closure? taxon)
           (is-stylist-product? taxon))))
