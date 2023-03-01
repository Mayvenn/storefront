(ns catalog.cms-dynamic-content
  (:require  #?@(:cljs [[goog.string]])
             [clojure.set :as set]
             [spice.selector :as selector]
             [spice.maps :as maps]
             [clojure.walk :as walk]))

(defn derive-product-details
  [cms-dynamic-content-data sku]
  #?(:cljs
     (do
       (->> {:product-overview-description :pdp.details.overview/description
             ;:pdp/colorable                :pdp.details.care/can-it-be-colored?
             ;:pdp/in-store-services        :pdp.details.customize-your-wig/in-store-services
             ;:pdp/maintenance-level        :pdp.details.care/maintenance-level
             }
            (set/rename-keys cms-dynamic-content-data)
            (maps/map-values
             (fn [template-slot]
               (selector/match-essentials (assoc sku :selector/essentials (into #{} (comp
                                                                                     (map :selector)
                                                                                     (mapcat keys))
                                                                                (:selectable-values template-slot)))
                                          ;; TODO Move the code responsible for merging the selector attributes into the top level
                                          ;; of the selectable value to the API / Handler areas
                                          (map (fn [selectable-value ]
                                                 (merge selectable-value
                                                        (:selector selectable-value)))
                                               (:selectable-values template-slot)))
               template-slot))))))

;; TODO: Remap keys from contentful to our keys
