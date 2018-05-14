(ns storefront.components.checkout-processing
  (:require [storefront.component :as component]
            [storefront.components.ui :as ui]))

(defn component
  [{:keys [guest? sign-up-data]} _]
  (component/create
   (ui/narrow-container
    [:div.p3
     [:div]])))

(defn query [data]
  {}
  )

(defn built-component [data opts]
  (component/build component (query data) opts))
