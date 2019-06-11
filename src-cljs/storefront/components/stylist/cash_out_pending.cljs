(ns storefront.components.stylist.cash-out-pending
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.events :as events]
            [storefront.components.money-formatters :as mf]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.components.ui :as ui]
            [storefront.transitions :as transitions]
            [storefront.api :as api]
            [storefront.effects :as effects]
            [storefront.platform.messages :as messages]
            [storefront.platform.component-utils :as utils]))

(defn component [_ owner opts]
  (om/component
   (html
    [:div.container.p4.center
     (ui/large-spinner {:style {:height "6em"}})
     [:h2.my3 "Transfer in progress"]
     [:p "We are currently transferring your funds. Please stay on this page until the transfer completes."]])))

(defn query [data]
  {})

(defn built-component [data opts]
  (om/build component {} opts))

(defn- poll-status [user-id user-token status-id stylist-id]
  (js/setTimeout (fn [] (api/cash-out-status user-id user-token status-id stylist-id))
                 3000))

(defmethod effects/perform-effects events/navigate-stylist-dashboard-cash-out-pending
  [_ _ {:keys [status-id]} _ app-state]
  (let [user-id    (get-in app-state keypaths/user-id)
        user-token (get-in app-state keypaths/user-token)
        stylist-id (get-in app-state keypaths/user-store-id)]
    (poll-status user-id user-token status-id stylist-id)))

(defmethod effects/perform-effects events/api-success-cash-out-status
  [_ _ {:keys [status status-id] :as cash-out-status} _ app-state]
  (let [user-id    (get-in app-state keypaths/user-id)
        user-token (get-in app-state keypaths/user-token)
        stylist-id (get-in app-state keypaths/user-store-id)]
    (case status
      "failed"    (messages/handle-message events/api-success-cash-out-failed cash-out-status)
      "submitted" (messages/handle-message events/api-success-cash-out-complete cash-out-status)
      "paid"      (messages/handle-message events/api-success-cash-out-complete cash-out-status)
      (poll-status user-id user-token status-id stylist-id))))

(defmethod transitions/transition-state events/api-success-cash-out-complete
  [_ _ {:keys [balance-transfer-id]} app-state]
  (-> app-state
      (assoc-in keypaths/stylist-cash-out-balance-transfer-id balance-transfer-id)))

(defmethod effects/perform-effects events/api-success-cash-out-complete
  [_ _ cash-out-status _ app-state]
  (effects/redirect events/navigate-stylist-dashboard-cash-out-success cash-out-status))

(defmethod effects/perform-effects events/api-success-cash-out-failed
  [_ _ args _ app-state]
  (let [payout-method (get-in app-state (conj keypaths/stylist-payout-stats-next-payout :payout-method))
        message       (case (:type payout-method)
                        "Mayvenn::PaypalPayoutMethod"   (str "Your cash out failed. "
                                                             "Please confirm your PayPal account or switch to Mayvenn InstaPay.")
                        "Mayvenn::GreenDotPayoutMethod" (str "Your cash out failed. "
                                                             "Please try another card with Mayvenn InstaPay "
                                                             "or switch to PayPal and try again."))]
    (effects/redirect events/navigate-stylist-account-payout)
    (messages/handle-later events/flash-show-failure {:message message})))
