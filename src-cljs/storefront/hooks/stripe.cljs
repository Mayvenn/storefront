(ns storefront.hooks.stripe
  (:require [storefront.browser.tags :refer [insert-tag-with-callback
                                             src-tag]]
            [storefront.platform.messages :refer [handle-message]]
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
