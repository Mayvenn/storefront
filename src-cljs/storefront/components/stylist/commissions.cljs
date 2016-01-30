(ns storefront.components.stylist.commissions
  (:require [clojure.string :as str]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.formatters :as f]
            [storefront.components.stylist.nav :refer [stylist-dashboard-nav-component]]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn show-new-order [data new-order]
  [:.loose-table-row
   [:.left-content
    [:p (new-order :full-name)]
    [:p.top-pad (f/locale-date (new-order :commission_date))]
    [:p.top-pad [:a
                 (utils/route-to data events/navigate-order {:number (new-order :number)})
                 (new-order :number)]]]
   [:.right-content
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

(defn list-new-orders [data]
  (when-let [new-orders (->> (get-in data keypaths/stylist-commissions-new-orders)
                             not-empty
                             (sort-by :commission_date)
                             reverse)]
    (html
     [:.new-order-commissions
      [:h4.dashboard-details-header "Recently shipped orders"]
      [:p.commission-explanation "Orders that haven't shipped aren't shown."]
      [:p.commission-explanation "Commission is earned when order ships."]
      [:.solid-line-divider]
      [:.loose-table-header
       [:.left-header "Sale"]
       [:.right-header "Commission"]]
      (map (partial show-new-order data) new-orders)])))

(defn show-payout [data payout]
  [:.loose-table-row.short-row
   [:.left-content [:span (f/locale-date (payout :paid_at))]]
   [:.right-content
    [:span.payout-amount (f/as-money (payout :amount))]]])

(defn list-payouts [data]
  (when-let [paid-total (get-in data keypaths/stylist-commissions-paid-total)]
    [:.commission-payment-history
     [:h4.dashboard-details-header "Commission Payment History"]
     [:.solid-line-ider]
     [:.emphasized-banner
      [:span.emphasized-banner-header "Commissions Paid"]
      [:span.emphasized-banner-value (f/as-money paid-total)]]
     (map (partial show-payout data)
          (get-in data keypaths/stylist-commissions-payouts))]))

(defn stylist-commissions-component [data owner]
  (om/component
   (html
    [:main {:role "main"}
     [:.container
      [:h2.header-bar-heading.commissions "Commissions"]

      (om/build stylist-dashboard-nav-component data)
      [:.dashboard-content
       (when-let [next-commission-amount
                  (get-in data keypaths/stylist-commissions-next-amount)]
         [:#next-commission-summary.dashboard-summary

          [:.next-payout-description
           [:p "As of today, your next commission payment is:"]
           [:p.next-commissions-amount (f/as-money next-commission-amount)]]

          [:.next-payout-date-container
           [:p.accented-next-pay "W"]
           [:p.small-payout-description "Commission paid on Wednesdays"]]])

       (when-let [commission-rate (get-in data keypaths/stylist-commissions-rate)]
         [:#money-rules
          [:.gold-money-box]
          [:.money-rule-details
           [:p "You earn " commission-rate "% commission on each new order "
            "shipped from your store excluding tax."]]])

       (list-new-orders data)
       (list-payouts data)]]])))
