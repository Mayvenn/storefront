(ns storefront.components.checkout-delivery
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.utils.query :as query]
            [storefront.events :as events]
            [storefront.hooks.experiments :as experiments]
            [storefront.accessors.shipping :as shipping]
            [storefront.components.checkout-steps :refer [checkout-step-bar]]
            [storefront.components.validation-errors :refer [validation-errors-component]]
            [storefront.components.formatters :refer [as-money as-money-or-free]]
            [storefront.components.utils :as utils]))

(defn display-shipping-method [selected-sku {:keys [sku name price] :as shipping-method}]
  [:li.shipping-method
   (merge {:key sku
           :on-click (utils/send-event-callback events/control-checkout-shipping-method-select
                                                shipping-method)}
          (when (= selected-sku sku)
            {:class "selected"}))
   [:label
    [:input.ship-method-radio {:type "radio"}]
    [:div.checkbox-container
     [:figure.large-checkbox]]
    [:div.shipping-method-container
     [:div.rate-name name]
     [:div.rate-timeframe (shipping/timeframe sku)]]
    [:div.rate-cost (as-money-or-free price)]]])


(defn checkout-delivery-component [data owner]
  (om/component
   (html
    [:div#checkout
     (om/build validation-errors-component data)
     (checkout-step-bar data)
     [:div.checkout-form-wrapper
      [:form.edit_order
       [:div.checkout-container.delivery
        [:h2.checkout-header "Delivery Options"]
        [:div#methods
         [:div.shipment
          [:ul.field.radios.shipping-methods

           (map (partial display-shipping-method (get-in data keypaths/checkout-selected-shipping-method-sku))
                (get-in data keypaths/shipping-methods))]]]
        [:div.form-buttons
         (let [saving (query/get {:request-key request-keys/update-shipping-method}
                                 (get-in data keypaths/api-requests))]
           [:a.large.continue.button.primary
            {:on-click (when-not saving (utils/send-event-callback events/control-checkout-shipping-method-submit))
             :class (when saving "saving")}
            "Continue to Payment"])]]]]])))
