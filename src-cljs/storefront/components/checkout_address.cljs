(ns storefront.components.checkout-address
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.components.utils :as utils]
            [storefront.components.checkout-steps :refer [checkout-step-bar]]
            [storefront.components.validation-errors :refer [validation-errors-component]]
            [storefront.messages :refer [send]]
            [clojure.string :as string]))

(defn selected-value->int [evt]
  (let [elem (.-target evt)]
    (-> (.-value
         (aget (.-options elem)
               (.-selectedIndex elem)))
        (js/parseInt 10))))

(defn textfield [name & [{:keys [id value required? type on-change] :or {type "text"}}]]
  [:p.field
   [:label {:for id} name (when required? [:span.required "*"])]
   [:input {:id id
            :name id
            :type type
            :class (if required? "required" "")
            :value value
            :on-change on-change}]])

(defn selectfield [name & [{:keys [id value options required? on-change]}]]
  [:p.field
   [:label {:for id} name (when required? [:span.required "*"])]
   [:span
    [:br]
    [:select {:class (if required? "required" "")
              :name id
              :id id
              :on-change on-change
              :value value}
     [:option ""]
     (map (fn [{name :name val :id}]
            [:option {:value val} (str name)])
          options)]]])

(defn checkbox [name & [{:keys [id class value on-change checked]}]]
  [:p.field {:class class}
   [:input {:type "checkbox" :id id :name id :value value :on-change on-change :checked checked}]
   [:label {:for id} " " name]])

(defn billing-address-form [data owner]
  [:div.billing-address-wrapper
   [:fieldset#billing.billing-fieldset
    [:legend {:align "center"} "Billing Address"]
    [:p.checkout-address-warning
     "Please note: If your billing address does not match your credit card your order will be delayed."]

    [:div.inner
     (textfield "First Name"
                (merge (utils/change-text data owner keypaths/checkout-billing-address-firstname)
                       {:id :firstname
                        :required? true}))
     (textfield "Last Name"
                (merge (utils/change-text data owner keypaths/checkout-billing-address-lastname)
                       {:id :lastname
                        :required? true}))
     (textfield "Street Address"
                (merge (utils/change-text data owner keypaths/checkout-billing-address-address1)
                       {:id :address1
                        :required? true}))
     (textfield "Street Address (cont'd)"
                (merge (utils/change-text data owner keypaths/checkout-billing-address-address2)
                       {:id :address2}))
     (textfield "City"
                (merge (utils/change-text data owner keypaths/checkout-billing-address-city)
                       {:id :city
                        :required? true}))
     (selectfield "State"
                  {:id :state
                   :required? true
                   :options (get-in data keypaths/states)
                   :value (get-in data keypaths/checkout-billing-address-state)
                   :on-change #(send data
                                     events/control-change-state
                                     {:keypath keypaths/checkout-billing-address-state
                                      :value (selected-value->int %)})})
     (textfield "Zip"
                (merge (utils/change-text data owner keypaths/checkout-billing-address-zip)
                       {:id :zipcode
                        :required? true}))
     (textfield "Mobile Phone"
                (merge (utils/change-text data owner keypaths/checkout-billing-address-phone)
                       {:id :order_bill_address_attributes_phone
                        :required? true
                        :type "tel"}))
     (checkbox "Save my address"
               (merge (utils/change-checkbox
                       data
                       keypaths/checkout-billing-address-save-my-address)
                      {:id "save_user_address" :class "checkout-save-address"}))]]])

(defn shipping-address-form [data owner]
  [:div.shipping-address-wrapper
   [:fieldset#shipping.shipping-fieldset
    [:legend {:align "center"} "Shipping Address"]
    (checkbox "Use Billing Address"
              (merge (utils/change-checkbox
                      data
                      keypaths/checkout-shipping-address-use-billing-address)
                     {:id "use_billing" :class "checkbox checkout-use-billing-address"}))

    [:div.inner {:class (if (get-in data keypaths/checkout-shipping-address-use-billing-address) "hidden" "")}
     (textfield "First Name"
                (merge (utils/change-text data owner keypaths/checkout-shipping-address-firstname)
                       {:id :firstname
                        :required? true}))
     (textfield "Last Name"
                (merge (utils/change-text data owner keypaths/checkout-shipping-address-lastname)
                       {:id :lastname
                        :required? true}))
     (textfield "Street Address"
                (merge (utils/change-text data owner keypaths/checkout-shipping-address-address1)
                       {:id :address1
                        :required? true}))
     (textfield "Street Address (cont'd)"
                (merge (utils/change-text data owner keypaths/checkout-shipping-address-address2)
                       {:id :address2}))
     (textfield "City"
                (merge (utils/change-text data owner keypaths/checkout-shipping-address-city)
                       {:id :city
                        :required? true}))
     (selectfield "State"
                  {:id :state
                   :required? true
                   :options (get-in data keypaths/states)
                   :value (get-in data keypaths/checkout-shipping-address-state)
                   :on-change #(send data
                                     events/control-change-state
                                     {:keypath keypaths/checkout-shipping-address-state
                                      :value (selected-value->int %)})})
     (textfield "Zip"
                (merge (utils/change-text data owner keypaths/checkout-shipping-address-zip)
                       {:id :zipcode
                        :required? true}))
     (textfield "Mobile Phone"
                (merge (utils/change-text data owner keypaths/checkout-shipping-address-phone)
                       {:id :order_bill_address_attributes_phone
                        :required? true
                                :type "tel"}))]]])

(defn checkout-address-component [data owner]
  (om/component
   (html
    [:div#checkout
     (om/build validation-errors-component data)
     (checkout-step-bar data)
     [:div.row
      [:div.checkout-form-wrapper
       [:form.edit_order
        {:method "POST"
         :on-submit (utils/send-event-callback data events/control-checkout-update-addresses-submit)}

        (billing-address-form data owner)

        (shipping-address-form data owner)

        [:div.form-buttons.checkout.save-and-continue
         [:input.continue.button.primary {:type "submit" :name "Commit" :value "Save and Continue"}]]]]]])))
