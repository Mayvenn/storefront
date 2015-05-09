(ns storefront.transitions.core
  (:require [storefront.events :as events]
            [storefront.state :as state]
            [storefront.routes :as routes]
            [storefront.taxons :refer [taxon-path-for]]))

(defmulti transition-state identity)
(defmethod transition-state :default [dispatch event arg app-state]
  app-state)

(defmethod transition-state events/navigate [_ event args app-state]
  (assoc-in app-state state/navigation-event-path event))

(defmethod transition-state events/navigate-category [_ event {:keys [taxon-path]} app-state]
  (let [taxons (get-in app-state state/taxons-path)
        taxon (first (filter #(= (taxon-path-for %) taxon-path) taxons))]
    (assoc-in app-state state/browse-taxon-path taxon)))

(defmethod transition-state events/control-menu-expand [_ event args app-state]
  (assoc-in app-state state/menu-expanded-path true))

(defmethod transition-state events/control-menu-collapse [_ event args app-state]
  (assoc-in app-state state/menu-expanded-path false))

(defmethod transition-state events/control-sign-in-change [_ event args app-state]
  (update-in app-state state/sign-in-path merge args))

(defmethod transition-state events/api-success-taxons [_ event args app-state]
  (assoc-in app-state state/taxons-path (:taxons args)))

(defmethod transition-state events/api-success-store [_ event args app-state]
  (assoc-in app-state state/store-path args))

(defmethod transition-state events/api-success-products [_ event {:keys [taxon-id products]} app-state]
  (update-in app-state state/products-for-taxons-path assoc taxon-id products))

(defmethod transition-state events/api-success-sign-in [_ event {:keys [email token store_slug]} app-state]
  (-> app-state
      (assoc-in state/user-email-path email)
      (assoc-in state/user-token-path token)
      (assoc-in state/sign-in-path {})
      (assoc-in state/user-store-slug-path store_slug)))
