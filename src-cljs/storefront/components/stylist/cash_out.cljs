(ns storefront.components.stylist.cash-out
  (:require [storefront.accessors.experiments :as experiments]
            [storefront.accessors.payouts :as payouts]
            [storefront.api :as api]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.money-formatters :as mf]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [storefront.transitions :as transitions]
            [ui.molecules :as molecules]))

(defcomponent component [{:cashout/keys [amount total-amount fee-due? fee-amount title #_sub-title name account payout-timeframe footnote] :as data} owner opts]
  (let [return-link [:div.px2.my2 (molecules/return-link (:back/target data))]]
    [:div.container
     [:div.hide-on-tb-dt
      [:div.border-bottom.border-gray.border-width-1.m-auto.col-7-on-dt
       return-link]]
     [:div.hide-on-mb
      [:div.m-auto.container
       return-link]]
     [:div.px6.py10.center
      [:div.mb5.title-2.canela.light title]
      [:div.shout.proxima.title-3.dark-gray name]
      [:div.proxima.content-2 account]
      [:div.proxima.content-4.py1 payout-timeframe]
      [:div.mt5.mx2
       [:div.flex.justify-between.bg-cool-gray.p2
        [:div "Your Earnings"] [:div amount]]
       (when fee-due?
         [:div.flex.justify-between.p2
         [:div "Instapay Fee*"] [:div fee-amount]])
       [:hr.border-top.col-12.m0]
       [:div.flex.justify-between.p2
        [:div.shout.proxima.title-3 "TOTAL"] [:div.canela.title-2 total-amount]]]
      [:h2.p-color ]
      [:div
       (let [{:cashout.cta/keys [id ref target disabled? spinning? content]} data]
         [:div.my3.mx8
          {:data-test id
           :data-ref  ref}
          (ui/button-medium-primary {:on-click  (apply utils/send-event-callback target)
                                     :disabled? disabled?
                                     :spinning? spinning?}
                                    content)])]
      [:div.proxima.content-4.flex.left-align
       [:div.pp2 "*"]
       footnote]]]))

(defn return-link<-
  [app-state]
  #:return-link{:back          (-> app-state (get-in keypaths/navigation-undo-stack) first)
                :id            "back"
                :copy          "back"
                :event-message [events/navigate-v2-stylist-dashboard-payments]})

(defn query [data]
  (let [{:keys [amount payout-method]}                    (get-in data keypaths/stylist-payout-stats-next-payout)
        {:keys [name type last-4 email payout-timeframe]} payout-method
        greendot?                                         (= type "Mayvenn::GreenDotPayoutMethod")
        friday?                                           (= (spice.date/weekday-index (spice.date/now)) 5) ; 5 is friday
        fee-due?                                          (and (experiments/instapay? data)
                                                               greendot?
                                                               (not friday?))
        fee-amount                                        1
        total-amount                                      (if fee-due?
                                                            (- amount fee-amount)
                                                            amount)]
    {:back/target              (return-link<- data)
     :cashout/title            "Cashout Your Earnings"
     :cashout/amount           (mf/as-money amount)
     :cashout/fee-amount       (mf/as-money (- fee-amount))
     :cashout/total-amount     (mf/as-money total-amount)
     :cashout/name             name
     :cashout/account          (if greendot?
                                 (str "Linked Card xxxx-xxxx-xxxx-" (or last-4 "????"))
                                 email)
     :cashout/fee-due?         fee-due?
     :cashout/payout-timeframe (case payout-timeframe
                                 "immediate"                 "Instant: Funds typically arrive in minutes"
                                 "next_business_day"         "Funds paid out to this card will become available the next business day."
                                 "two_to_five_business_days" "Funds paid out to this card will become available two to five business days later."
                                 "test")
     :cashout/footnote         (when greendot?
                                 (str "Instapay allows you to cashout instantly with $" fee-amount " fee.
                         To avoid the fee, cashout on No Fee Friday(ET) or
                         select other cashout methods. "))
     :cashout.cta/id           "cash-out-commit-button"
     :cashout.cta/ref          "cash-out-button"
     :cashout.cta/target       [events/control-stylist-dashboard-cash-out-commit {:amount             amount
                                                                                  :payout-method-name name}]
     :cashout.cta/disabled?    (not (payouts/cash-out-eligible? payout-method))
     :cashout.cta/spinning?    (utils/requesting? data request-keys/cash-out-commit)
     :cashout.cta/content      "Cash out"}))

(defn ^:export built-component [data opts]
  (component/build component (query data) opts))

#_(defn ^:private should-redirect? [next-payout]
  (cond
    (some-> next-payout :payout-method payouts/cash-out-eligible? not) true
    (some-> next-payout :amount pos? not)                              true
    :else                                                              false))

(defmethod effects/perform-effects events/navigate-stylist-dashboard-cash-out-begin [_ _ _ _ app-state]
  #_(if true #_(should-redirect? (get-in app-state keypaths/stylist-payout-stats-next-payout))
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
