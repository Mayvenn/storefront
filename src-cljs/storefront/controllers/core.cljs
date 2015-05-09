(ns storefront.controllers.core
  (:require [storefront.events :as events]
            [storefront.state :as state]
            [storefront.api :as api]
            [storefront.routes :as routes]))

(defmulti perform-effects identity)
(defmethod perform-effects :default [dispatch event args app-state])

(defmethod perform-effects events/navigate [_ event args app-state]
  (set! (.. js/document -body -scrollTop) 0))

(defmethod perform-effects events/navigate-home [_ event args app-state]
  (api/get-store (get-in app-state state/event-ch-path)
                 (get-in app-state state/store-slug-path))
  (api/get-taxons (get-in app-state state/event-ch-path)))

(defmethod perform-effects events/navigate-category [_ event args app-state]
  (api/get-products (get-in app-state state/event-ch-path)
                    (:id (get-in app-state state/browse-taxon-path))))

(defmethod perform-effects events/control-menu-expand [_ event args app-state]
  (set! (.. js/document -body -style -overflow) "hidden"))

(defmethod perform-effects events/control-menu-collapse [_ event args app-state]
  (set! (.. js/document -body -style -overflow) "auto"))

(defmethod perform-effects events/control-sign-in-submit [_ event args app-state]
  (api/sign-in (get-in app-state state/event-ch-path)
               (get-in app-state state/sign-in-email-path)
               (get-in app-state state/sign-in-password-path)))

(defmethod perform-effects events/api-success-sign-in [_ event args app-state]
  (routes/enqueue-navigate app-state events/navigate-home))
