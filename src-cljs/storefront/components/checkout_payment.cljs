(ns storefront.components.checkout-payment
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.accessors.credit-cards :as cc]
            [storefront.accessors.orders :as orders]
            [storefront.components.checkout-steps :as checkout-steps]
            [storefront.components.money-formatters :refer [as-money]]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]))

(defn credit-card-form-component
  [{{:keys [guest?
            name
            number
            expiration
            ccv
            save-credit-card?
            selected-saved-card-id
            saved-cards
            fetching-saved-cards?]} :credit-card}
   owner]
  (om/component
   (html
    [:div
     [:div.h2.my2 "Payment Information"]
     (if fetching-saved-cards?
       [:div.img-large-spinner.bg-center.bg-contain.bg-no-repeat.col-12
        {:style {:height "4rem"}
         :data-test "spinner"}]
       [:div
        (when (seq saved-cards)
          (ui/select-field "Payment Card"
                           keypaths/checkout-credit-card-selected-id
                           selected-saved-card-id
                           (conj (mapv (juxt cc/display-credit-card :id) saved-cards)
                                 ["Add a new payment card" "add-new-card"])
                           {:id        "selected-saved-card"
                            :data-test "payment-form-selected-saved-card"
                            :required  true}))

        (when (or (empty? saved-cards) (= selected-saved-card-id "add-new-card"))
          [:div
           (ui/text-field "Cardholder's Name"
                          keypaths/checkout-credit-card-name
                          name
                          {:name      "name"
                           :data-test "payment-form-name"
                           :required  true})
           (ui/text-field "Card Number"
                          keypaths/checkout-credit-card-number
                          (cc/format-cc-number number)
                          {:max-length    19
                           :data-test     "payment-form-number"
                           :auto-complete "off"
                           :class         "cardNumber rounded"
                           :type          "tel"
                           :required      true})
           [:div.flex.col-12
            [:div.col-6 (ui/text-field "Expiration (MM/YY)"
                                       keypaths/checkout-credit-card-expiration
                                       (cc/format-expiration expiration)
                                       {:max-length    9
                                        :data-test     "payment-form-expiry"
                                        :auto-complete "off"
                                        :class         "cardExpiry rounded-left"
                                        :type          "tel"
                                        :required      true})]
            [:div.col-6 (ui/text-field "Security Code"
                                       keypaths/checkout-credit-card-ccv
                                       ccv
                                       {:max-length    4
                                        :auto-complete "off"
                                        :data-test     "payment-form-code"
                                        :class         "cardCode rounded-right border-width-left-0"
                                        :type          "tel"
                                        :required      true})]]
           (when (and (not guest?) (empty? saved-cards))
             [:div.mb2
              [:label.light-gray
               [:input.mr1 (merge (utils/toggle-checkbox keypaths/checkout-credit-card-save save-credit-card?)
                                  {:type "checkbox"
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
                                                (empty? saved-cards))}}))

(defn component
  [{:keys [step-bar
           saving?
           disabled?
           loaded-stripe?
           store-credit
           credit-card]}
   owner]
  (om/component
   (html
    (ui/narrow-container
     (om/build checkout-steps/component step-bar)

     (let [{:keys [credit-available credit-applicable fully-covered?]} store-credit]
       [:form
        {:on-submit (utils/send-event-callback events/control-checkout-payment-method-submit)
         :data-test "payment-form"}

        (when (pos? credit-available)
          (ui/note-box
           {:color "green"
            :data-test "store-credit-note"}
           [:.p2.navy
            [:.h4 [:span.medium (as-money credit-applicable)] " in store credit will be applied to this order."]
            (when-not fully-covered?
              [:.h6.mt1.line-height-2
               "Please enter an additional payment method below for the remaining total on your order."])]))

        (when-not fully-covered?
          [:div
           (om/build credit-card-form-component {:credit-card credit-card})
           [:.h4.light-gray
            "You can review your order on the next page before we charge your card."]])

        (when loaded-stripe?
          [:.my2
           (ui/submit-button "Go to Review Order" {:spinning? saving?
                                                   :disabled? disabled?
                                                   :data-test "payment-form-submit"})])])))))

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
      :step-bar       (checkout-steps/query data)}
     (credit-card-form-query data))))

(defn built-component [data opts]
  (om/build component (query data) opts))
