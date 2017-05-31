(ns storefront.components.slideout-nav
  (:require [storefront.platform.component-utils :as utils]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.events :as events]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.platform.messages :as messages]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.accessors.stylists :refer [own-store? community-url]]
            [storefront.components.money-formatters :refer [as-money]]
            [storefront.routes :as routes]
            [clojure.string :as str]))

(defn component [data owner opts]
  (component/create
   [:div {:style {:min-height "100vh"}}
    [:div.p3 {:on-click (utils/send-event-callback events/control-menu-collapse-all)}
     "x"]]))

(defn query [data]
  {})

(defn built-component [data opts]
  (component/build component (query data) nil))
