(ns storefront.components.stylist.commissions
  (:require [clojure.string :as str]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.accessors.products :as products]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.stylists :as stylists]
            [storefront.components.stylist.pagination :as pagination]
            [storefront.components.formatters :as f]
            [storefront.components.svg :as svg]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn status-look [status]
  (case status
    "pending" "teal"
    "paid" "green"))

(defn four-up [a b c d]
  [:.clearfix.mxn1
   [:.col.col-3.px1 a]
   [:.col.col-3.px1 b]
   [:.col.col-3.px1 c]
   [:.col.col-3.px1 d]])

(defn show-item [data {:keys [product-name product-id unit-price variant-attrs quantity] :as item}]
  [:.py2.clearfix
   [:img.left.border.border-silver.mr3
    {:style {:width "5rem"}
     :src   (first (products/thumbnail-urls data product-id))
     :alt   product-name}]
   [:.overflow-hidden
    [:.h3.medium.titleize (products/product-title item)]
    [:.line-height-3.h4.mt1
     [:div "Length: " (:length variant-attrs)]
     [:div "Price: " (f/as-money unit-price)]
     [:div "Quantity: " quantity]]]])

(defn short-shipping-name [shipping-methods shipping-item]
  (-> shipping-methods
      (orders/shipping-method-details shipping-item)
      :name
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

(defn shipping-subtotals [shipping-methods order]
  (when (seq shipping-methods)
    (let [shipping-item (orders/shipping-item order)]
      [{:name  (short-shipping-name shipping-methods shipping-item)
        :price (:unit-price shipping-item)}])))

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

(defn show-subtotals [shipping-methods order]
  (for [{:keys [name price]} (remove nil? (concat (product-subtotals order)
                                                  (discount-subtotals order)
                                                  (shipping-subtotals shipping-methods order)
                                                  (store-credit-subtotals order)))]
    [:.clearfix.mxn1.my2
     [:.px1.col.col-8 name]
     [:.px1.col.col-4.medium.right-align (f/as-money price)]]))

(defn show-grand-total [commissionable-amount]
  [:.h2.mt1.py2.col-12.right-align
   [:span.h4.mr1.gray "USD"]
   (f/as-money commissionable-amount)])

(defn show-order [data {:keys [order commissionable-amount]}]
  [:.bg-white.px2
   (for [item (orders/product-items order)]
     (show-item data item))

   (show-subtotals (get-in data keypaths/shipping-methods) order)
   (show-grand-total commissionable-amount)])

(defn payout-bar [& content]
  [:.bg-lighten-4.flex.items-center.px2.py1
   [:.img-coins-icon.bg-no-repeat {:style {:width "18px" :height "12px"}}]
   [:.center.flex-auto content]])

(defn show-payout [{:keys [amount status payout-date]}]
  [:.border-dotted-top.border-dotted-bottom.border-gray.h6
   (if (= status "paid")
     [:.bg-green
      (payout-bar
       (f/as-money amount) " paid on " (f/long-date payout-date))]
     [:.bg-blue
      (payout-bar
       (f/as-money amount) " has been added to your next payout.")])])

(defn commission-expanded? [data number]
  (= number
     (get-in data keypaths/expanded-commission-order-id)))

(defn toggle-expanded-commission [data number]
  (utils/send-event-callback data
                             events/control-commission-order-expand
                             {:number (when-not (commission-expanded? data number)
                                        number)}))

(defn show-collapsed-commission [data
                                 {:keys [number amount status commission-date order] :as commission}]
  [:.p2.border-bottom.border-right.border-white.pointer
   {:on-click (toggle-expanded-commission data number)}
   [:.mb2
    [:.px1.h5.right.border.capped
     {:style {:padding-top "3px" :padding-bottom "2px"}
      :class (status-look status)}
     (when (= status "paid") "+") (f/as-money amount)]
    [:.h2 (:full-name order)]]

   [:.gray.h5
    (four-up "Status" "Ship Date" "Order"
             [:.right.h1.mtn2.mr1
              {:class (if (commission-expanded? data number) "gray" "black")}
              "..."])]

   [:.medium.h5.line-height-3
    (four-up
     [:.titleize {:class (status-look status)} status]
     (f/short-date commission-date)
     number
     nil)]])

(defn show-commission [data commission]
  (list
   (show-collapsed-commission data commission)

   [:div.transition-2.transition-ease.overflow-auto
    {:style {:max-height (if (commission-expanded? data (:number commission))
                           "35rem"
                           "0px")}}
    (show-order data commission)
    (show-payout commission) ]))

(def empty-commissions
  (html
   [:.center
    [:.p2
     [:.img-receipt-icon.bg-no-repeat.bg-center {:style {:height "8em" }}]
     [:p.h2.gray.muted "Looks like you don't have any commissions yet."]]
    [:hr.border.border-white ]
    [:.py3.h3
     [:p.mx4.pb2 "Get started by sharing your store with your clients:"]
     [:p.medium stylists/store-url]]]))

(defn show-commission-rate [rate]
  (let [message (list "Earn " rate "% commission on all sales. (tax and store credit excluded)")]
    [:.h6.muted
     [:.p2.to-sm-hide
      [:.mb1.center svg/micro-dollar-sign]
      [:div message]]
     [:.my3.flex.justify-center.items-center.sm-up-hide
      [:.mr1 svg/micro-dollar-sign]
      [:.center message]]]))

(defn stylist-commissions-component [data owner]
  (om/component
   (html
    [:.mx-auto.container.border.border-white {:data-test "commissions-panel"}
     [:.clearfix
      [:.sm-col.sm-col-9
       (let [commissions (get-in data keypaths/stylist-commissions-history)]
         (when (seq commissions)
           [:div.mb3
            (for [commission commissions]
              (show-commission data commission))
            (pagination/fetch-more
             data
             events/control-stylist-commissions-fetch
             (get-in data keypaths/stylist-commissions-page)
             (get-in data keypaths/stylist-commissions-pages))]))
       (when (zero? (get-in data keypaths/stylist-commissions-pages))
         empty-commissions)]

      [:.sm-col.sm-col-3
       (when-let [commission-rate (get-in data keypaths/stylist-commissions-rate)]
         (show-commission-rate commission-rate))]]])))
