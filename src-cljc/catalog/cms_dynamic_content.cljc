(ns catalog.cms-dynamic-content
  (:require [clojure.set :refer [rename-keys]]
            [spice.selector :as selector]
            [spice.maps]))

(defn derive-product-details
  [cms-dynamic-content-data sku]
  (rename-keys (->> cms-dynamic-content-data
                    vals
                    (filter #(seq (selector/match-all {} (-> %
                                                             :selector
                                                             clojure.walk/keywordize-keys) [sku])))
                    (map (juxt (comp keyword :content-slot-id) :content-value))
                    (group-by first)
                    (spice.maps/map-values (partial map second))
                    (into {}))
               {:pdp/colorable :pdp.details.care/can-it-be-colored?}))
