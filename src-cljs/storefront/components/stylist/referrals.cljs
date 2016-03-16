(ns storefront.components.stylist.referrals
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.formatters :as f]
            [storefront.keypaths :as keypaths]))

(defn stylist-referral-component [earning-amount {:keys [stylist-name paid-at commissioned-revenue bonus-due]}]
  (html
   [:.loose-table-row
    [:.left-content
     [:p.chopped-content stylist-name]
     (if paid-at
       [:p.referral-paid-time (f/epoch-date paid-at)]
       [:.referral-progress
        [:.progress-bar
         [:.progress-bar-progress {:style {:width (str (min 100 (double (* (/ commissioned-revenue earning-amount) 100))) "%")}}]]
        [:p.progress-text
         "Sales so far: "
         (f/as-money commissioned-revenue)
         " of "
         (f/as-money-without-cents earning-amount)]])]

    [:.right-content
     [:p.paid-amount (f/as-money-without-cents bonus-due) " bonus"]
     (if paid-at
       [:p.referral-label.paid-label "Paid"]
       [:p.referral-label.pending-label "Pending"])]]))

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
         [:.dashboard-content
          [:a#email-referral.dashboard-summary {:href sales-rep-email :target "_top"}
           [:figure.email-icon]
           "Email us a new referral"
           [:figure.right-arrow-icon]]

          [:#money-rules
           [:.gold-money-box]
           [:.money-rule-details
            [:p
             "Earn "
             (f/as-money-without-cents bonus-amount)
             " in bonus credit when each stylist makes their first "
             (f/as-money-without-cents earning-amount)]]]

          [:.my-referrals
           [:h4.dashboard-details-header "My Referrals"]
           [:.solid-line-divider]

           [:.emphasized-banner
            [:span.emphasized-banner-header "Total Referral Bonuses"]
            [:span.emphasized-banner-value
             (f/as-money-without-cents lifetime-total)]]

           (map (partial stylist-referral-component earning-amount) referrals)]])]))))
