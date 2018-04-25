(ns checkout.header
  (:require
   [storefront.accessors.orders :as orders]
   [storefront.components.slideout-nav :as slideout-nav]
   [storefront.platform.component-utils :as utils]
   [storefront.components.svg :as svg]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   #?(:clj [storefront.component-shim :as component]
      :cljs [storefront.component :as component])))

(defn component [{:keys [item-count back]}]
  (component/create
   [:div.border-bottom.border-light-gray.flex.items-center
    [:div.col-1]
    [:div.flex-auto.py3.center.dark-gray "Shopping Bag - " (prn-str item-count) " items"]
    [:div.col-1
     [:a.h3.pointer.flex.items-center
      (merge
       (utils/route-back-or-to back events/navigate-home)
       {:data-test "auto-complete-close" :title "Close"})
      (svg/close-x {:class "stroke-dark-gray fill-white"})]]]))

(defn query [data]
  {:item-count (orders/product-quantity (get-in data keypaths/order))
   :back       (first (get-in data keypaths/navigation-undo-stack))})

(defn built-component [data opts]
  (component/build component (query data) nil))
