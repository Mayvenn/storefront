(ns storefront.components.checkout-address
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.components.utils :as utils]
            [storefront.components.ui :as ui]
            [storefront.request-keys :as request-keys]
            [storefront.utils.query :as query]
            [storefront.components.checkout-steps :as checkout-steps]
            [storefront.components.validation-errors :refer [validation-errors-component redesigned-validation-errors-component]]
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
            [:option {:key val :value val} (str name)])
          options)]]])

(defn checkbox [label & [{:keys [id class value on-change checked]}]]
  [:p.field {:class class}
   [:input {:type "checkbox" :id id :name id :value value :on-change on-change :checked checked}]
   [:label {:for id} " " label]])

(defn redesigned-places-component [{:keys [id address-keypath keypath value]} owner]
  (reify
    om/IDidMount
    (did-mount [this]
      (handle-message events/checkout-address-component-mounted {:address-elem id
                                                                 :address-keypath address-keypath}))
    om/IRender
    (render [_]
      (html
       (ui/text-field "Address" keypath value
                      {:type "text"
                       :name id
                       :id id
                       :required true
                       :on-key-down utils/suppress-return-key})))))

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
  (let [expanded? (not (empty? (get-in data keypaths/checkout-billing-address-address1)))]
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
       (when (get-in data keypaths/checkout-as-guest)
         (textfield "Email"
                    (merge (utils/change-text data owner keypaths/checkout-guest-email)
                           {:id :guest-email
                            :required? true
                            :type "email"})))
       (textfield "Mobile Phone"
                  (merge (utils/change-text data owner keypaths/checkout-billing-address-phone)
                         {:id :billing-phone :required? true :type "tel"}))
       (if (and (get-in data keypaths/loaded-places)
                (get-in data keypaths/places-enabled))
         (om/build places-component data {:opts {:id :billing-address1
                                                 :address-keypath keypaths/checkout-billing-address
                                                 :keypath keypaths/checkout-billing-address-address1}})
         (textfield "Street Address"
                    (merge (utils/change-text data owner keypaths/checkout-billing-address-address1)
                           {:id :billing-address1})))
       (when expanded?
         (textfield "Street Address (cont'd)"
                    (merge (utils/change-text data owner keypaths/checkout-billing-address-address2)
                           {:id :billing-address2})))
       (when expanded?
         (textfield "City"
                    (merge (utils/change-text data owner keypaths/checkout-billing-address-city)
                           {:id :billing-city :required? true})))
       (when expanded?
         (selectfield "State"
                      {:id :billing-state
                       :required? true
                       :options (get-in data keypaths/states)
                       :value (get-in data keypaths/checkout-billing-address-state)
                       :on-change #(handle-message events/control-change-state
                                                   {:keypath keypaths/checkout-billing-address-state
                                                    :value (selected-value %)})}))
       (when expanded?
         (textfield "Zip"
                    (merge (utils/change-text data owner keypaths/checkout-billing-address-zip)
                           {:id :billing-zipcode :required? true})))]]]))

(defn shipping-address-form [data owner]
  (let [expanded? (not (empty? (get-in data keypaths/checkout-shipping-address-address1)))]
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
       (when expanded?
         (textfield "Street Address (cont'd)"
                    (merge (utils/change-text data owner keypaths/checkout-shipping-address-address2)
                           {:id :shipping-address2})))

       (when expanded?
         (textfield "City"
                    (merge (utils/change-text data owner keypaths/checkout-shipping-address-city)
                           {:id :shipping-city
                            :required? true})))
       (when expanded?
         (selectfield "State"
                      {:id :shipping-state
                       :required? true
                       :options (get-in data keypaths/states)
                       :value (get-in data keypaths/checkout-shipping-address-state)
                       :on-change #(handle-message events/control-change-state
                                                   {:keypath keypaths/checkout-shipping-address-state
                                                    :value (selected-value %)})}))
       (when expanded?
         (textfield "Zip"
                    (merge (utils/change-text data owner keypaths/checkout-shipping-address-zip)
                           {:id :shipping-zipcode
                            :required? true})))]]]))

(defn redesigned-shipping-address-component [{:keys [shipping-address
                                                     states
                                                     email
                                                     guest?
                                                     places-loaded?
                                                     shipping-expanded?]}
                                             owner]
  (om/component
   (html
    [:.flex.flex-column.items-center.col-12
     [:.h3.black.col-12.my1 "Shipping Address"]
     [:.flex.col-12
      [:.col-6
       (ui/text-field "First Name" keypaths/checkout-shipping-address-first-name (:first-name shipping-address)
                      {:autofocus "autofocus"
                       :type      "text"
                       :name      "shipping-first-name"
                       :id        "shipping-first-name"
                       :class     "rounded-left-1"
                       :required  true})]

      [:.col-6
       (ui/text-field "Last Name" keypaths/checkout-shipping-address-last-name (:last-name shipping-address)
                      {:type     "text"
                       :name     "shipping-last-name"
                       :id       "shipping-last-name"
                       :class    "rounded-right-1 border-width-left-0"
                       :required true})]]

     ;; TODO: hide when not guest, and query
     (when guest?
       (ui/text-field "Email" keypaths/checkout-guest-email email
                      {:type     "email"
                       :name     "shipping-email"
                       :id       "shipping-email"
                       :required true}))

     (ui/text-field "Mobile Phone" keypaths/checkout-shipping-address-phone (:phone shipping-address)
                    {:type     "tel"
                     :name     "shipping-phone"
                     :id       "shipping-phone"
                     :required true})

     (when places-loaded?
       (om/build redesigned-places-component {:id              :shipping-address1
                                              :address-keypath keypaths/checkout-shipping-address
                                              :keypath         keypaths/checkout-shipping-address-address1
                                              :value           (:address1 shipping-address)}))

     (when shipping-expanded?
       [:.flex.flex-column.items-center.col-12
        [:.flex.col-12
         [:.col-6
          (ui/text-field "Apt/Suite" keypaths/checkout-shipping-address-address2 (:address2 shipping-address)
                         {:type  "text"
                          :name  "shipping-address2"
                          :class "rounded-left-1"
                          :id    "shipping-address2"})]
         [:.col-6
          (ui/text-field "Zip Code" keypaths/checkout-shipping-address-zip (:zipcode shipping-address)
                         {:type       "text"
                          :name       "shipping-zip"
                          :id         "shipping-zip"
                          :class      "rounded-right-1 border-width-left-0"
                          :required   true
                          :max-length 5
                          :min-length 5
                          :pattern    "\\d{5}"
                          :title      "zip code must be 5 digits"})]]

        (ui/text-field "City" keypaths/checkout-shipping-address-city (:city shipping-address)
                       {:type     "text"
                        :name     "shipping-city"
                        :id       "shipping-city"
                        :required true})

        (ui/select-field "State" keypaths/checkout-shipping-address-state (:state shipping-address) states
                         {:id       :shipping-state
                          :required true})])])))

(defn redesigned-billing-address-component [{:keys [billing-address
                                                    states
                                                    bill-to-shipping-address?
                                                    places-loaded?
                                                    billing-expanded?]}
                                             owner]
  (om/component
   (html
    [:.flex.flex-column.items-center.col-12
     [:.h3.black.col-12.my1 "Billing Address"]
     [:.col-12
      [:label.h5.gray
       [:input.mr1
        (merge (utils/toggle-checkbox keypaths/checkout-bill-to-shipping-address bill-to-shipping-address?)
               {:type  "checkbox"
                :id    "use_billing"
                :class "checkbox  checkout-use-billing-address"})]
       "Use same address?"]]
     (when-not bill-to-shipping-address?
       [:.col-12
        [:.flex.col-12
         [:.col-6
          (ui/text-field "First Name" keypaths/checkout-billing-address-first-name (:first-name billing-address)
                         {:autofocus "autofocus"
                          :type      "text"
                          :name      "billing-first-name"
                          :id        "billing-first-name"
                          :class     "rounded-left-1"
                          :required  true})]

         [:.col-6
          (ui/text-field "Last Name" keypaths/checkout-billing-address-last-name (:last-name billing-address)
                         {:type     "text"
                          :name     "billing-last-name"
                          :id       "billing-last-name"
                          :class    "rounded-right-1 border-width-left-0"
                          :required true})]]

        (ui/text-field "Mobile Phone" keypaths/checkout-billing-address-phone (:phone billing-address)
                       {:type     "tel"
                        :name     "billing-phone"
                        :id       "billing-phone"
                        :required true})

        (when places-loaded?
          (om/build redesigned-places-component {:id              :billing-address1
                                                 :address-keypath keypaths/checkout-billing-address
                                                 :keypath         keypaths/checkout-billing-address-address1
                                                 :value           (:address1 billing-address)}))

        (when billing-expanded?
          [:.flex.flex-column.items-center.col-12
           [:.flex.col-12
            [:.col-6
             (ui/text-field "Apt/Suite" keypaths/checkout-billing-address-address2 (:address2 billing-address)
                            {:type  "text"
                             :name  "billing-address2"
                             :class "rounded-left-1"
                             :id    "billing-address2"})]
            [:.col-6
             (ui/text-field "Zip Code" keypaths/checkout-billing-address-zip (:zipcode billing-address)
                            {:type       "text"
                             :name       "billing-zip"
                             :id         "billing-zip"
                             :class      "rounded-right-1 border-width-left-0"
                             :required   true
                             :max-length 5
                             :min-length 5
                             :pattern    "\\d{5}"
                             :title      "zip code must be 5 digits"})]]

           (ui/text-field "City" keypaths/checkout-billing-address-city (:city billing-address)
                          {:type     "text"
                           :name     "billing-city"
                           :id       "billing-city"
                           :required true})

           (ui/select-field "State" keypaths/checkout-billing-address-state (:state billing-address) states
                            {:id       :billing-state
                             :required true})])])])))

(defn redesigned-checkout-address-component [{:keys [saving? errors step-bar] :as data} owner]
  (om/component
   (html
    [:.bg-white.black.sans-serif
     [ui/container
      (om/build redesigned-validation-errors-component errors)
      (om/build checkout-steps/redesigned-checkout-step-bar step-bar)

      [:form.col-12.flex.flex-column.items-center
       {:on-submit (utils/send-event-callback events/control-checkout-update-addresses-submit)}

       (om/build redesigned-shipping-address-component data)
       (om/build redesigned-billing-address-component data)]

      [:.my2.col-12
       (ui/submit-button "Continue to Payment" saving?)]]])))

(defn old-checkout-address-component [data owner]
  (om/component
   (html
    [:div#checkout
     (om/build validation-errors-component data)
     (checkout-steps/checkout-step-bar data)
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
            (if (experiments/three-steps? data)
              "Continue to Payment"
              "Continue to Shipping")])]]]]])))

(defn query [data]
  {:billing-address           (get-in data keypaths/checkout-billing-address)
   :shipping-address          (get-in data keypaths/checkout-shipping-address)
   :states                    (get-in data keypaths/states)
   :email                     (get-in data keypaths/checkout-guest-email)
   :saving?                   (query/get {:request-key request-keys/update-addresses} (get-in data keypaths/api-requests))
   :errors                    (get-in data keypaths/validation-errors-details)
   :bill-to-shipping-address? (get-in data keypaths/checkout-bill-to-shipping-address)
   :places-loaded?            (get-in data keypaths/loaded-places)
   :guest?                    (get-in data keypaths/checkout-as-guest)
   :shipping-expanded?        (not (empty? (get-in data keypaths/checkout-shipping-address-address1)))
   :billing-expanded?         (not (empty? (get-in data keypaths/checkout-shipping-address-address1)))
   :step-bar                  (checkout-steps/query data)})

(defn checkout-address-component [data owner]
  (om/component
   (html
    [:div
     (if (experiments/three-steps-redesign? data)
       (om/build redesigned-checkout-address-component (query data))
       (om/build old-checkout-address-component data))])))
