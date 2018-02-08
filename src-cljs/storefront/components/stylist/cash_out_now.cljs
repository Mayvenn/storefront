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

(defn component [{:keys [amount payout-method cash-out-pending?]} owner opts]
  (let [{:keys [name last4 email payout-timeframe]} payout-method]
    (om/component
     (html
      [:div.container.p4
       [:h3.mb3 "Cash Out Your Earnings"]
       [:div.col-12.flex.items-center.justify-between.my3
        [:div
         [:div.h6 name]
         (if last4
           [:div.h7 "Linked Card XXXX-XXXX-XXXX-" last4]
           [:div.h7 "PayPal Email: " email])]
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
          :data-ref "cash-out-button"}
         (ui/teal-button (utils/route-to events/navigate-stylist-dashboard-cash-out-pending) "Cash out")]]]))))

(defn query [data]
  (let [{:keys [amount payout-method]} (get-in data keypaths/stylist-next-payout)]
    {:amount            amount
     :payout-method     payout-method}))

(defn built-component [data opts]
  (om/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-stylist-dashboard-cash-out-now [_ _ _ _ app-state]
  (api/get-stylist-next-payout (get-in app-state keypaths/user-id) (get-in app-state keypaths/user-token)))

(defmethod transitions/transition-state events/api-success-stylist-next-payout
  [_ _ {:keys [amount payout-method]} app-state]
  (let [{:keys [name email last4 payout-timeframe]} payout-method]
    (assoc-in app-state keypaths/stylist-next-payout {:amount amount
                                                      :payout-method {:name             name
                                                                      :email            email
                                                                      :last4            last4
                                                                      :payout-timeframe payout-timeframe}})))
