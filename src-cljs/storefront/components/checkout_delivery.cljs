(ns storefront.components.checkout-delivery
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.state :as state]
            [storefront.events :as events]
            [storefront.components.checkout-steps :refer [checkout-step-bar]]))

(def shipping-example
  [])

(defn shipping-timeframe [rate-name]
  (str
   (condp = rate-name
     "Priority Shipping" "3-5"
     "Express Shipping" "1-2 (No Weekends)"
     :else "?")))

(defn display-shipping-method [method]
  [:li.shipping-method.selected
   [:label
    [:input.ship-method-radio {:type "radio"}]
    [:div.checkbox-container
     [:figure.large-checkbox]]
    [:div.shipping-method-container
     [:div.rate-name "TODO: rate-name"]
     [:div.rate-timeframe "TODO: rate-timeframe"]]
    [:div.rate-cost "FREE or $20.00"]]])

(defn checkout-delivery-component [data owner]
  (om/component
   (html
    [:div#checkout
     (checkout-step-bar data)
     [:div.checkout-container.delivery
      [:h2.checkout-header "Delivery Options"]
      [:div#methods
       [:p.warning "Please note: Express shipping cannot deliver to PO boxes."]
       [:div.shipment
        [:ul.field.radios.shipping-methods

         (display-shipping-method {})]]]
      [:div.form-buttons
       [:input.continue.button.primary
        {:type "submit" :value "Continue"}]]]])))
