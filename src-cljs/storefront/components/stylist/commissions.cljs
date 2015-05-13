(ns storefront.components.stylist.commissions
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.state :as state]))

(defn display-new-order [data new-order]
  [:div.loose-table-row
   [:div.left-content
    [:p (new-order :fullname)]
    [:p.top-pad (new-order :completed_at)] ;;"05/08/2015"]
    [:p.top-pad [:a {:href (str "/orders/" (new-order :number))} (new-order :number)]]]
   [:div.right-content
    (cond
      (and (= "complete" (new-order :state))
           (= "shipped" (new-order :shipment_state)))
      (html
       [:p.commission-money (str "$" (new-order :commissionable_amount))]
       [:p.commission-label.shipped-label.top-pad "Shipped"])

      (= "complete" (new-order :state))
      (html
       [:p.commission-money "$_.__"]
       [:p.commission-label.pending-label.top-pad "Pending"])

      :else
       [:p.commission-label.refunded-label "Refunded"])]])

(defn display-new-orders [data new-orders]
  (html
   [:div.new-order-commissions
    [:h4.dashboard-details-header "New Orders"]
    [:p.commission-explanation "Commission is earned when order ships."]
    [:div.solid-line-divider]
    [:div.loose-table-header
     [:div.left-header "Sale"]
     [:div.right-header "Commission"]]
    (map (partial display-new-order data) new-orders)]))

(defn display-pay-outs [data pay-outs]
  [:div.commission-payment-history
   [:h4.dashboard-details-header "Commission Payment History"]
   [:div.solid-line-diveder]
   [:div.emphasized-banner
    [:span.emphasized-banner-header "Commissions Paid"]
    [:span.emphasized-banner-value "$255.87"]]
   [:div.loose-table-row.short-row
    [:div.left-content [:span "12/16/2014"]]
    [:div.right-content
     [:span.payout-amount "$58.18"]]]])

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

       (display-new-orders data
                           (get-in data state/stylist-commissions-new-orders-path))

       (display-pay-outs data
                         (get-in data state/stylist-commissions-pay-outs-path))]]])))
