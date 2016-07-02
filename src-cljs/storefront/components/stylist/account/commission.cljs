(ns storefront.components.stylist.account.commission
  (:require [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.platform.component-utils :as utils]))

(defn component [{:keys [payout-method
                         payout-methods
                         venmo-phone
                         paypal-email
                         address1
                         address2
                         zipcode
                         city
                         state-id
                         states]} owner opts]
  (component/create
   [:form {:on-submit
           (utils/send-event-callback events/control-stylist-manage-account-submit)}
    [:h1.h2.light.col-12.my3.center "Update commission info"]

    [:div
     (ui/select-field "Payout Method"
                      payout-method
                      payout-methods
                      {:id           "payout-method"
                       :data-test    "payout-method"
                       :required     true
                       :on-change    #(handle-message events/control-change-state
                                                      {:keypath (conj keypaths/stylist-manage-account :chosen_payout_method)
                                                       :value   (ui/selected-value %)})})]


    (condp = payout-method
      "venmo"         (ui/text-field "Venmo Phone #"
                                     (conj keypaths/stylist-manage-account :venmo_payout_attributes :phone)
                                     venmo-phone
                                     {:type      "tel"
                                      :name      "venmo-phone"
                                      :id        "venmo-phone"
                                      :data-test "venmo-phone"
                                      :required  true})
      "paypal"        (ui/text-field "PayPal Email"
                                     (conj keypaths/stylist-manage-account :paypal_payout_attributes :email)
                                     paypal-email
                                     {:type      "email"
                                      :name      "paypal-email"
                                      :id        "paypal-email"
                                      :data-test "paypal-email"
                                      :required  true})
      "mayvenn_debit" [:div [:p "A prepaid Visa debit card will be mailed to the below address"]]
      "check"         [:div [:p "Checks will mail to the below address"]]
      [:div [:p "Checks will mail to the below address"]])

    [:div.border-top.border-light-silver.py3.mt3
     (ui/text-field "Address"
                    (conj keypaths/stylist-manage-account :address :address1)
                    address1
                    {:autofocus "autofocus"
                     :type      "text"
                     :name      "account-address1"
                     :id        "account-address1"
                     :data-test "account-address1"
                     :required  true})

     [:.flex.col-12
      [:.col-6 (ui/text-field "Apt/Suite"
                              (conj keypaths/stylist-manage-account :address :address2)
                              address2
                              {:type      "text"
                               :name      "account-address2"
                               :data-test "account-address2"
                               :id        "account-address2"
                               :class     "rounded-left"
                               :required  true})]

      [:.col-6 (ui/text-field "Zip Code"
                              (conj keypaths/stylist-manage-account :address :zipcode)
                              zipcode
                              {:type      "text"
                               :name      "account-zipcode"
                               :id        "account-zipcode"
                               :data-test "account-zipcode"
                               :class     "rounded-right border-width-left-0"
                               :required  true})]]

     (ui/text-field "City"
                    (conj keypaths/stylist-manage-account :address :city)
                    city
                    {:type      "text"
                     :name      "account-password-city"
                     :id        "account-password-city"
                     :data-test "account-password-city"
                     :required  true})

     (ui/select-field "State"
                      state-id
                      states
                      {:id          :account-state
                       :data-test   "account-state"
                       :placeholder "State"
                       :required    true
                       :on-change   #(handle-message events/control-change-state
                                                     {:keypath (conj keypaths/stylist-manage-account :address :state_id)
                                                      :value   (ui/selected-value %)})})

     [:.my2.col-12
      (ui/submit-button "Update" {:spinning? false
                                  :data-test "account-form-submit"})]]]))

(defn payout-methods [original-payout-method]
  (cond-> [["Venmo" "venmo"]
           ["PayPal" "paypal"]
           ["Check" "check"]]
    (= original-payout-method "mayvenn_debit") (conj ["Mayvenn Debit" "mayvenn_debit"])))

(defn query [data]
  {:payout-method  (get-in data (conj keypaths/stylist-manage-account :chosen_payout_method))
   :payout-methods (payout-methods (get-in data (conj keypaths/stylist-manage-account :original_payout_method)))
   :paypal-email    (get-in data (conj keypaths/stylist-manage-account :paypal_payout_attributes :email))
   :venmo-phone    (get-in data (conj keypaths/stylist-manage-account :venmo_payout_attributes :phone))
   :address1       (get-in data (conj keypaths/stylist-manage-account :address :address1))
   :address2       (get-in data (conj keypaths/stylist-manage-account :address :address2))
   :city           (get-in data (conj keypaths/stylist-manage-account :address :city))
   :zipcode        (get-in data (conj keypaths/stylist-manage-account :address :zipcode))
   :state-id       (get-in data (conj keypaths/stylist-manage-account :address :state_id))
   :states         (map (juxt :name :id) (get-in data keypaths/states))})
