(ns storefront.components.stylist.referrals
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.stylist.nav :refer [stylist-dashboard-nav-component]]
            [storefront.components.formatters :as f]
            [storefront.state :as state]))

(defn stylist-referral-component [referral]
  (html
   (let [{:keys [stylist-name paid-at percent-complete
                 commissioned-revenue bonus-due earning-amount]} referral]
     [:div.loose-table-row
      [:div.left-content
       [:p.chopped-content stylist-name]
       (if paid-at
         [:p.referral-paid-time paid-at]
         [:div.referral-progress
          [:div.progress-bar
           [:div.progress-bar-progress {:style {:width (str percent-complete "%")}}]]
          [:p.progress-text
           "Sales so far: "
           (f/float-as-money commissioned-revenue)
           " of "
           (f/float-as-money earning-amount :cents false)]])]

      [:div.right-content
       [:p.paid-amount (f/float-as-money bonus-due :cents false) " bonus"]
       (if paid-at
         [:p.referral-label.paid-label "Paid"]
         [:p.referral-label.pending-label "Pending"])]])))

(defn stylist-referrals-component [data owner]
  (om/component
   (html
    (let [sales-rep-email (str "mailto:" (get-in data state/stylist-sales-rep-email-path) "?Subject=Referral")
          bonus-amount (get-in data state/stylist-referral-program-bonus-amount-path)
          earning-amount (get-in data state/stylist-referral-program-earning-amount-path)
          total-amount (get-in data state/stylist-referral-program-total-amount-path)
          referrals (get-in data state/stylist-referral-program-referrals-path)]
      [:div
       [:h2.header-bar-heading.referrals "Referrals"]

       (om/build stylist-dashboard-nav-component data)

       [:div.dashboard-content
        [:a#email-referral.dashboard-summary {:href sales-rep-email :target "_top"}
         [:figure.email-icon]
         "Email us a new referral"
         [:figure.right-arrow-icon]]

        [:div#money-rules
         [:div.gold-money-box]
         [:div.money-rule-details
          [:p
           "Earn "
           (f/float-as-money bonus-amount :cents false)
           " in bonus credit when each stylist makes their first "
           (f/float-as-money earning-amount :cents false)]]]

        [:div.my-referrals
         [:h4.dashboard-details-header "My Referrals"]
         [:div.solid-line-divider]

         [:div.emphasized-banner
          [:span.emphasized-banner-header "Total Referral Bonuses"]
          [:span.emphasized-banner-value
          (f/float-as-money total-amount :cents false)]]

         (map stylist-referral-component referrals)]]]))))

