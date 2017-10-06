(ns storefront.components.checkout-confirmation
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.components.checkout-delivery :as checkout-delivery]
            [storefront.components.checkout-credit-card :as checkout-credit-card]
            [storefront.components.checkout-steps :as checkout-steps]
            [storefront.components.order-summary :as summary]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]))

(defn requires-additional-payment? [data]
  (let [no-stripe-payment?  (nil? (get-in data keypaths/order-cart-payments-stripe))
        no-affirm-payment?  (nil? (get-in data keypaths/order-cart-payments-affirm))
        store-credit-amount (or (get-in data keypaths/order-cart-payments-store-credit-amount) 0)
        order-total         (get-in data keypaths/order-total)]
    (and
     ;; stripe can charge any amount
     no-stripe-payment?
     ;; affirm will be setup after this screen and will try to cover any amount
     no-affirm-payment?
     ;; is total covered by remaining store-credit?
     (> order-total store-credit-amount))))

(defn old-component
  [{:keys [available-store-credit
           checkout-steps
           payment delivery order
           placing-order?
           products
           requires-additional-payment?
           saving-card?
           updating-shipping?]}
   owner]
  (om/component
   (html
    [:div.container.p2
     (om/build checkout-steps/component checkout-steps)

     [:.clearfix.mxn3
      [:.col-on-tb-dt.col-6-on-tb-dt.px3
       [:.h3.left-align "Order Summary"]

       [:div.my2
        {:data-test "confirmation-line-items"}
        (summary/display-line-items (orders/product-items order) products)]]

      [:.col-on-tb-dt.col-6-on-tb-dt.px3
       (om/build checkout-delivery/component delivery)
       [:form
        {:on-submit
         (utils/send-event-callback events/control-checkout-confirmation-submit
                                    {:place-order? requires-additional-payment?})}
        (when requires-additional-payment?
          [:div
           (ui/note-box
            {:color     "teal"
             :data-test "additional-payment-required-note"}
            [:.p2.navy
             "Please enter an additional payment method below for the remaining total on your order."])
           (om/build checkout-credit-card/component payment)])
        (summary/display-order-summary order
                                       {:read-only?             true
                                        :use-store-credit?      true
                                        :available-store-credit available-store-credit})
        [:div.col-12.col-6-on-tb-dt.mx-auto
         (ui/submit-button "Place Order" {:spinning? (or saving-card? placing-order?)
                                          :disabled? updating-shipping?
                                          :data-test "confirm-form-submit"})]]]]])))

(defn component
  [{:keys [affirm-payment?
           available-store-credit
           checkout-steps
           payment delivery order
           placing-order?
           products
           requires-additional-payment?
           saving-card?
           updating-shipping?]}
   owner]
  (om/component
   (html
    [:div.container.p2
     (om/build checkout-steps/component checkout-steps)

     [:.clearfix.mxn3
      [:.col-on-tb-dt.col-6-on-tb-dt.px3
       [:.h3.left-align "Order Summary"]

       [:div.my2
        {:data-test "confirmation-line-items"}
        (summary/display-line-items (orders/product-items order) products)]]

      [:.col-on-tb-dt.col-6-on-tb-dt.px3
       (om/build checkout-delivery/component delivery)
       [:form
        {:on-submit
         (utils/send-event-callback events/control-checkout-confirmation-submit
                                    {:place-order? requires-additional-payment?})}
        (when requires-additional-payment?
          [:div
           (ui/note-box
            {:color     "teal"
             :data-test "additional-payment-required-note"}
            [:.p2.navy
             "Please enter an additional payment method below for the remaining total on your order."])
           (om/build checkout-credit-card/component payment)])
        (summary/display-order-summary order
                                       {:read-only?             true
                                        :use-store-credit?      false
                                        :available-store-credit available-store-credit})
        [:div.col-12.col-6-on-tb-dt.mx-auto
         (ui/submit-button "Checkout with Affirm" {:spinning? (or saving-card? placing-order?) ;; We need a boolean for affirm request
                                                   :disabled? updating-shipping?
                                                   :data-test "confirm-affirm-form-submit"})]]]]])))

(defn query [data]
  {:updating-shipping?           (utils/requesting? data request-keys/update-shipping-method)
   :saving-card?                 (checkout-credit-card/saving-card? data)
   :placing-order?               (utils/requesting? data request-keys/place-order)
   :requires-additional-payment? (requires-additional-payment? data)
   :affirm-payment?              (get-in data keypaths/order-cart-payments-affirm)
   :checkout-steps               (checkout-steps/query data)
   :products                     (get-in data keypaths/products)
   :order                        (get-in data keypaths/order)
   :payment                      (checkout-credit-card/query data)
   :delivery                     (checkout-delivery/query data)
   :available-store-credit       (get-in data keypaths/user-total-available-store-credit)})

(defn built-component [data opts]
  (om/build (if (experiments/affirm? data)
              component
              old-component)
            (query data)
            opts))
