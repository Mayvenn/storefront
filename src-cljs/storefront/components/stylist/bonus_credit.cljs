(ns storefront.components.stylist.bonus-credit
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.formatters :as f]
            [storefront.keypaths :as keypaths]))

(defn display-stylist-bonus [{:keys [revenue-surpassed amount created-at]}]
  [:div
   [:p "Stylist Bonus Credit"]
   (when revenue-surpassed
     [:p "For reaching " (f/as-money-without-cents revenue-surpassed)])
   [:p (f/epoch-date created-at)]
   [:p (f/as-money-without-cents amount)]])

(defn bonus-history-component [data]
  (om/component
   (html
    (let [lifetime-total (get-in data keypaths/stylist-bonuses-lifetime-total)
          bonuses        (get-in data keypaths/stylist-bonuses-history)]
      [:div
       [:h4 "Bonus History"]
       [:div
        [:span "Total Bonuses to Date"]
        [:span (f/as-money-without-cents lifetime-total)]]

       (map display-stylist-bonus bonuses)]))))

(defn pending-bonus-progress-component [data]
  (om/component
   (html
    (let [progress  (get-in data keypaths/stylist-bonuses-progress-to-next-bonus)
          milestone (get-in data keypaths/stylist-bonuses-milestone-amount)
          bar-width (min 100 (/ progress (/ milestone 100.0)))]
      [:div
       [:div (f/as-money-without-cents 0)]
       [:div {:style {:width (str bar-width "%")}}
        [:div (f/as-money progress)]]
       [:div (f/as-money-without-cents milestone)]]))))

(defn stylist-bonus-credit-component [data]
  (om/component
   (html
    (let [available-credit (get-in data keypaths/user-total-available-store-credit)
          award-amount     (get-in data keypaths/stylist-bonuses-award-amount)
          milestone-amount (get-in data keypaths/stylist-bonuses-milestone-amount)
          progress-amount  (get-in data keypaths/stylist-bonuses-progress-to-next-bonus)]
      [:div {:data-test "bonuses-panel"}
       (when award-amount
         [:div
          [:div
           [:span "Available Bonus Credits"]
           [:span (f/as-money available-credit)]]

          [:p
           "Sell "
           (f/as-money (- milestone-amount progress-amount))
           " more to earn your next "
           (f/as-money-without-cents award-amount)
           " bonus credit."]

          [:p
           "You earn "
           (f/as-money-without-cents award-amount)
           " in bonus credit for every "
           (f/as-money-without-cents milestone-amount)
           " in sales you make."]

          (om/build pending-bonus-progress-component data)

          (om/build bonus-history-component data)])]))))
