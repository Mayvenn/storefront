(ns storefront.components.stylist.commissions
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]))

(defn stylist-commissions-component [data owner]
  (om/component
   (html
    [:main {:role "main"}
     [:div.container
     [:h2.header-bar-heading.commissions "Commissions"]
     [:nav.stylist-dashboard-nav
     [:a.selected {:href "/stylist/commissions"} "Commissions"]
     [:a {:href "/stylist/store_credits"} "Bonus Credit"]
     [:a {:href "/stylist/referrals"} "Referrals"]]
    [:div.dashboard-content
     [:div#next-commission-summary.dashboard-summary

      [:div.next-payout-description
       [:p "As of today, your next commission payment is:"]
       [:p.next-commissions-amount "$203.52"]]

      [:div.next-payout-date-container
       [:p.accented-next-pay "W"]
       [:p.small-payout-description "Commission paid on Wednesdays"]]]

     [:div#money-rules
      [:div.gold-money-box]
      [:div.money-rule-details
       [:p "You earn 15% commission on each new order shipped from your store excluding tax and shipping."]]]

     [:div.new-order-commissions
      [:h4.dashboard-details-header "New Orders"]
      [:p.commission-explanation "Commission is earned when order ships."]
      [:div.solid-line-divider]
      [:div.loose-table-header
       [:div.left-header "Sale"]
       [:div.right-header "Commission"]]
      [:div.loose-table-row
       [:div.left-content
        [:p "commission"]
        [:p.top-pad "05/08/2015"]
        [:p.top-pad [:a {:href "/orders/R566535142"} "R566535142"]]]
       [:div.right-content
        [:p.commission-money "$47.28"]
        [:p.commission-label.shipped-label.top-pad "Shipped"]]]]

     [:div.commission-payment-history
      [:h4.dashboard-details-header "Commission Payment History"]
      [:div.solid-line-diveder]
      [:div.emphasized-banner
       [:span.emphasized-banner-header "Commissions Paid"]
       [:span.emphasized-banner-value "$255.87"]]
      [:div.loose-table-row.short-row
       [:div.left-content [:span "12/16/2014"]]
       [:div.right-content
        [:span.payout-amount "$58.18"]]]]]]])))
