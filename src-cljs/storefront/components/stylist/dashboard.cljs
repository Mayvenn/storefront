(ns storefront.components.stylist.dashboard
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.stylist.stats :refer [stylist-dashboard-stats-component]]
            [storefront.components.stylist.commissions :as commissions]
            [storefront.components.stylist.bonus-credit :as bonuses]
            [storefront.components.stylist.referrals :as referrals]
            [storefront.components.tabs :as tabs]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn stylist-dashboard-component [data owner]
  (om/component
   (html
    [:main {:role "main"}
     [:.legacy-container.sans-serif.black
      (om/build stylist-dashboard-stats-component
                {:stats (get-in data keypaths/stylist-stats)})

      [:div.bg-white
       [:div.md-up-col-6.mx-auto
        (om/build tabs/component {:selected-tab (get-in data keypaths/navigation-event)}
                  {:opts {:tab-refs ["bonuses" "commissions" "referrals"]
                          :labels   ["Bonuses" "Commissions" "Referrals"]
                          :tabs     [events/navigate-stylist-dashboard-bonus-credit
                                     events/navigate-stylist-dashboard-commissions
                                     events/navigate-stylist-dashboard-referrals]}})]]
      (condp = (get-in data keypaths/navigation-event)

        events/navigate-stylist-dashboard-commissions
        (om/build commissions/stylist-commissions-component (commissions/stylist-commissions-query data))

        events/navigate-stylist-dashboard-bonus-credit
        (om/build bonuses/stylist-bonuses-component (bonuses/stylist-bonuses-query data))

        events/navigate-stylist-dashboard-referrals
        (om/build referrals/stylist-referrals-component (referrals/stylist-referrals-query data)))]])))
