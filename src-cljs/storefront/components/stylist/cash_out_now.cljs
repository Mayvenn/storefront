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

(defn component [{:keys [amount payout-method]} owner opts]
  (let [{:keys [name last4 email]} payout-method]
    (om/component
     (html
      [:.container.p4
       [:h3.mb3 "Cash Out Your Earnings"]
       [:div.col-12.flex.items-center.justify-between.my3
        [:div
         [:div.h6 name]
         (if last4
           [:div.h7 "Linked Card XXXX-XXXX-XXXX-" last4]
           [:div.h7 "PayPal Email: " email])]
        [:h2.teal (mf/as-money amount)]]
       (when last4 [:div.navy.center.h7 "Instant: Funds typically arrive in minutes"])
       [:div.my3
        {:data-test "cash-out-button"
         :data-ref "cash-out-button"}
        (ui/teal-button (utils/route-to events/navigate-cart) "Cash out")]
       [:div.h7.mt3.dark-gray "Transfers may take up to 30 minutes and vary by bank."]]))))

(defn query [data]
  (let [{:keys [amount payout-method]} (get-in data keypaths/stylist-next-payout)]
    {:amount        amount
     :payout-method payout-method}))

(defn built-component [data opts]
  (om/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-stylist-dashboard-cash-out-now [_ _ _ _ app-state]
  (api/get-stylist-next-payout (get-in app-state keypaths/user-id) (get-in app-state keypaths/user-token)))

(defmethod transitions/transition-state events/api-success-stylist-next-payout
  [_ _ {:keys [amount payout-method]} app-state]
  (let [{:keys [name email last4]} payout-method]
    (assoc-in app-state keypaths/stylist-next-payout {:amount amount
                                                      :payout-method {:name  name
                                                                      :email email
                                                                      :last4 last4}})))
