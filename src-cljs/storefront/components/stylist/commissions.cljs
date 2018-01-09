(ns storefront.components.stylist.commissions
  (:require [clojure.string :as str]
            [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.accessors.products :as products]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.stylist-urls :as stylist-urls]
            [storefront.accessors.images :as images]
            [storefront.components.stylist.pagination :as pagination]
            [storefront.components.money-formatters :as mf]
            [storefront.components.formatters :as f]
            [storefront.components.svg :as svg]
            [storefront.platform.component-utils :as utils]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.request-keys :as request-keys]
            [storefront.keypaths :as keypaths]
            [storefront.utils.query :as query]
            [goog.string]
            [goog.userAgent.product :as product]
            [spice.core :as spice]))

(defn status-look [status]
  (case status
    "pending"   "teal"
    "paid"      "navy"
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

(defn toggle-expanded-commission [expanded? number]
  (utils/send-event-callback events/control-commission-order-expand
                             {:number (when-not (expanded? number)
                                        number)}))

(defn show-collapsed-commission [expanded?
                                 {:keys [number amount status commission-date order]}]
  [:div.p2.border-bottom.border-right.border-left.border-light-gray
   (when order
     {:class "pointer"
      :on-click (toggle-expanded-commission expanded? number)})
   [:div.mb2
    [:div.px1.h6.right.border.capped
     {:style {:padding-top "3px" :padding-bottom "2px"}
      :class (status-look status)}
     (when (= status "paid") "+") (mf/as-money amount)]
    [:div.h3.navy (:full-name order)]]

   [:div.dark-gray.h6.flex.justify-between
    [:div "Status" [:div.medium.titleize {:class (status-look status)} status]]
    [:div "Ship Date" [:div.medium (f/short-date commission-date)]]
    [:div "Order" [:div.medium number]]
    [:div (when order
            [:div.medium.h2
             {:class (if (expanded? number) "gray" "dark-gray")}
             "..."])]]])

(defn transition-group [options & children]
  (apply js/React.createElement js/ReactTransitionGroup.CSSTransitionGroup (clj->js options) (html children)))

(defn show-commission [{:keys [id number order commissionable-amount] :as commission}
                       expanded?
                       products
                       skus]
  [:div {:key id}
   (show-collapsed-commission expanded? commission)
   (when order
     (transition-group {:transitionName "commission-order"
                        :transitionEnterTimeout 1000
                        :transitionLeaveTimeout 1000
                        :component "div"}
                       (when (expanded? number)
                         [:div.transition-3.transition-ease.overflow-auto.commission-order
                          [:.dark-gray.bg-light-gray
                           (show-order products skus order)
                           (show-grand-total commissionable-amount)]
                          (show-payout commission)])))])

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

(defn component [{:keys [commissions expanded? products skus fetching?]}]
  (om/component
   (let [{:keys [history page pages rate]} commissions]
     (html
      (if (and (empty? (seq history)) fetching?)
        [:div.my2.h2 ui/spinner]
        [:div.clearfix
         {:data-test "earnings-panel"}
         [:div.col-on-tb-dt.col-9-on-tb-dt
          (when-let [history (seq history)]
            [:div.mb3
             (for [commission history]
               (show-commission commission expanded? products skus))
             (pagination/fetch-more events/control-stylist-commissions-fetch fetching? page pages)])
          (when (zero? pages) empty-commissions)]
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
        commission-skus (all-skus-in-commissions commissions)
        products        (get-in data keypaths/v2-products)
        skus            (select-keys (get-in data keypaths/v2-skus) commission-skus)
        products        (into {}
                              (map (fn [sku-id]
                                     [sku-id (products/find-product-by-sku-id products sku-id)]))
                              (all-skus-in-commissions commissions))]
    {:commissions commissions
     :expanded?   (get-in data keypaths/expanded-commission-order-id)
     :products    products
     :skus        skus
     :fetching?   (utils/requesting? data request-keys/get-stylist-commissions)}))
