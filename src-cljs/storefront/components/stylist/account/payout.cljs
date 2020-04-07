(ns storefront.components.stylist.account.payout
  (:require [storefront.accessors.credit-cards :as cc]
            [storefront.component :as component :refer [defcomponent defdynamic-component]]
            [storefront.components.ui :as ui]
            [storefront.hooks.spreedly :as spreedly]
            [storefront.accessors.experiments :as experiments]
            [storefront.events :as events]
            [storefront.effects :as effects]
            [storefront.transitions :as transitions]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.request-keys :as request-keys]))

(defmethod transitions/transition-state events/spreedly-did-mount
  [_ _ _ app-state]
  (assoc-in app-state
            keypaths/spreedly-frame nil))

(defmethod transitions/transition-state events/spreedly-did-unmount
  [_ _ _ app-state]
  (assoc-in app-state
            keypaths/spreedly-frame nil))

(defmethod effects/perform-effects events/spreedly-did-mount
  [_ event _ _ app-state]
  (spreedly/create-frame {:number-id "green-dot-card-number"
                          :cvv-id    "green-dot-cvv"}
                         events/spreedly-frame-initialized))

(defmethod transitions/transition-state events/spreedly-frame-initialized
  [_ event {:keys [frame]} app-state]
  (assoc-in app-state keypaths/spreedly-frame frame))

(defdynamic-component credit-card-fields
  (constructor [_ props] {:ready? false})
  (did-mount [_] (handle-message events/spreedly-did-mount))
  (will-unmount [_] (handle-message events/spreedly-did-unmount))
  (render [this]
          (let [{:keys [ready?]} (component/get-props this)]
            (component/html
             [:div.clearfix
              [:div#green-dot-card-number.col.col-8.h4.line-height-1.rounded-left.rounded.border.x-group-item.border-gray.p2.mb2
               {:style {:height "43px"}
                :class (when-not ready? "bg-gray")}]
              [:div#green-dot-cvv.col.col-4.h4.line-height-1.rounded.rounded-right.border.x-group-item.border-gray.p2.mb2
               {:style {:height "43px"}
                :class (when-not ready? "bg-gray")}]]))))

(def green-dot-keypath (partial conj keypaths/stylist-manage-account-green-dot-payout-attributes))

(defn green-dot-query [data]
  (let [green-dot        #(get-in data (green-dot-keypath %))
        card-last-4      (green-dot :last-4)
        card-selected-id (get-in data keypaths/stylist-manage-account-green-dot-card-selected-id)]
    {:first-name       (green-dot :card-first-name)
     :last-name        (green-dot :card-last-name)
     :card-number      (green-dot :card-number)
     :expiration-date  (green-dot :expiration-date)
     :postalcode       (green-dot :postalcode)
     :card-selected-id card-selected-id
     :card-last-4      card-last-4
     :payout-timeframe (green-dot :payout-timeframe)
     :spreedly-ready?  (boolean (get-in data keypaths/spreedly-frame))
     :new-card-entry?  (or (empty? card-last-4)
                           (= "replace-card" card-selected-id))}))

(defcomponent green-dot-component
  [{:keys [green-dot focused field-errors]} owner opts]
  (let [{:keys [first-name last-name card-number card-last-4 expiration-date card-selected-id payout-timeframe postalcode spreedly-ready?]} green-dot]
    [:div
     (when card-last-4
       (let [card-options [[(str "xxxx-xxxx-xxxx-" card-last-4) card-last-4] ["Replace Card" "replace-card"]]]
         (ui/select-field {:data-test "green-dot-saved-card"
                           :id        "green-dot-saved-card"
                           :keypath   keypaths/stylist-manage-account-green-dot-card-selected-id
                           :focused   focused
                           :label     "Payout Card"
                           :options   card-options
                           :required  true
                           :value     card-selected-id})))
     (when (or (empty? card-last-4)
               (= "replace-card" card-selected-id))
       [:div
        (ui/text-field-group {:data-test "green-dot-first-name"
                              :errors    (get field-errors ["payout-method" "card-first-name"])
                              :id        "green-dot-first-name"
                              :keypath   (green-dot-keypath :card-first-name)
                              :focused   focused
                              :label     "Card First Name"
                              :name      "green-dot-first-name"
                              :required  true
                              :value     first-name}
                             {:data-test "green-dot-last-name"
                              :errors    (get field-errors ["payout-method" "card-last-name"])
                              :id        "green-dot-last-name"
                              :keypath   (green-dot-keypath :card-last-name)
                              :focused   focused
                              :label     "Card Last Name"
                              :name      "green-dot-last-name"
                              :required  true
                              :value     last-name})
        (component/build credit-card-fields {:ready? spreedly-ready?} nil)
        (ui/field-error-message (first (get field-errors ["payout-method" "base"])) "payout-method-error")
        (ui/text-field-group
         {:data-test     "green-dot-expiration-date"
          :errors        (get field-errors ["payout-method" "expiration-date"])
          :id            "green-dot-expiration-date"
          :keypath       (green-dot-keypath :expiration-date)
          :focused       focused
          :label         "Expiration Date (MM/YY)"
          :name          "green-dot-expiration-date"
          :required      true
          :auto-complete "off"
          :type          "tel"
          :value         (cc/format-expiration expiration-date)
          :column-size   "5fr"}
         {:data-test     "green-dot-postalcode"
          :errors        (get field-errors ["payout-method" "postalcode"])
          :id            "green-dot-postalcode"
          :keypath       (green-dot-keypath :postalcode)
          :focused       focused
          :label         "Zip Code"
          :name          "green-dot-postalcode"
          :required      true
          :auto-complete "off"
          :max-length    5
          :type          "postalcode"
          :value         postalcode
          :column-size   "3fr"})])
     [:div.mx1
      [:p.h6
       "We accept most bank or debit cards. Your commissions will be sent to this card and ready for use after payout is complete."]
      [:p.h6.mt1.mb3.black.medium
       (case payout-timeframe
         "next_business_day"         "NOTE: Funds paid out to this card will become available the next business day."
         "two_to_five_business_days" "NOTE: Funds paid out to this card will become available two to five business days later."
         nil)]]]))

(defcomponent component [{:keys [focused
                                 spinning?
                                 payout-method
                                 payout-methods
                                 venmo-phone
                                 paypal-email
                                 green-dot
                                 address-1
                                 address-2
                                 zipcode
                                 city
                                 state-id
                                 states
                                 phone
                                 field-errors
                                 disabled?]} owner opts]
  [:form
   {:on-submit
    (utils/send-event-callback events/control-stylist-account-commission-submit)}
   [:div.clearfix
    [:div.col.col-12.col-6-on-tb-dt
     [:h1.h3.light.my3.center.col-12 "Update Payout Info"]
     [:div.col-12.col-10-on-tb-dt.mx-auto
      (ui/select-field {:data-test "payout-method"
                         ;;TODO We should update field-error construction to be kebabed how we like it.
                        :errors    (get field-errors ["chosen-payout-method"])
                        :id        "payout-method"
                        :keypath   (conj keypaths/stylist-manage-account :chosen-payout-method)
                        :focused   focused
                        :label     "Payout Method"
                        :options   payout-methods
                        :required  true
                        :value     payout-method})
      (condp = payout-method
        "venmo"     (ui/text-field {:data-test "venmo-phone"
                                    :errors    (get field-errors ["payout-method" "phone"])
                                    :id        "venmo-phone"
                                    :keypath   (conj keypaths/stylist-manage-account :venmo-payout-attributes :phone)
                                    :focused   focused
                                    :label     "Venmo Phone #"
                                    :name      "venmo-phone"
                                    :required  true
                                    :type      "tel"
                                    :value     venmo-phone})
        "paypal"    (ui/text-field {:data-test "paypal-email"
                                    :errors    (get field-errors ["payout-method" "email"])
                                    :id        "paypal-email"
                                    :keypath   (conj keypaths/stylist-manage-account :paypal-payout-attributes :email)
                                    :focused   focused
                                    :label     "PayPal Email"
                                    :name      "paypal-email"
                                    :required  true
                                    :type      "email"
                                    :value     paypal-email})
        "green_dot" (component/build green-dot-component
                                     {:green-dot    green-dot
                                      :focused      focused
                                      :field-errors field-errors}
                                     opts)
        "missing"   [:div]
        "check"     [:p.ml1.mb3.h6 "Checks will mail to the address entered here"]
        [:div])]

     [:div.col.col-12.pt3.pb4.underline.center
      [:a.p-color (utils/route-to events/navigate-v2-stylist-dashboard-payments) "View your earnings"]]]

    [:div.col.col-12.col-6-on-tb-dt
     [:div.mx-auto.col-12.col-10-on-tb-dt
      [:div.border-gray.border-top.hide-on-tb-dt.mb3]
      (ui/text-field {:data-test "account-address1"
                      :errors    (get field-errors ["address" "address1"])
                      :id        "account-address1"
                      :keypath   (conj keypaths/stylist-manage-account :address :address-1)
                      :focused   focused
                      :label     "Address"
                      :name      "account-address1"
                      :required  true
                      :type      "text"
                      :value     address-1})

      [:div.col-12
       (ui/text-field-group
        {:type      "text"
         :label     "Apt/Suite"
         :keypath   (conj keypaths/stylist-manage-account :address :address-2)
         :focused   focused
         :value     address-2
         :errors    (get field-errors ["address" "address2"])
         :name      "account-address2"
         :data-test "account-address2"
         :id        "account-address2"}

        {:type       "text"
         :label      "Zip Code"
         :keypath    (conj keypaths/stylist-manage-account :address :zipcode)
         :focused    focused
         :value      zipcode
         :errors     (get field-errors ["address" "zipcode"])
         :name       "account-zipcode"
         :id         "account-zipcode"
         :data-test  "account-zipcode"
         :required   true
         :max-length 5
         :min-length 5
         :pattern    "\\d{5}"
         :title      "zip code must be 5 digits"})]

      (ui/text-field {:data-test "account-city"
                      :errors    (get field-errors ["address" "city"])
                      :id        "account-city"
                      :keypath   (conj keypaths/stylist-manage-account :address :city)
                      :focused   focused
                      :label     "City"
                      :name      "account-city"
                      :required  true
                      :type      "text"
                      :value     city})

      (ui/select-field {:data-test   "account-state"
                        :errors      (get field-errors ["address" "state"])
                        :id          :account-state
                        :keypath     (conj keypaths/stylist-manage-account :address :state-id)
                        :focused     focused
                        :label       "State"
                        :options     states
                        :placeholder "State"
                        :required    true
                        :value       state-id})

      (ui/text-field {:data-test "account-phone"
                      :errors    (get field-errors ["address" "phone"])
                      :id        :account-phone
                      :keypath   (conj keypaths/stylist-manage-account :address :phone)
                      :focused   focused
                      :label     "Mobile Phone"
                      :name      "account-phone"
                      :required  true
                      :type      "tel"
                      :value     phone})]]]

   [:div.my2.col-12
    ui/nbsp
    [:div.border-gray.border-top.hide-on-mb.mb3]
    [:div.col-12.col-5-on-tb-dt.mx-auto
     (ui/submit-button "Update" {:spinning? spinning?
                                 :data-test "account-form-submit"})]]])

(defn payout-methods [original-payout-method]
  (cond-> [["PayPal" "paypal"]
           ["Mayvenn InstaPay" "green_dot"]]

    (= original-payout-method "venmo")
    (conj ["Venmo" "venmo"])

    (= original-payout-method "check")
    (conj ["Check" "check"])

    (= original-payout-method "missing")
    (conj ["Select Payout Method" "missing"])))

(defn ^:private ->permitted-method
  [method]
  (let [allowed-methods #{"venmo" "paypal" "green_dot" "check" "missing"}]
    (get allowed-methods method "missing")))

(def ^:private chosen-payout-method
  (conj keypaths/stylist-manage-account :chosen-payout-method))

(defn query
  [data]
  (let [payout-method                           (->permitted-method (get-in data chosen-payout-method))
        instapay?                               (= "green_dot" payout-method)
        spreedly-ready?                         (boolean (get-in data keypaths/spreedly-frame))
        {:keys [new-card-entry?] :as green-dot} (green-dot-query data)]
    {:payout-method  payout-method
     :payout-methods (payout-methods (get-in data (conj keypaths/stylist-manage-account :original-payout-method)))
     :paypal-email   (get-in data (conj keypaths/stylist-manage-account :paypal-payout-attributes :email))
     :venmo-phone    (get-in data (conj keypaths/stylist-manage-account :venmo-payout-attributes :phone))
     :green-dot      green-dot
     :address-1      (get-in data (conj keypaths/stylist-manage-account :address :address-1))
     :address-2      (get-in data (conj keypaths/stylist-manage-account :address :address-2))
     :city           (get-in data (conj keypaths/stylist-manage-account :address :city))
     :zipcode        (get-in data (conj keypaths/stylist-manage-account :address :zipcode))
     :state-id       (get-in data (conj keypaths/stylist-manage-account :address :state-id))
     :phone          (get-in data (conj keypaths/stylist-manage-account :address :phone))
     :states         (map (juxt :name :id) (get-in data keypaths/states))
     :field-errors   (get-in data keypaths/field-errors)
     :focused        (get-in data keypaths/ui-focus)
     :spinning?      (or
                      (utils/requesting? data request-keys/update-stylist-account)
                      (and instapay? new-card-entry? (not spreedly-ready?)))}))
