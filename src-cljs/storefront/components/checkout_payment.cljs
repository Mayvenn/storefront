(ns storefront.components.checkout-payment
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.components.utils :as utils]
            [storefront.components.formatters :refer [as-money]]
            [storefront.components.checkout-steps :refer [checkout-step-bar]]
            [storefront.components.validation-errors :refer [validation-errors-component]]
            [storefront.accessors.credit-cards :as cc]
            [storefront.accessors.orders :as orders]
            [storefront.messages :refer [send]]
            [clojure.string :as string]))

(defn stylist? [user]
  (seq (:store-slug user)))

(defn change-radio [app-state keypath value-if-checked]
  (let [keypath-value (get-in app-state keypath)]
    {:checked (when (= keypath-value value-if-checked) "checked" "")
     :value value-if-checked
     :on-change (fn [e]
                  (send app-state
                        events/control-change-state
                        {:keypath keypath
                         :value value-if-checked}))}))

(defn display-radio [data keypath value-if-checked text additional-text-when-stylist]
  [:li.store-credit-option
   (when (= (get-in data keypath) value-if-checked)
     {:class "selected"})
   [:label
    [:input.store-credit-radio
     (merge {:type "radio"
             :name "use_store_credits"}
            (change-radio data keypath value-if-checked))
     [:div.checkbox-container [:figure.large-checkbox]]
     [:div.store-credit-container
      [:div#select_store_credit.use-store-credit-option
       [:div text]
       (when (stylist? (get-in data keypaths/user))
         [:span additional-text-when-stylist])]]]]])

(defn display-use-store-credit-option [data]
  [:div
   [:h2.checkout-header "Store credit can be applied for this order!"]
   [:ul.field.radios.store-credit-options
    (display-radio
     data
     keypaths/checkout-use-store-credits
     true
     (str "Use store credit: "
          (as-money (get-in data keypaths/user-total-available-store-credit))
          " available")
     "(Coupons will be automatically removed for Stylists)")

    (display-radio
     data
     keypaths/checkout-use-store-credits
     false
     "Do not use store credit"
     "(Coupons can be used by Stylists)")]])

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
  [:div.credit-card-container
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
    "You can review your order on the next page"
    (when-not (get-in data keypaths/order-covered-by-store-credit)
      " before we can charge your credit card")]])

(defn fully-covered-by-store-credit? [app-state]
  (and (get-in app-state keypaths/checkout-use-store-credits)
       (get-in app-state keypaths/order-covered-by-store-credit)))

(defn partially-covered-by-store-credit? [app-state]
  (and (get-in app-state keypaths/checkout-use-store-credits)
       (orders/partially-covered-by-store-credit? (get-in app-state keypaths/order))))

(defn checkout-payment-component [data owner]
  (om/component
   (html
    [:div#checkout
     (om/build validation-errors-component data)
     (checkout-step-bar data)
     (when (seq (get-in data keypaths/payment-methods))
       [:div.row
        [:div.checkout-form-wrapper
         [:form.edit_order
          {:method "POST"
           :on-submit (utils/send-event-callback data events/control-checkout-payment-method-submit)}

          [:div.checkout-container.payment
           (when (pos? (get-in data keypaths/user-total-available-store-credit))
             (display-use-store-credit-option data))
           (when-not (fully-covered-by-store-credit? data)
             [:div#cc-form
              [:div
               (if (partially-covered-by-store-credit? data)
                 [:h2.checkout-header "Credit Card Info (Required for remaining balance)"]
                 [:h2.checkout-header "Credit Card Info (Required)"])

               (display-credit-card-form data)]])

           [:div.form-buttons
            [:input.continue.button.primary {:type "submit" :name "Commit" :value "Continue"}]]]]]])])))
