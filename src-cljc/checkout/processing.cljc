(ns checkout.processing
  (:require [storefront.components.ui :as ui]
            #?@(:cljs [[storefront.component :as component]]
                :clj [[storefront.component-shim :as component]])))

(defn component
  [{:keys [guest? sign-up-data]} _ _]
  (component/create
   (ui/narrow-container
    [:div.py6.h2
     [:div.py4 (ui/large-spinner {:style {:height "6em"}})]
     [:h2.center.navy "Processing your order..."]])))

(defn query [data]
  {})

(defn built-component [data opts]
  (component/build component (query data) opts))

