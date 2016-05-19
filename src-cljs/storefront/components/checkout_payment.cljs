(ns storefront.components.checkout-payment
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.request-keys :as request-keys]
            [storefront.components.utils :as utils]
            [storefront.components.ui :as ui]
            [storefront.components.formatters :refer [as-money]]
            [storefront.components.checkout-steps :as checkout-steps :refer [checkout-step-bar]]
            [storefront.components.validation-errors :refer [validation-errors-component redesigned-validation-errors-component]]
            [storefront.hooks.experiments :as experiments]
            [storefront.accessors.credit-cards :as cc]
            [storefront.accessors.orders :as orders]
            [storefront.messages :refer [handle-message]]
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
                                (handle-message events/control-change-state
                                                {:keypath keypath
                                                 :value (.. e -target -value)}))}
                  text-attrs)]])

(defn display-credit-card-form [data]
  [:.credit-card-container
   (field "name" "Cardholder's Name" data keypaths/checkout-credit-card-name identity)
   (field "cardNumber" "Credit Card Number" data keypaths/checkout-credit-card-number cc/format-cc-number
          {:size 19 :max-length 19 :auto-complete "off" :class "required cardNumber" :type "tel"})
   (field "card_expiry" "Expiration" data keypaths/checkout-credit-card-expiration cc/format-expiration
          {:class "required cardExpiry" :placeholder "MM / YY"
           :max-length 9 :type "tel"})
   (field "card_code" "3 digit number on back of card" data keypaths/checkout-credit-card-ccv identity
          {:size 5 :auto-complete "off" :class "required cardCode"
           :max-length 4 :type "tel"})])

(defn checkout-payment-credit-card-component [data owner]
  (om/component
   (html
    [:#cc-form
     [:div
      [:h2.checkout-header "Credit Card Info (Required)"]
      (display-credit-card-form data)]])))

(defn old-checkout-payment-component [data owner]
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
           [:div
            (om/build checkout-payment-credit-card-component data)
            [:p.review-message
             "You can review your order on the next page before we charge your credit card"]])

         (when (get-in data keypaths/loaded-stripe)
           [:.form-buttons
            (let [saving (or (utils/requesting? data request-keys/stripe-create-token)
                             (utils/requesting? data request-keys/update-cart-payments))]
              [:a.large.continue.button.primary
               {:on-click (when-not saving
                            (utils/send-event-callback events/control-checkout-payment-method-submit))
                :class (when saving "saving")}
               "Review Order"])])]]]]])))

(defn redesigned-credit-card-form-component [{:keys [credit-card]} owner]
  (let [{:keys [name number expiration ccv]} credit-card]
    (om/component
     (html
      [:div
       [:.h2.my2 "Payment Information"]
       [:div
        (ui/text-field "Cardholder's Name" keypaths/checkout-credit-card-name name
                       {:name     "name"
                        :required true})
        (ui/text-field "Credit Card Number" keypaths/checkout-credit-card-number (cc/format-cc-number number)
                       {:max-length    19
                        :auto-complete "off"
                        :class         "cardNumber rounded-1"
                        :type          "tel"
                        :required      true})
        [:.flex.col-12
         [:.col-6
          (ui/text-field "Expiration (MM/YY)" keypaths/checkout-credit-card-expiration (cc/format-expiration expiration)
                         {:max-length    9
                          :auto-complete "off"
                          :class         "cardExpiry rounded-left-1"
                          :type          "tel"
                          :required      true})]
         [:.col-6
          (ui/text-field "Security Code" keypaths/checkout-credit-card-ccv ccv
                         {:max-length    4
                          :auto-complete "off"
                          :class         "cardCode rounded-right-1 border-width-left-0"
                          :type          "tel"
                          :required      true})]]]]))))

(defn redesigned-credit-card-form-query [data]
  {:credit-card {:name (get-in data keypaths/checkout-credit-card-name)
                 :number (get-in data keypaths/checkout-credit-card-number)
                 :expiration (get-in data keypaths/checkout-credit-card-expiration)
                 :ccv (get-in data keypaths/checkout-credit-card-ccv)}})

(defn redesigned-checkout-payment-component [{:keys [step-bar saving? loaded-stripe? store-credit errors credit-card]} owner]
  (om/component
   (html
    (ui/narrow-container
     (om/build redesigned-validation-errors-component errors)
     (om/build checkout-steps/redesigned-checkout-step-bar step-bar)

     (let [{:keys [credit-available credit-applicable fully-covered?]} store-credit]
       [:form
        {:on-submit (utils/send-event-callback events/control-checkout-payment-method-submit)}

        (when (pos? credit-available)
          (ui/note-box
           "green"
           [:.p2.navy
            [:.h4 [:span.medium (as-money credit-applicable)] " in store credit will be applied to this order."]
            (when-not fully-covered?
              [:.h5.mt1.line-height-2
               "Please enter an additional payment method below for the remaining total on your order."])]))

        (when-not fully-covered?
          [:div
           (om/build redesigned-credit-card-form-component {:credit-card credit-card})
           [:.h4.gray
            "You can review your order on the next page before we charge your credit card."]] )

        (when loaded-stripe?
          [:.my2
           (ui/submit-button "Go to Review Order" {:spinning? saving?})])])))))

(defn saving-card? [data]
  (or (utils/requesting? data request-keys/stripe-create-token)
      (utils/requesting? data request-keys/update-cart-payments)))

(defn query [data]
  (let [available-store-credit (get-in data keypaths/user-total-available-store-credit)
        credit-to-use          (min available-store-credit (get-in data keypaths/order-total))]
    (merge
     {:store-credit   {:credit-available  available-store-credit
                       :credit-applicable credit-to-use
                       :fully-covered? (orders/fully-covered-by-store-credit?
                                        (get-in data keypaths/order)
                                        (get-in data keypaths/user))}
      :errors         (get-in data keypaths/validation-errors-details)
      :saving?        (saving-card? data)
      :loaded-stripe? (get-in data keypaths/loaded-stripe)
      :step-bar       (checkout-steps/query data)}
     (redesigned-credit-card-form-query data))))

(defn checkout-payment-component [data owner]
  (om/component
   (html
    (if (experiments/three-steps-redesign? data)
      (om/build redesigned-checkout-payment-component (query data))
      (om/build old-checkout-payment-component data)))))
