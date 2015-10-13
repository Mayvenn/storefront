(ns storefront.components.checkout-payment
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.request-keys :as request-keys]
            [storefront.utils.query :as query]
            [storefront.components.utils :as utils]
            [storefront.components.formatters :refer [as-money]]
            [storefront.components.checkout-steps :refer [checkout-step-bar]]
            [storefront.components.validation-errors :refer [validation-errors-component]]
            [storefront.accessors.credit-cards :as cc]
            [storefront.accessors.orders :as orders]
            [storefront.messages :refer [send]]
            [clojure.string :as string]))

(defn display-use-store-credit [data]
  (let [available-credit (get-in data keypaths/user-total-available-store-credit)
        credit-to-use (min available-credit (get-in data keypaths/order-total))
        remaining-credit (- available-credit credit-to-use)]
    [:div
     [:h2.checkout-header "Store credit will be applied for this order!"]
     [:p.store-credit-instructions
      (as-money credit-to-use) " in store credit will be applied to this order ("
      (as-money remaining-credit) " remaining)"]
     (when (zero? remaining-credit)
       [:p.store-credit-instructions
        "Please enter an additional payment method below for the remaining total on your order."])]))

(defn field [id name app-state keypath presenter-fn & [text-attrs]]
  [:p.field
   [:label {:for id} name]
   [:input (merge {:type "text"
                   :id id
                   :name id
                   :value (presenter-fn (get-in app-state keypath))
                   :required true
                   :on-change (fn [e]
                                (send app-state
                                      events/control-change-state
                                      {:keypath keypath
                                       :value (.. e -target -value)}))}
                  text-attrs)]])

(defn display-credit-card-form [data]
  [:.credit-card-container
   (field "name" "Cardholder's Name" data keypaths/checkout-credit-card-name identity)
   (field "card_number" "Credit Card Number" data keypaths/checkout-credit-card-number cc/format-cc-number
          {:size 19 :max-length 19 :auto-complete "off" :data-hook "card_number" :class "required cardNumber"})
   (field "card_expiry" "Expiration" data keypaths/checkout-credit-card-expiration cc/format-expiration
          {:data-hook "card_expiration" :class "required cardExpiry" :placeholder "MM / YY"
           :max-length 9})
   (field "card_code" "3 digit number on back of card" data keypaths/checkout-credit-card-ccv identity
          {:size 5 :auto-complete "off" :data-hook "card_number" :class "required cardCode"
           :max-length 4})
   [:p.review-message
    "You can review your order on the next page before we charge your credit card"]])

(defn checkout-payment-component [data owner]
  (om/component
   (html
    [:#checkout
     (om/build validation-errors-component data)
     (checkout-step-bar data)
     [:.row
      [:.checkout-form-wrapper
       [:form.edit_order
        [:.checkout-container.payment
         (when (pos? (get-in data keypaths/user-total-available-store-credit))
           (display-use-store-credit data))
         (when-not (orders/fully-covered-by-store-credit?
                    (get-in data keypaths/order)
                    (get-in data keypaths/user))
           [:#cc-form
            [:div
             [:h2.checkout-header "Credit Card Info (Required)"]
             (display-credit-card-form data)]])

         [:.form-buttons
          (let [saving (query/get {:request-key request-keys/update-cart-payments}
                                  (get-in data keypaths/api-requests))]
            [:a.large.continue.button.primary
             {:on-click (when-not saving
                          (utils/send-event-callback data events/control-checkout-payment-method-submit))
              :class (when saving "saving")}
             "Continue"])]]]]]])))
