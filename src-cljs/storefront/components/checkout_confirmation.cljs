(ns storefront.components.checkout-confirmation
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.components.checkout-delivery :as checkout-delivery]
            [storefront.components.checkout-payment :as checkout-payment]
            [storefront.components.checkout-steps :as checkout-steps]
            [storefront.components.order-summary :as summary]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]))

(defn requires-additional-payment? [data]
  (and (nil? (get-in data keypaths/order-cart-payments-stripe))
       (> (get-in data keypaths/order-total)
          (or (get-in data keypaths/order-cart-payments-store-credit-amount) 0))))

(defn component
  [{:keys [checkout-steps
           updating-shipping?
           saving-card?
           placing-order?
           requires-additional-payment?
           payment delivery order
           products
           price-strikeout?]}
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
        (summary/display-line-items (orders/product-items order) products price-strikeout?)]]

      [:.col-on-tb-dt.col-6-on-tb-dt.px3
       (om/build checkout-delivery/component delivery)
       [:form
        {:on-submit (utils/send-event-callback events/control-checkout-confirmation-submit
                                               {:place-order? requires-additional-payment?})}
        (when requires-additional-payment?
          [:div
           (ui/note-box
            {:color "teal"
             :data-test "additional-payment-required-note"}
            [:.p2.navy
             "Please enter an additional payment method below for the remaining total on your order."])
           (om/build checkout-payment/credit-card-form-component payment)])
        (summary/display-order-summary order {:read-only? true} price-strikeout?)
        (ui/submit-button "Place Order" {:spinning? (or saving-card? placing-order?)
                                         :disabled? updating-shipping?
                                         :data-test "confirm-form-submit"})]]]])))

(defn query [data]
  {:updating-shipping?           (utils/requesting? data request-keys/update-shipping-method)
   :saving-card?                 (checkout-payment/saving-card? data)
   :placing-order?               (utils/requesting? data request-keys/place-order)
   :requires-additional-payment? (requires-additional-payment? data)
   :checkout-steps               (checkout-steps/query data)
   :products                     (get-in data keypaths/products)
   :order                        (get-in data keypaths/order)
   :payment                      (checkout-payment/credit-card-form-query data)
   :delivery                     (checkout-delivery/query data)
   :price-strikeout?             (experiments/price-strikeout? data)})

(defn built-component [data opts]
  (om/build component (query data) opts))
