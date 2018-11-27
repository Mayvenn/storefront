(ns storefront.components.customer.orders
  (:require [storefront.effects :as effects]
            [storefront.component :as component]))

(defn component
  [{:keys []}]
  (component/create
   [:div
    [:h1 "Yeahhhaw"]]))

(defn query [data]
  (let []
    {}))

(defn built-component [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-customer-orders [_ event args _ app-state]

  )
