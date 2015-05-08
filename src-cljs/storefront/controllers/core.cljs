(ns storefront.controllers.core
  (:require [storefront.events :as events]
            [storefront.state :as state]
            [storefront.api :as api]))

(defmulti perform-effects identity)
(defmethod perform-effects :default [event args app-state])

(defmethod perform-effects events/navigate-home [event args app-state]
  (api/get-store (get-in app-state state/event-ch-path)
                 (get-in app-state state/store-slug-path))
  (api/get-taxons (get-in app-state state/event-ch-path)))

(defmethod perform-effects events/navigate-category [event args app-state]
  (api/get-products (get-in app-state state/event-ch-path)
                    (:id (get-in app-state state/browse-taxon-path))))
