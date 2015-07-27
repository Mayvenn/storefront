(ns storefront.components.checkout-delivery
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.components.checkout-steps :refer [checkout-step-bar]]
            [storefront.components.validation-errors :refer [validation-errors-component]]
            [storefront.components.utils :as utils]))

(defn shipping-timeframe [rate-name]
  (condp = rate-name
    "Priority Shipping" "3-5 business days"
    "Express Shipping" "1-2 business days (No Weekends)"
    "Overnight Shipping" "1 business day (No Weekends)"
    "?"))

(defn display-shipping-method [app-state shipping-method]
  [:li.shipping-method
   (merge (if (= (get-in app-state keypaths/checkout-selected-shipping-method-id)
                 (:id shipping-method))
            {:class "selected"})
          {:on-click (utils/send-event-callback app-state
                                                events/control-checkout-shipping-method-select
                                                shipping-method)})
   [:label
    [:input.ship-method-radio {:type "radio"}]
    [:div.checkbox-container
     [:figure.large-checkbox]]
    [:div.shipping-method-container
     [:div.rate-name (:name shipping-method)]
     [:div.rate-timeframe (shipping-timeframe (:name shipping-method))]]
    [:div.rate-cost (:display_cost shipping-method)]]])


(defn checkout-delivery-component [data owner]
  (om/component
   (html
    [:div#checkout
     (om/build validation-errors-component data)
     (checkout-step-bar data)
     [:div.checkout-form-wrapper
      [:form.edit_order
       {:on-submit (utils/send-event-callback data events/control-checkout-shipping-method-submit)}
       [:div.checkout-container.delivery
        [:h2.checkout-header "Delivery Options"]
        [:div#methods
         [:p.warning "Please note: Express shipping cannot deliver to PO boxes."]
         [:div.shipment
          [:ul.field.radios.shipping-methods

           (map (partial display-shipping-method data)
                (get-in data [:order :shipments 0 :shipping_rates]))]]]
        [:div.form-buttons
         [:input.continue.button.primary
          {:type "submit" :value "Continue"}]]]]]])))
