(ns storefront.components.stylist.referrals
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.stylist.nav :refer [stylist-dashboard-nav-component]]
            [storefront.components.formatters :as f]
            [storefront.keypaths :as keypaths]))

(defn stylist-referral-component [referral]
  (html
   (let [{:keys [stylist-name paid-at percent-complete
                 commissioned-revenue bonus-due earning-amount]} referral]
     [:.loose-table-row
      [:.left-content
       [:p.chopped-content stylist-name]
       (if paid-at
         [:p.referral-paid-time (f/locale-date paid-at)]
         [:.referral-progress
          [:.progress-bar
           [:.progress-bar-progress {:style {:width (str percent-complete "%")}}]]
          [:p.progress-text
           "Sales so far: "
           (f/as-money commissioned-revenue)
           " of "
           (f/as-money-without-cents earning-amount)]])]

      [:.right-content
       [:p.paid-amount (f/as-money-without-cents bonus-due) " bonus"]
       (if paid-at
         [:p.referral-label.paid-label "Paid"]
         [:p.referral-label.pending-label "Pending"])]])))

(defn stylist-referrals-component [data owner]
  (om/component
   (html
    (let [sales-rep-email (str "mailto:" (get-in data keypaths/stylist-sales-rep-email) "?Subject=Referral")
          bonus-amount (get-in data keypaths/stylist-referral-program-bonus-amount)
          earning-amount (get-in data keypaths/stylist-referral-program-earning-amount)
          total-amount (get-in data keypaths/stylist-referral-program-total-amount)
          referrals (get-in data keypaths/stylist-referral-program-referrals)]
      [:div
       [:h2.header-bar-heading.referrals "Referrals"]

       (om/build stylist-dashboard-nav-component data)

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
          (f/as-money-without-cents total-amount)]]

         (map stylist-referral-component referrals)]]]))))
