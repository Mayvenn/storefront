(ns storefront.components.stylist.referrals
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.formatters :as f]
            [storefront.keypaths :as keypaths]))

(defn stylist-referral-component [earning-amount {:keys [stylist-name paid-at commissioned-revenue bonus-due]}]
  (html
   [:div
    [:div
     [:p stylist-name]
     (if paid-at
       [:p (f/epoch-date paid-at)]
       [:div
        [:div {:style {:width (str (min 100 (double (* (/ commissioned-revenue earning-amount) 100))) "%")}}]
        [:p
         "Sales so far: "
         (f/as-money commissioned-revenue)
         " of "
         (f/as-money-without-cents earning-amount)]])]

    [:p (f/as-money-without-cents bonus-due) " bonus"]
    (if paid-at
      [:p "Paid"]
      [:p "Pending"])]))

(defn stylist-referrals-component [data owner]
  (om/component
   (html
    (let [sales-rep-email (str "mailto:"
                               (get-in data keypaths/stylist-sales-rep-email)
                               "?Subject=Referral&body=name:%0D%0Aemail:%0D%0Aphone:")
          bonus-amount (get-in data keypaths/stylist-referral-program-bonus-amount)
          earning-amount (get-in data keypaths/stylist-referral-program-earning-amount)
          lifetime-total (get-in data keypaths/stylist-referral-program-lifetime-total)
          referrals (get-in data keypaths/stylist-referral-program-referrals)]
      [:div {:data-test "referrals-panel"}
       (when bonus-amount
         [:div
          [:a {:href sales-rep-email :target "_top"}
           "Email us a new referral"]

          [:p
           "Earn "
           (f/as-money-without-cents bonus-amount)
           " in bonus credit when each stylist makes their first "
           (f/as-money-without-cents earning-amount)]

          [:h4 "My Referrals"]

          [:div
           [:span "Total Referral Bonuses"]
           [:span
            (f/as-money-without-cents lifetime-total)]]

          (map (partial stylist-referral-component earning-amount) referrals)])]))))
