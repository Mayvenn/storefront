(ns storefront.components.stylist.dashboard
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.formatters :as f]
            [storefront.components.stylist.stats :refer [stylist-dashboard-stats-component]]
            [storefront.components.stylist.commissions :refer [stylist-commissions-component]]
            [storefront.components.stylist.bonus-credit :refer [stylist-bonus-credit-component]]
            [storefront.components.stylist.referrals :refer [stylist-referrals-component]]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn link-with-selected [data event label]
  (let [navigation-state (get-in data keypaths/navigation-event)]
    [:a.black.py1.mx3.lg-mt2.border-teal
     (merge (when (= navigation-state event) {:class "border-bottom"})
            {:style {:border-width "2px"}}
            (utils/route-to data event)) label]))

(defn stylist-dashboard-nav-component [data]
  (om/component
   (html
    [:nav.bg-silver.h5
     [:div.bg-lighten-4
      [:div.bg-lighten-4.flex.items-start.justify-center
       (link-with-selected data events/navigate-stylist-dashboard-bonus-credit "Bonuses")
       (link-with-selected data events/navigate-stylist-dashboard-commissions "Commissions")
       (link-with-selected data events/navigate-stylist-dashboard-referrals "Referrals")]]])))

(defn stylist-dashboard-component [data owner]
  (om/component
   (html
    [:main {:role "main"}
     [:.legacy-container
      (om/build stylist-dashboard-stats-component data)

      (om/build stylist-dashboard-nav-component data)
      (om/build
       (condp = (get-in data keypaths/navigation-event)
         events/navigate-stylist-dashboard-commissions  stylist-commissions-component
         events/navigate-stylist-dashboard-bonus-credit stylist-bonus-credit-component
         events/navigate-stylist-dashboard-referrals    stylist-referrals-component)
       data)]])))
