(ns storefront.components.stylist.bonus-credit
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.stylist.nav :refer [stylist-dashboard-nav-component]]
            [storefront.components.formatters :as f]
            [storefront.state :as state]))

(defn stylist-bonus-credit-component [data]
  (om/component
   (html
    [:div
     [:h2.header-bar-heading.bonus-credit "Bonus Credit"]

     (om/build stylist-dashboard-nav-component data)

     [:div.dashboard-content
      [:div.store-credit-detail
       [:div.available-bonus.emphasized-banner.extra-emphasis
        [:span.emphasized-banner-header "Available Bonus Credits"]
        [:span.emphasized-banner-value "FIXME"]]

       [:div.dashboard-summary#next-reward-tracker
        [:p "Sell $289.00 more to earn your next $100 bonus credit."]]

       [:div#money-rules
        [:div.gold-money-box]
        [:div.money-rule-details
         [:p "You earn $100 in bonus credit for every $600 in sales you make."]]]]

      [:div.progress-container
       [:div.progress-bar-limit
        "FIXME"
        [:div.progress-bar-container
         [:div.progress-bar
          [:div.progress-bar-progress ;;{:style "width: 51.833333333333333333%;"}
           [:div.progress-marker "FIXME"]]]]]
       [:div.progress-bar-limit "FIXME"]]

      [:div.bonus-history
       [:h4.dashboard-details-header "Bonus History"]
       [:div.solid-line-divider]
       [:div.emphasized-banner
        [:span.emphasized-banner-header "Total Bonuses to Date"]
        [:span.emphasized-banner-value "FIXME"]]]]])))

