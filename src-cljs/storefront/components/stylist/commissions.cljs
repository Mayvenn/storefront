(ns storefront.components.stylist.commissions
  (:require [clojure.string :as str]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.formatters :as f]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn show-new-order [data new-order]
  [:div
   [:p (new-order :full-name)]
   [:p (f/locale-date (new-order :commission_date))]
   [:p [:a (utils/route-to data events/navigate-order {:number (new-order :number)})
        (new-order :number)]]
   (cond
     (and (= "complete" (new-order :state))
          (= "shipped" (new-order :shipment_state)))
     (html
      [:p (f/as-money (new-order :commissionable_amount))]
      [:p "Shipped"])

     (= "complete" (new-order :state))
     (html
      [:p "$_.__"]
      [:p "Pending"])

     :else
     [:p "Refunded"])])

(defn list-new-orders [data]
  (when-let [new-orders (->> (get-in data keypaths/stylist-commissions-new-orders)
                             (sort-by :commission_date)
                             reverse
                             not-empty)]
    (html
     [:div
      [:h4 "Recently shipped orders"]
      [:p "Orders that haven't shipped aren't shown."]
      [:p "Commission is earned when order ships."]
      [:div "Sale"]
      [:div "Commission"]
      (map (partial show-new-order data) new-orders)])))

(defn show-payout [data payout]
  [:div
   [:span (f/locale-date (payout :paid_at))]
   [:span (f/as-money (payout :amount))]])

(defn list-payouts [data]
  (when-let [paid-total (:amount (get-in data keypaths/stylist-stats-lifetime-payouts))]
    [:div
     [:h4 "Commission Payment History"]
     [:div
      [:span "Commissions Paid"]
      [:span (f/as-money paid-total)]]
     (map (partial show-payout data)
          (get-in data keypaths/stylist-commissions-payouts))]))

(defn stylist-commissions-component [data owner]
  (om/component
   (html
    [:div {:data-test "commissions-panel"}
     (when-let [next-commission-amount
                (:amount (get-in data keypaths/stylist-stats-next-payout))]
       [:p "As of today, your next commission payment is:"]
       [:p (f/as-money next-commission-amount)]

       [:p "W"]
       [:p "Commission paid on Wednesdays"])

     (when-let [commission-rate (get-in data keypaths/stylist-commissions-rate)]
       [:p "You earn " commission-rate "% commission on each new order "
        "shipped from your store excluding tax."])

     (list-new-orders data)
     (list-payouts data)])))
