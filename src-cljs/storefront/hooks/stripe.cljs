(ns storefront.hooks.stripe
  (:require [storefront.browser.tags :refer [insert-tag-with-callback
                                             src-tag]]
            [storefront.messages :refer [send]]
            [storefront.events :as events]
            [storefront.config :as config]
            [storefront.hooks.exception-handler :as exception-handler]))

(defn insert []
  (insert-tag-with-callback
   (src-tag "https://js.stripe.com/v2/stripe.js" "stripe-is-great")
   (fn [] (js/Stripe.setPublishableKey config/stripe-publishable-key))))

(defn create-token [app-state number cvc exp-month exp-year]
  (when (.hasOwnProperty js/window "Stripe")
    (js/Stripe.card.createToken (clj->js {:number number
                                          :cvc cvc
                                          :exp_month (js/parseInt exp-month)
                                          :exp_year (js/parseInt exp-year)})
                                (fn [status response]
                                  (when (= 200 status)
                                    (send app-state
                                          events/stripe-success-create-token
                                          (js->clj response :keywordize-keys true)))))))
