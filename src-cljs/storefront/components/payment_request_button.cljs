(ns storefront.components.payment-request-button
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.hooks.browser-pay :as browser-pay]
            [storefront.browser.cookie-jar :as cookie-jar]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.assets :as assets]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.transitions :as transitions]
            [storefront.effects :as effects]
            [storefront.component :as component :refer [defcomponent defdynamic-component]]
            ))


(defmethod effects/perform-effects events/stripe-payment-request-button-inserted
  [_ event {:keys []} _ app-state]
  (browser-pay/payment-request (get-in app-state keypaths/order)
                               (get-in app-state keypaths/session-id)
                               (cookie-jar/retrieve-utm-params (get-in app-state keypaths/cookie))
                               (get-in app-state keypaths/states)
                               (get-in app-state keypaths/shipping-methods)))

(defdynamic-component component [data owner opts]
  (constructor [_ _] {})
  (did-mount [_] (handle-message events/stripe-payment-request-button-inserted))
  (will-unmount [_] (handle-message events/stripe-payment-request-button-removed))
  (render [_] (component/html [:div.pb2
                               [:div#request-payment-button-container]])))

(defn query [data]
  data)

(defn built-component [data opts]
  (component/build component (query data) opts))
