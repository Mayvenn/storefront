(ns storefront.components.stylist.dashboard
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.components.stylist.stats :refer [stylist-dashboard-stats-component
                                                         new-stylist-dashboard-stats-component]]
            [storefront.components.stylist.earnings :as earnings]
            [storefront.components.stylist.new-earnings :as new-earnings]
            [storefront.components.stylist.bonus-credit :as bonuses]
            [storefront.components.stylist.referrals :as referrals]
            [storefront.components.tabs :as tabs]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.payouts :as payouts]
            [storefront.accessors.experiments :as experiments]))

(defn component [{:keys [nav-event
                         stats
                         payout-stats
                         earnings
                         new-earnings
                         bonuses
                         referrals
                         payout-method
                         stylist-transfers?
                         cash-out-now?
                         next-payout-slide]} owner opts]
  (om/component
   (html
    [:.container
     (if stylist-transfers?
       (om/build new-stylist-dashboard-stats-component {:payout-stats          payout-stats
                                                        :next-payout-slide     next-payout-slide})
       (om/build stylist-dashboard-stats-component {:stats                 stats
                                                    :payout-method         payout-method
                                                    :next-payout-slide     next-payout-slide}))

     [:div.bg-light-gray
      [:div.col-6-on-tb-dt.mx-auto
       (om/build tabs/component {:selected-tab nav-event}
                 {:opts {:tab-refs ["bonuses" "earnings" "referrals"]
                         :labels   ["Bonuses" "Earnings" "Referrals"]
                         :tabs     [events/navigate-stylist-dashboard-bonus-credit
                                    events/navigate-stylist-dashboard-earnings
                                    events/navigate-stylist-dashboard-referrals]}})]]
     (condp = nav-event
       events/navigate-stylist-dashboard-earnings
       (if stylist-transfers?
         (om/build new-earnings/component new-earnings)
         (om/build earnings/component earnings))

       events/navigate-stylist-dashboard-bonus-credit
       (om/build bonuses/component bonuses)

       events/navigate-stylist-dashboard-referrals
       (om/build referrals/component referrals))])))

(defn query [data]
  (let [payout-stats       (get-in data keypaths/stylist-payout-stats)
        payout-method      (-> payout-stats :next-payout :payout-method)

        cash-out-now?      (experiments/cash-out-now? data)
        cash-out-eligible? (and cash-out-now?
                                (payouts/cash-out-eligible? payout-method))]
    {:nav-event          (get-in data keypaths/navigation-event)
     :stats              (get-in data keypaths/stylist-stats)
     :payout-stats       payout-stats
     :earnings           (earnings/query data)
     :new-earnings       (new-earnings/query data)
     :payout-method      payout-method
     :bonuses            (bonuses/query data)
     :cash-out-now?      cash-out-now?
     :referrals          (referrals/query data)
     :stylist-transfers? (experiments/stylist-transfers? data)
     :next-payout-slide  (cond
                           (-> payout-stats
                               :initiated-payout
                               :status-id)    :stats/transfer-in-progress
                           cash-out-eligible? :stats/cash-out-now
                           :else              :stats/next-payout)}))

(defn built-component [data opts]
  (om/build component (query data) opts))
