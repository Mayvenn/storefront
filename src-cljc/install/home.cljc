(ns install.home
  (:require [storefront.component :as component]))

(defn ^:private component
  [queried-data owner opts]
  (component/create
   [:div "install.home"]))

(defn ^:private query
  [data]
  {})

(defn built-component
  [data opts]
  (component/build component (query data) opts))
