(ns adventure.utils.facets
  (:require [clojure.set :as set]))

(defn adventure-facet-options
  "Return all facet options available for adventure for a specific facet in the
   natural facet option ordering."
  [facet-slug facets]
  (->> facets
       (filter (comp #{facet-slug} :facet/slug))
       first
       :facet/options
       (filter :adventure/name)
       (sort-by :filter/order)))

(defn ^:private available-facet-options
  "Returns a set of all facet options that the coll of skuers have"
  [facet-slug skuers]
  (apply set/union (map (comp set facet-slug) skuers)))

(defn available-adventure-facet-options
  "Returns a coll of facet options available in the collection of skuers
   sorted by natural ordering.

   Or more verbosely, returns all facet options from the facet where:

   - There's at least one skuer that has that option value
   - sorted by the facet option's :filter/order key
   - is available for adventure"
  [facet-slug facets skuers]
  (let [facet-options               (adventure-facet-options facet-slug facets)
        options-in-skuers?          (available-facet-options facet-slug skuers)
        facet-options-has-products? (comp options-in-skuers? str :option/slug)]
    (filter facet-options-has-products? facet-options)))

(defn available-options
  [facets facet skuers]
  (available-adventure-facet-options facet facets skuers))
