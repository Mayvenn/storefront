(ns storefront.accessors.named-searches
  (:require [storefront.utils.query :as query]
            [storefront.accessors.products :as products]
            [storefront.keypaths :as keypaths]))

(defn current-named-search [app-state]
  (query/get (get-in app-state keypaths/browse-named-search-query)
             (get-in app-state keypaths/named-searches)))

(defn current-named-searches [app-state]
  (->> (get-in app-state keypaths/named-searches)
       (query/all (dissoc (get-in app-state keypaths/browse-named-search-query) :slug))))

(defn first-with-product-id [named-searches product-id]
  (->> named-searches
       (filter #(-> % :product-ids set (contains? product-id)))
       first))

(def new-named-search?
  "NB: changes here should be reflected in accessors.categories until experiments/new-taxon-launch? is 100%"
  #{"360-frontals"
    "yaki-straight"
    "water-wave"})

(defn is-closure? [named-search] (some-> named-search :search :category set (contains? "closures")))
(defn is-frontal? [named-search] (some-> named-search :search :category set (contains? "frontals")))
(defn is-360-frontal? [named-search] (some-> named-search :search :category set (contains? "360-frontals")))
(defn is-extension? [named-search] (some-> named-search :search :category set (contains? "hair")))
(defn is-closure-or-frontal? [named-search] (or (is-closure? named-search)
                                                (is-frontal? named-search)
                                                (is-360-frontal? named-search)))
(defn is-hair? [named-search] (or (is-extension? named-search) (is-closure-or-frontal? named-search)))

(def is-stylist-product? :stylist_only?)
(def eligible-for-reviews? (complement is-stylist-product?))
(def eligible-for-triple-bundle-discount? is-hair?)
