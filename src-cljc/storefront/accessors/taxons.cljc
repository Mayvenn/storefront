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

(defn first-with-product-id [taxons product-id]
  (->> taxons
       (filter (fn [taxon] (-> (:product-ids taxon)
                              set
                              (contains? product-id))))
       first))

(def new-taxon? #{"frontals"})

(defn is-closure? [taxon] (some-> taxon :search :category set (contains? "closures")))
(defn is-frontal? [taxon] (some-> taxon :search :category set (contains? "frontals")))
(defn is-extension? [taxon] (some-> taxon :search :category set (contains? "hair")))
(defn is-closure-or-frontal? [taxon] (or (is-closure? taxon) (is-frontal? taxon)))
(defn is-hair? [taxon] (or (is-extension? taxon) (is-closure-or-frontal? taxon)))

(def is-stylist-product? :stylist_only?)
(def eligible-for-reviews? (complement is-stylist-product?))
(def eligible-for-triple-bundle-discount? is-hair?)
