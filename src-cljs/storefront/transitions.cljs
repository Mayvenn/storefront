(ns storefront.transitions
  (:require [storefront.events :as events]
            [storefront.state :as state]
            [storefront.routes :as routes]
            [storefront.taxons :refer [taxon-path-for]]))

(defmulti transition-state identity)
(defmethod transition-state [] [dispatch event args app-state]
  ;; (js/console.log (clj->js event) (clj->js args)) ;; enable to see all events
  app-state)
(defmethod transition-state :default [dispatch event args app-state]
  app-state)

(defmethod transition-state events/navigate [_ event args app-state]
  (assoc-in app-state state/navigation-event-path event))

(defmethod transition-state events/navigate-category [_ event {:keys [taxon-path]} app-state]
  (assoc-in app-state state/browse-taxon-query-path {taxon-path-for taxon-path}))

(defmethod transition-state events/navigate-product [_ event {:keys [product-path query-params]} app-state]
  (let [taxon-id (js/parseInt (:taxon_id query-params))]
    (-> app-state
        (assoc-in state/browse-taxon-query-path {:id taxon-id})
        (assoc-in state/browse-product-query-path {:slug product-path})
        (assoc-in state/browse-variant-query-path nil)
        (assoc-in state/browse-variant-quantity-path 1))))

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
  (assoc-in app-state state/browse-variant-query-path {:id (variant :id)}))

(defmethod transition-state events/control-variant-inc-quantity [_ event args app-state]
  (update-in app-state state/browse-variant-quantity-path inc))

(defmethod transition-state events/control-variant-dec-quantity [_ event args app-state]
  (update-in app-state state/browse-variant-quantity-path (comp (partial max 1) dec)))

(defmethod transition-state events/control-variant-set-quantity [_ event {:keys [value-str]} app-state]
  (assoc-in app-state state/browse-variant-quantity-path
            (-> (js/parseInt value-str 10)
                (Math/abs)
                (max 1))))

(defmethod transition-state events/control-forgot-password-change [_ event args app-state]
  (update-in app-state state/forgot-password-path merge args))

(defmethod transition-state events/api-success-taxons [_ event args app-state]
  (assoc-in app-state state/taxons-path (:taxons args)))

(defmethod transition-state events/api-success-store [_ event args app-state]
  (assoc-in app-state state/store-path args))

(defmethod transition-state events/api-success-products [_ event {:keys [taxon-path products]} app-state]
  (update-in app-state state/products-path
             merge
             (->> products
                  (mapcat (fn [p] [(:id p) p]))
                  (apply hash-map))))

(defmethod transition-state events/api-success-product [_ event {:keys [product-path product]} app-state]
  (-> app-state
      (assoc-in state/browse-product-query-path {:slug product-path})
      (assoc-in (conj state/products-path (:id product)) product)))

(defn sign-in-user [{:keys [email token store_slug]} app-state]
  (-> app-state
      (assoc-in state/user-email-path email)
      (assoc-in state/user-token-path token)
      (assoc-in state/user-store-slug-path store_slug)
      (assoc-in state/sign-in-email-path "")
      (assoc-in state/sign-in-password-path "")
      (assoc-in state/sign-up-email-path "")
      (assoc-in state/sign-up-password-path "")
      (assoc-in state/sign-up-password-confirmation-path "")))

(defmethod transition-state events/api-success-sign-in [_ event args app-state]
  (sign-in-user args app-state))

(defmethod transition-state events/api-success-sign-up [_ event args app-state]
  (sign-in-user args app-state))

(defmethod transition-state events/api-success-forgot-password [_ event args app-state]
  (assoc-in app-state state/forgot-password-email-path ""))
