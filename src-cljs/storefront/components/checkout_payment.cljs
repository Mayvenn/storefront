(ns storefront.components.checkout-payment
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.accessors.credit-cards :as cc]
            [storefront.accessors.orders :as orders]
            [storefront.components.checkout-steps :as checkout-steps]
            [storefront.components.money-formatters :refer [as-money]]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]))

(defn credit-card-form-component
  [{{:keys [focused
            guest?
            name
            number
            expiration
            ccv
            save-credit-card?
            selected-saved-card-id
            saved-cards
            fetching-saved-cards?]} :credit-card
    field-errors                    :field-errors}
   owner]
  (om/component
   (html
    [:div
     [:div.h3.my2 "Payment Information"]
     (if fetching-saved-cards?
       [:div.img-large-spinner.bg-center.bg-contain.bg-no-repeat.col-12
        {:style     {:height "4rem"}
         :data-test "spinner"}]
       [:div
        (when (seq saved-cards)
          (let [card-options (conj (mapv (juxt cc/display-credit-card :id) saved-cards)
                                   ["Add a new payment card" "add-new-card"])]
            (ui/select-field {:data-test "payment-form-selected-saved-card"
                              :id        "selected-saved-card"
                              :keypath   keypaths/checkout-credit-card-selected-id
                              :focused   focused
                              :label     "Payment Card"
                              :options   card-options
                              :required  true
                              :value     selected-saved-card-id})))

        (when (or (empty? saved-cards) (= selected-saved-card-id "add-new-card"))
          [:div
           (ui/text-field {:errors    (get field-errors ["cardholder-name"])
                           :data-test "payment-form-name"
                           :keypath   keypaths/checkout-credit-card-name
                           :focused   focused
                           :label     "Cardholder's Name"
                           :name      "name"
                           :required  true
                           :value     name})
           (ui/text-field {:errors        (get field-errors ["card-number"])
                           :auto-complete "off"
                           :class         "cardNumber"
                           :data-test     "payment-form-number"
                           :keypath       keypaths/checkout-credit-card-number
                           :focused       focused
                           :label         "Card Number"
                           :max-length    19
                           :required      true
                           :type          "tel"
                           :value         (cc/format-cc-number number)})
           [:div.col-12
            (ui/text-field-group
             {:errors        (get field-errors ["card-expiration"])
              :label         "Exp. (MM/YY)"
              :keypath       keypaths/checkout-credit-card-expiration
              :focused       focused
              :value         (cc/format-expiration expiration)
              :max-length    9
              :data-test     "payment-form-expiry"
              :auto-complete "off"
              :class         "cardExpiry"
              :type          "tel"
              :required      true}
             {:errors        (get field-errors ["security-code"])
              :label         "Security Code"
              :keypath       keypaths/checkout-credit-card-ccv
              :focused       focused
              :value         ccv
              :max-length    4
              :auto-complete "off"
              :data-test     "payment-form-code"
              :class         "cardCode"
              :type          "tel"
              :required      true})]
           (when (and (not guest?) (empty? saved-cards))
             [:div.mb2
              [:label.dark-gray
               [:input.mr1 (merge (utils/toggle-checkbox keypaths/checkout-credit-card-save save-credit-card?)
                                  {:type      "checkbox"
                                   :data-test "payment-form-save-credit-card"})]
               "Save my card for easier checkouts."]])])])])))

(defn credit-card-form-query [data]
  (let [saved-cards (get-in data keypaths/checkout-credit-card-existing-cards)]
    {:credit-card {:guest?                 (get-in data keypaths/checkout-as-guest)
                   :name                   (get-in data keypaths/checkout-credit-card-name)
                   :number                 (get-in data keypaths/checkout-credit-card-number)
                   :expiration             (get-in data keypaths/checkout-credit-card-expiration)
                   :ccv                    (get-in data keypaths/checkout-credit-card-ccv)
                   :save-credit-card?      (get-in data keypaths/checkout-credit-card-save)
                   :selected-saved-card-id (get-in data keypaths/checkout-credit-card-selected-id)
                   :saved-cards            saved-cards
                   :fetching-saved-cards?  (and (utils/requesting? data request-keys/get-saved-cards)
                                                (empty? saved-cards))
                   :focused                (get-in data keypaths/ui-focus)}}))

(defn component
  [{:keys [step-bar
           saving?
           disabled?
           loaded-stripe?
           store-credit
           field-errors
           credit-card]}
   owner]
  (om/component
   (html
    [:div.container.p2
     (om/build checkout-steps/component step-bar)

     (ui/narrow-container
      (let [{:keys [credit-available credit-applicable fully-covered?]} store-credit]
        [:div.p2
         [:form
          {:on-submit (utils/send-event-callback events/control-checkout-payment-method-submit)
           :data-test "payment-form"}

          (when (pos? credit-available)
            (ui/note-box
             {:color "teal"
              :data-test "store-credit-note"}
             [:.p2.navy
              [:div [:span.medium (as-money credit-applicable)] " in store credit will be applied to this order."]
              (when-not fully-covered?
                [:.h6.mt1
                 "Please enter an additional payment method below for the remaining total on your order."])]))

          (when-not fully-covered?
            [:div
             (om/build credit-card-form-component {:credit-card credit-card
                                                   :field-errors field-errors})
             [:div.h5
              "You can review your order on the next page before we charge your card."]])

          (when loaded-stripe?
            [:div.my2.col-6-on-tb-dt.mx-auto
             (ui/submit-button "Go to Review Order" {:spinning? saving?
                                                     :disabled? disabled?
                                                     :data-test "payment-form-submit"})])]]))])))

(defn ^:private saving-card? [data]
  (or (utils/requesting? data request-keys/stripe-create-token)
      (utils/requesting? data request-keys/update-cart-payments)))

(defn query [data]
  (let [available-store-credit (get-in data keypaths/user-total-available-store-credit)
        credit-to-use          (min available-store-credit (get-in data keypaths/order-total))
        fully-covered?         (orders/fully-covered-by-store-credit?
                                (get-in data keypaths/order)
                                (get-in data keypaths/user))]
    (merge
     {:store-credit   {:credit-available  available-store-credit
                       :credit-applicable credit-to-use
                       :fully-covered?    fully-covered?}
      :saving?        (saving-card? data)
      :disabled?      (and (utils/requesting? data request-keys/get-saved-cards)
                           (empty? (get-in data keypaths/checkout-credit-card-existing-cards))
                           (not fully-covered?))
      :loaded-stripe? (get-in data keypaths/loaded-stripe)
      :step-bar       (checkout-steps/query data)
      :field-errors   (:field-errors (get-in data keypaths/errors))}
     (credit-card-form-query data))))

(defn built-component [data opts]
  (om/build component (query data) opts))
