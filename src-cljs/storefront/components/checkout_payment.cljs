(ns storefront.components.checkout-payment
  (:require api.orders
            [storefront.accessors.orders :as orders]
            [storefront.accessors.experiments :as experiments]
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
            [storefront.components.svg :as svg]))

(defmethod effects/perform-effects events/control-checkout-choose-payment-method-submit [_ event _ _ app-state]
  (handle-message events/flash-dismiss)
  (let [{waiter-order :waiter/order} (api.orders/current app-state)
        discounted-service?          (-> (api.orders/free-mayvenn-service nil waiter-order) ;; Stylist is optional and not needed in this context
                                         :free-mayvenn-service/discounted?)
        user                         (get-in app-state keypaths/user)
        not-covered-by-store-credit? (not (orders/fully-covered-by-store-credit? waiter-order user))
        selected-payment-methods     (get-in app-state keypaths/checkout-selected-payment-methods)
        not-quadpay-selected?        (-> selected-payment-methods :quadpay nil?)
        user-is-stylist?             (get-in app-state keypaths/user-store-id)
        must-use-card?               (or (and not-covered-by-store-credit?
                                              not-quadpay-selected?)
                                         (and user-is-stylist?
                                              discounted-service?
                                              not-quadpay-selected?))
        selected-saved-card-id       (when must-use-card?
                                       (get-in app-state keypaths/checkout-credit-card-selected-id))
        needs-stripe-token?          (and (or
                                           (nil? selected-saved-card-id)
                                           (= "add-new-card" selected-saved-card-id))
                                          must-use-card?)]
    (if needs-stripe-token?
      (create-stripe-token app-state {:place-order? false})
      (api/update-cart-payments
       (get-in app-state keypaths/session-id)
       {:order    (cond-> (merge
                           (select-keys waiter-order [:token :number])
                           {:cart-payments selected-payment-methods})
                    selected-saved-card-id
                    (assoc-in [:cart-payments :stripe] {:source         selected-saved-card-id
                                                        :idempotent-key (str (random-uuid))}))
        :navigate events/navigate-checkout-confirmation}))))

(defmethod transitions/transition-state events/control-checkout-payment-select
  [_ _ {:keys [payment-method]} app-state]
  (assoc-in app-state keypaths/checkout-selected-payment-methods
            (condp = payment-method
              :stripe  (orders/form-payment-methods (get-in app-state keypaths/order)
                                                    (get-in app-state keypaths/user))
              :quadpay {:quadpay {}})))

(defn store-credit-note
  [{:credit-note/keys [id color content subcontent]}]
  (when id
    (ui/note-box
     {:color     color
      :data-test id}
     [:div.p2
      [:div.proxima.content-3
       content]
      subcontent])))

(defn credit-card-entry
  [{:credit-card-entry/keys [id credit-card field-errors]
    :as data}]
  (when id
    [:div.p2 (store-credit-note data)
     [:div (component/build cc/component
                            {:credit-card  credit-card
                             :field-errors field-errors})
      [:div.mt2.h5 "You can review your order on the next page before we charge your card."]]]))

(def credit-card-label
  [:div.overflow-hidden
   [:div "Pay with Credit/Debit Card"]
   [:p.h6 "All transactions are secure and encrypted."]])

(defn payment-method-selection
  [{:payment-method/keys
    [show-quadpay-component?
     selected-stripe-or-store-credit?
     selected-quadpay?]
    :as                  data}]
  [:div
   (ui/radio-section
    (merge {:name         "payment-method"
            :id           "payment-method-credit-card"
            :data-test    "payment-method"
            :data-test-id "credit-card"
            :on-click     (utils/send-event-callback events/control-checkout-payment-select {:payment-method :stripe})}
           (when selected-stripe-or-store-credit?
             {:checked "checked"}))
    credit-card-label)


   (when selected-stripe-or-store-credit?
     [:div.ml5
      (credit-card-entry data)])

   (list
    (ui/radio-section
     (merge {:name         "payment-method"
             :id           "payment-method-quadpay"
             :data-test    "payment-method"
             :data-test-id "quadpay"
             :on-click     (utils/send-event-callback events/control-checkout-payment-select
                                                      {:payment-method :quadpay})}
            (when selected-quadpay?
              {:checked "checked"}))

     [:div.overflow-hidden
      [:div.flex
       [:div.mr1 "Pay with "]
       [:div ^:inline (svg/quadpay-logo)]]
      [:div.h6 "4 interest-free payments with Zip. "
       [:a.quadpay.block {:href     "#"
                          :on-click (fn [e]
                                      (.preventDefault e)
                                      (quadpay/show-modal))}
        "Learn more."]
       (when show-quadpay-component?
         [:div.hide (component/build quadpay/widget-component {} nil)])]])

    (when selected-quadpay?
      [:div.h6.px2.ml5
       "Before completing your purchase, you will be redirected to Zip to securely set up your payment plan."]))])

(defn cta-submit [{:cta/keys [id saving? disabled? label]}]
  (when id
    [:div.my4.col-6-on-tb-dt.mx-auto
     (ui/submit-button label {:spinning? saving?
                              :disabled? disabled?
                              :data-test id})]) )

(defcomponent component
  [{:keys              [step-bar]
    :store-credit/keys [fully-covered? can-use-store-credit?]
    :as                data} _ _]
  [:div.container.p2
   (component/build checkout-steps/component step-bar)

   (ui/narrow-container
    [:div.m2
     [:h3.my2 "Payment Information"]
     [:form
      {:on-submit (utils/send-event-callback events/control-checkout-choose-payment-method-submit)
       :data-test "payment-form"}

      (if (and fully-covered? can-use-store-credit?)
        (store-credit-note data)
        (payment-method-selection data))

      (cta-submit data)]])])

(defn query [data]
  (let [available-store-credit   (get-in data keypaths/user-total-available-store-credit)
        credit-to-use            (min available-store-credit (get-in data keypaths/order-total))
        order                    (get-in data keypaths/order)
        fully-covered?           (orders/fully-covered-by-store-credit?
                                  order
                                  (get-in data keypaths/user))
        selected-payment-methods (set (keys (get-in data keypaths/checkout-selected-payment-methods)))
        user                     (get-in data keypaths/user)
        can-use-store-credit?    (orders/can-use-store-credit? order user)
        loaded-stripe?           (get-in data keypaths/loaded-stripe)
        selected-quadpay?        (some #{:quadpay} selected-payment-methods)]
    (merge
     (cond-> {:store-credit/fully-covered?        fully-covered?
              :store-credit/can-use-store-credit? can-use-store-credit?}



       (pos? available-store-credit)
       (merge {:credit-note/id "store-credit-note"})

       (not can-use-store-credit?)
       (merge {:credit-note/color   "warning-yellow"
               :credit-note/content (str
                                     "Your "
                                     (as-money credit-to-use)
                                     " in store credit cannot be used with Mayvenn Service orders."
                                     " To use store credit, please remove any Mayvenn Services from your bag.")})

       can-use-store-credit?
       (merge
        {:credit-note/color   "s-color"
         :credit-note/content (str
                               (as-money credit-to-use)
                               " in store credit will be applied to this order."
                               (when-not fully-covered?
                                 " Please enter an additional payment method below for the remaining total on your order."))}))

     {:credit-card-entry/credit-card  (:credit-card (cc/query data))
      :credit-card-entry/id           (when loaded-stripe?
                                        "credit-card-entry")
      :credit-card-entry/field-errors (:field-errors (get-in data keypaths/errors))}

     {:payment-method/show-quadpay-component?          (get-in data keypaths/loaded-quadpay)
      :payment-method/selected-stripe-or-store-credit? (some #{:stripe :store-credit} selected-payment-methods)
      :payment-method/selected-quadpay?                selected-quadpay?}

     {:cta/disabled? (or (and (utils/requesting? data request-keys/get-saved-cards)
                              ;; Requesting cards, no existing cards, or not fully covered
                              (empty? (get-in data keypaths/checkout-credit-card-existing-cards))
                              (not fully-covered?))
                         (empty? selected-payment-methods))
      :cta/spinning? (cc/saving-card? data)
      :cta/label     "Review Order"
      :cta/id        (when loaded-stripe?
                       "payment-form-submit")}
     {:step-bar       (checkout-steps/query data)
      :loaded-stripe? loaded-stripe?})))

(defn ^:private built-non-auth-component [data opts]
  (component/build component (query data) opts))

(defn ^:export built-component [data opts]
  (checkout-returning-or-guest/requires-sign-in-or-initiated-guest-checkout
   built-non-auth-component
   data
   opts))
