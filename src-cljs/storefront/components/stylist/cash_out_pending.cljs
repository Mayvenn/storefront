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

(defmethod effects/perform-effects events/navigate-stylist-dashboard-cash-out-pending
  [_ _ _ _ app-state]
  (let [stylist-id (get-in app-state keypaths/store-stylist-id)
        user-id (get-in app-state keypaths/user-id)
        user-token (get-in app-state keypaths/user-token)]
    (api/cash-out-now user-id user-token stylist-id)))

(defmethod transitions/transition-state events/api-success-cash-out-now
  [_ _ {:keys [status-id balance-transfer-id]} app-state]
  (-> app-state
      (assoc-in keypaths/stylist-cash-out-status-id status-id)
      (assoc-in keypaths/stylist-cash-out-balance-transfer-id balance-transfer-id)))

(defn- poll-status [user-id user-token status-id stylist-id]
  (js/setTimeout (fn [] (api/cash-out-status user-id user-token status-id stylist-id))
                 3000))

(defmethod effects/perform-effects events/api-success-cash-out-now
  [_ _ _ _ app-state]
  (let [status-id  (get-in app-state keypaths/stylist-cash-out-status-id)
        user-id    (get-in app-state keypaths/user-id)
        user-token (get-in app-state keypaths/user-token)
        stylist-id (get-in app-state keypaths/store-stylist-id)]
    (poll-status user-id user-token status-id stylist-id)))


(defmethod effects/perform-effects events/api-success-cash-out-status
  [_ _ {:keys [status]} _ app-state]
  (let [status-id  (get-in app-state keypaths/stylist-cash-out-status-id)
        user-id    (get-in app-state keypaths/user-id)
        user-token (get-in app-state keypaths/user-token)
        stylist-id (get-in app-state keypaths/store-stylist-id)]
    (case status
      "failed" (messages/handle-message events/api-success-cash-out-failed)
      "paid"   (messages/handle-message events/api-success-cash-out-complete)
      (poll-status user-id user-token status-id stylist-id))))

(defmethod effects/perform-effects events/api-success-cash-out-complete
  [_ _ _ _ app-state]
  (effects/redirect events/navigate-stylist-dashboard-cash-out-success))

(defmethod effects/perform-effects events/api-success-cash-out-failed
  [_ _ _ _ app-state]
  (effects/redirect events/navigate-stylist-account-commission))
