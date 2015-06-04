(ns storefront.components.checkout-payment
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.components.utils :as utils]
            [storefront.components.formatters :refer [as-money]]
            [storefront.components.checkout-steps :refer [checkout-step-bar]]
            [storefront.credit-cards :as cc]
            [storefront.messages :refer [enqueue-message]]
            [clojure.string :as string]))

(defn stylist? [user]
  (seq (:store-slug user)))

(defn display-use-store-credit-option [data]
  [:div
   [:h2.checkout-header "Store credit can be applied for this order!"]
   [:ul.field.radios.store-credit-options
    [:li.store-credit-option.selected
     [:label
      [:input.store-credit-radio
       (merge {:type "radio"
               :name "use_store_credits"
               :value true}
              (if (pos? (get-in data keypaths/order-total-applicable-store-credit))
                {:checked "checked"}
                {}))
       [:div.checkbox-container [:figure.large-checkbox]]
       [:div.store-credit-container
        [:div#select_store_credit.use-store-credit-option
         [:div (str
                "Use store credit: "
                (:store_credits (get-in data keypaths/user)))
          "Use store credit: "
          (as-money (get-in data keypaths/user-total-available-store-credit))
          " available"]
         [:br]
         (when (stylist? (get-in data keypaths/user))
           [:span "(Coupons will be automatically removed for Stylists)"])]]]]]
    [:li.store-credit-option
     [:label
      [:input.store-credit-radio
       (merge {:type "radio"
               :name "use_store_credits"
               :value false}
              (if-not (pos? (get-in data keypaths/order-total-applicable-store-credit))
                {:checked "checked"}))
       [:div.checkbox-container [:figure.large-checkbox]]
       [:div.store-credit-container
        [:div#select_store_credit.use-store-credit-option
         [:div "Do not use store credit"]
         [:br]
         (when (stylist? (get-in data keypaths/user))
           [:span "(Coupons can be used by Stylists)"])]]]]]]])

(defn field [id name app-state keypath presenter-fn & [text-attrs]]
  [:p.field
   [:label {:for id} name]
   [:input (merge {:type "text"
                   :id id
                   :name id
                   :value (presenter-fn (get-in app-state keypath))
                   :required true
                   :on-change (fn [e]
                                (enqueue-message (get-in @app-state keypaths/event-ch)
                                                 [events/control-change-state {:keypath keypath
                                                                               :value (.. e -target -value)}]))}
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

(defn checkout-payment-component [data owner]
  (om/component
   (html
    [:div#checkout
     (checkout-step-bar data)
     [:div.row
      [:div.checkout-form-wrapper
       [:form.edit_order
        {:method "POST"
         :on-submit (utils/enqueue-event data events/control-checkout-payment-method-submit)}

        [:div.checkout-container.payment
         (when (pos? (get-in data keypaths/user-total-available-store-credit))
           (display-use-store-credit-option data))
         [:div#cc-form
          [:div
           (if (and (> (get-in data keypaths/order-total-applicable-store-credit) 0)
                    (not (get-in data keypaths/order-covered-by-store-credit)))
             [:h2.checkout-header "Credit Card Info (Required for remaining balance)"]
             [:h2.checkout-header "Credit Card Info (Required)"])

           (display-credit-card-form data)]]

         [:div.form-buttons
          [:input.continue.button.primary {:type "submit" :name "Commit" :value "Continue"}]]]]]]])))
