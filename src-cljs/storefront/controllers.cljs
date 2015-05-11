(ns storefront.controllers
  (:require [storefront.events :as events]
            [storefront.state :as state]
            [storefront.api :as api]
            [storefront.routes :as routes]
            [storefront.cookie-jar :as cookie-jar]))

(defmulti perform-effects identity)
(defmethod perform-effects :default [dispatch event args app-state])

(defmethod perform-effects events/navigate [_ event args app-state]
  (api/get-taxons (get-in app-state state/event-ch-path))
  (api/get-store (get-in app-state state/event-ch-path)
                 (get-in app-state state/store-slug-path))
  (set! (.. js/document -body -scrollTop) 0))


(defmethod perform-effects events/navigate-category [_ event args app-state]
  (api/get-products (get-in app-state state/event-ch-path)
                    (get-in app-state state/browse-taxon-path)))

(defmethod perform-effects events/control-menu-expand [_ event args app-state]
  (set! (.. js/document -body -style -overflow) "hidden"))

(defmethod perform-effects events/control-menu-collapse [_ event args app-state]
  (set! (.. js/document -body -style -overflow) "auto"))

(defmethod perform-effects events/control-sign-in-submit [_ event args app-state]
  (api/sign-in (get-in app-state state/event-ch-path)
               (get-in app-state state/sign-in-email-path)
               (get-in app-state state/sign-in-password-path)))

(defmethod perform-effects events/control-sign-up-submit [_ event args app-state]
  (api/sign-up (get-in app-state state/event-ch-path)
               (get-in app-state state/sign-up-email-path)
               (get-in app-state state/sign-up-password-path)
               (get-in app-state state/sign-up-password-confirmation-path)))

(defmethod perform-effects events/control-sign-out [_ event args app-state]
  (cookie-jar/clear-login (get-in app-state state/cookie-path)))

(defn store-auth-and-redirect [app-state]
  (cookie-jar/set-login (get-in app-state state/cookie-path)
                        (get-in app-state state/user-path))
  (routes/enqueue-navigate app-state events/navigate-home))

(defmethod perform-effects events/api-success-sign-in [_ event args app-state]
  (store-auth-and-redirect app-state))

(defmethod perform-effects events/api-success-sign-up [_ event args app-state]
  (store-auth-and-redirect app-state))
