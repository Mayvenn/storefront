(ns storefront.components.stylist.earnings
  (:require [clojure.string :as str]
            goog.string
            [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.accessors.images :as images]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.stylist-urls :as stylist-urls]
            [storefront.components.formatters :as f]
            [storefront.components.money-formatters :as mf]
            [storefront.components.stylist.pagination :as pagination]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [storefront.platform.messages :as messages]))

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

(defn earnings-table [history]
  [:table.col-12.mb3 {:style {:border-spacing 0}}
   [:tbody
    (map-indexed
     (fn [i {:keys [id number order amount commission-date commissionable-amount] :as commission}]
       [:tr (merge {:key id}
                   (utils/route-to events/navigate-stylist-dashboard-commission-details {:commission-id id})
                   (when (odd? i)
                     {:class "bg-too-light-teal"}))
        [:td.px3.py2 (f/less-year-more-day-date commission-date)]
        [:td.py2 (:full-name order) [:div.h7 "Commission Earned"]]
        [:td.pr3.py2.green.right-align "+" (mf/as-money amount)]])
     history)]])

(def empty-commissions
  (html
   [:div.center
    [:div.p2.border-bottom.border-light-gray
     [:div.img-receipt-icon.bg-no-repeat.bg-center {:style {:height "8em"}}]
     [:p.h3.gray "Looks like you don't have any commissions yet."]]
    [:.py3.h4
     [:p.mx4.pb2 "Get started by sharing your store with your clients:"]
     [:p.medium stylist-urls/store-url]]]))

(defn show-commission-rate [rate]
  (let [message (list "Earn " rate "% commission on all sales. (tax and store credit excluded)")]
    [:div.h6.dark-gray
     [:div.p2.hide-on-mb
      [:div.mb1.center svg/micro-dollar-sign]
      [:div message]]
     [:div.my3.hide-on-tb-dt
      [:div.center message]]]))

(def show-program-terms
  [:div.col-on-tb-dt.col-3-on-tb-dt
   [:div.border-top.border-gray.mx-auto.my2 {:style {:width "100px"}}]
   [:div.center.my2.h6
    [:a.dark-gray (utils/route-to events/navigate-content-program-terms) "Mayvenn Program Terms"]]])

(defn component [{:keys [commissions products skus fetching?]}]
  (om/component
   (let [{:keys [history page pages rate]} commissions]
     (html
      (if (and (empty? (seq history)) fetching?)
        [:div.my2.h2 ui/spinner]
        [:div.clearfix
         {:data-test "earnings-panel"}
         [:div.col-on-tb-dt.col-9-on-tb-dt
          (when (seq history)
            (earnings-table history))
          (pagination/fetch-more events/control-stylist-commissions-fetch fetching? page pages)
          (when (zero? pages)
            empty-commissions)]

         [:div.col-on-tb-dt.col-3-on-tb-dt
          (when rate (show-commission-rate rate))]
         show-program-terms])))))

(defn all-skus-in-commissions [commissions]
  (->> (:history commissions)
       (map :order)
       (mapcat orders/product-items)
       (map :sku)))

(defn query [data]
  (let [commissions     (get-in data keypaths/stylist-commissions)
        commission-skus (all-skus-in-commissions commissions)]
    {:commissions commissions
     :fetching?   (utils/requesting? data request-keys/get-stylist-commissions)}))
