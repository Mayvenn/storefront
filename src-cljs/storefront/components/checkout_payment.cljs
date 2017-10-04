(ns storefront.components.checkout-payment
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.components.checkout-credit-card :as cc]
            [storefront.components.checkout-steps :as checkout-steps]
            [storefront.components.money-formatters :refer [as-money]]
            [storefront.components.ui :as ui]
            [storefront.transitions :as transitions]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [clojure.set :as set]))

(defmethod transitions/transition-state events/control-checkout-payment-select
  [_ _ {:keys [payment-method]} app-state]
  (assoc-in app-state keypaths/checkout-selected-payment-methods
            (condp = payment-method
              :stripe (orders/form-payment-methods (get-in app-state keypaths/order-total)
                                                   (get-in app-state keypaths/user-total-available-store-credit))
              :affirm {:affirm {}})))

(defn old-component
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
             {:color     "teal"
              :data-test "store-credit-note"}
             [:.p2.navy
              [:div [:span.medium (as-money credit-applicable)] " in store credit will be applied to this order."]
              (when-not fully-covered?
                [:.h6.mt1
                 "Please enter an additional payment method below for the remaining total on your order."])]))

          (when-not fully-covered?
            [:div
             (om/build cc/component
                       {:credit-card  credit-card
                        :field-errors field-errors})
             [:div.h5
              "You can review your order on the next page before we charge your card."]])

          (when loaded-stripe?
            [:div.my2.col-6-on-tb-dt.mx-auto
             (ui/submit-button "Go to Review Order" {:spinning? saving?
                                                     :disabled? disabled?
                                                     :data-test "payment-form-submit"})])]]))])))


(defn component
  [{:keys [step-bar
           saving?
           disabled?
           loaded-stripe?
           store-credit
           field-errors
           credit-card
           selected-payment-methods]}
   owner]
  (om/component
   (html
    [:div.container.p2
     (om/build checkout-steps/component step-bar)

     (ui/narrow-container
      [:div.py1
       [:h3 "Payment Information"]
       [:form
        {:on-submit (utils/send-event-callback events/control-checkout-payment-method-submit)
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
               (let [{:keys [credit-available credit-applicable fully-covered?]} store-credit]
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
               [:div "Pay with " [:img {:alt "Affirm"}]]
               [:p.h6 "Make easy monthly payments over 3, 6, or 12 months. "
                [:a {:href "https://google.com"} "Learn more."]]
               [:p.h6.dark-gray "*Promotion codes excluded with Affirm."]])]))

        (when loaded-stripe?
          [:div.my2.col-6-on-tb-dt.mx-auto
           (ui/submit-button "Go to Review Order" {:spinning? saving?
                                                   :disabled? disabled?
                                                   :data-test "payment-form-submit"})])]])])))

(defn query [data]
  (let [available-store-credit (get-in data keypaths/user-total-available-store-credit)
        credit-to-use          (min available-store-credit (get-in data keypaths/order-total))
        fully-covered?         (orders/fully-covered-by-store-credit?
                                (get-in data keypaths/order)
                                (get-in data keypaths/user))]
    (merge
     {:store-credit             {:credit-available  available-store-credit
                                 :credit-applicable credit-to-use
                                 :fully-covered?    fully-covered?}
      :saving?                  (cc/saving-card? data)
      :disabled?                (and (utils/requesting? data request-keys/get-saved-cards)
                                     (empty? (get-in data keypaths/checkout-credit-card-existing-cards))
                                     (not fully-covered?))
      :loaded-stripe?           (and (get-in data keypaths/loaded-stripe-v2)
                                     (get-in data keypaths/loaded-stripe-v3))
      :step-bar                 (checkout-steps/query data)
      :field-errors             (:field-errors (get-in data keypaths/errors))
      ;; affirm keys
      :selected-payment-methods (set (keys (get-in data keypaths/checkout-selected-payment-methods)))}
     (cc/query data))))

(defn built-component [data opts]
  (om/build (if (experiments/affirm? data) component old-component)
            (query data)
            opts))
