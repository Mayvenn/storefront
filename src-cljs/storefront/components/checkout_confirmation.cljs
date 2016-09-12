(ns storefront.components.checkout-confirmation
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.experiments :as experiments]
            [storefront.assets :as assets]
            [storefront.components.checkout-delivery :as checkout-delivery]
            [storefront.components.checkout-payment :as checkout-payment]
            [storefront.components.checkout-steps :as checkout-steps]
            [storefront.components.order-summary :as summary]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
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
           essence?
           requires-additional-payment?
           payment delivery order
           products]}
   owner]
  (om/component
   (html
    (ui/container
     (om/build checkout-steps/component checkout-steps)

     [:.clearfix.mxn3
      [:.md-up-col.md-up-col-6.px3
       [:.h2.left-align "Order Summary"]

       [:div.my2 {:data-test "confirmation-line-items"}
        (when essence?
          [:div
           [:div.flex.border.border-orange.py1
            [:div.flex-none.mx1 {:style {:width "7.33em"}}
             [:div.to-lg-hide
              [:img {:src (assets/path "/images/essence/essence@2x.png") :width "94px" :height "96px"}]]
             [:div.lg-up-hide
              [:img {:src (assets/path "/images/essence/essence@2x.png") :width "72px" :height "70px"}]]]
            [:div.flex-auto.mr1
             [:div.h5.mb1.line-height-2
              [:div.bold.shout.mb1.h4 "bonus offer!"]
              "A one-year subscription to " [:span.bold "ESSENCE "] "magazine is included with your order ($10 value)."]
             [:a.h5.navy
              (utils/fake-href events/control-essence-offer-details)
              "Offer and Rebate Details ➤"]]]
           [:div.border-bottom.border-light-silver ui/nbsp]])

        (summary/display-line-items (orders/product-items order) products)]]
      [:.md-up-col.md-up-col-6.px3
       (om/build checkout-delivery/component delivery)
       [:form
        {:on-submit (utils/send-event-callback events/control-checkout-confirmation-submit
                                               {:place-order? requires-additional-payment?})}
        (when requires-additional-payment?
          [:div
           (ui/note-box
            {:color "green"
             :data-test "additional-payment-required-note"}
            [:.p2.navy
             "Please enter an additional payment method below for the remaining total on your order."])
           (om/build checkout-payment/credit-card-form-component payment)])
        (summary/display-order-summary order)
        (ui/submit-button "Place Order" {:spinning? (or saving-card? placing-order?)
                                         :disabled? updating-shipping?
                                         :data-test "confirm-form-submit"})]]]))))

(defn query [data]
  {:updating-shipping?           (utils/requesting? data request-keys/update-shipping-method)
   :saving-card?                 (checkout-payment/saving-card? data)
   :placing-order?               (utils/requesting? data request-keys/place-order)
   :requires-additional-payment? (requires-additional-payment? data)
   :essence?                     (experiments/essence? data)
   :checkout-steps               (checkout-steps/query data)
   :products                     (get-in data keypaths/products)
   :order                        (get-in data keypaths/order)
   :payment                      (checkout-payment/credit-card-form-query data)
   :delivery                     (checkout-delivery/query data)})

(defn built-component [data opts]
  (om/build component (query data) opts))
