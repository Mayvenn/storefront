(ns storefront.components.checkout-address
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.checkout-steps :as checkout-steps]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.request-keys :as request-keys]))

(defn ^:private places-component [{:keys [id address-keypath keypath value data-test errors]} owner]
  (reify
    om/IDidMount
    (did-mount [this]
      (handle-message events/checkout-address-component-mounted {:address-elem    id
                                                                 :address-keypath address-keypath}))
    om/IRender
    (render [_]
      (html
       (ui/text-field {:data-test   data-test
                       :errors      errors
                       :id          id
                       :keypath     keypath
                       :label       "Address"
                       :name        id
                       :on-key-down utils/suppress-return-key
                       :required    true
                       :type        "text"
                       :value       value})))))

(defn ^:private shipping-address-component
  [{:keys [shipping-address states email guest? places-loaded? shipping-expanded? field-errors]} owner]
  (om/component
   (html
    [:.flex.flex-column.items-center.col-12
     [:.h4.dark-gray.col-12.my1 "Shipping Address"]
     [:.col-12
      (ui/text-field-group
       {:type       "text"
        :label      "First Name"
        :keypath    keypaths/checkout-shipping-address-first-name
        :value      (:first-name shipping-address)
        :errors     (get field-errors ["shipping-address" "first-name"])
        :auto-focus "autofocus"
        :name       "shipping-first-name"
        :data-test  "shipping-first-name"
        :id         "shipping-first-name"
        :required   true}
       {:type      "text"
        :label     "Last Name"
        :keypath   keypaths/checkout-shipping-address-last-name
        :value     (:last-name shipping-address)
        :errors    (get field-errors ["shipping-address" "last-name"])
        :name      "shipping-last-name"
        :id        "shipping-last-name"
        :data-test "shipping-last-name"
        :required  true})]

     (when guest?
       (ui/text-field {:data-test "shipping-email"
                       :errors    (get field-errors ["email"])
                       :id        "shipping-email"
                       :keypath   keypaths/checkout-guest-email
                       :label     "Email"
                       :name      "shipping-email"
                       :required  true
                       :type      "email"
                       :value     email}))

     (ui/text-field {:data-test "shipping-phone"
                     :errors    (get field-errors ["shipping-address" "phone"])
                     :id        "shipping-phone"
                     :keypath   keypaths/checkout-shipping-address-phone
                     :label     "Mobile Phone"
                     :name      "shipping-phone"
                     :required  true
                     :type      "tel"
                     :value     (:phone shipping-address)})

     (when places-loaded?
       (om/build places-component {:id              :shipping-address1
                                   :data-test       "shipping-address1"
                                   :address-keypath keypaths/checkout-shipping-address
                                   :keypath         keypaths/checkout-shipping-address-address1
                                   :errors          (get field-errors ["shipping-address" "address1"])
                                   :value           (:address1 shipping-address)}))

     (when shipping-expanded?
       [:.flex.flex-column.items-center.col-12
        [:.col-12
         (ui/text-field-group
          {:data-test "shipping-address2"
           :errors    (get field-errors ["shipping-address" "address2"])
           :id        "shipping-address2"
           :keypath   keypaths/checkout-shipping-address-address2
           :label     "Apt/Suite"
           :name      "shipping-address2"
           :type      "text"
           :value     (:address2 shipping-address)}
          {:data-test  "shipping-zip"
           :errors     (get field-errors ["shipping-address" "zipcode"])
           :id         "shipping-zip"
           :keypath    keypaths/checkout-shipping-address-zip
           :label      "Zip Code"
           :max-length 5
           :min-length 5
           :name       "shipping-zip"
           :pattern    "\\d{5}"
           :required   true
           :title      "zip code must be 5 digits"
           :type       "text"
           :value      (:zipcode shipping-address)})]

        (ui/text-field {:data-test "shipping-city"
                        :errors    (get field-errors ["shipping-address" "city"])
                        :id        "shipping-city"
                        :keypath   keypaths/checkout-shipping-address-city
                        :label     "City"
                        :name      "shipping-city"
                        :required  true
                        :type      "text"
                        :value     (:city shipping-address)})

        (ui/select-field {:data-test   "shipping-state"
                          :errors      (get field-errors ["shipping-address" "state"])
                          :id          :shipping-state
                          :keypath     keypaths/checkout-shipping-address-state
                          :label       "State"
                          :options     states
                          :placeholder "State"
                          :required    true
                          :value       (:state shipping-address)})])])))

(defn ^:private billing-address-component
  [{:keys [billing-address states bill-to-shipping-address? places-loaded? billing-expanded? field-errors]} owner]
  (om/component
   (html
    [:.flex.flex-column.items-center.col-12
     [:.h4.dark-gray.col-12.my1 "Billing Address"]
     [:.col-12.my1
      [:label.h6.gray.py1
       [:input.mr1
        (merge (utils/toggle-checkbox keypaths/checkout-bill-to-shipping-address
                                      bill-to-shipping-address?)
               {:type      "checkbox"
                :id        "use_billing"
                :data-test "use-billing"})]
       "Use same address?"]]
     (when-not bill-to-shipping-address?
       [:.col-12
        [:.col-12
         (ui/text-field-group
          {:type       "text"
           :label      "First Name"
           :keypath    keypaths/checkout-billing-address-first-name
           :value      (:first-name billing-address)
           :errors     (get field-errors ["billing-address" "first-name"])
           :auto-focus "autofocus"
           :name       "billing-first-name"
           :id         "billing-first-name"
           :data-test  "billing-first-name"
           :required   true}

          {:type      "text"
           :label     "Last Name"
           :keypath   keypaths/checkout-billing-address-last-name
           :value     (:last-name billing-address)
           :errors    (get field-errors ["billing-address" "last-name"])
           :name      "billing-last-name"
           :id        "billing-last-name"
           :data-test "billing-last-name"
           :required  true})]

        (ui/text-field {:data-test "billing-phone"
                        :errors    (get field-errors ["billing-address" "phone"])
                        :id        "billing-phone"
                        :keypath   keypaths/checkout-billing-address-phone
                        :label     "Mobile Phone"
                        :name      "billing-phone"
                        :required  true
                        :type      "tel"
                        :value     (:phone billing-address)})

        (when places-loaded?
          (om/build places-component {:id              :billing-address1
                                      :data-test       "billing-address1"
                                      :address-keypath keypaths/checkout-billing-address
                                      :keypath         keypaths/checkout-billing-address-address1
                                      :errors          (get field-errors ["billing-address" "address1"])
                                      :value           (:address1 billing-address)}))

        (when billing-expanded?
          [:.flex.flex-column.items-center.col-12
           [:.col-12
            (ui/text-field-group
             {:type      "text"
              :label     "Apt/Suite"
              :keypath   keypaths/checkout-billing-address-address2
              :value     (:address2 billing-address)
              :errors    (get field-errors ["billing-address" "address2"])
              :name      "billing-address2"
              :id        "billing-address2"
              :data-test "billing-address2"}
             {:type       "text"
              :label      "Zip Code"
              :keypath    keypaths/checkout-billing-address-zip
              :value      (:zipcode billing-address)
              :errors     (get field-errors ["billing-address" "zipcode"])
              :name       "billing-zip"
              :id         "billing-zip"
              :data-test  "billing-zip"
              :required   true
              :max-length 5
              :min-length 5
              :pattern    "\\d{5}"
              :title      "zip code must be 5 digits"})]

           (ui/text-field {:data-test "billing-city"
                           :errors    (get field-errors ["billing-address" "city"])
                           :id        "billing-city"
                           :keypath   keypaths/checkout-billing-address-city
                           :label     "City"
                           :name      "billing-city"
                           :required  true
                           :type      "text"
                           :value     (:city billing-address)})

           (ui/select-field {:data-test   "billing-state"
                             :errors      (get field-errors ["billing-address" "state"])
                             :id          :billing-state
                             :keypath     keypaths/checkout-billing-address-state
                             :label       "State"
                             :options     states
                             :placeholder "State"
                             :required    true
                             :value       (:state billing-address)})])])])))

(defn component
  [{:keys [saving? step-bar billing-address-data shipping-address-data]} owner]
  (om/component
   (html
    (ui/narrow-container
     (om/build checkout-steps/component step-bar)

     [:form.col-12.flex.flex-column.items-center
      {:on-submit (utils/send-event-callback events/control-checkout-update-addresses-submit)
       :data-test "address-form"}

      (om/build shipping-address-component shipping-address-data)
      (om/build billing-address-component billing-address-data)

      [:.my2.col-12
       (ui/submit-button "Continue to Payment" {:spinning? saving?
                                                :data-test "address-form-submit"})]]))))

(defn query [data]
  (let [places-loaded? (get-in data keypaths/loaded-places)
        states         (map (juxt :name :abbr) (get-in data keypaths/states))
        field-errors   (get-in data keypaths/field-errors)]
    {:saving?               (utils/requesting? data request-keys/update-addresses)
     :step-bar              (checkout-steps/query data)
     :billing-address-data  {:billing-address           (get-in data keypaths/checkout-billing-address)
                             :states                    states
                             :bill-to-shipping-address? (get-in data keypaths/checkout-bill-to-shipping-address)
                             :places-loaded?            places-loaded?
                             :billing-expanded?         (not (empty? (get-in data keypaths/checkout-billing-address-address1)))
                             :field-errors              field-errors}
     :shipping-address-data {:shipping-address   (get-in data keypaths/checkout-shipping-address)
                             :states             states
                             :email              (get-in data keypaths/checkout-guest-email)
                             :guest?             (get-in data keypaths/checkout-as-guest)
                             :places-loaded?     places-loaded?
                             :shipping-expanded? (not (empty? (get-in data keypaths/checkout-shipping-address-address1)))
                             :field-errors       field-errors}}))

(defn built-component [data opts]
  (om/build component (query data)))
