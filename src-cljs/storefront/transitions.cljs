(ns storefront.transitions
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
  (assoc-in app-state state/browse-taxon-path taxon-path))

(defmethod transition-state events/navigate-product [_ event {:keys [product-path]} app-state]
  (-> app-state
      (assoc-in state/browse-product-path product-path)
      (assoc-in state/browse-variant-path nil)
      (assoc-in state/browse-variant-quantity-path 1)))

(defmethod transition-state events/control-menu-expand [_ event args app-state]
  (assoc-in app-state state/menu-expanded-path true))

(defmethod transition-state events/control-menu-collapse [_ event args app-state]
  (assoc-in app-state state/menu-expanded-path false))

(defmethod transition-state events/control-account-menu-expand [_ event args app-state]
  (assoc-in app-state state/account-menu-expanded-path true))

(defmethod transition-state events/control-account-menu-collapse [_ event args app-state]
  (assoc-in app-state state/account-menu-expanded-path false))

(defmethod transition-state events/control-sign-in-change [_ event args app-state]
  (update-in app-state state/sign-in-path merge args))

(defmethod transition-state events/control-sign-up-change [_ event args app-state]
  (update-in app-state state/sign-up-path merge args))

(defmethod transition-state events/control-sign-out [_ event args app-state]
  ;; FIXME clear other user specific pieces of state
  (assoc-in app-state state/user-path {}))

(defmethod transition-state events/control-variant-select [_ event {:keys [variant]} app-state]
  (assoc-in app-state state/browse-variant-path (variant :id)))

(defmethod transition-state events/control-variant-inc-quantity [_ event args app-state]
  (update-in app-state state/browse-variant-quantity-path inc))

(defmethod transition-state events/control-variant-dec-quantity [_ event args app-state]
  (update-in app-state state/browse-variant-quantity-path (comp (partial max 1) dec)))

(defmethod transition-state events/control-variant-set-quantity [_ event {:keys [value-str]} app-state]
  (assoc-in app-state state/browse-variant-quantity-path
            (-> (js/parseInt value-str 10)
                (Math/abs)
                (max 1))))

(defmethod transition-state events/api-success-taxons [_ event args app-state]
  (assoc-in app-state state/taxons-path (:taxons args)))

(defmethod transition-state events/api-success-store [_ event args app-state]
  (assoc-in app-state state/store-path args))

(defmethod transition-state events/api-success-products [_ event {:keys [taxon-path products]} app-state]
  (update-in app-state state/products-for-taxons-path assoc taxon-path products))

(defn sign-in-user [{:keys [email token store_slug]} app-state]
  (-> app-state
      (assoc-in state/user-email-path email)
      (assoc-in state/user-token-path token)
      (assoc-in state/user-store-slug-path store_slug)
      (update-in state/sign-in-path merge {:email "" :password ""})
      (update-in state/sign-up-path merge {:email "" :password "" :password-confirmation ""})))

(defmethod transition-state events/api-success-sign-in [_ event args app-state]
  (sign-in-user args app-state))

(defmethod transition-state events/api-success-sign-up [_ event args app-state]
  (sign-in-user args app-state))
