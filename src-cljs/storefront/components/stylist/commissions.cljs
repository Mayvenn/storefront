(ns storefront.components.stylist.commissions
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.stylist.nav :refer [stylist-dashboard-nav-component]]
            [storefront.components.formatters :as f]
            [storefront.state :as state]))

(defn display-new-order [new-order]
  [:div.loose-table-row
   [:div.left-content
    [:p (new-order :fullname)]
    [:p.top-pad (f/locale-date (new-order :completed_at))]
    [:p.top-pad [:a {:href (str "/orders/" (new-order :number))} (new-order :number)]]]
   [:div.right-content
    (cond
      (and (= "complete" (new-order :state))
           (= "shipped" (new-order :shipment_state)))
      (html
       [:p.commission-money (f/as-money (new-order :commissionable_amount))]
       [:p.commission-label.shipped-label.top-pad "Shipped"])

      (= "complete" (new-order :state))
      (html
       [:p.commission-money "$_.__"]
       [:p.commission-label.pending-label.top-pad "Pending"])

      :else
       [:p.commission-label.refunded-label "Refunded"])]])

(defn display-new-orders [new-orders]
  (html
   [:div.new-order-commissions
    [:h4.dashboard-details-header "New Orders"]
    [:p.commission-explanation "Commission is earned when order ships."]
    [:div.solid-line-divider]
    [:div.loose-table-header
     [:div.left-header "Sale"]
     [:div.right-header "Commission"]]
    (map display-new-order new-orders)]))

(defn display-payout [payout]
  [:div.loose-table-row.short-row
   [:div.left-content [:span (f/locale-date (payout :paid_at))]]
    [:div.right-content
     [:span.payout-amount (f/as-money (payout :amount))]]])

(defn display-payouts [paid-total payouts]
  [:div.commission-payment-history
   [:h4.dashboard-details-header "Commission Payment History"]
   [:div.solid-line-diveder]
   [:div.emphasized-banner
    [:span.emphasized-banner-header "Commissions Paid"]
    [:span.emphasized-banner-value (f/as-money paid-total)]]
   (map display-payout payouts)])

(defn stylist-commissions-component [data owner]
  (om/component
   (html
    [:main {:role "main"}
     [:div.container
      [:h2.header-bar-heading.commissions "Commissions"]

      (om/build stylist-dashboard-nav-component data)

      [:div.dashboard-content
       [:div#next-commission-summary.dashboard-summary

        [:div.next-payout-description
         [:p "As of today, your next commission payment is:"]
         [:p.next-commissions-amount
          (f/as-money (get-in data state/stylist-commissions-next-amount-path))]]

        [:div.next-payout-date-container
         [:p.accented-next-pay "W"]
         [:p.small-payout-description "Commission paid on Wednesdays"]]]

       [:div#money-rules
        [:div.gold-money-box]
        [:div.money-rule-details
         [:p
          "You earn "
          (get-in data state/stylist-commissions-rate-path)
          "% commission on each new order shipped from your store excluding tax and shipping."]]]

       (display-new-orders (get-in data state/stylist-commissions-new-orders-path))
       (display-payouts (get-in data state/stylist-commissions-paid-total-path)
                        (get-in data state/stylist-commissions-payouts-path))]]])))
