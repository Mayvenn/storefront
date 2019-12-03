(ns storefront.components.stylist.cash-out
  (:require [storefront.events :as events]
            [storefront.components.money-formatters :as mf]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.accessors.payouts :as payouts]
            [storefront.components.ui :as ui]
            [storefront.transitions :as transitions]
            [storefront.api :as api]
            [storefront.effects :as effects]
            [storefront.request-keys :as request-keys]
            [storefront.component :as component :refer [defcomponent]]))

(defcomponent component [{:keys [amount payout-method cash-out-pending? cashing-out?]} owner opts]
  (let [{:keys [name type last-4 email payout-timeframe]} payout-method]
    [:div.container.p4
     [:h3.mb3 "Cash Out Your Earnings"]
     [:div.col-12.flex.items-center.justify-between.my3
      [:div
       [:div.h6 name]
       (if (= type "Mayvenn::GreenDotPayoutMethod")
         [:div.h6 "Linked Card xxxx-xxxx-xxxx-" (or last-4 "????")]
         [:div.h6 email])]
      [:h2.p-color (mf/as-money amount)]]
     [:div
      [:div.navy.center.h8
       (case payout-timeframe
         "immediate"                 "Instant: Funds typically arrive in minutes"
         "next_business_day"         "Funds paid out to this card will become available the next business day."
         "two_to_five_business_days" "Funds paid out to this card will become available two to five business days later."
         "")]
      [:div.my3
       {:data-test "cash-out-commit-button"
        :data-ref  "cash-out-button"}
       (ui/p-color-button {:on-click  (utils/send-event-callback events/control-stylist-dashboard-cash-out-commit
                                                                 {:amount amount
                                                                  :payout-method-name name})
                           :disabled? (not (payouts/cash-out-eligible? payout-method))
                           :spinning? cashing-out?}
                          "Cash out")]]]))

(defn query [data]
  (let [{:keys [amount payout-method]} (get-in data keypaths/stylist-payout-stats-next-payout)]
    {:amount            amount
     :payout-method     payout-method
     :cashing-out?      (utils/requesting? data request-keys/cash-out-commit)}))

(defn ^:export built-component [data opts]
  (component/build component (query data) opts))

(defn ^:private should-redirect? [next-payout]
  (cond
    (some-> next-payout :payout-method payouts/cash-out-eligible? not) true
    (some-> next-payout :amount pos? not)                              true
    :else                                                              false))

(defmethod effects/perform-effects events/navigate-stylist-dashboard-cash-out-begin [_ _ _ _ app-state]
  (if (should-redirect? (get-in app-state keypaths/stylist-payout-stats-next-payout))
    (effects/redirect events/navigate-v2-stylist-dashboard-payments)
    (api/get-stylist-payout-stats events/api-success-stylist-payout-stats-cash-out
                                  (get-in app-state keypaths/user-store-id)
                                  (get-in app-state keypaths/user-id)
                                  (get-in app-state keypaths/user-token))))

(defmethod effects/perform-effects events/control-stylist-dashboard-cash-out-commit [_ _ _ _ app-state]
  (let [stylist-id (get-in app-state keypaths/user-store-id)
        user-id    (get-in app-state keypaths/user-id)
        user-token (get-in app-state keypaths/user-token)]
    (api/cash-out-commit user-id user-token stylist-id)))

(defmethod effects/perform-effects events/api-success-cash-out-commit
  [_ _ {:keys [status-id balance-transfer-id]} _ app-state]
  (effects/redirect events/navigate-stylist-dashboard-cash-out-pending {:status-id status-id}))

(defmethod transitions/transition-state events/api-success-cash-out-commit
  [_ _ {:keys [status-id balance-transfer-id amount payout-method]} app-state]
  (-> app-state
      (assoc-in keypaths/stylist-payout-stats-initiated-payout {:amount amount
                                                                :payout-method payout-method})
      (assoc-in keypaths/stylist-cash-out-status-id status-id)
      (assoc-in keypaths/stylist-cash-out-balance-transfer-id balance-transfer-id)))

(defmethod effects/perform-effects events/api-success-stylist-payout-stats-cash-out
  [_ _ {next-payout :next-payout} _ app-state]
  (when (should-redirect? next-payout)
    (effects/redirect events/navigate-v2-stylist-dashboard-payments)))
