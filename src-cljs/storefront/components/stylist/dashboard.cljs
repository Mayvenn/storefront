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
            [storefront.accessors.experiments :as experiments]))

(def cash-out-eligible-payout-methods
  #{"green_dot"})

(defn component [{:keys [nav-event
                         stats
                         payout-stats
                         earnings
                         new-earnings
                         bonuses
                         referrals
                         payout-method
                         stylist-transfers?
                         cash-out-now?]} owner opts]
  (om/component
   (html
    [:.container
     (if stylist-transfers?
       (om/build new-stylist-dashboard-stats-component {:payout-stats          payout-stats
                                                        :show-cash-out-now-ui? (and cash-out-now?
                                                                                    (cash-out-eligible-payout-methods
                                                                                     payout-method))})
       (om/build stylist-dashboard-stats-component {:stats                 stats
                                                    :payout-method         payout-method
                                                    :show-cash-out-now-ui? (and cash-out-now?
                                                                                (cash-out-eligible-payout-methods
                                                                                 payout-method))}))

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
  (let [payout-method         (get-in data (conj keypaths/stylist-manage-account :original_payout_method))
        cash-out-now?         (experiments/cash-out-now? data)]
      {:nav-event             (get-in data keypaths/navigation-event)
       :stats                 (get-in data keypaths/stylist-stats)
       :payout-stats          (get-in data keypaths/stylist-payout-stats)
       :earnings              (earnings/query data)
       :new-earnings          (new-earnings/query data)
       :payout-method         payout-method
       :bonuses               (bonuses/query data)
       :cash-out-now?         cash-out-now?
       :referrals             (referrals/query data)
       :stylist-transfers?    (experiments/stylist-transfers? data)
       :show-cash-out-now-ui? (and cash-out-now?
                                   (cash-out-eligible-payout-methods payout-method))}))

(defn built-component [data opts]
  (om/build component (query data) opts))
