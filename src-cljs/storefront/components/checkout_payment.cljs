(ns storefront.components.checkout-payment
  (:require [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.checkout-credit-card :as cc]
            [storefront.components.checkout-returning-or-guest :as checkout-returning-or-guest]
            [storefront.components.checkout-steps :as checkout-steps]
            [storefront.components.money-formatters :refer [as-money]]
            [ui.promo-banner :as promo-banner]
            [storefront.effects :as effects]
            [storefront.hooks.quadpay :as quadpay]
            [storefront.components.ui :as ui]
            [storefront.transitions :as transitions]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :refer [handle-message handle-later]]
            [storefront.api :as api]
            [storefront.frontend-effects :refer [create-stripe-token]]
            [storefront.request-keys :as request-keys]
            [clojure.set :as set]
            [storefront.components.svg :as svg]))

(defmethod effects/perform-effects events/control-checkout-choose-payment-method-submit [_ event _ _ app-state]
  (handle-message events/flash-dismiss)
  (let [order                                (get-in app-state keypaths/order)
        user                                 (get-in app-state keypaths/user)
        not-covered-by-store-credit?         (not (orders/fully-covered-by-store-credit? order user))
        selected-payment-methods             (get-in app-state keypaths/checkout-selected-payment-methods)
        not-quadpay-selected?                (-> selected-payment-methods :quadpay nil?)
        service-line-item-promotion-applied? (orders/service-line-item-promotion-applied? order)
        user-is-stylist?                     (get-in app-state keypaths/user-store-id)
        must-use-card?                       (or (and not-covered-by-store-credit?
                                                      not-quadpay-selected?)
                                                 (and user-is-stylist?
                                                      service-line-item-promotion-applied?
                                                      not-quadpay-selected?))
        selected-saved-card-id               (when must-use-card? (get-in app-state keypaths/checkout-credit-card-selected-id))
        needs-stripe-token?                  (and (or (nil? selected-saved-card-id) (= "add-new-card" selected-saved-card-id))
                                                  must-use-card?)]
    (if needs-stripe-token?
      (create-stripe-token app-state {:place-order? false})
      (api/update-cart-payments
       (get-in app-state keypaths/session-id)
       {:order    (cond-> order
                    :always                (select-keys [:token :number])
                    :always                (merge {:cart-payments selected-payment-methods})
                    selected-saved-card-id (assoc-in [:cart-payments :stripe :source] selected-saved-card-id))
        :navigate events/navigate-checkout-confirmation}))))

(defmethod transitions/transition-state events/control-checkout-payment-select
  [_ _ {:keys [payment-method]} app-state]
  (assoc-in app-state keypaths/checkout-selected-payment-methods
            (condp = payment-method
              :stripe  (orders/form-payment-methods (get-in app-state keypaths/order)
                                                    (get-in app-state keypaths/user))
              :quadpay {:quadpay {}})))

(defcomponent component
  [{:keys [step-bar
           saving?
           disabled?
           loaded-quadpay?
           loaded-stripe?
           store-credit
           field-errors
           credit-card
           promo-code
           selected-payment-methods
           can-use-store-credit?
           freeinstall-applied?
           promo-banner]}
   owner _]
  [:div.container.p2
   (component/build promo-banner/sticky-organism promo-banner nil)
   (component/build checkout-steps/component step-bar)

   (ui/narrow-container
    [:div.m2
     [:h3.my2 "Payment Information"]
     [:form
      {:on-submit (utils/send-event-callback events/control-checkout-choose-payment-method-submit)
       :data-test "payment-form"}

      (let [{:keys [credit-applicable fully-covered?]} store-credit
            selected-stripe-or-store-credit?           (and (seq selected-payment-methods)
                                                            (set/subset? selected-payment-methods #{:stripe :store-credit}))
            selected-quadpay?                          (contains? selected-payment-methods :quadpay)]
        (if (and fully-covered? can-use-store-credit?)
          (ui/note-box
           {:color     "p-color"
            :data-test "store-credit-note"}
           [:.p2
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
               [:div.p2.ml5
                (when (pos? credit-available)
                  (if can-use-store-credit?
                    (ui/note-box
                     {:color     "p-color"
                      :data-test "store-credit-note"}
                     [:.p2
                      [:div [:span.medium (as-money credit-applicable)] " in store credit will be applied to this order."]
                      [:.h6.mt1
                       "Please enter an additional payment method below for the remaining total on your order."]])
                    (ui/note-box
                     {:color     "s-color"
                      :data-test "store-credit-note"}
                     [:div.p2.black
                      [:div "Your "
                       [:span.medium (as-money credit-applicable)]
                       " in store credit "
                       [:span.medium "cannot"]
                       " be used with " [:span.shout freeinstall-applied?] " orders."]
                      [:div.h6.mt1
                       "To use store credit, please remove promo code " [:span.shout freeinstall-applied?] " from your bag."]])))

                [:div
                 (component/build cc/component
                                  {:credit-card  credit-card
                                   :field-errors field-errors})
                 [:div.h5
                  "You can review your order on the next page before we charge your card."]]]))
           (ui/radio-section
            (merge {:name         "payment-method"
                    :id           "payment-method-quadpay"
                    :data-test    "payment-method"
                    :data-test-id "quadpay"
                    :on-click     (utils/send-event-callback events/control-checkout-payment-select
                                                             {:payment-method :quadpay})}
                   (when selected-quadpay? {:checked "checked"}))

            [:div.overflow-hidden
             [:div.flex
              [:div.mr1 "Pay with "]
              [:div.mt1 {:style {:width "85px" :height "17px"}}
               ^:inline (svg/quadpay-logo)]]
             [:div.h6 "4 interest-free payments with QuadPay. "
              [:a.blue.block {:href "#"
                              :on-click (fn [e]
                                          (.preventDefault e)
                                          (quadpay/show-modal))}
               "Learn more."]
              (when loaded-quadpay?
                [:div.hide (component/build quadpay/widget-component {} nil)])]])

           (when selected-quadpay?
             [:div.h6.px2.ml5
              "Before completing your purchase, you will be redirected to Quadpay to securely set up your payment plan."])]))

      (when loaded-stripe?
        [:div.my4.col-6-on-tb-dt.mx-auto
         (ui/submit-button "Review Order" {:spinning? saving?
                                           :disabled? disabled?
                                           :data-test "payment-form-submit"})])]])])

(defn query [data]
  (let [available-store-credit   (get-in data keypaths/user-total-available-store-credit)
        credit-to-use            (min available-store-credit (get-in data keypaths/order-total))
        order                    (get-in data keypaths/order)
        fully-covered?           (orders/fully-covered-by-store-credit?
                                  order
                                  (get-in data keypaths/user))
        selected-payment-methods (set (keys (get-in data keypaths/checkout-selected-payment-methods)))
        freeinstall-applied?     (orders/service-line-item-promotion-applied? order)
        user                     (get-in data keypaths/user)]
    (merge
     {:store-credit          {:credit-available  available-store-credit
                              :credit-applicable credit-to-use
                              :fully-covered?    fully-covered?}
      :promo-banner          (promo-banner/query data)
      :promo-code            (first (get-in data keypaths/order-promotion-codes))
      :saving?               (cc/saving-card? data)
      :disabled?             (or (and (utils/requesting? data request-keys/get-saved-cards)
                                      ;; Requesting cards, no existing cards, or not fully covered
                                      (empty? (get-in data keypaths/checkout-credit-card-existing-cards))
                                      (not fully-covered?))
                                 (empty? selected-payment-methods))
      :freeinstall-applied?  freeinstall-applied?
      :can-use-store-credit? (orders/can-use-store-credit? order user)

      :loaded-stripe?           (get-in data keypaths/loaded-stripe)
      :step-bar                 (checkout-steps/query data)
      :field-errors             (:field-errors (get-in data keypaths/errors))
      :selected-payment-methods selected-payment-methods
      :loaded-quadpay?          (get-in data keypaths/loaded-quadpay)}
     (cc/query data))))

(defn ^:private built-non-auth-component [data opts]
  (component/build component (query data) opts))

(defn ^:export built-component [data opts]
  (checkout-returning-or-guest/requires-sign-in-or-initiated-guest-checkout
   built-non-auth-component
   data
   opts))
