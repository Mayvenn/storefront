(ns storefront.components.checkout-confirmation
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.events :as events]
            [storefront.hooks.experiments :as experiments]
            [storefront.components.utils :as utils]
            [storefront.components.ui :as ui]
            [storefront.components.validation-errors :as validation]
            [storefront.components.checkout-steps :as checkout-steps :refer [checkout-step-bar]]
            [storefront.components.checkout-payment :as checkout-payment :refer [checkout-payment-credit-card-component]]
            [storefront.components.checkout-delivery :as checkout-delivery :refer [checkout-confirm-delivery-component]]
            [storefront.components.order-summary :as summary :refer [display-order-summary display-line-items]]))

(defn requires-additional-payment? [data]
  (and (nil? (get-in data keypaths/order-cart-payments-stripe))
       (> (get-in data keypaths/order-total)
          (or (get-in data keypaths/order-cart-payments-store-credit-amount) 0))))

(defn redesigned-checkout-confirmation-component [{:keys [errors checkout-steps saving? submitting? updating-shipping?
                                                          requires-additional-payment?
                                                          payment delivery order
                                                          shipping-methods
                                                          products]}
                                                  owner]
  (om/component
   (html
    (ui/narrow-container
     (om/build checkout-steps/redesigned-checkout-step-bar checkout-steps)
     [:.h2.left-align.col-12 "Order Summary"]
     [:form.col-12
      {:on-submit (utils/send-event-callback events/control-checkout-confirmation-submit
                                             {:place-order? requires-additional-payment?})}
      (summary/redesigned-display-line-items products order)
      (om/build checkout-delivery/redesigned-confirm-delivery-component delivery)
      (when requires-additional-payment?
        [:div
         (ui/note-box
          "green"
          [:.p2.navy
           "Please enter an additional payment method below for the remaining total on your order."])
         (om/build checkout-payment/redesigned-credit-card-form-component payment)])
      [:div.border-top.border-light-silver.mt2
       [:.mt2
        (summary/redesigned-display-order-summary shipping-methods order)]]
      (ui/submit-button "Place Order" {:spinning? submitting?
                                       :disabled? updating-shipping?})]))))

(defn old-checkout-confirmation-component [data owner]
  (let [placing-order?         (utils/requesting? data request-keys/place-order)
        updating-shipping?     (utils/requesting? data request-keys/update-shipping-method)
        updating-payments?     (utils/requesting? data request-keys/update-cart-payments)
        creating-stripe-token? (utils/requesting? data request-keys/stripe-create-token)
        saving?                (or creating-stripe-token? updating-shipping? updating-payments? placing-order?)]
    (om/component
     (html
      [:div#checkout
       (checkout-step-bar data)
       [:div.row
        [:div.checkout-form-wrapper
         [:form.edit_order
          [:div.checkout-container
           (display-line-items data (get-in data keypaths/order))
           (om/build checkout-confirm-delivery-component data)
           (when (requires-additional-payment? data)
             [:div
              [:p.store-credit-instructions "Please enter an additional payment method below for the remaining total on your order"]
              (om/build checkout-payment-credit-card-component data)])
           (display-order-summary data (get-in data keypaths/order))
           [:div.form-buttons.pay-for-order
            [:a.large.continue.button.primary
             (merge
              {:on-click (when-not saving?
                           (utils/send-event-callback events/control-checkout-confirmation-submit
                                                      {:place-order? (requires-additional-payment? data)}))
               :class    (str (when (or creating-stripe-token? updating-payments? placing-order?)
                                "saving") " "
                              (when updating-shipping? "disabled"))}
              (when saving? {:disabled "disabled"}))
             "Complete my Purchase"]]]]]]]))))

(defn query [data]
  (let [placing-order?         (utils/requesting? data request-keys/place-order)
        updating-shipping?     (utils/requesting? data request-keys/update-shipping-method)
        updating-payments?     (utils/requesting? data request-keys/update-cart-payments)
        creating-stripe-token? (utils/requesting? data request-keys/stripe-create-token)
        saving?                (or creating-stripe-token? updating-shipping? updating-payments? placing-order?)]
    {:submitting?                  (or creating-stripe-token? updating-payments? placing-order?)
     :updating-shipping?           updating-shipping?
     :saving?                      saving?
     :requires-additional-payment? (requires-additional-payment? data)
     :checkout-steps               (checkout-steps/query data)
     :errors                       (get-in data keypaths/validation-errors)
     :shipping-methods             (get-in data keypaths/shipping-methods)
     :products                     (get-in data keypaths/products)
     :order                        (get-in data keypaths/order)
     :payment                      (checkout-payment/redesigned-credit-card-form-query data)
     :delivery                     (checkout-delivery/query data)}))

(defn checkout-confirmation-component [data owner]
  (om/component
   (html
    (if (experiments/three-steps-redesign? data)
      (om/build redesigned-checkout-confirmation-component (query data))
      (om/build old-checkout-confirmation-component data)))))
