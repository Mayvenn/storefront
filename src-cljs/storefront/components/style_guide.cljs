(ns storefront.components.style-guide
  (:require [storefront.component :as component]))

(defn component [data owner opts]
  (component/create
   [:div
    [:section "hello"]]))

(defn query [data]
  {})

(defn built-component [data opts]
  (component/build component (query data) opts))
