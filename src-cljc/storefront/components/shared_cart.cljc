(ns storefront.components.shared-cart
  (:require [storefront.platform.component-utils :as utils]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.request-keys :as request-keys]
            [storefront.keypaths :as keypaths]))

(defn component [{:keys [store creating-cart?]} owner opts]
  (component/create
   (ui/narrow-container
    [:p.m2.center.h2.navy.medium
     (:store_nickname store) " has created a bag for you!"]
    [:form
     {:on-submit (utils/send-event-callback events/control-create-order-from-shared-cart)}
     (ui/submit-button "View your bag"
                       {:spinning? creating-cart?})])))

(defn query [data]
  {:store          (get-in data keypaths/store)
   :creating-cart? (utils/requesting? data request-keys/create-order-from-shared-cart)})

(defn built-component [data opts]
  (component/build component (query data) opts))
