(ns storefront.components.checkout-confirmation
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.events :as events]
            [storefront.hooks.experiments :as experiments]
            [storefront.utils.query :as query]
            [storefront.components.utils :as utils]
            [storefront.components.checkout-steps :refer [checkout-step-bar]]
            [storefront.components.checkout-payment :refer [checkout-payment-credit-card-component]]
            [storefront.components.checkout-delivery :refer [checkout-confirm-delivery-component]]
            [storefront.components.order-summary :refer [display-order-summary display-line-items]]))

(defn requires-additional-payment? [data]
  (and (experiments/three-steps? data)
       (nil? (get-in data keypaths/order-cart-payments-stripe))
       (> (get-in data keypaths/order-total)
          (or (get-in data keypaths/order-cart-payments-store-credit-amount) 0))))

(defn checkout-confirmation-component [data owner]
  (let [placing-order? (query/get {:request-key request-keys/place-order}
                                  (get-in data keypaths/api-requests))
        updating-shipping? (query/get {:request-key request-keys/update-shipping-method}
                                      (get-in data keypaths/api-requests))
        updating-payments? (query/get {:request-key request-keys/update-cart-payments}
                                      (get-in data keypaths/api-requests))
        creating-stripe-token? (query/get {:request-key request-keys/stripe-create-token}
                                          (get-in data keypaths/api-requests))
        saving? (or creating-stripe-token? updating-shipping? updating-payments? placing-order?)]
    (om/component
     (html
      [:div#checkout
       (checkout-step-bar data)
       [:div.row
        [:div.checkout-form-wrapper
         [:form.edit_order
          [:div.checkout-container
           (display-line-items data (get-in data keypaths/order))
           (when (experiments/three-steps? data)
             (om/build checkout-confirm-delivery-component data))
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
               :class (str (when (or creating-stripe-token? updating-payments? placing-order?)
                             "saving") " "
                           (when updating-shipping? "disabled"))}
             (when saving? {:disabled "disabled"}))
             "Complete my Purchase"]]]]]]]))))
