(ns storefront.components.stylist.cash-out-now
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.events :as events]
            [storefront.components.money-formatters :as mf]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.components.ui :as ui]
            [storefront.transitions :as transitions]
            [storefront.api :as api]
            [storefront.effects :as effects]))

(defn cash-out-eligible? [payout-method]
  (boolean (= "Mayvenn InstaPay" (:name payout-method))))

(defn component [{:keys [amount payout-method cash-out-pending?]} owner opts]
  (let [{:keys [name last-4 email payout-timeframe]} payout-method]
    (om/component
     (html
      [:div.container.p4
       [:h3.mb3 "Cash Out Your Earnings"]
       [:div.col-12.flex.items-center.justify-between.my3
        [:div
         [:div.h6 name]
         [:div.h7 "Linked Card XXXX-XXXX-XXXX-" (or last-4 "????")]]
        [:h2.teal (mf/as-money amount)]]
       [:div
        [:div.navy.center.h7
         (case payout-timeframe
           "immediate"                 "Instant: Funds typically arrive in minutes"
           "next_business_day"         "Funds paid out to this card will become available the next business day."
           "two_to_five_business_days" "Funds paid out to this card will become available two to five business days later."
           "") ]
        [:div.my3
         {:data-test "cash-out-button"
          :data-ref  "cash-out-button"}
         (ui/teal-button {:on-click (utils/send-event-callback events/control-stylist-dashboard-cash-out-submit)
                          :disabled? (not (cash-out-eligible? payout-method))}
          "Cash out")]]]))))

(defn query [data]
  (let [{:keys [amount payout-method]} (get-in data keypaths/stylist-payout-stats-next-payout)]
    {:amount            amount
     :payout-method     payout-method}))

(defn built-component [data opts]
  (om/build component (query data) opts))

(defn ^:private should-redirect? [next-payout]
  (cond
    (some-> next-payout :payout-method cash-out-eligible? not) true
    (some-> next-payout :amount pos? not)                      true
    :else                                                      false))

(defmethod effects/perform-effects events/navigate-stylist-dashboard-cash-out-now [_ _ _ _ app-state]
  (if (should-redirect? (get-in app-state keypaths/stylist-payout-stats-next-payout))
    (effects/redirect events/navigate-stylist-dashboard-earnings)
    (api/get-stylist-payout-stats events/api-success-stylist-payout-stats-cash-out-now
                                  (get-in app-state keypaths/store-stylist-id)
                                  (get-in app-state keypaths/user-id)
                                  (get-in app-state keypaths/user-token))))

(defmethod effects/perform-effects events/control-stylist-dashboard-cash-out-submit [_ _ _ _ app-state]
  (let [stylist-id (get-in app-state keypaths/store-stylist-id)
        user-id    (get-in app-state keypaths/user-id)
        user-token (get-in app-state keypaths/user-token)]
    (api/cash-out-now user-id user-token stylist-id)))

(defmethod effects/perform-effects events/api-success-cash-out-now
  [_ _ {:keys [status-id balance-transfer-id]} _ app-state]
  (effects/redirect events/navigate-stylist-dashboard-cash-out-pending))

(defmethod transitions/transition-state events/api-success-cash-out-now
  [_ _ {:keys [status-id balance-transfer-id]} app-state]
  (-> app-state
      (assoc-in keypaths/stylist-cash-out-status-id status-id)
      (assoc-in keypaths/stylist-cash-out-balance-transfer-id balance-transfer-id)))

(defmethod effects/perform-effects events/api-success-stylist-payout-stats-cash-out-now
  [_ _ {next-payout :next-payout} _ app-state]
  (when (should-redirect? next-payout)
    (effects/redirect events/navigate-stylist-dashboard-earnings)))
