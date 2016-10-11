(ns storefront.hooks.stripe
  (:require [storefront.browser.tags :refer [insert-tag-with-callback
                                             src-tag]]
            [storefront.components.money-formatters :refer [as-money as-money-or-free]]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.products :as products]
            [storefront.request-keys :as request-keys]
            [storefront.events :as events]
            [storefront.config :as config]
            [storefront.hooks.exception-handler :as exception-handler]
            [clojure.set :refer [rename-keys]]))

(defn insert []
  (when-not (.hasOwnProperty js/window "Stripe")
    (insert-tag-with-callback
     (src-tag "https://js.stripe.com/v2/stripe.js" "stripe")
     (fn []
       (handle-message events/inserted-stripe)
       (js/Stripe.setPublishableKey config/stripe-publishable-key)))))

(defn create-token [cardholder-name number cvc exp-month exp-year address & [args]]
  (when (.hasOwnProperty js/window "Stripe")
    (let [api-id {:request-key request-keys/stripe-create-token
                  :request-id (str (random-uuid))}]
      (handle-message events/api-start api-id)
      (js/Stripe.card.createToken (clj->js (merge
                                            {:number number
                                             :cvc cvc
                                             :name cardholder-name
                                             :exp_month (js/parseInt exp-month)
                                             :exp_year (js/parseInt exp-year)}
                                            (rename-keys address {:address1 :address_line1
                                                                  :address2 :address_line2
                                                                  :city :address_city
                                                                  :state :address_state
                                                                  :zipcode :address_zip})))
                                  (fn [status response]
                                    (handle-message events/api-end api-id)
                                    (if (= 200 status)
                                      (handle-message events/stripe-success-create-token
                                                      (assoc (js->clj response :keywordize-keys true) :place-order? (:place-order? args) ))
                                      (handle-message events/stripe-failure-create-token
                                                      (js->clj response :keywordize-keys true))))))))


(defn verify-apple-pay-eligible []
  (when (.hasOwnProperty js/window "Stripe")
    (let [api-id {:request-key request-keys/stripe-apple-pay-availability
                  :request-id (str (random-uuid))}]
      (handle-message events/api-start api-id)
      (js/Stripe.applePay.checkAvailability (fn [available?]
                                              (handle-message events/api-end api-id)
                                              (handle-message events/apple-pay-availability {:available? available?}))))))

(defn charge-apple-pay [result complete]
  ;;TODO charge and then say its successful
  (complete (js/ApplePaySession.STATUS_SUCCESS)))

(defn ^:private line-items->apple-pay [order]
  (map (fn [{:keys [quantity unit-price] :as item}]
         {:label (str (products/product-title item) " - QTY " quantity)
          :amount (* quantity unit-price)})
       (orders/product-items order)))

(defn ^:private adjustments->apple-pay [order]
  (map (fn [{:keys [name price] :as item}]
         {:label name
          :amount price})
       (orders/all-order-adjustments order)))

(defn ^:private shipping->apple-pay [order]
  (let [{:keys [quantity unit-price]} (orders/shipping-item order)]
    {:label "Shipping"
     :amount (* quantity unit-price)}))

(defn ^:private shipping-methods->apple-pay [shipping-methods]
  (for [{:keys [name quantity price sku]} shipping-methods]
    {:label name
     :detail (as-money price)
     :amount price
     :identifier sku}))

(defn begin-apple-pay [order shipping-methods]
  (let [payment-request {:countryCode "US"
                         :currencyCode "USD"
                         :requiredBillingContactFields ["name" "postalAddress"]
                         :requiredShippingContactFields ["name" "phone" "email" "postalAddress"]
                         :shippingMethods (shipping-methods->apple-pay shipping-methods)
                         :lineItems (concat (line-items->apple-pay order)
                                            [(shipping->apple-pay order)]
                                            (adjustments->apple-pay order))
                         :total {:label "Mayvenn Hair"
                                 :amount (:total order)}}
        session (js/Stripe.applePay.buildSession (clj->js payment-request) charge-apple-pay)]
    (.begin session)))
