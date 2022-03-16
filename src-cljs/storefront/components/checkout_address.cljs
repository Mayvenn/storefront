(ns storefront.components.checkout-address
  (:require [checkout.ui.molecules :as molecules]
            [checkout.ui.checkout-address-form :refer [opt-in-section]]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as component :refer [defcomponent defdynamic-component]]
            [storefront.components.checkout-steps :as checkout-steps]
            [storefront.components.ui :as ui]
            [ui.promo-banner :as promo-banner]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.request-keys :as request-keys]))

(defdynamic-component ^:private places-component
  (did-mount [this]
             (let [{:keys [address-keypath id]} (component/get-props this)]
               (handle-message events/checkout-address-component-mounted {:address-elem    id
                                                                          :address-keypath address-keypath})))
  (render [this]
          (let [{:keys [focused id keypath value data-test errors max-length]} (component/get-props this)]
            (component/html
             (ui/text-field {:data-test   data-test
                             :errors      errors
                             :id          id
                             :keypath     keypath
                             :focused     focused
                             :label       "Address"
                             :name        id
                             :on-key-down utils/suppress-return-key
                             :required    true
                             :type        "text"
                             :max-length  max-length
                             :value       value})))))

(defcomponent shipping-address-component
  [{:keys [focused shipping-address states email become-guest? google-maps-loaded? field-errors]} owner _]
  [:.flex.flex-column.items-center.col-12
   [:div.col-12.my1.proxima.title-3.shout.bold "Shipping Address"]
   [:.col-12
    (ui/text-field-group
     {:type          "text"
      :label         "First Name"
      :keypath       keypaths/checkout-shipping-address-first-name
      :focused       focused
      :value         (:first-name shipping-address)
      :max-length    24
      :errors        (get field-errors ["shipping-address" "first-name"])
      :name          "shipping-first-name"
      :data-test     "shipping-first-name"
      :id            "shipping-first-name"
      :auto-complete "shipping given-name"
      :required      true}
     {:type          "text"
      :label         "Last Name"
      :keypath       keypaths/checkout-shipping-address-last-name
      :focused       focused
      :value         (:last-name shipping-address)
      :max-length    25
      :errors        (get field-errors ["shipping-address" "last-name"])
      :name          "shipping-last-name"
      :id            "shipping-last-name"
      :data-test     "shipping-last-name"
      :auto-complete "shipping family-name"
      :required      true})]

   (when become-guest?
     (ui/text-field {:data-test "shipping-email"
                     :errors    (get field-errors ["email"])
                     :id        "shipping-email"
                     :keypath   keypaths/checkout-guest-email
                     :focused   focused
                     :label     "Email"
                     :name      "shipping-email"
                     :required  true
                     :type      "email"
                     :value     email}))

   (ui/text-field {:data-test "shipping-phone"
                   :errors    (get field-errors ["shipping-address" "phone"])
                   :id        "shipping-phone"
                   :keypath   keypaths/checkout-shipping-address-phone
                   :focused   focused
                   :label     "Mobile Phone"
                   :name      "shipping-phone"
                   :required  true
                   :type      "tel"
                   :value     (:phone shipping-address)})

   (when google-maps-loaded?
     (component/build places-component {:id              :shipping-address1
                                        :data-test       "shipping-address1"
                                        :address-keypath keypaths/checkout-shipping-address
                                        :keypath         keypaths/checkout-shipping-address-address1
                                        :focused         focused
                                        :errors          (get field-errors ["shipping-address" "address1"])
                                        :auto-complete   "shipping address-line1"
                                        :value           (:address1 shipping-address)
                                        :max-length      100}))

   [:.flex.flex-column.items-center.col-12
    [:.col-12
     (ui/text-field-group
      {:data-test     "shipping-address2"
       :errors        (get field-errors ["shipping-address" "address2"])
       :id            "shipping-address2"
       :keypath       keypaths/checkout-shipping-address-address2
       :focused       focused
       :label         "Apt/Suite"
       :name          "shipping-address2"
       :type          "text"
       :auto-complete "shipping address-line2"
       :value         (:address2 shipping-address)
       :max-length    100}
      {:data-test     "shipping-zip"
       :errors        (get field-errors ["shipping-address" "zipcode"])
       :id            "shipping-zip"
       :keypath       keypaths/checkout-shipping-address-zip
       :focused       focused
       :label         "Zip Code"
       :max-length    5
       :min-length    5
       :name          "shipping-zip"
       :pattern       "\\d{5}"
       :required      true
       :title         "zip code must be 5 digits"
       :type          "text"
       :auto-complete "shipping postal-code"
       :value         (:zipcode shipping-address)})]

    (ui/text-field {:data-test "shipping-city"
                    :errors    (get field-errors ["shipping-address" "city"])
                    :id        "shipping-city"
                    :keypath   keypaths/checkout-shipping-address-city
                    :focused   focused
                    :label     "City"
                    :name      "shipping-city"
                    :required  true
                    :type      "text"
                    :value     (:city shipping-address)})

    (ui/select-field {:data-test   "shipping-state"
                      :errors      (get field-errors ["shipping-address" "state"])
                      :id          :shipping-state
                      :keypath     keypaths/checkout-shipping-address-state
                      :focused     focused
                      :label       "State"
                      :options     states
                      :placeholder "State"
                      :required    true
                      :value       (:state shipping-address)})]])

(defcomponent billing-address-component
  [{:keys [focused billing-address states bill-to-shipping-address? google-maps-loaded? field-errors]} owner _]
  [:.flex.flex-column.items-center.col-12
   [:div.col-12.my1.proxima.title-3.shout.bold "Billing Address"]
   [:.col-12.my1
    [:label.h6.py1
     [:div.mr1
      (ui/check-box
       {:type      "checkbox"
        :label     "Use same address?"
        :id        "use_billing"
        :data-test "use-billing"
        :value     bill-to-shipping-address?
        :keypath   keypaths/checkout-bill-to-shipping-address})]]]
   (when-not bill-to-shipping-address?
     [:.col-12
      [:.col-12
       (ui/text-field-group
        {:type          "text"
         :label         "First Name"
         :keypath       keypaths/checkout-billing-address-first-name
         :focused       focused
         :value         (:first-name billing-address)
         :errors        (get field-errors ["billing-address" "first-name"])
         :name          "billing-first-name"
         :id            "billing-first-name"
         :data-test     "billing-first-name"
         :auto-complete "billing given-name"
         :required      true}

        {:type          "text"
         :label         "Last Name"
         :keypath       keypaths/checkout-billing-address-last-name
         :focused       focused
         :value         (:last-name billing-address)
         :errors        (get field-errors ["billing-address" "last-name"])
         :name          "billing-last-name"
         :id            "billing-last-name"
         :data-test     "billing-last-name"
         :auto-complete "billing family-name"
         :required      true})]

      (ui/text-field {:data-test "billing-phone"
                      :errors    (get field-errors ["billing-address" "phone"])
                      :id        "billing-phone"
                      :keypath   keypaths/checkout-billing-address-phone
                      :focused   focused
                      :label     "Mobile Phone"
                      :name      "billing-phone"
                      :required  true
                      :type      "tel"
                      :value     (:phone billing-address)})

      (when google-maps-loaded?
        (component/build places-component {:id              :billing-address1
                                           :data-test       "billing-address1"
                                           :address-keypath keypaths/checkout-billing-address
                                           :keypath         keypaths/checkout-billing-address-address1
                                           :focused         focused
                                           :errors          (get field-errors ["billing-address" "address1"])
                                           :auto-complete   "billing address-line1"
                                           :value           (:address1 billing-address)} nil))

      [:.flex.flex-column.items-center.col-12
       [:.col-12
        (ui/text-field-group
         {:type          "text"
          :label         "Apt/Suite"
          :keypath       keypaths/checkout-billing-address-address2
          :focused       focused
          :value         (:address2 billing-address)
          :errors        (get field-errors ["billing-address" "address2"])
          :name          "billing-address2"
          :id            "billing-address2"
          :auto-complete "billing address-line2"
          :data-test     "billing-address2"}
         {:type          "text"
          :label         "Zip Code"
          :keypath       keypaths/checkout-billing-address-zip
          :focused       focused
          :value         (:zipcode billing-address)
          :errors        (get field-errors ["billing-address" "zipcode"])
          :name          "billing-zip"
          :id            "billing-zip"
          :data-test     "billing-zip"
          :required      true
          :auto-complete "billing postal-code"
          :max-length    5
          :min-length    5
          :pattern       "\\d{5}"
          :title         "zip code must be 5 digits"})]

       (ui/text-field {:data-test "billing-city"
                       :errors    (get field-errors ["billing-address" "city"])
                       :id        "billing-city"
                       :keypath   keypaths/checkout-billing-address-city
                       :focused   focused
                       :label     "City"
                       :name      "billing-city"
                       :required  true
                       :type      "text"
                       :value     (:city billing-address)})

       (ui/select-field {:data-test   "billing-state"
                         :errors      (get field-errors ["billing-address" "state"])
                         :id          :billing-state
                         :keypath     keypaths/checkout-billing-address-state
                         :focused     focused
                         :label       "State"
                         :options     states
                         :placeholder "State"
                         :required    true
                         :value       (:state billing-address)})]])])

(defcomponent component
  [{:keys [saving? step-bar billing-address-data shipping-address-data free-install-added] :as data} _ _]
  [:div.container
  (molecules/free-install-added-atom free-install-added)
   (component/build checkout-steps/component step-bar)

   [:div.m-auto.col-8-on-tb-dt
    [:div.p3
     [:form.col-12.flex.flex-column.items-center
      {:on-submit (utils/send-event-callback events/control-checkout-update-addresses-submit
                                             {:become-guest? (:become-guest? shipping-address-data)})
       :data-test "address-form"}

      (component/build shipping-address-component shipping-address-data)
      [:div.bg-read
       (component/build billing-address-component billing-address-data)
       (component/build opt-in-section data)]

      [:div.my2.col-12.col-8-on-tb-dt.mx-auto
       (ui/submit-button "Continue to Payment" {:spinning? saving?
                                                :data-test "address-form-submit"})]]]]])

(defn opt-in-query [prompt-marketing-opt-in? phone-transactional-opt-in-value phone-marketing-opt-in-value]
  (merge
   (when prompt-marketing-opt-in?
     {:marketing-opt-in/id              "phone-marketing-opt-in"
      :marketing-opt-in/label           "… text me recurring automated marketing promotions, surveys and personalized messages."
      :marketing-opt-in/value           phone-marketing-opt-in-value
      :marketing-opt-in/keypath         keypaths/checkout-phone-marketing-opt-in})
   {:transactional-opt-in/id          "phone-transactional-opt-in"
    :transactional-opt-in/label       "… text me updates about my order."
    :transactional-opt-in/value       phone-transactional-opt-in-value
    :transactional-opt-in/keypath     keypaths/checkout-phone-transactional-opt-in
    :opt-in-legalese/terms-nav        [events/navigate-content-sms]
    :opt-in-legalese/privacy-nav      [events/navigate-content-privacy]}))

(defn ^:private free-install-added-query
  [free-install-added?]
  (when free-install-added?
    {:free-install-added/primary "Free Install Added to Order"}))

(defn query [data]
  (let [google-maps-loaded? (get-in data keypaths/loaded-google-maps)
        states              (map (juxt :name :abbr) (get-in data keypaths/states))
        field-errors        (get-in data keypaths/field-errors)
        free-install-added? (:free-install-added (get-in data keypaths/navigation-query-params))]
    (merge
     {:saving?               (utils/requesting? data request-keys/update-addresses)
      :step-bar              (cond-> (checkout-steps/query data)
                               (= :guest (::auth/as (auth/signed-in data)))
                               (assoc :checkout-title "Guest Checkout"))
      :billing-address-data  {:billing-address           (get-in data keypaths/checkout-billing-address)
                              :states                    states
                              :bill-to-shipping-address? (get-in data keypaths/checkout-bill-to-shipping-address)
                              :google-maps-loaded?       google-maps-loaded?
                              :field-errors              field-errors
                              :focused                   (get-in data keypaths/ui-focus)}
      :shipping-address-data {:shipping-address    (get-in data keypaths/checkout-shipping-address)
                              :states              states
                              :email               (get-in data keypaths/checkout-guest-email)
                              :become-guest?       false
                              :google-maps-loaded? google-maps-loaded?
                              :field-errors        field-errors
                              :focused             (get-in data keypaths/ui-focus)}
      :free-install-added (free-install-added-query free-install-added?)}
     (opt-in-query true
                   (get-in data keypaths/checkout-phone-transactional-opt-in)
                   (get-in data keypaths/checkout-phone-marketing-opt-in)))))

(defn ^:export built-component [data opts]
  (component/build component (query data)))
