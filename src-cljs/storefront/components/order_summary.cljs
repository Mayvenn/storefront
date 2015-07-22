(ns storefront.components.order-summary
  (:require [storefront.components.formatters :refer [as-money]]
            [storefront.components.utils :as utils]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [clojure.string :as string]
            [storefront.experiments :as experiments]
            [storefront.components.counter :refer [counter-component]]
            [storefront.request-keys :as request-keys]
            [storefront.keypaths :as keypaths]))

(defn field [name value & [classes]]
  [:div.line-item-attr {:class classes}
   [:span.cart-label name]
   [:span.cart-value value]])

(defn display-variant-options [option-value]
  (field (str (:option_type_presentation option-value) ": ")
         (:presentation option-value)))

(defn display-line-item [data interactive? line-item]
  (let [variant (:variant line-item)]
    [:div.line-item
     [:a
      (utils/route-to data events/navigate-product {:product-path (:slug variant)})
      [:img {:src (-> variant :images first :small_url)
             :alt (:name variant)}]]
     [:div.line-item-detail.interactive
      [:h4
       [:a
        (utils/route-to data events/navigate-product {:product-path (:slug variant)})
        (:name variant)]]
      (map display-variant-options (:option_values variant))
      (when (not interactive?)
        (field "Quantity:" (:quantity line-item)))
      (field "Price:" (:single_display_amount line-item) "item-form" "price")
      (field "Subtotal: " (:single_display_amount line-item) "item-form" "subtotal")
      (when interactive?
        (let [update-spinner-path (concat keypaths/api-requests
                                          (conj request-keys/update-line-item (:id line-item)))
              delete-spinner-path (concat keypaths/api-requests
                                          (conj request-keys/delete-line-item (:id line-item)))
              delete-spinning (get-in data delete-spinner-path)]
          (list
           (om/build counter-component
                     data
                     {:opts {:path (conj keypaths/cart-quantities (:id line-item))
                             :inc-event events/control-cart-line-item-inc
                             :dec-event events/control-cart-line-item-dec
                             :set-event events/control-cart-line-item-set
                             :spinner-path update-spinner-path }})
           [:a.delete
            {:href "#"
             :class (when delete-spinning "saving")
             :on-click (utils/send-event-callback data
                                                  events/control-cart-remove
                                                  (select-keys line-item [:id]))}
            "Remove"])))]
     [:div {:style {:clear "both"}}]]))

(defn display-line-items [data order & [interactive?]]
  (map (partial display-line-item data interactive?) (:line_items order)))

(defn valid-payments [payments]
  (filter (comp not #{"failed" "invalid"} :state) payments))

(defn storecredit-payments [order]
  (filter #(= "Spree::StoreCredit" (:source_type %))
          (valid-payments (:payments order))))

(defn eligible-adjustments [adjustments]
  (filter :eligible adjustments))

(defn line-item-adjustments [order]
  (mapcat (comp eligible-adjustments :adjustments) (:line_items order)))

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
        [:h5 (as-money summed-amount)]]])))

(defn display-adjustments [adjustments]
  (map #(apply display-adjustment-row %) (group-by :display_label adjustments)))

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
      [:tbody
       (when-not (empty? all-adjustments)
         (list
          [:tr.cart-subtotal.order-summary-row
           [:td
            [:h5 (str "Subtotal (" quantity " Item"
                      (when (> quantity 1) "s") ")")]]
           [:td
            [:h5 (:display_item_total order)]]]
          (let [li-promotion-adjustments (line-item-promotion-adjustments order)
                tax-adjustments (tax-adjustments order)
                whole-order-adjustments (whole-order-adjustments order)]
            (list
             (display-adjustments li-promotion-adjustments)
             (display-adjustments tax-adjustments)
             (map display-shipment (order :shipments))
             (display-adjustments whole-order-adjustments)))))
       [:tr.cart-total.order-summary-row
        [:td [:h5 "Order Total"]]
        [:td [:h5 (:display_total order)]]]
       (when-not (empty? (storecredit-payments order))
         (list
          [:tr.store-credit-used.order-summary-row.adjustment
           [:td [:h5 "Store Credit"]]
           [:td [:h5 (:display_total_applicable_store_credit order)]]]
          [:tr.balance-due.order-summary-row.cart-total
           [:td [:h5 (if (= "complete" (:state order)) "Amount charged" "Balance Due")]]
           [:td [:h5 (:display_order_total_after_store_credit order)]]]))])]])
