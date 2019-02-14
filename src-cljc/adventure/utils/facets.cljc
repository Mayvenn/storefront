(ns adventure.utils.facets
  (:require [clojure.set :as set]))

(defn adventure-facet-options
  "Return all facet options available for adventure for a specific facet in the natural facet option ordering."
  [facet-slug facets]
  (->> facets
       (filter (comp #{facet-slug} :facet/slug))
       first
       :facet/options
       (filter :adventure/name)
       (sort-by :filter/order)))

(defn ^:private available-facet-options
  "Returns a set of all facet options that a collection of products or skus have"
  [facet-slug products-or-skus]
  (apply set/union (map (comp set facet-slug) products-or-skus)))

(defn available-adventure-facet-options
  "Returns a coll of facet options available in the collection of products or skus sorted by natural ordering.

  Or more verbosely, returns all facet options from the facet where:

   - There's at least one product or sku that has that option value
   - sorted by the facet option's :filter/order key
   - is available for adventure
  "
  [facet-slug facets products-or-skus]
  (let [matching-facet-options      (adventure-facet-options facet-slug facets)
        matching-product-options    (available-facet-options facet-slug products-or-skus)
        facet-options-has-products? (comp matching-product-options str :option/slug)]
    (filter facet-options-has-products? matching-facet-options)))
