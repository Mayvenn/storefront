(ns storefront.components.checkout-payment
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.state :as state]
            [storefront.events :as events]
            [storefront.components.utils :as utils]
            [storefront.components.checkout-steps :refer [checkout-step-bar]]
            [clojure.string :as string]))


(defn display-use-store-credit-option [data]
  [:div
   [:h2.checkout-header "Store credit can be applied for this order!"]
   [:ul.field.radios.store-credit-options
    [:li.store-credit-option.selected.no-cc-form ;; TODO: if order covered by store credit, clear no-cc-form?
     [:label
      [:input.store-credit-radio
       {:type "radio"
        :name "use_store_credits"
        :value true
        :checked "checked" ;; TODO:params[:use_store_credits].blank? || @order.using_store_credit?
        }
       [:div.checkbox-container [:figure.large-checkbox]]
       [:div.store-credit-container
        [:div#select_store_credit.use-store-credit-option
         [:div "Use store credit: TODO:Spree::Money.new( order.user.total_available_store_credit ) available"]
         [:br]
         (when true ;; TODO: order.user.stylist?
           [:span "(Coupons will be automatically removed for Stylists)"])]]]]]
    [:li.store-credit-option
     [:label
      [:input.store-credit-radio
       {:type "radio"
        :name "use_store_credits"
        :value false
        :checked "checked" ;; TODO: params[:use_store_credits] == 'false' || !@order.using_store_credit?
        }
       [:div.checkbox-container [:figure.large-checkbox]]
       [:div.store-credit-container
        [:div#select_store_credit.use-store-credit-option
         [:div "Do not use store credit"]
         [:br]
         (when true ;; TODO: order.user.stylist?
           [:span "(Coupons can be used by Stylists)"])]]]]]]])

(defn field [id name value & [text-attrs]]
  [:p.field
   [:label {:for id} name]
   [:input (merge {:type "text" :id id :name id :value value :required true} text-attrs)]])

(defn display-credit-card-form [data]
  [:div.credit-card-container
   (field "name" "Cardholder's Name" "<TODO: firstname + lastname>")
   (field "card_number" "Credit Card Number" ""
          {:size 19 :maxlength 19 :autocomplete "off" :data-hook "card_number"})
   (field "card_expiry" "Expiration" ""
          {:data-hook "card_expiration" :class "required cardExpiry" :placeholder "MM / YY"})
   (field "card_code" "3 digit number on back of card" ""
          {:size 5 :autocomplete "off" :data-hook "card_number" :class "required cardCode"})
   [:p.review-message
            "You can review your order on the next page"
            (when true ;; order.covered_by_store_credit?
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
         :on-submit (utils/enqueue-event data events/control-checkout-update-addresses-submit)}

        [:div.checkout-container.payment
         (when true ;; TODO:order.total_applicable_store_credit > 0
           (display-use-store-credit-option data))
         [:div#cc-form
          [:div
           (if true ;; order.total_applicable_store_credit > 0 && !order.covered_by_store_credit?
             [:h2.checkout-header "Credit Card Info (Required for remaining balance)"]
             [:h2.checkout-header "Credit Card Info (Required)"])

           (display-credit-card-form data)]]

         [:div.form-buttons
          [:input.continue.button.primary {:type "submit" :name "Commit" :value "Continue"}]]]]]]])))
