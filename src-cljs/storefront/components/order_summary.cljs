(ns storefront.components.order-summary
  (:require [storefront.components.formatters :refer [float-as-money]]))

(defn valid-payments [payments]
  (filter (comp not #{"failed" "invalid"} :state) payments))

(defn storecredit-payments [order]
  (filter #(= "Spree::StoreCredit" (:source_type %))
          (valid-payments (:payments order))))

(defn eligible-adjustments [adjustments]
  (filter :eligible adjustments))

(defn line-item-adjustments [order]
  (mapcat (comp eligible-adjustments :adjustments) (order :line_items)))

(defn whole-order-adjustments [order]
  (-> order :adjustments eligible-adjustments))

(defn all-order-adjustments [order]
  (concat (whole-order-adjustments order)
          (line-item-adjustments order)))

(defn line-item-promotion-adjustments [order]
  (filter (comp #{"Spree::PromotionAction"} :source_type)
          (line-item-adjustments order)))

(defn tax-adjustments [order]
  (filter (comp #{"Spree::TaxRate"} :source_type)
          (line-item-adjustments order)))

(defn display-adjustment-row [label adjustments]
  (let [summed-amount (reduce + (map (comp js/parseFloat :amount) adjustments))]
    (when-not  (= summed-amount 0)
      [:tr.order-summary-row.adjustment
       [:td
        [:h5 label]]
       [:td
        [:h5 (float-as-money summed-amount)]]])))

(defn display-adjustments [adjustments]
  (map #(apply display-adjustment-row %) (group-by :label adjustments)))

(defn display-shipment [shipment]
  [:tr.order-summary-row
   [:td
    [:h5 (-> shipment :selected_shipping_rate :name)]]
   [:td
    [:h5 (-> shipment :selected_shipping_rate :display_cost)]]])

(defn display-order-summary [order]
  [:div
   [:h4.order-summary-header "Order Summary"]
   [:table.order-summary-total
    (let [all-adjustments (all-order-adjustments order)
          quantity (:total_quantity order)]
      [:div
       (when-not (empty? all-adjustments)
         [:div
          [:tr.cart-subtotal.order-summary-row
           [:td
            [:h5 (str "Subtotal (" quantity " Item"
                      (when (> quantity 1) "s") ")")]]
           [:td
            [:h5 (:display_item_total order)]]]
          [:tbody#cart_adjustments
           (let [li-promotion-adjustments (line-item-promotion-adjustments order)
                 tax-adjustments (tax-adjustments order)
                 whole-order-adjustments (whole-order-adjustments order)]
             [:div
              (display-adjustments li-promotion-adjustments)
              (display-adjustments tax-adjustments)
              (map display-shipment (order :shipments))
              (display-adjustments whole-order-adjustments)])]])
       [:tr.cart-total.order-summary-row
        [:td [:h5 "Order Total"]]
        [:td [:h5 (:display_total order)]]]
       (when-not (empty? (storecredit-payments order))
         [:div
          [:tr.store-credit-used.order-summary-row.adjustment
           [:td [:h5 "Store Credit"]]
           [:td [:h5 (:display_total_applicable_store_credit order)]]]
          [:tr.balance-due.order-summary-row.cart-total
           [:td [:h5 (if (= "complete" (:state order)) "Amount charged" "Balance Due")]]
           [:td [:h5 (:display_order_total_after_store_credit order)]]]])])]])
