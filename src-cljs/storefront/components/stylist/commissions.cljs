(ns storefront.components.stylist.commissions
  (:require [clojure.string :as str]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.accessors.products :as products]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.stylist-urls :as stylist-urls]
            [storefront.components.stylist.pagination :as pagination]
            [storefront.components.money-formatters :as mf]
            [storefront.components.formatters :as f]
            [storefront.components.svg :as svg]
            [storefront.platform.component-utils :as utils]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.request-keys :as request-keys]
            [storefront.keypaths :as keypaths]))

(defn status-look [status]
  (case status
    "pending" "teal"
    "paid" "navy"))

(defn four-up [a b c d]
  [:.clearfix.mxn1
   [:.col.col-3.px1 a]
   [:.col.col-3.px1 b]
   [:.col.col-3.px1 c]
   [:.col.col-3.px1 d]])

(defn show-item [products {:keys [id product-name product-id unit-price variant-attrs quantity] :as item}]
  [:.py2.clearfix {:key id}
   [:img.left.border.border-silver.mr3
    {:style {:width "5rem"}
     :src   (products/thumbnail-url products product-id)
     :alt   product-name}]
   [:.overflow-hidden
    [:.h4.medium.titleize (products/product-title item)]
    [:.line-height-3.h5.mt1
     (when-let [length (:length variant-attrs)]
       [:div "Length: " length])
     [:div "Price: " (mf/as-money unit-price)]
     [:div "Quantity: " quantity]]]])

(defn short-shipping-name [shipping-methods shipping-item]
  (-> shipping-methods
      (orders/shipping-method-details shipping-item)
      :name
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
  (for [{:keys [name price]} (concat (product-subtotals order)
                                     (discount-subtotals order)
                                     (shipping-subtotals shipping-methods order)
                                     (store-credit-subtotals order))]
    [:.clearfix.mxn1.my2 {:key name}
     [:.px1.col.col-8
      {:class (when (neg? price) "teal")}
      name]
     [:.px1.col.col-4.medium.right-align
      {:class (if (neg? price) "teal" "navy")}
      (mf/as-money price)]]))

(defn show-grand-total [commissionable-amount]
  [:.h3.p2.col-12.right-align.navy.border-top.border-silver
   (mf/as-money commissionable-amount)])

(defn show-order [products shipping-methods order]
  [:.px2
   (for [item (orders/product-items order)]
     (show-item products item))

   (show-subtotals shipping-methods order)])

(defn payout-bar [& content]
  [:.bg-lighten-4.flex.items-center.px2.py1
   [:.img-coins-icon.bg-no-repeat.bg-contain {:style {:width "18px" :height "12px"}}]
   [:.center.flex-auto content]])

(defn show-payout [{:keys [amount status payout-date]}]
  [:.border-dotted-top.border-dotted-bottom.border-gray.h6
   (if (= status "paid")
     [:.bg-navy
      (payout-bar
       (mf/as-money amount) " paid on " (f/long-date payout-date))]
     [:.bg-teal
      (payout-bar
       (mf/as-money amount) " has been added to your next payment.")])])

(defn toggle-expanded-commission [expanded? number]
  (utils/send-event-callback events/control-commission-order-expand
                             {:number (when-not (expanded? number)
                                        number)}))

(defn show-collapsed-commission [expanded?
                                 {:keys [number amount status commission-date order]}]
  [:.p2.border-bottom.border-right.border-left.border-silver
   (when order
     {:class "pointer"
      :on-click (toggle-expanded-commission expanded? number)})
   [:.mb2
    [:.px1.h6.right.border.capped
     {:style {:padding-top "3px" :padding-bottom "2px"}
      :class (status-look status)}
     (when (= status "paid") "+") (mf/as-money amount)]
    [:.h3.navy (:full-name order)]]

   [:.light-gray.h6
    (four-up "Status" "Ship Date" "Order"
             (when order
               [:.right.h2.mtn2.mr1
                {:class (if (expanded? number) "gray" "dark-gray")}
                "..."]))]

   [:.medium.h6.line-height-3
    (four-up
     [:.titleize {:class (status-look status)} status]
     (f/short-date commission-date)
     number
     nil)]])

(defn transition-group [options & children]
  (apply js/React.createElement js/React.addons.CSSTransitionGroup (clj->js options) (html children)))

(defn show-commission [{:keys [id number order commissionable-amount] :as commission}
                       expanded?
                       shipping-methods
                       products]
  [:div {:key id}
   (show-collapsed-commission expanded? commission)
   (when order
     (transition-group {:transitionName "commission-order"
                        :transitionEnterTimeout 1000
                        :transitionLeaveTimeout 1000
                        :component "div"}
                       (when (expanded? number)
                         [:div.transition-3.transition-ease.overflow-auto.commission-order
                          [:.gray.bg-light-silver
                           (show-order products shipping-methods order)
                           (show-grand-total commissionable-amount)]
                          (show-payout commission)])))])

(def empty-commissions
  (html
   [:.center
    [:.p2.border-bottom.border-silver
     [:.img-receipt-icon.bg-no-repeat.bg-center {:style {:height "8em"}}]
     [:p.h3.light-gray "Looks like you don't have any commissions yet."]]
    [:.py3.h4
     [:p.mx4.pb2 "Get started by sharing your store with your clients:"]
     [:p.medium stylist-urls/store-url]]]))

(defn show-commission-rate [rate]
  (let [message (list "Earn " rate "% commission on all sales. (tax and store credit excluded)")]
    [:.h6.light-gray
     [:.p2.to-sm-hide
      [:.mb1.center svg/micro-dollar-sign]
      [:div message]]
     [:.my3.flex.justify-center.items-center.sm-up-hide
      [:.mr1 svg/micro-dollar-sign]
      [:.center message]]]))

(defn component [{:keys [commissions expanded? shipping-methods products fetching?]}]
  (om/component
   (let [{:keys [history page pages rate]} commissions]
     (html
      (if (and (empty? (seq history)) fetching?)
        [:.my2.h2 ui/spinner]
        [:.mx-auto.container {:data-test "commissions-panel"}
         [:.clearfix
          [:.sm-up-col.sm-up-col-9
           (when-let [history (seq history)]
             [:.mb3
              (for [commission history]
                (show-commission commission expanded? shipping-methods products))
              (pagination/fetch-more events/control-stylist-commissions-fetch fetching? page pages)])
           (when (zero? pages) empty-commissions)]

          [:.sm-up-col.sm-up-col-3
           (when rate (show-commission-rate rate))]]])))))

(defn query [data]
  {:commissions      (get-in data keypaths/stylist-commissions)
   :expanded?        (get-in data keypaths/expanded-commission-order-id)
   :shipping-methods (get-in data keypaths/shipping-methods)
   :products         (get-in data keypaths/products)
   :fetching?        (utils/requesting? data request-keys/get-stylist-commissions)})
