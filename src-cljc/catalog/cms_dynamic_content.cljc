(ns catalog.cms-dynamic-content
  (:require  #?@(:cljs [[goog.string]])
             [clojure.set :refer [rename-keys]]
             [spice.selector :as selector]
             [spice.maps]))

(defn derive-product-details
  [cms-dynamic-content-data sku]
  #?(:cljs
     (rename-keys
      (->> cms-dynamic-content-data
           vals
           (mapcat :applicable-pdp-products)
           (filter #(seq (selector/match-all {} (-> %
                                                    :selector
                                                    clojure.walk/keywordize-keys) [sku])))
           (map (juxt (comp keyword :content-slot-id) (fn [{:keys [content-value]}]
                                                        (goog.string/unescapeEntities content-value))))
           (group-by first)
           (spice.maps/map-values (comp second first))
           (into {}))
      {:pdp/colorable :pdp.details.care/can-it-be-colored?
       :pdp/description :pdp.details.overview/description
       :pdp/in-store-services :pdp.details.customize-your-wig/in-store-services
       :pdp/maintenance-level :pdp.details.care/maintenance-level})))

;; TODO: Remap keys from contentful to our keys
