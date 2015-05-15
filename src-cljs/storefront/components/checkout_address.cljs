(ns storefront.components.checkout-address
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.state :as state]
            [storefront.events :as events]
            [clojure.string :as string]))

(def states
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
        [38 "Wyoming"]]))

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

(defn textfield [name & [{:keys [id value required? type] :or {type "text"}}]]
  [:p.field
   [:label {:for id} name (when required? [:span.required "*"])]
   [:input {:id id
            :name id
            :type type
            :class (if required? "required" "")
            :value value}]])

(defn selectfield [name & [:keys [id value options required?]]]
  [:p.field
   [:label {:for id} name (when required? [:span.required "*"])]
   [:span
    [:select {:class (if required? "required" "")
              :name id
              :id id}
     [:option {:value ""} ""]
     (map (fn [{:keys [value name]}]
            [:option {:value value} (str name)])
          options)]]])

(defn checkout-address-component [data owner]
  (om/component
   (html
    (let [checkout-current-step (get-in data state/checkout-current-step-path)]
      [:div#checkout
       [:div.row
        [:div.columns.thirteen.omega (checkout-progress data checkout-current-step)]]
       [:div.row
        [:div.checkout-form-wrapper
         [:form
          {:id (str "checkout_form_" checkout-current-step)
           :action "#TODO:"
           :method "POST"}

          [:div.billing-address-wrapper
           [:fieldset#billing.billing-fieldset
            [:legend {:align "center"} "Billing Address"]
            [:p.checkout-address-warning
             "Please note: If your billing address does not match your credit card your order will be delayed."]

            [:div.inner
             (textfield "First Name" {:id :firstname :value "TODO: value" :required? true})
             (textfield "Last Name" {:id :lastname :value "TODO: value" :required? true})
             (textfield "Street Address" {:id :address1 :value "TODO: value" :required? true})
             (textfield "Street Address (cont'd)" {:id :address2 :value "TODO: value"})
             (textfield "City" {:id :city :value "TODO: value" :required? true})
             (selectfield "State" {:id :state :value "TODO: value" :required? true :options states})
             (textfield "Zip Code" {:id :zipcode :value "TODO: value" :required? true :type "tel"})
             ]
            ]]

          [:div.shipping-address-wrapper
           [:fieldset#shipping.shipping-fieldset
            ]]
          ]]]]))))
