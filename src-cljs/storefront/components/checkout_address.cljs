(ns storefront.components.checkout-address
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.state :as state]
            [storefront.events :as events]
            [storefront.components.utils :as utils]
            [clojure.string :as string]))

(def us-states
  (vec
   (map (fn [[value name]] {:name name :value value})
        [[39 "Alabama"]
         [50 "Alaska"]
         [5 "Arizona"]
         [11 "Arkansas"]
         [32 "California"]
         [45 "Colorado"]
         [47 "Connecticut"]
         [20 "Delaware"]
         [13 "District of Columbia"]
         [27 "Florida"]
         [44 "Georgia"]
         [17 "Hawaii"]
         [14 "Idaho"]
         [6 "Illinois"]
         [35 "Indiana"]
         [40 "Iowa"]
         [9 "Kansas"]
         [42 "Kentucky"]
         [23 "Louisiana"]
         [34 "Maine"]
         [26 "Maryland"]
         [46 "Massachusetts"]
         [1 "Michigan"]
         [29 "Minnesota"]
         [41 "Mississippi"]
         [10 "Missouri"]
         [24 "Montana"]
         [15 "Nebraska"]
         [12 "Nevada"]
         [7 "New Hampshire"]
         [30 "New Jersey"]
         [43 "New Mexico"]
         [48 "New York"]
         [8 "North Carolina"]
         [33 "North Dakota"]
         [31 "Ohio"]
         [22 "Oklahoma"]
         [37 "Oregon"]
         [16 "Pennsylvania"]
         [21 "Rhode Island"]
         [49 "South Carolina"]
         [2 "South Dakota"]
         [25 "Tennessee"]
         [36 "Texas"]
         [18 "Utah"]
         [19 "Vermont"]
         [28 "Virginia"]
         [3 "Washington"]
         [51 "West Virginia"]
         [4 "Wisconsin"]
         [38 "Wyoming"]])))

(def checkout-steps ["address" "delivery" "payment" "confirm"])

(defn display-progress-step [current-index index state]
  [:li.progress-step
   {:class (->> [state
                 (if (< index current-index) "completed")
                 (if (= index (inc current-index)) "next")
                 (if (= index current-index) "current")
                 (if (zero? index) "first")
                 (if (= index (dec (count state))) "last")]
                (filter seq)
                (string/join " "))}
   [:span
    (let [text [:div.progress-step-index
                (str (inc index) " ")
                (string/capitalize state)]]
      (if (< index current-index)
        [:a {:href "TODO:/checkout_state_path(state)"} text]
        text))]])

(defn checkout-progress [app-state current-step]
  (let [current-step-index (->> checkout-steps
                                (map-indexed vector)
                                (filter #(= (second %) current-step))
                                ffirst)]
    [:ol {:class (str "progress-steps checkout-step-" current-step)}
     (map-indexed (partial display-progress-step current-step-index)
                  checkout-steps)]))


(defn order-details [app-state checkout-current-step]
  (let [checkout-step (get-in app-state checkout-current-step)]
    [:div#checkout
     [:div.row
      [:div.columns.thirteen.omega (checkout-progress app-state checkout-current-step)]]
     [:div.row
      [:div.checkout-form-wrapper
       [:form
        {:id (str "checkout_form_" checkout-step)
         :action "#TODO:"
         :method "POST"}
        ]]]]))

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
              :on-change on-change}
     [:option {:value ""} ""]
     (map (fn [{name :name val :value}]
            [:option {:value val :checked (if (= value val) "checked" "")}
             (str name)])
          options)]]])

(defn checkbox [name & [{:keys [id class value on-change checked]}]]
  [:p.field {:class class}
   [:input {:type "checkbox" :id id :name id :value value :on-change on-change :checked checked}]
   [:label {:for id} " " name]])

(defn billing-address-form [data]
  [:div.billing-address-wrapper
   [:fieldset#billing.billing-fieldset
    [:legend {:align "center"} "Billing Address"]
    [:p.checkout-address-warning
     "Please note: If your billing address does not match your credit card your order will be delayed."]

    [:div.inner
     (textfield "First Name"
                (merge (utils/update-text data events/control-checkout-change [:billing-address :firstname])
                       {:id :firstname
                        :required? true
                        :value (str (get-in data state/checkout-billing-address-firstname-path))}))
     (textfield "Last Name"
                (merge (utils/update-text data events/control-checkout-change [:billing-address :lastname])
                       {:id :lastname
                        :required? true
                        :value (str (get-in data state/checkout-billing-address-lastname-path))}))
     (textfield "Street Address"
                (merge (utils/update-text data events/control-checkout-change [:billing-address :address1])
                       {:id :address1
                        :required? true
                        :value (str (get-in data state/checkout-billing-address-address1-path))}))
     (textfield "Street Address (cont'd)"
                (merge (utils/update-text data events/control-checkout-change [:billing-address :address2])
                       {:id :address2
                        :value (str (get-in data state/checkout-billing-address-address2-path))}))
     (textfield "City"
                (merge (utils/update-text data events/control-checkout-change [:billing-address :city])
                       {:id :city
                        :required? true
                        :value (str (get-in data state/checkout-billing-address-city-path))}))
     (selectfield "State"
                  (merge {:on-change #(utils/put-event data
                                                       events/control-checkout-change
                                                       {[:billing-address :state]
                                                        (let [elem (.-target %)]
                                                          (.-value
                                                           (aget (.-options elem)
                                                                 (.-selectedIndex elem))))})}
                         {:id :state
                          :required? true
                          :options us-states}))
     (textfield "Zip"
                (merge (utils/update-text data events/control-checkout-change [:billing-address :zip])
                       {:id :zipcode
                        :required? true
                        :value (str (get-in data state/checkout-billing-address-zip-path))}))
     (textfield "Mobile Phone"
                (merge (utils/update-text data events/control-checkout-change [:billing-address :phone])
                       {:id :order_bill_address_attributes_phone
                        :required? true
                        :type "tel"
                        :value (str (get-in data state/checkout-billing-address-phone-path))}))
     (checkbox "Save my address"
               (merge (utils/update-checkbox data
                                             (get-in data state/checkout-billing-address-save-my-address-path)
                                             events/control-checkout-change
                                             [:billing-address :save-my-address])
                      {:id "save_user_address" :class "checkout-save-address"}))]]])

(defn shipping-address-form [data]
  [:div.shipping-address-wrapper
   [:fieldset#shipping.shipping-fieldset
    [:legend {:align "center"} "Shipping Address"]
    (checkbox "Use Billing Address"
              (merge (utils/update-checkbox data
                                            (get-in data state/checkout-shipping-address-use-billing-address-path)
                                            events/control-checkout-change
                                            [:shipping-address :use-billing-address])
                     {:id "use_billing" :class "checkbox checkout-use-billing-address"}))

    [:div.inner {:class (if (get-in data state/checkout-shipping-address-use-billing-address-path) "hidden" "")}
     (textfield "First Name"
                (merge (utils/update-text data events/control-checkout-change [:shipping-address :firstname])
                       {:id :firstname
                        :required? true
                        :value (str (get-in data state/checkout-shipping-address-firstname-path))}))
     (textfield "Last Name"
                (merge (utils/update-text data events/control-checkout-change [:shipping-address :lastname])
                       {:id :lastname
                        :required? true
                        :value (str (get-in data state/checkout-shipping-address-lastname-path))}))
     (textfield "Street Address"
                (merge (utils/update-text data events/control-checkout-change [:shipping-address :address1])
                       {:id :address1
                        :required? true
                        :value (str (get-in data state/checkout-shipping-address-address1-path))}))
     (textfield "Street Address (cont'd)"
                (merge (utils/update-text data events/control-checkout-change [:shipping-address :address2])
                       {:id :address2
                        :value (str (get-in data state/checkout-shipping-address-address2-path))}))
     (textfield "City"
                (merge (utils/update-text data events/control-checkout-change [:shipping-address :city])
                       {:id :city
                        :required? true
                        :value (str (get-in data state/checkout-shipping-address-city-path))}))
     (selectfield "State"
                  (merge {:on-change #(utils/put-event data
                                                       events/control-checkout-change
                                                       {[:shipping-address :state]
                                                        (let [elem (.-target %)]
                                                          (.-value
                                                           (aget (.-options elem)
                                                                 (.-selectedIndex elem))))})}
                         {:id :state
                          :required? true
                          :options us-states}))
     (textfield "Zip"
                (merge (utils/update-text data events/control-checkout-change [:shipping-address :zip])
                       {:id :zipcode
                        :required? true
                        :value (str (get-in data state/checkout-shipping-address-zip-path))}))
     (textfield "Mobile Phone"
                (merge (utils/update-text data events/control-checkout-change [:shipping-address :phone])
                       {:id :order_bill_address_attributes_phone
                        :required? true
                                :type "tel"
                                :value (str (get-in data state/checkout-shipping-address-phone-path))}))]]])

(defn checkout-address-component [data owner]
  (om/component
   (html
    (let [checkout-current-step (get-in data state/checkout-current-step-path)]
      [:div#checkout
       [:div.row
        [:div.columns.thirteen.omega (checkout-progress data checkout-current-step)]]
       [:div.row
        [:div.checkout-form-wrapper
         [:form.edit_order
          {:id (str "checkout_form_" checkout-current-step)
           :action "#TODO:"
           :method "POST"}

          (billing-address-form data)

          (shipping-address-form data)

          [:div.form-buttons.checkout.save-and-continue
           [:input.continue.button.primary {:type "submit" :name "Commit" :value "Save and Continue"}]]]]]]))))
