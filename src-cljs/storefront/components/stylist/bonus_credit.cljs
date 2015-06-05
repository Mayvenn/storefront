(ns storefront.components.stylist.bonus-credit
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.stylist.nav :refer [stylist-dashboard-nav-component]]
            [storefront.components.formatters :as f]
            [storefront.keypaths :as keypaths]))

(defn stylist-bonus-component [bonus]
  (html
   [:.loose-table-row
    [:.left-content
     [:p.chopped-content (bonus :category-name)]
     (when (bonus :referred-stylist) [:p.extra-credit-info (bonus :referred-stylist)])
     (when (bonus :revenue-reached) [:p.extra-credit-info (bonus :revenue-reached)])
     [:p.extra-credit-info (f/locale-date (bonus :created-at))]]

    [:.right-content
     [:p (f/as-money-without-cents (bonus :amount))]]]))

 (defn stylist-bonus-credit-component [data]
  (om/component
   (html
    (let [bonuses (get-in data keypaths/stylist-bonus-credit-bonuses)
          total-credit (get-in data keypaths/stylist-bonus-credit-total-credit)
          available-credit (get-in data keypaths/stylist-bonus-credit-available-credit)
          commissioned-revenue (get-in data keypaths/stylist-bonus-credit-commissioned-revenue)
          bonus-amount (get-in data keypaths/stylist-bonus-credit-bonus-amount)
          earning-amount (get-in data keypaths/stylist-bonus-credit-earning-amount)
          progress-amount (mod commissioned-revenue earning-amount)
          remaining-amount (- earning-amount progress-amount)
          progress-bar-width (/ progress-amount (/ earning-amount 100.0))]
      [:div
       [:h2.header-bar-heading.bonus-credit "Bonus Credit"]

       (om/build stylist-dashboard-nav-component data)

       [:.dashboard-content
        [:.store-credit-detail
         [:.available-bonus.emphasized-banner.extra-emphasis
          [:span.emphasized-banner-header "Available Bonus Credits"]
          [:span.emphasized-banner-value (f/as-money-without-cents available-credit)]]

         [:.dashboard-summary#next-reward-tracker
          [:p
           "Sell "
           (f/as-money remaining-amount)
           " more to earn your next "
           (f/as-money-without-cents bonus-amount)
           " bonus credit."]]

         [:#money-rules
          [:.gold-money-box]
          [:.money-rule-details
           [:p
            "You earn "
            (f/as-money-without-cents bonus-amount)
            " in bonus credit for every "
            (f/as-money-without-cents earning-amount)
            " in sales you make."]]]]

        [:.progress-container
         [:.progress-bar-limit (f/as-money-without-cents 0)]
         [:.progress-bar-container
          [:.progress-bar
           [:.progress-bar-progress {:style {:width (str progress-bar-width "%")}}
            [:.progress-marker (f/as-money progress-amount)]]]]
         [:.progress-bar-limit (f/as-money-without-cents earning-amount)]]

        [:.bonus-history
         [:h4.dashboard-details-header "Bonus History"]
         [:.solid-line-divider]
         [:.emphasized-banner
          [:span.emphasized-banner-header "Total Bonuses to Date"]
          [:span.emphasized-banner-value (f/as-money-without-cents total-credit)]]

         (map stylist-bonus-component bonuses)]]]))))
