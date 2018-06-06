(ns storefront.components.checkout-payment
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.component :as component]
            [storefront.components.checkout-credit-card :as cc]
            [storefront.components.checkout-steps :as checkout-steps]
            [storefront.components.money-formatters :refer [as-money]]
            [storefront.components.promotion-banner :as promotion-banner]
            [storefront.effects :as effects]
            [storefront.components.ui :as ui]
            [storefront.transitions :as transitions]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :refer [handle-message handle-later]]
            [storefront.api :as api]
            [storefront.frontend-effects :refer [create-stripe-token]]
            [storefront.request-keys :as request-keys]
            [storefront.components.affirm :as affirm]
            [clojure.set :as set]
            [storefront.components.svg :as svg]))

(defmethod transitions/transition-state events/control-checkout-payment-select
  [_ _ {:keys [payment-method]} app-state]
  (assoc-in app-state keypaths/checkout-selected-payment-methods
            (condp = payment-method
              :stripe (orders/form-payment-methods (get-in app-state keypaths/order-total)
                                                   (get-in app-state keypaths/user-total-available-store-credit))
              :affirm {:affirm {}})))

(defmethod effects/perform-effects events/control-checkout-choose-payment-method-submit [_ event _ _ app-state]
  (handle-message events/flash-dismiss)
  (let [covered-by-store-credit (orders/fully-covered-by-store-credit?
                                 (get-in app-state keypaths/order)
                                 (get-in app-state keypaths/user))
        selected-payment-methods (get-in app-state keypaths/checkout-selected-payment-methods)
        selected-saved-card-id (when (and (not covered-by-store-credit)
                                          (not (contains? selected-payment-methods :affirm)))
                                 (get-in app-state keypaths/checkout-credit-card-selected-id))
        needs-stripe-token? (and (contains? #{"add-new-card" nil} selected-saved-card-id)
                                 (not covered-by-store-credit)
                                 (not (contains? selected-payment-methods :affirm)))]
    (if needs-stripe-token?
      (create-stripe-token app-state {:place-order? false})
      (api/update-cart-payments
       (get-in app-state keypaths/session-id)
       {:order (cond-> app-state
                 :always (get-in keypaths/order)
                 :always (select-keys [:token :number])
                 :always (merge {:cart-payments selected-payment-methods})

                 selected-saved-card-id (assoc-in [:cart-payments :stripe :source] selected-saved-card-id))
        :navigate events/navigate-checkout-confirmation}))))

(defn component
  [{:keys [step-bar
           saving?
           disabled?
           loaded-stripe?
           store-credit
           field-errors
           credit-card
           promo-code
           selected-payment-methods
           promotion-banner]}
   owner]
  (om/component
   (html
    [:div.container.p2
     (component/build promotion-banner/sticky-component promotion-banner nil)
     (om/build checkout-steps/component step-bar)

     (ui/narrow-container
      [:div.m2
       [:h3.my2 "Payment Information"]
       [:form
        {:on-submit (utils/send-event-callback events/control-checkout-choose-payment-method-submit)
         :data-test "payment-form"}

        (let [{:keys [credit-applicable fully-covered?]} store-credit
              selected-stripe-or-store-credit? (and (seq selected-payment-methods)
                                                    (set/subset? selected-payment-methods #{:stripe :store-credit}))
              selected-affirm? (contains? selected-payment-methods :affirm)]
          (if fully-covered?
            (ui/note-box
             {:color     "teal"
              :data-test "store-credit-note"}
             [:.p2.navy
              [:div [:span.medium (as-money credit-applicable)] " in store credit will be applied to this order."]])

            [:div
             (ui/radio-section
              (merge {:name         "payment-method"
                      :id           "payment-method-credit-card"
                      :data-test    "payment-method"
                      :data-test-id "credit-card"
                      :on-click     (utils/send-event-callback events/control-checkout-payment-select {:payment-method :stripe})}
                     (when selected-stripe-or-store-credit? {:checked "checked"}))
              [:div.overflow-hidden
               [:div "Pay with Credit/Debit Card"]
               [:p.h6 "All transactions are secure and encrypted."]])

             (when selected-stripe-or-store-credit?
               (let [{:keys [credit-available credit-applicable]} store-credit]
                 [:div.p2
                  (when (pos? credit-available)
                    (ui/note-box
                     {:color     "teal"
                      :data-test "store-credit-note"}
                     [:.p2.navy
                      [:div [:span.medium (as-money credit-applicable)] " in store credit will be applied to this order."]
                      [:.h6.mt1
                       "Please enter an additional payment method below for the remaining total on your order."]]))

                  [:div
                   (om/build cc/component
                             {:credit-card  credit-card
                              :field-errors field-errors})
                   [:div.h5
                    "You can review your order on the next page before we charge your card."]]]))
             (ui/radio-section
              (merge {:name         "payment-method"
                      :id           "payment-method-affirm"
                      :data-test    "payment-method"
                      :data-test-id "affirm"
                      :on-click     (utils/send-event-callback events/control-checkout-payment-select {:payment-method :affirm})}
                     (when selected-affirm? {:checked "checked"}))
              [:div.overflow-hidden
               [:div "Pay with " (svg/affirm {:alt "Affirm"})]
               ;; NOTE(jeff): We will add promo messaging in the future.
               [:p.h6 (str "Make easy monthly payments over 3, 6, or 12 months. "
                           #_"Promo codes are excluded when you pay with Affirm. ")
                (om/build affirm/modal-component {})
                #_(when promo-code
                  [:p.h6.ml2.dark-gray "* " [:span.shout promo-code] " promo code excluded with Affirm"])]])

             (when selected-affirm?
               [:div.h6.px2.ml4.dark-gray
                "Before completing your purchase, you will be redirected to Affirm to securely set up your payment plan."])]))

        (when loaded-stripe?
          [:div.my4.col-6-on-tb-dt.mx-auto
           (ui/submit-button "Review Order" {:spinning? saving?
                                             :disabled? disabled?
                                             :data-test "payment-form-submit"})])]])])))

(defn query [data]
  (let [available-store-credit   (get-in data keypaths/user-total-available-store-credit)
        credit-to-use            (min available-store-credit (get-in data keypaths/order-total))
        fully-covered?           (orders/fully-covered-by-store-credit?
                                  (get-in data keypaths/order)
                                  (get-in data keypaths/user))
        selected-payment-methods (set (keys (get-in data keypaths/checkout-selected-payment-methods)))]
    (merge
     {:store-credit             {:credit-available  available-store-credit
                                 :credit-applicable credit-to-use
                                 :fully-covered?    fully-covered?}
      :promotion-banner         (promotion-banner/query data)
      :promo-code               (first (get-in data keypaths/order-promotion-codes))
      :saving?                  (cc/saving-card? data)
      :disabled?                (or (and (utils/requesting? data request-keys/get-saved-cards)
                                         ;; Requesting cards, no existing cards, or not fully covered
                                         (empty? (get-in data keypaths/checkout-credit-card-existing-cards))
                                         (not fully-covered?))
                                    (empty? selected-payment-methods))
      :loaded-stripe?           (and (get-in data keypaths/loaded-stripe-v2)
                                     (get-in data keypaths/loaded-stripe-v3))
      :step-bar                 (checkout-steps/query data)
      :field-errors             (:field-errors (get-in data keypaths/errors))
      :selected-payment-methods selected-payment-methods}
     (cc/query data))))

(defn built-component [data opts]
  (om/build component (query data) opts))
