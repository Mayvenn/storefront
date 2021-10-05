(ns checkout.free-install
  (:require
   #?@(:cljs [[storefront.accessors.auth :as auth]
              [storefront.api :as api]
              [storefront.history :as history]])
   [api.catalog :refer [select ?addons ?discountable]]
   api.orders
   api.current
   api.products
   clojure.set
   [storefront.component :as component :refer [defcomponent]]
   [storefront.components.money-formatters :as mf]
   [storefront.components.svg :as svg]
   [storefront.components.ui :as ui]
   [storefront.effects :as effects]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.platform.messages :as messages]
   [storefront.request-keys :as request-keys]
   [checkout.addons :as addons]
   storefront.utils))

(defcomponent component
  [data _ _]
  [:div.container.p2
   {:style {:max-width "580px"}}
   "HI welcome to the new page"])

(defn query [state]
  {})

(defn ^:export built-component [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/control-checkout-add-continued
  [_ _ _ _ _]
  (messages/handle-message events/checkout-add-flow-completed))

(defmethod effects/perform-effects events/checkout-add-flow-completed
  [_ event args _ app-state]
  #?(:cljs
     (if (-> app-state auth/signed-in :storefront.accessors.auth/at-all)
       (history/enqueue-navigate events/navigate-checkout-address {})
       (history/enqueue-navigate events/navigate-checkout-returning-or-guest {}))))
