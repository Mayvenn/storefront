(ns storefront.components.stylist.bonus-credit
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.formatters :as f]
            [storefront.keypaths :as keypaths]))

(defn display-stylist-bonus [{:keys [revenue-surpassed amount created-at]}]
  [:.loose-table-row
   [:.left-content
    [:p.chopped-content "Stylist Bonus Credit"]
    (when revenue-surpassed
      [:p.extra-credit-info
       "For reaching " (f/as-money-without-cents revenue-surpassed)])
    [:p.extra-credit-info (f/epoch-date created-at)]]
   [:.right-content
    [:p (f/as-money-without-cents amount)]]])

(defn bonus-history-component [data]
  (om/component
   (html
    (let [lifetime-total (get-in data keypaths/stylist-bonuses-lifetime-total)
          bonuses        (get-in data keypaths/stylist-bonuses-history)]
      [:.bonus-history
       [:h4.dashboard-details-header "Bonus History"]
       [:.solid-line-divider]
       [:.emphasized-banner
        [:span.emphasized-banner-header "Total Bonuses to Date"]
        [:span.emphasized-banner-value (f/as-money-without-cents lifetime-total)]]

       (map display-stylist-bonus bonuses)]))))

(defn pending-bonus-progress-component [data]
  (om/component
   (html
    (let [progress  (get-in data keypaths/stylist-bonuses-progress-to-next-bonus)
          milestone (get-in data keypaths/stylist-bonuses-milestone-amount)
          bar-width (min 100 (/ progress (/ milestone 100.0)))]
      [:.progress-container
       [:.progress-bar-limit (f/as-money-without-cents 0)]
       [:.progress-bar-container
        [:.progress-bar
         [:.progress-bar-progress {:style {:width (str bar-width "%")}}
          [:.progress-marker (f/as-money progress)]]]]
       [:.progress-bar-limit (f/as-money-without-cents milestone)]]))))

(defn stylist-bonus-credit-component [data]
  (om/component
   (html
    (let [available-credit (get-in data keypaths/user-total-available-store-credit)
          award-amount     (get-in data keypaths/stylist-bonuses-award-amount)
          milestone-amount (get-in data keypaths/stylist-bonuses-milestone-amount)
          progress-amount  (get-in data keypaths/stylist-bonuses-progress-to-next-bonus)]
      [:div
       [:h2.header-bar-heading.bonus-credit "Bonus Credit"]

       (when award-amount
         [:.dashboard-content
          [:.store-credit-detail
           [:.available-bonus.emphasized-banner.extra-emphasis
            [:span.emphasized-banner-header "Available Bonus Credits"]
            [:span.emphasized-banner-value (f/as-money available-credit)]]

           [:.dashboard-summary#next-reward-tracker
            [:p
             "Sell "
             (f/as-money (- milestone-amount progress-amount))
             " more to earn your next "
             (f/as-money-without-cents award-amount)
             " bonus credit."]]

           [:#money-rules
            [:.gold-money-box]
            [:.money-rule-details
             [:p
              "You earn "
              (f/as-money-without-cents award-amount)
              " in bonus credit for every "
              (f/as-money-without-cents milestone-amount)
              " in sales you make."]]]]

          (om/build pending-bonus-progress-component data)

          (om/build bonus-history-component data)])]))))
