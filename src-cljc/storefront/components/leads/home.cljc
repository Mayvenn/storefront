(ns storefront.components.leads.home
  (:require #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.components.leads.header :as header]
            ))

(defn ^:private component [data owner opts]
  (component/create
   [:div
    (header/built-component data nil)
    [:div "Imma leads component!"]]))

(defn ^:private query [data]
  {})

(defn built-component [data opts]
  (component/build component (query data) opts))
