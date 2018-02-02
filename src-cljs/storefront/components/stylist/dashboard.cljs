(ns storefront.components.stylist.dashboard
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.components.stylist.stats :refer [stylist-dashboard-stats-component]]
            [storefront.components.stylist.earnings :as earnings]
            [storefront.components.stylist.new-earnings :as new-earnings]
            [storefront.components.stylist.bonus-credit :as bonuses]
            [storefront.components.stylist.referrals :as referrals]
            [storefront.components.tabs :as tabs]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.experiments :as experiments]))

(defn component [{:keys [nav-event stats earnings new-earnings bonuses referrals stylist-transfers?]} owner opts]
  (om/component
   (html
    [:.container
     (om/build stylist-dashboard-stats-component {:stats stats})

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
  {:nav-event          (get-in data keypaths/navigation-event)
   :stats              (get-in data keypaths/stylist-stats)
   :earnings           (earnings/query data)
   :new-earnings       (new-earnings/query data)
   :bonuses            (bonuses/query data)
   :referrals          (referrals/query data)
   :stylist-transfers? (experiments/stylist-transfers? data)})

(defn built-component [data opts]
  (om/build component (query data) opts))
