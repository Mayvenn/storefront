(ns storefront.components.stylist.bonus-credit
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.stylist.nav :refer [stylist-dashboard-nav-component]]
            [storefront.components.formatters :as f]
            [storefront.state :as state]))

(defn stylist-bonus-component [bonus]
  (html
   [:div.loose-table-row
    [:div.left-content
     [:p.chopped-content (bonus :category-name)]
     (when (bonus :referred-stylist) [:p.extra-credit-info (bonus :referred-stylist)])
     (when (bonus :revenue-reached) [:p.extra-credit-info (bonus :revenue-reached)])
     [:p.extra-credit-info (f/locale-date (bonus :created-at))]]

    [:div.right-content
     [:p (f/float-as-money (bonus :amount))]]]))

 (defn stylist-bonus-credit-component [data]
  (om/component
   (html
    (let [bonuses (get-in data state/stylist-bonus-credit-bonuses-path)
          total-credit (get-in data state/stylist-bonus-credit-total-credit-path)
          available-credit (get-in data state/stylist-bonus-credit-available-credit-path)
          commissioned-revenue (get-in data state/stylist-bonus-credit-commissioned-revenue-path)
          bonus-amount (get-in data state/stylist-bonus-credit-bonus-amount-path)
          earning-amount (get-in data state/stylist-bonus-credit-earning-amount-path)
          progress-amount (mod commissioned-revenue earning-amount)
          remaining-amount (- earning-amount progress-amount)
          progress-bar-width (/ progress-amount (/ earning-amount 100.0))]
      [:div
       [:h2.header-bar-heading.bonus-credit "Bonus Credit"]

       (om/build stylist-dashboard-nav-component data)

       [:div.dashboard-content
        [:div.store-credit-detail
         [:div.available-bonus.emphasized-banner.extra-emphasis
          [:span.emphasized-banner-header "Available Bonus Credits"]
          [:span.emphasized-banner-value (f/float-as-money available-credit :cents false)]]

         [:div.dashboard-summary#next-reward-tracker
          [:p
           "Sell "
           (f/float-as-money remaining-amount)
           " more to earn your next "
           (f/float-as-money bonus-amount :cents false)
           " bonus credit."]]

         [:div#money-rules
          [:div.gold-money-box]
          [:div.money-rule-details
           [:p
            "You earn "
            (f/float-as-money bonus-amount :cents false)
            " in bonus credit for every "
            (f/float-as-money earning-amount :cents false)
            " in sales you make."]]]]

        [:div.progress-container
         [:div.progress-bar-limit (f/float-as-money 0 :cents false)]
         [:div.progress-bar-container
          [:div.progress-bar
           [:div.progress-bar-progress {:style {:width (str progress-bar-width "%")}}
            [:div.progress-marker (f/float-as-money progress-amount)]]]]
         [:div.progress-bar-limit (f/float-as-money earning-amount :cents false)]]

        [:div.bonus-history
         [:h4.dashboard-details-header "Bonus History"]
         [:div.solid-line-divider]
         [:div.emphasized-banner
          [:span.emphasized-banner-header "Total Bonuses to Date"]
          [:span.emphasized-banner-value (f/float-as-money total-credit)]]

         (map stylist-bonus-component bonuses)]]]))))

