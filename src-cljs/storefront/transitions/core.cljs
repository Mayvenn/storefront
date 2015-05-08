(ns storefront.transitions.core
  (:require [storefront.events :as events]
            [storefront.state :as state]
            [storefront.routes :as routes]
            [storefront.taxons :refer [taxon-path-for]]))

(defmulti transition-state identity)
(defmethod transition-state :default [event arg app-state]
  (js/console.error "Transitioned via default, probably shouldn't. " (prn-str event))
  app-state)

(defmethod transition-state events/navigate-home [event args app-state]
  (assoc-in app-state state/navigation-event-path events/navigate-home))

(defmethod transition-state events/navigate-category [event {:keys [taxon-path]} app-state]
  (let [taxons (get-in app-state state/taxons-path)
        taxon (first (filter #(= (taxon-path-for %) taxon-path) taxons))]
    (-> app-state
        (assoc-in state/browse-taxon-path taxon)
        (assoc-in state/navigation-event-path events/navigate-category))))

(defmethod transition-state events/api-success-taxons [event args app-state]
  (assoc-in app-state state/taxons-path (:taxons args)))

(defmethod transition-state events/api-success-store [event args app-state]
  (assoc-in app-state state/store-path args))

(defmethod transition-state events/api-success-products [event {:keys [taxon-id products]} app-state]
  (update-in app-state state/products-for-taxons-path assoc taxon-id products))
