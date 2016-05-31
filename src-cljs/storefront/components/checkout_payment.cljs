(ns storefront.components.checkout-payment
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.accessors.credit-cards :as cc]
            [storefront.accessors.orders :as orders]
            [storefront.components.checkout-steps :as checkout-steps]
            [storefront.components.formatters :refer [as-money]]
            [storefront.components.ui :as ui]
            [storefront.components.utils :as utils]
            [storefront.components.validation-errors :as validation-errors]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]))

(defn credit-card-form-component
  [{{:keys [name
            number
            expiration
            ccv]} :credit-card}
   owner]
  (om/component
   (html
    [:div
     [:.h2.my2 "Payment Information"]
     [:div
      (ui/text-field "Cardholder's Name"
                     keypaths/checkout-credit-card-name
                     name
                     {:name      "name"
                      :data-test "payment-form-name"
                      :required  true})
      (ui/text-field "Credit Card Number"
                     keypaths/checkout-credit-card-number
                     (cc/format-cc-number number)
                     {:max-length    19
                      :data-test     "payment-form-number"
                      :auto-complete "off"
                      :class         "cardNumber rounded-1"
                      :type          "tel"
                      :required      true})
      [:.flex.col-12
       [:.col-6 (ui/text-field "Expiration (MM/YY)"
                               keypaths/checkout-credit-card-expiration
                               (cc/format-expiration expiration)
                               {:max-length    9
                                :data-test     "payment-form-expiry"
                                :auto-complete "off"
                                :class         "cardExpiry rounded-left-1"
                                :type          "tel"
                                :required      true})]
       [:.col-6 (ui/text-field "Security Code"
                               keypaths/checkout-credit-card-ccv
                               ccv
                               {:max-length    4
                                :auto-complete "off"
                                :data-test     "payment-form-code"
                                :class         "cardCode rounded-right-1 border-width-left-0"
                                :type          "tel"
                                :required      true})]]]])))

(defn credit-card-form-query [data]
  {:credit-card {:name       (get-in data keypaths/checkout-credit-card-name)
                 :number     (get-in data keypaths/checkout-credit-card-number)
                 :expiration (get-in data keypaths/checkout-credit-card-expiration)
                 :ccv        (get-in data keypaths/checkout-credit-card-ccv)}})

(defn component
  [{:keys [step-bar
           saving?
           loaded-stripe?
           store-credit
           errors
           credit-card]}
   owner]
  (om/component
   (html
    (ui/narrow-container
     [:.p2
      (om/build validation-errors/component errors)
      (om/build checkout-steps/component step-bar)

      (let [{:keys [credit-available credit-applicable fully-covered?]} store-credit]
        [:form
         {:on-submit (utils/send-event-callback events/control-checkout-payment-method-submit)
          :data-test "payment-form"}

         (when (pos? credit-available)
           (ui/note-box
            {:color "green"
             :data-test "store-credit-note"}
            [:.p2.navy
             [:.h4 [:span.medium (as-money credit-applicable)] " in store credit will be applied to this order."]
             (when-not fully-covered?
               [:.h5.mt1.line-height-2
                "Please enter an additional payment method below for the remaining total on your order."])]))

         (when-not fully-covered?
           [:div
            (om/build credit-card-form-component {:credit-card credit-card})
            [:.h4.gray
             "You can review your order on the next page before we charge your credit card."]])

         (when loaded-stripe?
           [:.my2
            (ui/submit-button "Go to Review Order" {:spinning? saving?
                                                    :data-test "payment-form-submit"})])])]))))

(defn ^:private saving-card? [data]
  (or (utils/requesting? data request-keys/stripe-create-token)
      (utils/requesting? data request-keys/update-cart-payments)))

(defn query [data]
  (let [available-store-credit (get-in data keypaths/user-total-available-store-credit)
        credit-to-use          (min available-store-credit (get-in data keypaths/order-total))]
    (merge
     {:store-credit   {:credit-available  available-store-credit
                       :credit-applicable credit-to-use
                       :fully-covered?    (orders/fully-covered-by-store-credit?
                                           (get-in data keypaths/order)
                                           (get-in data keypaths/user))}
      :errors         (get-in data keypaths/validation-errors-details)
      :saving?        (saving-card? data)
      :loaded-stripe? (get-in data keypaths/loaded-stripe)
      :step-bar       (checkout-steps/query data)}
     (credit-card-form-query data))))

(defn built-component [data owner]
  (om/component (html (om/build component (query data)))))
