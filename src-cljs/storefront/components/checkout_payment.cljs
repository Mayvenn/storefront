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
            [clojure.set :as set]
            [storefront.components.svg :as svg]))

(defmethod effects/perform-effects events/control-checkout-choose-payment-method-submit [_ event _ _ app-state]
  (handle-message events/flash-dismiss)
  (let [order                    (get-in app-state keypaths/order)
        covered-by-store-credit  (orders/fully-covered-by-store-credit?
                                  order
                                  (get-in app-state keypaths/user))
        selected-payment-methods (get-in app-state keypaths/checkout-selected-payment-methods)
        selected-saved-card-id   (when (or (not covered-by-store-credit)
                                           (orders/applied-install-promotion order))
                                   (get-in app-state keypaths/checkout-credit-card-selected-id))
        needs-stripe-token?      (and (contains? #{"add-new-card" nil} selected-saved-card-id)
                                      (not covered-by-store-credit))]
    (if needs-stripe-token?
      (create-stripe-token app-state {:place-order? false})
      (api/update-cart-payments
       (get-in app-state keypaths/session-id)
       {:order (cond-> order
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
           applied-install-promotion
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

        (let [{:keys [credit-available credit-applicable fully-covered?]} store-credit]
          (if (and fully-covered?
                   (not applied-install-promotion))
            (ui/note-box
             {:color     "teal"
              :data-test "store-credit-note"}
             [:.p2.navy
              [:div [:span.medium (as-money credit-applicable)] " in store credit will be applied to this order."]])

            [:div.p2
             [:div "Pay with Credit/Debit Card"]
             [:p.h6 "All transactions are secure and encrypted."]
             (when (pos? credit-available)
               (if applied-install-promotion
                 (ui/note-box
                  {:color     "orange"
                   :data-test "store-credit-note"}
                  [:div.p2.black
                   [:div "Your "
                    [:span.medium (as-money credit-applicable)]
                    " in store credit "
                    [:span.medium "cannot"]
                    " be used with " [:span.shout applied-install-promotion] " orders."]
                   [:div.h6.mt1
                    "To use store credit, please remove promo code " [:span.shout applied-install-promotion] " from your bag."]])
                 (ui/note-box
                  {:color     "teal"
                   :data-test "store-credit-note"}
                  [:.p2.navy
                   [:div [:span.medium (as-money credit-applicable)] " in store credit will be applied to this order."]
                   [:.h6.mt1
                    "Please enter an additional payment method below for the remaining total on your order."]])))

             [:div
              (om/build cc/component
                        {:credit-card  credit-card
                         :field-errors field-errors})
              [:div.h5
               "You can review your order on the next page before we charge your card."]]]))

        (when loaded-stripe?
          [:div.my4.col-6-on-tb-dt.mx-auto
           (ui/submit-button "Review Order" {:spinning? saving?
                                             :disabled? disabled?
                                             :data-test "payment-form-submit"})])]])])))

(defn query [data]
  (let [available-store-credit   (get-in data keypaths/user-total-available-store-credit)
        credit-to-use            (min available-store-credit (get-in data keypaths/order-total))
        order                    (get-in data keypaths/order)
        fully-covered?           (orders/fully-covered-by-store-credit?
                                  order
                                  (get-in data keypaths/user))
        selected-payment-methods (set (keys (get-in data keypaths/checkout-selected-payment-methods)))]
    (merge
     {:store-credit                     {:credit-available  available-store-credit
                                         :credit-applicable credit-to-use
                                         :fully-covered?    fully-covered?}
      :promotion-banner                 (promotion-banner/query data)
      :promo-code                       (first (get-in data keypaths/order-promotion-codes))
      :saving?                          (cc/saving-card? data)
      :disabled?                        (or (and (utils/requesting? data request-keys/get-saved-cards)
                                                 ;; Requesting cards, no existing cards, or not fully covered
                                                 (empty? (get-in data keypaths/checkout-credit-card-existing-cards))
                                                 (not fully-covered?))
                                            (empty? selected-payment-methods))
      :applied-install-promotion        (->> (orders/all-applied-promo-codes order)
                                             (filter #{"freeinstall" "install"})
                                             first)
      :loaded-stripe?                   (and (get-in data keypaths/loaded-stripe-v2)
                                             (get-in data keypaths/loaded-stripe-v3))
      :step-bar                         (checkout-steps/query data)
      :field-errors                     (:field-errors (get-in data keypaths/errors))
      :selected-payment-methods         selected-payment-methods}
     (cc/query data))))

(defn built-component [data opts]
  (om/build component (query data) opts))
