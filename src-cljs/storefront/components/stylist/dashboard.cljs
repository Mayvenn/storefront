(ns storefront.components.stylist.dashboard
  (:require [storefront.component :as component]
            [storefront.components.stylist.bonus-credit :as bonuses]
            [storefront.components.stylist.earnings :as earnings]
            [storefront.components.stylist.referrals :as referrals]
            [storefront.components.stylist.stats :as stats]
            [storefront.components.tabs :as tabs]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn component
  [{:keys [nav-event payout-stats earnings bonuses referrals stats]} owner opts]
  (component/create
   [:div.container
    (component/build stats/component stats nil)

    [:div.bg-light-gray
     [:div.col-6-on-tb-dt.mx-auto
      (component/build tabs/component
                       {:selected-tab nav-event}
                       {:opts {:tab-refs ["bonuses" "earnings" "referrals"]
                               :labels   ["Bonuses" "Earnings" "Referrals"]
                               :tabs     [events/navigate-stylist-dashboard-bonus-credit
                                          events/navigate-stylist-dashboard-earnings
                                          events/navigate-stylist-dashboard-referrals]}})]]
    (condp = nav-event
      events/navigate-stylist-dashboard-earnings
      (component/build earnings/component earnings nil)

      events/navigate-stylist-dashboard-bonus-credit
      (component/build bonuses/component bonuses nil)

      events/navigate-stylist-dashboard-referrals
      (component/build referrals/component referrals nil))]))

(defn query
  [data]
  {:nav-event (get-in data keypaths/navigation-event)
   :earnings  (earnings/query data)
   :bonuses   (bonuses/query data)
   :referrals (referrals/query data)
   :stats     (stats/query data)})

(defn built-component [data opts]
  (component/build component (query data) opts))
