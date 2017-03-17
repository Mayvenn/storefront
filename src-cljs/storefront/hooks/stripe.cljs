(ns storefront.hooks.stripe
  (:require [storefront.browser.tags :refer [insert-tag-with-callback
                                             src-tag]]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.request-keys :as request-keys]
            [storefront.events :as events]
            [storefront.config :as config]
            [clojure.string :as string]
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
                  :request-id  (str (random-uuid))}]
      (handle-message events/api-start api-id)
      (js/Stripe.card.createToken (clj->js {:number        number
                                            :cvc           cvc
                                            :name          cardholder-name
                                            :exp_month     (js/parseInt exp-month)
                                            :exp_year      (js/parseInt exp-year)
                                            :address_line1 (:address1 address)
                                            :address_line2 (:address2 address)
                                            :address_city  (:city address)
                                            :address_state (:state address)
                                            :address_zip   (:zipcode address)})
                                  (fn [status response]
                                    (handle-message events/api-end api-id)
                                    (if (= 200 status)
                                      (handle-message events/stripe-success-create-token
                                                      (assoc (js->clj response :keywordize-keys true) :place-order? (:place-order? args) ))
                                      (handle-message events/stripe-failure-create-token
                                                      (js->clj response :keywordize-keys true))))))))

