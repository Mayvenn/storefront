(ns storefront.components.checkout-address
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.components.utils :as utils]
            [storefront.request-keys :as request-keys]
            [storefront.utils.query :as query]
            [storefront.components.checkout-steps :refer [checkout-step-bar]]
            [storefront.components.validation-errors :refer [validation-errors-component]]
            [storefront.messages :refer [send]]
            [storefront.hooks.experiments :as experiments]
            [clojure.string :as string]))

(defn selected-value [evt]
  (let [elem (.-target evt)]
    (.-value
     (aget (.-options elem)
           (.-selectedIndex elem)))))

(defn textfield [label & [{:keys [id value required? type on-change label-class] :or {type "text"}}]]
  [:p.field
   [:label {:for id :class label-class} label (when required? [:span.required "*"])]
   [:input {:id id
            :name id
            :type type
            :class (when required? "required")
            :value value
            :on-change on-change}]])

(defn selectfield [name & [{:keys [id value options required? on-change label-class]}]]
  [:p.field
   [:label {:for id :class label-class} name (when required? [:span.required "*"])]
   [:span
    [:br]
    [:select {:class (if required? "required" "")
              :name id
              :id id
              :on-change on-change
              :value value}
     [:option ""]
     (map (fn [{name :name val :abbr}]
            [:option {:value val} (str name)])
          options)]]])

(defn checkbox [label & [{:keys [id class value on-change checked]}]]
  [:p.field {:class class}
   [:input {:type "checkbox" :id id :name id :value value :on-change on-change :checked checked}]
   [:label {:for id} " " label]])

(defn billing-address-form [data owner]
  (let [label-class (when (experiments/simplify-funnel? data) "straight-text")]
    [:div.billing-address-wrapper
     [:fieldset#billing.billing-fieldset
      [:legend {:align "center"} "Billing Address"]
      (when-not (experiments/simplify-funnel? data)
        [:p.checkout-address-warning
         "Please note: If your billing address does not match your credit card your order will be delayed."])

      [:div.inner
       (textfield "First Name"
                  (merge (utils/change-text data owner keypaths/checkout-billing-address-first-name)
                         {:id :first-name :required? true :label-class label-class}))
       (textfield "Last Name"
                  (merge (utils/change-text data owner keypaths/checkout-billing-address-last-name)
                         {:id :last-name :required? true :label-class label-class}))
       (textfield "Street Address"
                  (merge (utils/change-text data owner keypaths/checkout-billing-address-address1)
                         {:id :address1 :required? true :label-class label-class}))
       (textfield "Street Address (cont'd)"
                  (merge (utils/change-text data owner keypaths/checkout-billing-address-address2)
                         {:id :address2 :label-class label-class}))
       (textfield "City"
                  (merge (utils/change-text data owner keypaths/checkout-billing-address-city)
                         {:id :city :required? true :label-class label-class}))
       (selectfield "State"
                    {:id :state
                     :required? true
                     :options (get-in data keypaths/states)
                     :value (get-in data keypaths/checkout-billing-address-state)
                     :label-class label-class
                     :on-change #(send data
                                       events/control-change-state
                                       {:keypath keypaths/checkout-billing-address-state
                                        :value (selected-value %)})})
       (textfield "Zip"
                  (merge (utils/change-text data owner keypaths/checkout-billing-address-zip)
                         {:id :zipcode :required? true :label-class label-class}))
       (textfield "Mobile Phone"
                  (merge (utils/change-text data owner keypaths/checkout-billing-address-phone)
                         {:id :order_bill_address_attributes_phone :required? true :type "tel" :label-class label-class}))
       (checkbox "Save my address"
                 (merge (utils/change-checkbox
                         data
                         keypaths/checkout-save-my-addresses-no-op)
                        {:id "save_user_address" :class "checkout-save-address" :label-class label-class}))]]]))

(defn shipping-address-form [data owner]
  (let [label-class (when (experiments/simplify-funnel? data) "straight-text")]
    [:div.shipping-address-wrapper
     [:fieldset#shipping.shipping-fieldset
      [:legend {:align "center"} "Shipping Address"]
      (checkbox "Use Billing Address"
                (merge (utils/change-checkbox
                        data
                        keypaths/checkout-ship-to-billing-address)
                       {:id "use_billing" :class "checkbox checkout-use-billing-address" :label-class label-class}))

      [:div.inner {:class (if (get-in data keypaths/checkout-ship-to-billing-address) "hidden" "")}
       (textfield "First Name"
                  (merge (utils/change-text data owner keypaths/checkout-shipping-address-first-name)
                         {:id :first-name
                          :required? true
                          :label-class label-class}))
       (textfield "Last Name"
                  (merge (utils/change-text data owner keypaths/checkout-shipping-address-last-name)
                         {:id :last-name
                          :required? true
                          :label-class label-class}))
       (textfield "Street Address"
                  (merge (utils/change-text data owner keypaths/checkout-shipping-address-address1)
                         {:id :address1
                          :required? true
                          :label-class label-class}))
       (textfield "Street Address (cont'd)"
                  (merge (utils/change-text data owner keypaths/checkout-shipping-address-address2)
                         {:id :address2
                          :label-class label-class}))
       (textfield "City"
                  (merge (utils/change-text data owner keypaths/checkout-shipping-address-city)
                         {:id :city
                          :required? true
                          :label-class label-class}))
       (selectfield "State"
                    {:id :state
                     :required? true
                     :label-class label-class
                     :options (get-in data keypaths/states)
                     :value (get-in data keypaths/checkout-shipping-address-state)
                     :on-change #(send data
                                       events/control-change-state
                                       {:keypath keypaths/checkout-shipping-address-state
                                        :value (selected-value %)})})
       (textfield "Zip"
                  (merge (utils/change-text data owner keypaths/checkout-shipping-address-zip)
                         {:id :zipcode
                          :required? true
                          :label-class label-class}))
       (textfield "Mobile Phone"
                  (merge (utils/change-text data owner keypaths/checkout-shipping-address-phone)
                         {:id :order_bill_address_attributes_phone
                          :required? true
                          :label-class label-class
                          :type "tel"}))]]]))

(defn checkout-address-component [data owner]
  (om/component
   (html
    [:div#checkout
     (om/build validation-errors-component data)
     (checkout-step-bar data)
     [:div.row
      [:div.checkout-form-wrapper
       [:form.edit_order
        (billing-address-form data owner)
        (shipping-address-form data owner)
        [:div.form-buttons.checkout.save-and-continue
         (let [saving (query/get {:request-key request-keys/update-addresses}
                                 (get-in data keypaths/api-requests))]
           [:a.large.continue.button.primary
            {:on-click (when-not saving (utils/send-event-callback data events/control-checkout-update-addresses-submit))
             :class [(when saving "saving")
                     (when (experiments/simplify-funnel? data) "bright")]}
            (if (experiments/simplify-funnel? data)
              "Continue to Shipping"
              "Save and Continue")])]]]]])))
