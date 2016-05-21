(ns storefront.components.checkout-delivery
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.events :as events]
            [storefront.messages :as messages]
            [storefront.hooks.experiments :as experiments]
            [storefront.accessors.shipping :as shipping]
            [storefront.components.checkout-steps :refer [checkout-step-bar]]
            [storefront.components.validation-errors :refer [validation-errors-component]]
            [storefront.components.formatters :refer [as-money as-money-or-free as-money-without-cents-or-free]]
            [storefront.components.utils :as utils]))

(defn select-shipping-method [shipping-method]
  (utils/send-event-callback events/control-checkout-shipping-method-select shipping-method))

(defn display-shipping-method [{:keys [sku name price]} {:keys [selected-sku saving? on-click]}]
  (let [selected? (= selected-sku sku)]
    [:li.shipping-method
     {:key sku
      :on-click on-click
      :class (when selected?
               (str "selected" (when saving? " saving")))}
     [:label
      [:input.ship-method-radio {:type "radio"}]
      [:div.checkbox-container
       [:figure.large-checkbox]]
      [:div.shipping-method-container
       [:div.rate-name name]
       [:div.rate-timeframe (shipping/timeframe sku)]]
      [:div.rate-cost (as-money-or-free price)]]]))

(defn checkout-confirm-delivery-component [data owner]
  (let [saving? (utils/requesting? data request-keys/update-shipping-method)]
    (om/component
     (html
      [:div.checkout-container.delivery
       [:h2.checkout-header "Delivery Options"]
       [:div#methods
        [:div.shipment
         [:ul.field.radios.shipping-methods
          (for [shipping-method (get-in data keypaths/shipping-methods)]
            (display-shipping-method shipping-method
                                     {:selected-sku (get-in data keypaths/checkout-selected-shipping-method-sku)
                                      :saving?      saving?
                                      :on-click     (select-shipping-method shipping-method)}))]]]]))))

(defn redesigned-confirm-delivery-component [{:keys [shipping-methods selected-sku]} owner]
  (om/component
   (html
    [:div
     [:.h2 "Shipping Method"]
     [:.py1
      (for [{:keys [sku name price] :as shipping-method} shipping-methods]
        [:label.flex.items-center.col-12.py1 {:key sku}
         [:input.mx2.h1
          {:type "radio"
           :name "shipping-method"
           :id (str "shipping-method-" sku)
           :checked (= selected-sku sku)
           :on-change (select-shipping-method shipping-method)}]
         [:.clearfix.col-12
          [:.right.medium {:class (if (pos? price) "navy" "green")} (as-money-without-cents-or-free price)]
          [:.overflow-hidden
           [:.mb1 name]
           [:.h5 (shipping/timeframe sku)]]]])]])))

(defn query [data]
  {:shipping-methods (get-in data keypaths/shipping-methods)
   :selected-sku     (get-in data keypaths/checkout-selected-shipping-method-sku)})
