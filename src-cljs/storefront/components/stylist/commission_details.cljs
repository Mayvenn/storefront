(ns storefront.components.stylist.commission-details
  (:require [clojure.string :as str]
            [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.assets :as assets]
            [storefront.accessors.products :as products]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.stylist-urls :as stylist-urls]
            [storefront.accessors.images :as images] [storefront.components.stylist.pagination :as pagination]
            [storefront.components.money-formatters :as mf]
            [storefront.components.formatters :as f]
            [storefront.components.svg :as svg]
            [storefront.components.order-summary :as summary]
            [storefront.platform.component-utils :as utils]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.request-keys :as request-keys]
            [storefront.keypaths :as keypaths]
            [storefront.utils.query :as query]
            [goog.string]
            [goog.userAgent.product :as product]
            [spice.core :as spice]
            [spice.date :as date]
            [storefront.component :as component]))

(defn status-look [status]
  (case status
    "pending"    "teal"
    "paid"       "navy"
    "processing" "teal"))

(defn show-item [products skus {:keys [id sku product-id unit-price quantity] :as line-item}]
  (let [product  (get products sku)
        full-sku (get skus sku)]
    [:div.py2.clearfix {:key id}
     [:img.left.border.border-light-gray.mr3
      (assoc (images/cart-image full-sku)
             :style {:width "5rem"})]
     [:div.overflow-hidden
      [:div.h4.medium.titleize (:sku/title full-sku)]
      [:div.h5.mt1
       (when-let [length (:hair/length sku)]
         [:div "Length: " length])
       [:div "Price: " (mf/as-money unit-price)]
       [:div "Quantity: " quantity]]]]))

(defn short-shipping-name [shipping-item]
  (-> shipping-item
      :product-name
      str
      (str/replace #"^Free " "")))

(defn store-credit-subtotals [order]
  (let [subtotal (->> order
                      :payments
                      (filter #(and (= (:payment-type %) "store-credit")
                                    (= (:shipment-number %) "S1")))
                      (map :amount)
                      (reduce + 0))]
    (when (pos? subtotal)
      [{:name "Store Credit"
        :price (- subtotal)}])))

(defn product-subtotals [order]
  (let [quantity (orders/product-quantity order)]
    [{:name  (goog.string/format
              "Subtotal (%s Item%s)"
              quantity
              (if (> quantity 1) "s" ""))
      :price (orders/products-subtotal order)}]))

(defn shipping-subtotals [order]
  (let [shipping-item (orders/shipping-item order)]
    [{:name  (short-shipping-name shipping-item)
      :price (:unit-price shipping-item)}]))

(defn discount-subtotals [order]
  ;; Can't use (:adjustments order) because stylists cannot see
  ;; adjustments on their customer's orders
  (->> order
       orders/line-items
       (mapcat :applied-promotions)
       (map (fn [{:keys [amount promotion]}]
              {(:name promotion) amount}))
       (apply merge-with +)
       (map (fn [[name amount]]
              {:name name :price amount}))))

(defn show-subtotals [order]
  (for [{:keys [name price]} (concat (product-subtotals order)
                                     (discount-subtotals order)
                                     (shipping-subtotals order)
                                     (store-credit-subtotals order))]
    [:div.clearfix.mxn1.my2 {:key name}
     [:div.px1.col.col-8
      {:class (when (neg? price) "teal")}
      name]
     [:div.px1.col.col-4.medium.right-align
      {:class (if (neg? price) "teal" "navy")}
      (mf/as-money price)]]))

(defn show-grand-total [commissionable-amount]
  [:div.h3.p2.col-12.right-align.navy.border-top.border-light-gray
   (mf/as-money commissionable-amount)])

(defn show-order [products skus order]
  [:div.px2
   (for [item (orders/product-items order)]
     (show-item products skus item))

   (show-subtotals order)])

(defn payout-bar [& content]
  [:div.bg-lighten-4.flex.items-center.px2.py1
   [:div.img-coins-icon.bg-no-repeat.bg-contain {:style {:width "18px" :height "12px"}}]
   [:div.center.flex-auto content]])

;; Going to be transfers
(defn show-payout [{:keys [amount status payout-date]}]
  [:div.border-dotted-top.border-dotted-bottom.border-dark-gray.h5
   (case status
     "paid"
     [:div.bg-aqua
      (payout-bar
       (mf/as-money amount) " paid on " (f/long-date payout-date))]

     "processing"
     [:div.bg-teal
      (payout-bar
       (mf/as-money amount) " is in the process of being paid.")]

     [:div.bg-teal
      (payout-bar
       (mf/as-money amount) " has been added to your next payment.")])])

(defn all-skus-in-commission [skus commission]
  (->> (:order commission)
       orders/product-items
       (mapv :sku)
       (select-keys skus)))

(defn query [data]
  (let [commission          (get-in data keypaths/stylist-commissions-detailed-commission)
        skus-for-commission (all-skus-in-commission (get-in data keypaths/v2-skus)
                                                    commission)]
    {:commission commission
     :fetching? (utils/requesting? data request-keys/get-stylist-commission)
     :skus       skus-for-commission}))

(def back-caret
  (component/html
   [:div.inline-block.pr1
    (svg/left-caret {:class  "stroke-gray align-middle"
                     :width  "15px"
                     :height "1.5rem"})]))

(defn component [{:keys [commission fetching? skus]} owner opts]
  (let [{:keys [id number order amount commission-date commissionable-amount]} commission
        ship-date (f/less-year-more-day-date (date/to-iso (->> order
                                                               :shipments
                                                               first
                                                               :shipped-at)))]
    (component/create
     (if fetching?
       [:div.my2.h2 ui/spinner]

       [:div.container.mb4.px3
        [:a.left.col-12.dark-gray.flex.items-center.py3
         (utils/route-to events/navigate-stylist-dashboard-earnings)
         back-caret
         "back to earnings"]
        [:h3.my4 "Details - Commission Earned"]
        [:div.flex.justify-between.col-12
         [:div (f/less-year-more-day-date commission-date)]
         [:div (:full-name order)]
         [:div.green "+" (mf/as-money amount)]]

        [:div.col-12
         [:div.col-4.inline-block
          [:span.h6.dark-gray "Order Number"]
          [:div.h6 (:number order)]]
         [:div.col-8.inline-block
          [:span.h6.dark-gray "Ship Date"]
          [:div.h6 ship-date]]]

        [:div.mt2.mbnp2.mtnp2.border-top.border-gray
         (summary/display-line-items (orders/product-items order) skus)]

        (summary/display-order-summary order {:read-only? true})

        [:div.h5.center
         (str (mf/as-money amount) " has been added to your next payment.")]]))))

(defn built-component [data opts]
  (component/build component (query data) opts))
