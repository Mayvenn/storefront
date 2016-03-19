(ns storefront.components.stylist.commissions
  (:require [clojure.string :as str]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.accessors.products :as products]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.stylists :as stylists]
            [storefront.components.formatters :as f]
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

(defn show-item [data {:keys [product-name product-id id unit-price variant-attrs quantity] :as item}]
  [:.py2.clearfix
   [:.left
    [:img.border-top.border-bottom.border-gray.mr3
     {:style {:width "5rem"}
      :src   (first (products/thumbnail-urls data product-id))
      :alt   product-name}]]
   [:.overflow-hidden
    [:.h3.medium.titleize (products/product-title item)]
    [:.line-height-4.h4
     [:.mt1 "Length: " (:length variant-attrs)]
     [:div "Price: " (f/as-money unit-price)]
     [:div "Quantity: " quantity]]]])

(defn short-shipping-name [name]
  (str/replace name #"^Free " ""))

(defn show-order [data order]
  [:.bg-white.px2
   (for [item (orders/product-items order)]
     (show-item data item))

   (let [{shipping-name :options-text shipping-price :unit-price} (orders/shipping-item order)
         rows (concat [{:name "Subtotal" :price (orders/products-subtotal order)}]
                      (:adjustments order)
                      [{:name (short-shipping-name shipping-name) :price shipping-price}]
                      [(orders/tax-adjustment order)])]
     (for [{:keys [name price]} rows]
       [:.clearfix.mxn1.my2
        [:.px1.col.col-6 name]
        [:.px1.col.col-6.medium.right-align (f/as-money price)]]))

   [:.clearfix.mxn1.h2.mt3.pb2
    [:.px1.col.col-6 "Total"]
    [:.px1.col.col-6.right-align
     [:span.h4.mr1.gray "USD"]
     (f/as-money (:total order))]]])

(defn payout-bar [& content]
  [:.clearfix.bg-lighten-4.px2.py1
   [:.left "coins"]
   [:.overflow-hidden.center content]])

(defn show-commission
  [data {:keys [number amount status commission_date payout_date order] :as commission}]
  (let [order-open? true]
    (list
     [:.p2.border-bottom.border-right.border-white
      [:div
       (utils/route-to data events/navigate-order {:number number})
       [:.mb2
        [:.px1.h6.right.border.capped
         {:style {:padding-top "3px" :padding-bottom "2px"}
          :class (status-look status)}
         (when (= status "paid") "+") (f/as-money amount)]
        [:.h2 (:full-name order)]]

       [:.gray.h5.mb1
        (four-up "Status" "Ship Date" "Order" [:.right.h2.mtn1 {:class (if order-open? "gray" "black")}"..."])]]

      [:.medium.h5
       (four-up
        [:.titleize {:class (status-look status)} status]
        (f/locale-date commission_date)
        number
        nil)]]

     (when order-open?
       (list
        (show-order data order)
        [:.border-dotted-top.border-dotted-bottom.border-gray.h6
         (if (= status "paid")
           [:.bg-green
            (payout-bar
             (f/as-money amount) " paid on " (f/long-date payout_date))]
           [:.bg-blue
            (payout-bar
             (f/as-money amount) " has been added to your next payout.")])]) ))))

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

(def micro-dollar-sign
  [:svg {:viewbox "0 0 14 13", :height "13", :width "14"}
   [:g {:stroke-width "1" :stroke "#9B9B9B" :fill "none"}
    [:path {:d "M13 6.5c0 3.3-2.7 6-6 6s-6-2.7-6-6 2.7-6 6-6 6 2.7 6 6z"}]
    [:path {:d "M5.7 7.8c0 .72.58 1.3 1.3 1.3.72 0 1.3-.58 1.3-1.3 0-.72-.58-1.3-1.3-1.3-.72 0-1.3-.58-1.3-1.3 0-.72.58-1.3 1.3-1.3.72 0 1.3.58 1.3 1.3M7 3.1v6.8"}]]])

(defn stylist-commissions-component [data owner]
  (om/component
   (html
    (let [commissions (get-in data keypaths/stylist-commissions-history)
          commission-rate (get-in data keypaths/stylist-commissions-rate)]
      [:.mx-auto.container.border.border-white
       [:.clearfix {:data-test "commissions-panel"}
        [:.lg-col.lg-col-9
         (if (seq commissions)
           (for [commission commissions]
             (show-commission data commission))
           empty-commissions)]

        [:.lg-col.lg-col-3
         (when commission-rate
           (let [message (list "Earn " commission-rate "% commission on all sales. (tax and store credit excluded)")]
             [:.h6.muted
              ;; TODO: should we have a not-lg-hide?
              [:.p2.xs-hide.sm-hide.md-hide
               [:div.mb1.center micro-dollar-sign]
               [:div message]]
              [:.mt3.flex.justify-center.items-center.lg-hide
               [:.mr1 micro-dollar-sign]
               [:.center message]]]))]]]))))
