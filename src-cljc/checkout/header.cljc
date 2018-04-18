(ns checkout.header
  #?@
   (:clj
    [(:require
      [storefront.accessors.experiments :as experiments]
      [storefront.accessors.nav :as nav]
      [storefront.accessors.orders :as orders]
      [storefront.component-shim :as component]
      [storefront.components.slideout-nav :as slideout-nav]
      [storefront.components.ui :as ui]
      [storefront.events :as events]
      [storefront.keypaths :as keypaths])]
    :cljs
    [(:require
      [storefront.accessors.experiments :as experiments]
      [storefront.accessors.nav :as nav]
      [storefront.accessors.orders :as orders]
      [storefront.component :as component]
      [storefront.components.slideout-nav :as slideout-nav]
      [storefront.components.ui :as ui]
      [storefront.events :as events]
      [storefront.keypaths :as keypaths])]))

(defn component [{:keys [item-count]}]
  (component/create
   [:div.border-bottom.border-gray.flex.items-center
    [:div.flex-auto.py3.center.dark-gray "Shopping Bag - " item-count " items X"]]))

(defn query [data]
  :item-count (orders/product-quantity (get-in data keypaths/order)))

(defn built-component [data opts]
  (component/build component (query data) nil))
