(ns catalog.cms-dynamic-content
  (:require  #?@(:cljs [[goog.string]])
             [clojure.set :refer [rename-keys]]
             [spice.selector :as selector]
             [spice.maps]))

(defn derive-product-details
  [cms-dynamic-content-data sku]
  #?(:cljs
     (rename-keys (->> cms-dynamic-content-data
                       vals
                       (filter #(seq (selector/match-all {} (-> %
                                                                :selector
                                                                clojure.walk/keywordize-keys) [sku]))) 
                       (remove (fn [{:keys [content-value]}]
                                 (nil? content-value)))
                       (map (juxt (comp keyword :content-slot-id) (fn [{:keys [content-value]}]
                                                                    (goog.string/unescapeEntities content-value))))
                       (group-by first)
                       ;; TODO not sure this is right
                       (spice.maps/map-values (comp second first))
                       (into {}))
                  {:pdp/colorable :pdp.details.care/can-it-be-colored?})))
