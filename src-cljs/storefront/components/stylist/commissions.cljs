(ns storefront.components.stylist.commissions
  (:require [clojure.string :as str]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.accessors.products :as products]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.stylists :as stylists]
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
   [:img.left.border-top.border-bottom.border-gray.mr3
    {:style {:width "5rem"}
     :src   (first (products/thumbnail-urls data product-id))
     :alt   product-name}]
   [:.overflow-hidden
    [:.h3.medium.titleize (products/product-title item)]
    [:.line-height-4.h4
     [:.mt1 "Length: " (:length variant-attrs)]
     [:div "Price: " (f/as-money unit-price)]
     [:div "Quantity: " quantity]]]])

(defn short-shipping-name [name]
  (str/replace name #"^Free " ""))

(defn show-subtotals [order]
  (let [{shipping-name  :options-text
         shipping-price :unit-price} (orders/shipping-item order)
        shipping-subtotal            {:name  (short-shipping-name shipping-name)
                                      :price shipping-price}
        quantity                     (orders/product-quantity order)
        product-subtotal             {:name  (goog.string/format
                                              "Subtotal (%s Item%s)"
                                              quantity
                                              (if (> quantity 1) "s" ""))
                                      :price (orders/products-subtotal order)}
        ;; Can't use (:adjustments order) because stylists cannot see
        ;; adjustments on their customer's orders
        discount-subtotals           (->> order
                                          orders/line-items
                                          (mapcat :applied-promotions)
                                          (map (fn [{:keys [amount promotion]}]
                                                 {(:name promotion) amount}))
                                          (apply merge-with +)
                                          (map (fn [[name amount]]
                                                 {:name name :price amount})))
        tax-subtotal                 (orders/tax-adjustment order)]
    (for [{:keys [name price]} (concat [product-subtotal]
                                       discount-subtotals
                                       [shipping-subtotal]
                                       [tax-subtotal])]
      [:.clearfix.mxn1.my2
       [:.px1.col.col-8 name]
       [:.px1.col.col-4.medium.right-align (f/as-money price)]])))

(defn show-grand-total [order]
  [:.clearfix.mxn1.h2.mt3.pb2
   [:.px1.col.col-6 "Total"]
   [:.px1.col.col-6.right-align
    [:span.h4.mr1.gray "USD"]
    (f/as-money (:total order))]])

(defn show-order [data order]
  [:.bg-white.px2
   (for [item (orders/product-items order)]
     (show-item data item))

   (show-subtotals order)
   (show-grand-total order)])

(defn payout-bar [& content]
  [:.clearfix.bg-lighten-4.px2.py1
   [:.left "coins"]
   [:.overflow-hidden.center content]])

(defn show-payout [{:keys [amount status payout_date]}]
  [:.border-dotted-top.border-dotted-bottom.border-gray.h6
   (if (= status "paid")
     [:.bg-green
      (payout-bar
       (f/as-money amount) " paid on " (f/long-date payout_date))]
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
                                 {:keys [number amount status commission_date order] :as commission}]
  [:.p2.border-bottom.border-right.border-white.pointer
   {:on-click (toggle-expanded-commission data number)}
   [:.mb2
    [:.px1.h6.right.border.capped
     {:style {:padding-top "3px" :padding-bottom "2px"}
      :class (status-look status)}
     (when (= status "paid") "+") (f/as-money amount)]
    [:.h2 (:full-name order)]]

   [:.gray.h5.mb1
    (four-up "Status" "Ship Date" "Order"
             [:.right.h2.mtn1
              {:class (if (commission-expanded? data number) "gray" "black")}
              "..."])]

   [:.medium.h5
    (four-up
     [:.titleize {:class (status-look status)} status]
     (f/locale-date commission_date)
     number
     nil)]])

(defn show-commission [data commission]
  (list
   (show-collapsed-commission data commission)

   [:div.transition-3.transition-ease-in-out.overflow-scroll
    {:style {:max-height (if (commission-expanded? data (:number commission))
                           "35rem"
                           "0px")}}
    (show-order data (:order commission))
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
     ;; TODO: should we have a not-lg-hide?
     [:.p2.xs-hide.sm-hide.md-hide
      [:.mb1.center svg/micro-dollar-sign]
      [:div message]]
     [:.mt3.flex.justify-center.items-center.lg-hide
      [:.mr1 svg/micro-dollar-sign]
      [:.center message]]]))

(defn stylist-commissions-component [data owner]
  (om/component
   (html
    [:.mx-auto.container.border.border-white {:data-test "commissions-panel"}
     [:.clearfix
      [:.lg-col.lg-col-9
       (let [commissions (get-in data keypaths/stylist-commissions-history)]
         (if (seq commissions)
           (for [commission commissions]
             (show-commission data commission))
           empty-commissions))]

      [:.lg-col.lg-col-3
       (when-let [commission-rate (get-in data keypaths/stylist-commissions-rate)]
         (show-commission-rate commission-rate))]]])))
