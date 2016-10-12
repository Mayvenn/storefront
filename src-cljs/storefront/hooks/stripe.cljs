(ns storefront.hooks.stripe
  (:require [storefront.api :as api]
            [storefront.browser.tags :refer [insert-tag-with-callback
                                             src-tag]]
            [storefront.components.money-formatters :refer [as-money as-money-or-free]]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.products :as products]
            [storefront.request-keys :as request-keys]
            [storefront.events :as events]
            [storefront.config :as config]
            [clojure.string :as string]
            [storefront.hooks.exception-handler :as exception-handler]
            [clojure.set :refer [rename-keys]]
            [clojure.set :as set]))

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

(defn ^:private product-items->apple-line-items [order]
  (map (fn [{:keys [quantity unit-price] :as item}]
         {:label (str (products/product-title item) " - QTY " quantity)
          :amount (* quantity unit-price)})
       (orders/product-items order)))

(defn ^:private adjustment->apple-line-item [{:keys [name price]}]
  {:label name
   :amount price})

(defn ^:private adjustments->apple-line-items [order]
  (map adjustment->apple-line-item (:adjustments order)))

(defn ^:private shipping->apple-line-items [order]
  (let [{:keys [quantity unit-price]} (orders/shipping-item order)]
    (adjustment->apple-line-item {:name "Shipping"
                                  :amount (* quantity unit-price)})))

(defn ^:private tax->apple-line-items [order]
  (adjustment->apple-line-item (orders/tax-adjustment order)))

(defn ^:private waiter->apple-shipping-methods [shipping-methods]
  (for [{:keys [name quantity price sku]} shipping-methods]
    {:label name
     :detail (as-money price)
     :amount price
     :identifier sku}))

(defn ^:private order->apple-line-items [order]
  (concat (product-items->apple-line-items order)
          (adjustments->apple-line-items order)
          [(shipping->apple-line-items order)
           (tax->apple-line-items order)]))

(defn ^:private order->apple-total [order]
  {:label "Mayvenn Hair"
   :amount (:total order)})


(defn ^:private find-state-id [states state-name]
  (let [lower-cased-state-name (string/lower-case (string/trim state-name))]
    (->> states
         (filter #(or (= lower-cased-state-name (string/lower-case (:name %)))
                      (= lower-cased-state-name (string/lower-case (:abbr %)))))
         first
         :id)))


(defn ^:private apple->waiter-shipping [apple-pay-address state-name->id]
  (let [{[address1 address2] :addressLines
         state-name :administrativeArea}
        apple-pay-address]
    (merge (set/rename-keys apple-pay-address
                            {:locality    :city
                             :givenName   :firstname
                             :familyName  :lastname
                             :phoneNumber :phone
                             :postalCode  :zipcode})
           {:address1  address1
            :address2  address2
            :state_id  (state-name->id state-name)})))

(defn ^:private apple-pay-updated-shipping [{:keys [number token]} states shipping-methods session line-items event]
  (let [shipping-contact                 (js->clj (.-shippingContact event))
        params                           {:number           number
                                          :token            token
                                          :email            (:emailAddress shipping-contact)
                                          :shipping-address (apple->waiter-shipping shipping-contact (partial find-state-id states))}
        complete-shipping-address-update (fn [updated-order]
                                           (.completeShippingContactSelected session
                                                                             js/ApplePaySession.STATUS_SUCCESS
                                                                             shipping-methods
                                                                             (order->apple-total updated-order)
                                                                             (order->apple-line-items updated-order)))]
    (api/update-shipping-address params complete-shipping-address-update)))

(defn begin-apple-pay [order shipping-methods states]
  (let [shipping-methods (waiter->apple-shipping-methods shipping-methods)
        payment-request {:countryCode                   "US"
                         :currencyCode                  "USD"
                         :requiredBillingContactFields  ["name" "postalAddress"]
                         :requiredShippingContactFields ["name" "phone" "email" "postalAddress"]
                         :shippingMethods               shipping-methods
                         :lineItems                     (order->apple-line-items order)
                         :total                         (order->apple-total order)}
        session         (js/Stripe.applePay.buildSession (clj->js payment-request) charge-apple-pay)]
    (set! (.-onshippingcontactselected session) (partial apple-pay-updated-shipping order states shipping-methods session))
    (.begin session)))


