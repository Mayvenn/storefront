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
       (ui/text-field "Address"
                      keypath
                      value
                      {:type        "text"
                       :name        id
                       :id          id
                       :required    true
                       :errors      errors
                       :data-test   data-test
                       :on-key-down utils/suppress-return-key})))))

(defn ^:private shipping-address-component
  [{:keys [shipping-address states email guest? places-loaded? shipping-expanded? field-errors]} owner]
  (om/component
   (html
    [:.flex.flex-column.items-center.col-12
     [:.h3.black.col-12.my1 "Shipping Address"]
     [:.flex.col-12
      [:.col-6 (ui/text-field "First Name"
                              keypaths/checkout-shipping-address-first-name
                              (:first-name shipping-address)
                              {:autofocus "autofocus"
                               :type      "text"
                               :name      "shipping-first-name"
                               :data-test "shipping-first-name"
                               :id        "shipping-first-name"
                               :class     "rounded-left"
                               :errors    (get field-errors ["shipping-address" "first-name"])
                               :required  true})]

      [:.col-6 (ui/text-field "Last Name"
                              keypaths/checkout-shipping-address-last-name
                              (:last-name shipping-address)
                              {:type      "text"
                               :name      "shipping-last-name"
                               :id        "shipping-last-name"
                               :data-test "shipping-last-name"
                               :class     "rounded-right border-width-left-0"
                               :errors    (get field-errors ["shipping-address" "last-name"])
                               :required  true})]]

     (when guest?
       (ui/text-field "Email"
                      keypaths/checkout-guest-email
                      email
                      {:type      "email"
                       :name      "shipping-email"
                       :id        "shipping-email"
                       :data-test "shipping-email"
                       :errors    (get field-errors ["email"])
                       :required  true}))

     (ui/text-field "Mobile Phone"
                    keypaths/checkout-shipping-address-phone
                    (:phone shipping-address)
                    {:type      "tel"
                     :name      "shipping-phone"
                     :id        "shipping-phone"
                     :data-test "shipping-phone"
                     :errors    (get field-errors ["shipping-address" "phone"])
                     :required  true})

     (when places-loaded?
       (om/build places-component {:id              :shipping-address1
                                   :data-test       "shipping-address1"
                                   :address-keypath keypaths/checkout-shipping-address
                                   :keypath         keypaths/checkout-shipping-address-address1
                                   :errors          (get field-errors ["shipping-address" "address1"])
                                   :value           (:address1 shipping-address)}))

     (when shipping-expanded?
       [:.flex.flex-column.items-center.col-12
        [:.flex.col-12
         [:.col-6 (ui/text-field "Apt/Suite"
                                 keypaths/checkout-shipping-address-address2
                                 (:address2 shipping-address)
                                 {:type      "text"
                                  :name      "shipping-address2"
                                  :data-test "shipping-address2"
                                  :class     "rounded-left"
                                  :errors    (get field-errors ["shipping-address" "address2"])
                                  :id        "shipping-address2"})]
         [:.col-6 (ui/text-field "Zip Code"
                                 keypaths/checkout-shipping-address-zip
                                 (:zipcode shipping-address)
                                 {:type       "text"
                                  :name       "shipping-zip"
                                  :id         "shipping-zip"
                                  :data-test  "shipping-zip"
                                  :class      "rounded-right border-width-left-0"
                                  :required   true
                                  :max-length 5
                                  :min-length 5
                                  :pattern    "\\d{5}"
                                  :errors     (get field-errors ["shipping-address" "zipcode"])
                                  :title      "zip code must be 5 digits"})]]

        (ui/text-field "City"
                       keypaths/checkout-shipping-address-city
                       (:city shipping-address)
                       {:type      "text"
                        :name      "shipping-city"
                        :id        "shipping-city"
                        :data-test "shipping-city"
                        :errors    (get field-errors ["shipping-address" "city"])
                        :required  true})

        (ui/select-field "State"
                         keypaths/checkout-shipping-address-state
                         (:state shipping-address)
                         states
                         {:id          :shipping-state
                          :data-test   "shipping-state"
                          :placeholder "State"
                          :errors      (get field-errors ["shipping-address" "state"])
                          :required    true})])])))

(defn ^:private billing-address-component
  [{:keys [billing-address states bill-to-shipping-address? places-loaded? billing-expanded? field-errors]} owner]
  (om/component
   (html
    [:.flex.flex-column.items-center.col-12
     [:.h3.black.col-12.my1 "Billing Address"]
     [:.col-12.my1
      [:label.h5.gray.py1
       [:input.mr1
        (merge (utils/toggle-checkbox keypaths/checkout-bill-to-shipping-address
                                      bill-to-shipping-address?)
               {:type      "checkbox"
                :id        "use_billing"
                :data-test "use-billing"})]
       "Use same address?"]]
     (when-not bill-to-shipping-address?
       [:.col-12
        [:.flex.col-12
         [:.col-6
          (ui/text-field "First Name"
                         keypaths/checkout-billing-address-first-name
                         (:first-name billing-address)
                         {:autofocus "autofocus"
                          :type      "text"
                          :name      "billing-first-name"
                          :id        "billing-first-name"
                          :data-test "billing-first-name"
                          :class     "rounded-left"
                          :errors    (get field-errors ["billing-address" "first-name"])
                          :required  true})]

         [:.col-6
          (ui/text-field "Last Name"
                         keypaths/checkout-billing-address-last-name
                         (:last-name billing-address)
                         {:type      "text"
                          :name      "billing-last-name"
                          :id        "billing-last-name"
                          :data-test "billing-last-name"
                          :class     "rounded-right border-width-left-0"
                          :errors    (get field-errors ["billing-address" "last-name"])
                          :required  true})]]

        (ui/text-field "Mobile Phone"
                       keypaths/checkout-billing-address-phone
                       (:phone billing-address)
                       {:type      "tel"
                        :name      "billing-phone"
                        :id        "billing-phone"
                        :data-test "billing-phone"
                        :errors    (get field-errors ["billing-address" "phone"])
                        :required  true})

        (when places-loaded?
          (om/build places-component {:id              :billing-address1
                                      :data-test       "billing-address1"
                                      :address-keypath keypaths/checkout-billing-address
                                      :keypath         keypaths/checkout-billing-address-address1
                                      :errors          (get field-errors ["billing-address" "address1"])
                                      :value           (:address1 billing-address)}))

        (when billing-expanded?
          [:.flex.flex-column.items-center.col-12
           [:.flex.col-12
            [:.col-6 (ui/text-field "Apt/Suite"
                                    keypaths/checkout-billing-address-address2
                                    (:address2 billing-address)
                                    {:type      "text"
                                     :name      "billing-address2"
                                     :class     "rounded-left"
                                     :id        "billing-address2"
                                     :errors    (get field-errors ["billing-address" "address2"])
                                     :data-test "billing-address2" })]
            [:.col-6 (ui/text-field "Zip Code"
                                    keypaths/checkout-billing-address-zip
                                    (:zipcode billing-address)
                                    {:type       "text"
                                     :name       "billing-zip"
                                     :id         "billing-zip"
                                     :data-test  "billing-zip"
                                     :class      "rounded-right border-width-left-0"
                                     :errors     (get field-errors ["billing-address" "zipcode"])
                                     :required   true
                                     :max-length 5
                                     :min-length 5
                                     :pattern    "\\d{5}"
                                     :title      "zip code must be 5 digits"})]]

           (ui/text-field "City"
                          keypaths/checkout-billing-address-city
                          (:city billing-address)
                          {:type      "text"
                           :name      "billing-city"
                           :id        "billing-city"
                           :data-test "billing-city"
                           :errors    (get field-errors ["billing-address" "city"])
                           :required  true})

           (ui/select-field "State"
                            keypaths/checkout-billing-address-state
                            (:state billing-address)
                            states
                            {:id          :billing-state
                             :data-test   "billing-state"
                             :placeholder "State"
                             :errors      (get field-errors ["billing-address" "state"])
                             :required    true})])])])))

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

(defn built-component [data owner]
  (om/component (html (om/build component (query data)))))
