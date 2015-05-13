(ns storefront.controllers
  (:require [storefront.events :as events]
            [storefront.state :as state]
            [storefront.api :as api]
            [storefront.routes :as routes]
            [storefront.cookie-jar :as cookie-jar]
            [storefront.taxons :refer [taxon-name-from]]
            [cljs.core.async :refer [put!]]))

(defmulti perform-effects identity)
(defmethod perform-effects :default [dispatch event args app-state])

(defmethod perform-effects events/navigate [_ event args app-state]
  (api/get-taxons (get-in app-state state/event-ch-path))
  (api/get-store (get-in app-state state/event-ch-path)
                 (get-in app-state state/store-slug-path))
  (api/get-sms-number (get-in app-state state/event-ch-path))
  (set! (.. js/document -body -scrollTop) 0)
  (when-not (or
             (empty? (get-in app-state state/flash-success-nav-path))
             (= [event args] (get-in app-state state/flash-success-nav-path)))
    (put! (get-in app-state state/event-ch-path)
          [events/flash-dismiss-success])))

(defmethod perform-effects events/navigate-category [_ event {:keys [taxon-path]} app-state]
  (api/get-products (get-in app-state state/event-ch-path)
                    (taxon-name-from taxon-path)))

(defmethod perform-effects events/navigate-product [_ event {:keys [product-path]} app-state]
  (api/get-product (get-in app-state state/event-ch-path)
                   product-path))

(defmethod perform-effects events/navigate-stylist-commissions [_ event args app-state]
  (api/get-stylist-commissions (get-in app-state state/event-ch-path)
                               (get-in app-state state/user-token-path)))

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

(defmethod perform-effects events/control-forgot-password-submit [_ event args app-state]
  (api/forgot-password (get-in app-state state/event-ch-path)
                       (get-in app-state state/forgot-password-email-path)))

(defmethod perform-effects events/control-reset-password-submit [_ event args app-state]
  (api/reset-password (get-in app-state state/event-ch-path)
                      (get-in app-state state/reset-password-password-path)
                      (get-in app-state state/reset-password-password-confirmation-path)
                      (get-in app-state state/reset-password-token-path)))

(defmethod perform-effects events/control-manage-account-submit [_ event args app-state]
  (api/update-account (get-in app-state state/event-ch-path)
                      (get-in app-state state/user-id-path)
                      (get-in app-state state/manage-account-email-path)
                      (get-in app-state state/manage-account-password-path)
                      (get-in app-state state/manage-account-password-confirmation-path)
                      (get-in app-state state/user-token-path)))

(defmethod perform-effects events/api-success-sign-in [_ event args app-state]
  (cookie-jar/set-login (get-in app-state state/cookie-path)
                        (get-in app-state state/user-path)
                        {:remember? (get-in app-state state/sign-in-remember-path)})
  (routes/enqueue-navigate app-state events/navigate-home)
  (put! (get-in app-state state/event-ch-path)
        [events/flash-show-success {:message "Logged in successfully"
                                    :navigation [events/navigate-home {}]}]))

(defmethod perform-effects events/api-success-sign-up [_ event args app-state]
    (cookie-jar/set-login (get-in app-state state/cookie-path)
                        (get-in app-state state/user-path)
                        {:remember? true})
  (routes/enqueue-navigate app-state events/navigate-home)
  (put! (get-in app-state state/event-ch-path)
        [events/flash-show-success {:message "Welcome! You have signed up successfully."
                                    :navigation [events/navigate-home {}]}]))

(defmethod perform-effects events/api-success-forgot-password [_ event args app-state]
  (routes/enqueue-navigate app-state events/navigate-home))

(defmethod perform-effects events/api-success-reset-password [_ event args app-state]
  (routes/enqueue-navigate app-state events/navigate-home)
  (put! (get-in app-state state/event-ch-path)
        [events/flash-show-success {:message "Your password was changed successfully. You are now signed in."
                                    :navigation [events/navigate-home {}]}]))

(defmethod perform-effects events/api-success-manage-account [_ event args app-state]
  (cookie-jar/set-login (get-in app-state state/cookie-path)
                        (get-in app-state state/user-path)
                        {:remember? true})
  (routes/enqueue-navigate app-state events/navigate-home)
  (put! (get-in app-state state/event-ch-path)
        [events/flash-show-success {:message "Account updated"
                                    :navigation [events/navigate-home {}]}]))
