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
            [storefront.messages :refer [handle-message]]
            [storefront.hooks.experiments :as experiments]
            [clojure.string :as string]))

(defn selected-value [evt]
  (let [elem (.-target evt)]
    (.-value
     (aget (.-options elem)
           (.-selectedIndex elem)))))

(defn textfield [label & [{:keys [id placeholder value required? type on-change] :or {type "text"}}]]
  [:p.field
   [:label {:for id} label (when required? [:span.required "*"])]
   [:input {:id id
            :name id
            :type type
            :class (when required? "required")
            :value value
            :placeholder placeholder
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
     (map (fn [{name :name val :abbr}]
            [:option {:value val} (str name)])
          options)]]])

(defn checkbox [label & [{:keys [id class value on-change checked]}]]
  [:p.field {:class class}
   [:input {:type "checkbox" :id id :name id :value value :on-change on-change :checked checked}]
   [:label {:for id} " " label]])

(defn places-component [data owner {:keys [id address-keypath keypath]}]
  (reify
    om/IDidMount
    (did-mount [this]
      (handle-message events/checkout-address-component-mounted {:address-elem id
                                                                 :address-keypath address-keypath}))
    om/IRender
    (render [_]
      (html (textfield "Street Address"
                       (merge (utils/change-text data owner keypath)
                              {:id id
                               :placeholder ""
                               :required? true}))))))

(defn billing-address-form [data owner]
  [:div.billing-address-wrapper
   [:fieldset#billing.billing-fieldset
    [:legend {:align "center"} "Billing Address"]
    [:div.inner
     (textfield "First Name"
                (merge (utils/change-text data owner keypaths/checkout-billing-address-first-name)
                       {:id :billing-first-name :required? true}))
     (textfield "Last Name"
                (merge (utils/change-text data owner keypaths/checkout-billing-address-last-name)
                       {:id :billing-last-name :required? true}))
     (when (and (experiments/guest-checkout? data)
                (get-in data keypaths/checkout-as-guest))
       (textfield "Email"
                  (merge (utils/change-text data owner keypaths/checkout-guest-email)
                         {:id :guest-email
                          :required? true
                          :type "email"})))
     (textfield "Mobile Phone"
                (merge (utils/change-text data owner keypaths/checkout-billing-address-phone)
                       {:id :billing-phone :required? true :type "tel"}))
     (when (get-in data keypaths/loaded-places)
       (om/build places-component data {:opts {:id :billing-address1
                                               :address-keypath keypaths/checkout-billing-address
                                               :keypath keypaths/checkout-billing-address-address1}}))
     (when (not (empty? (get-in data keypaths/checkout-billing-address-address1)))
       (list
        (textfield "Street Address (cont'd)"
                   (merge (utils/change-text data owner keypaths/checkout-billing-address-address2)
                          {:id :billing-address2}))
        (textfield "City"
                   (merge (utils/change-text data owner keypaths/checkout-billing-address-city)
                          {:id :billing-city :required? true}))
        (selectfield "State"
                     {:id :billing-state
                      :required? true
                      :options (get-in data keypaths/states)
                      :value (get-in data keypaths/checkout-billing-address-state)
                      :on-change #(handle-message events/control-change-state
                                                  {:keypath keypaths/checkout-billing-address-state
                                                   :value (selected-value %)})})
        (textfield "Zip"
                   (merge (utils/change-text data owner keypaths/checkout-billing-address-zip)
                          {:id :billing-zipcode :required? true}))))]]])

(defn shipping-address-form [data owner]
  [:div.shipping-address-wrapper
   [:fieldset#shipping.shipping-fieldset
    [:legend {:align "center"} "Shipping Address"]
    (checkbox "Use Billing Address"
              (merge (utils/change-checkbox
                      data
                      keypaths/checkout-ship-to-billing-address)
                     {:id "use_billing" :class "checkbox checkout-use-billing-address"}))

    [:div.inner {:class (if (get-in data keypaths/checkout-ship-to-billing-address) "hidden" "")}
     (textfield "First Name"
                (merge (utils/change-text data owner keypaths/checkout-shipping-address-first-name)
                       {:id :shipping-first-name
                        :required? true}))
     (textfield "Last Name"
                (merge (utils/change-text data owner keypaths/checkout-shipping-address-last-name)
                       {:id :shipping-last-name
                        :required? true}))
     (textfield "Mobile Phone"
                (merge (utils/change-text data owner keypaths/checkout-shipping-address-phone)
                       {:id :shipping-phone
                        :required? true
                        :type "tel"}))
     (when (get-in data keypaths/loaded-places)
       (om/build places-component data {:opts {:id :shipping-address1
                                               :address-keypath keypaths/checkout-shipping-address
                                               :keypath keypaths/checkout-shipping-address-address1}}))
     (when (not (empty? (get-in data keypaths/checkout-shipping-address-address1)))
       (list
        (textfield "Street Address (cont'd)"
                   (merge (utils/change-text data owner keypaths/checkout-shipping-address-address2)
                          {:id :shipping-address2}))
        (textfield "City"
                   (merge (utils/change-text data owner keypaths/checkout-shipping-address-city)
                          {:id :shipping-city
                           :required? true}))
        (selectfield "State"
                     {:id :shipping-state
                      :required? true
                      :options (get-in data keypaths/states)
                      :value (get-in data keypaths/checkout-shipping-address-state)
                      :on-change #(handle-message events/control-change-state
                                                  {:keypath keypaths/checkout-shipping-address-state
                                                   :value (selected-value %)})})
        (textfield "Zip"
                   (merge (utils/change-text data owner keypaths/checkout-shipping-address-zip)
                          {:id :shipping-zipcode
                           :required? true}))))]]])

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
            {:on-click (when-not saving (utils/send-event-callback events/control-checkout-update-addresses-submit))
             :class (when saving "saving")}
            "Continue to Shipping"])]]]]])))
