(ns storefront.components.checkout-delivery
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.state :as state]
            [storefront.events :as events]
            [storefront.components.checkout-steps :refer [checkout-step-bar]]
            [storefront.components.utils :as utils]))

(defn shipping-timeframe [rate-name]
  (str
   (condp = rate-name
     "Priority Shipping" "3-5"
     "Express Shipping" "1-2 (No Weekends)"
     :else "?")))

(defn display-shipping-method [app-state method]
  [:li.shipping-method
   (merge (if (= (get-in app-state state/checkout-selected-shipping-method-id)
                 (:id method))
            {:class "selected"})
          {:on-click (utils/enqueue-event app-state
                                          events/control-checkout-shipping-method-select
                                          {:id (:id method)})})
   [:label
    [:input.ship-method-radio {:type "radio"}]
    [:div.checkbox-container
     [:figure.large-checkbox]]
    [:div.shipping-method-container
     [:div.rate-name (:name method)]
     [:div.rate-timeframe "TODO: rate-timeframe"]]
    [:div.rate-cost (:display_cost method)]]])


(defn checkout-delivery-component [data owner]
  (om/component
   (html
    [:div#checkout
     (checkout-step-bar data)
     [:div.checkout-form-wrapper
      [:form.edit_order
       {:on-submit (utils/enqueue-event data events/control-checkout-shipping-method-submit)}
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
