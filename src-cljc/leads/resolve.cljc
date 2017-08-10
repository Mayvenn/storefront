(ns leads.resolve
  (:require #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [leads.header :as header]
            [storefront.components.footer :as footer]))

(defn ^:private component [data owner opts]
  (component/create
   [:div
    (header/built-component data nil)
    (component/build footer/minimal-component (:footer data) nil)]))

(defn ^:private query [data]
  (let [call-number "1-866-424-7201"
        text-number "1-510-447-1504"]
    {:footer {:call-number call-number}
     :faq    {:text-number text-number
              :call-number call-number}}))

(defn built-component [data opts]
  (component/build component (query data) opts))
