(ns storefront.components.stylist.balance-transfer-details
  (:require [checkout.cart :as cart]
            [ui.molecules :as ui-molecules]
            [storefront.accessors.orders :as orders]
            [storefront.component :as component]
            [storefront.components.formatters :as f]
            [storefront.components.money-formatters :as mf]
            [storefront.components.stylist.line-items :as line-items]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.transitions :as transitions]
            [storefront.effects :as effects]
            [storefront.request-keys :as request-keys]
            [storefront.api :as api]))

;; TODO Remove handling of underscored keys after storeback has been deployed.

(defn ^:private info-block [header content]
  [:div.align-top.mb2
   [:span.h6.dark-gray.shout.nowrap header]
   [:div.h6.medium (or content "--")]])

(defn ^:private info-columns [[left-header left-content] [right-header right-content]]
  [:div.col-12.pt2
   [:div.inline-block.col-6
    (info-block left-header left-content)]
   [:div.inline-block.col-6
    (info-block right-header right-content)]])

(defn ^:private display-line-item
  ([line-item] (display-line-item line-item true))
  ([{:keys [product-title color-name unit-price quantity sku id legacy/variant-id variant-attrs]} show-price?]
   [:div.h6.pb2 {:key (or variant-id id)}
    [:div.medium {:data-test (str "line-item-title-" sku)} product-title]
    [:div {:data-test (str "line-item-color-" sku)} color-name]
    (when show-price?
      [:div {:data-test (str "line-item-price-ea-" sku)} "Price: " (mf/as-money-without-cents unit-price) " ea"])
    [:div
     (when-let [length (:length variant-attrs)]
       [:span {:data-test (str "line-item-length-" sku)} length "” " ])
     [:span {:data-test (str "line-item-quantity-" sku)} "(Qty: " quantity ")"]]]))

(defn ^:private summary-row
  ([name amount] (summary-row {} name amount))
  ([row-attrs name amount]
   [:div.h6.pb1.clearfix
    row-attrs
    [:div.col.col-6 name]
    [:div.col.col-6.right-align (mf/as-money-or-free amount)]]))

(defn display-order-summary-for-commissions [order commissionable-amount]
  (let [adjustments       (:adjustments order)
        store-credit-used (:total-store-credit-used order)
        shipping-item     (orders/shipping-item order)
        subtotal          (orders/commissioned-products-subtotal order)
        shipping-total    (* (:quantity shipping-item) (:unit-price shipping-item))]
    [:div.pt2.pb6.border-top.border-gray
     (summary-row "Subtotal" subtotal)

     (for [{:keys [name price coupon-code]} adjustments]
       (when (or (not (= price 0))
                 (#{"amazon" "freeinstall" "install"} coupon-code))
         (summary-row {:key name} [:div (orders/display-adjustment-name name)] price)))

     (when (pos? store-credit-used)
       (summary-row "Store Credit" (- store-credit-used)))

     (when shipping-item
       (summary-row "Shipping" shipping-total))

     (summary-row "Total" commissionable-amount)]))


(defn ^:private commission-component
  [{:keys [balance-transfer fetching? line-items] :as queried-data}]
  (let [{:keys [id number amount data]}                       balance-transfer
        {:keys [order commission-date commissionable-amount]} data
        shipment        (orders/first-commissioned-shipment order)
        commission-date (or commission-date (:commission_date data))
        shipped-at      (:shipped-at shipment)]
    (if fetching?
      [:div.my2.h2 ui/spinner]

      [:div.container.mb4.px3
       [:div.py3 (ui-molecules/return-link queried-data)]
       [:div.col.col-1 (svg/coin-in-slot {:height 14
                                          :width  20})]
       [:div.col.col-11.pl1
        [:div.col.col-9.pb2
         [:h5.col-12.left.medium (str "Commission Earned" (when-let [name (orders/first-name-plus-last-name-initial order)]
                                                            (str " - " name)))]]
        [:div.col.col-3.mtp1.right-align.pb2
         [:div.h5.medium.teal (mf/as-money amount)]]

        (info-columns
         ["Deposit Date" (f/long-date commission-date)]
         ["Order Number" (:number order)])

        [:h6.shout.bold.pt2 "Payment Details"]
        (info-block
         "Shipped Date" (f/long-date (or shipped-at commission-date)))

        [:div.align-top
         [:span.h6.dark-gray.shout.nowrap "order details"]
         (component/build line-items/component {:line-items  line-items
                                                :show-price? true}
                          {})]

        (display-order-summary-for-commissions order (or commissionable-amount (:commissionable_amount data)))

        (info-block "Commission" [:div.light "Added to next payment: " [:span.medium (mf/as-money amount)]])]])))

(defn ^:private estimated-arrival [payout-method]
  (let [payout-method-name (or (:payout-timeframe payout-method)
                               (:payout_timeframe payout-method)
                               "Unknown")]
    (if (= "immediate" payout-method-name) "Instant" payout-method-name)))

(defn ^:private instapay-payout-details [date-string payout-method]
  [:div
   (info-columns
     ["Date Sent" date-string]
     ["Estimated Arrival" (estimated-arrival payout-method)])
   (info-block
     "Card" (str "xxxx-xxxx-xxxx-" (or (:last-4 payout-method)
                                       "????")))])

(defn ^:private paypal-payout-details [date-string payout-method]
  [:div
   (info-columns
     ["Date Sent" date-string]
     ["Estimated Arrival" "Instant"])
   (info-block "Paypal Email Address" (:email payout-method))])

(defn ^:private check-payout-details [date-string payout-method]
  [:div
   (info-columns
     ["Date Sent" date-string]
     ["Estimated Arrival" "7-10 Business Days"])
   (info-block  "Mailing Address " [:div
                                    [:div (-> payout-method :address :address1)]
                                    [:div (-> payout-method :address :address2)]
                                    [:div (str (-> payout-method :address :city)
                                               ", "
                                               (or (-> payout-method :address :state-name)
                                                   (-> payout-method :address :state_name))
                                               " "
                                               (-> payout-method :address :zipcode))]])])

(defn ^:private venmo-payout-details [date-string payout-method]
  [:div
   (info-columns
     ["Date Sent" date-string]
     ["Estimated Arrival" "Instant"])
   (info-block "Venmo Phone" (:phone payout-method))])

(defn ^:private payout-method-details [date-string payout-method]
  (when (:name payout-method)
    (case (:name payout-method)
      "Mayvenn InstaPay" (instapay-payout-details date-string payout-method)
      "Paypal"           (paypal-payout-details date-string payout-method)
      "Check"            (check-payout-details date-string payout-method)
      "Venmo"            (venmo-payout-details date-string payout-method))))

(defn ^:private payout-component
  [{:keys [balance-transfer] :as queried-data}]
  (let [{:keys [id
                number
                amount
                data]}    balance-transfer
        {:keys [created-at
                payout-method
                payout-method-name
                by-self]} data]
    [:div.container.mb4.px3
     [:div.py3 (ui-molecules/return-link queried-data)]
     [:div
      [:div.col.col-1 (svg/stack-o-cash {:height 14
                                         :width  20})]
      [:div.col.col-11.pl1
       [:div.col.col-9
        [:h4.col-12.left.medium "Money Transfer"]]
       [:div.col.col-3.mtp1.right-align
        [:div.h5.medium.green (mf/as-money amount)]]
       [:div.h8.dark-gray.col.col-12.pb4.right-align payout-method-name]
       (payout-method-details
        (f/long-date (or created-at (:transfered_at data)))
        (or payout-method (:payout_method data)))]]]))

(defn ^:private award-component
  [{:keys [balance-transfer] :as queried-data}]
  (let [{:keys [id transfered-at amount data]} balance-transfer
        {:keys [reason]}                       data]
    [:div.container.mb4.px3
     [:div.py3.pl1 (ui-molecules/return-link queried-data)]
     [:div
      [:div.col.col-1.px2 (svg/coin-in-slot {:height 14
                                             :width  20})]
      [:div.col.col-9.pl2
       [:h4.col-12.left.medium.pb4 reason]
       [:div.col-12
        (info-block "Deposit Date" (f/long-date (or transfered-at (:transfered_at data))))]]
      [:div.col.col-2.mtp1.right-align
       [:div.h5.medium.green (mf/as-money amount)]
       [:div.h8.dark-gray "Cash"]]]]))

(defn ^:private voucher-award-component
  [{:keys [balance-transfer] :as queried-data}]
  (let [{:keys [id transfered-at amount data]}     balance-transfer
        {:keys [order campaign-name order-number]} data
        client-name                                (orders/first-name-plus-last-name-initial order)]
    [:div.container.mb4.px3
     [:div.py3 (ui-molecules/return-link queried-data)]
     [:div
      [:div.col.col-1.px2 (svg/coin-in-slot {:height 14
                                             :width  20})]
      [:div.col.col-9.pl2
       [:h4.col-12.left.medium.pb4 "Service Payment"
        (when client-name (str " - " client-name))]
       [:div.col-12
        (info-block "deposit date" (f/long-date (or transfered-at (:transfered_at data))))
        (info-block "client" client-name)
        (info-block "service type" campaign-name)

        (when order-number
          (info-block "order number"
                      [:a.inherit-color
                       (merge
                        {:data-test "view-order"}
                        (utils/route-to
                         events/navigate-stylist-dashboard-order-details
                         {:order-number order-number}))
                       order-number
                       [:span.teal " View" ]]))]]
      [:div.col.col-2.mtp1.right-align
       [:div.h5.medium.green (mf/as-money amount)]]]]))


(defn query [data]
  (let [balance-transfer-id (get-in data keypaths/stylist-earnings-balance-transfer-details-id)
        balance-transfer    (get-in data (conj keypaths/stylist-earnings-balance-transfers
                                               balance-transfer-id))
        type                (:type balance-transfer)]
    (merge
     {:return-link/id            "back-link"
      :return-link/event-message [events/navigate-v2-stylist-dashboard-payments]
      :return-link/copy          "Back"
      :balance-transfer          balance-transfer
      :fetching?                 (utils/requesting? data request-keys/get-stylist-balance-transfer)}
     (when (= type "commission")
       (let [line-items (->> (:order (:data balance-transfer))
                             orders/first-commissioned-shipment
                             orders/product-items-for-shipment)]
         {:line-items (mapv (partial cart/add-product-title-and-color-to-line-item
                                     (get-in data keypaths/v2-products)
                                     (get-in data keypaths/v2-facets))
                            line-items)})))))

(defn component [{:keys [fetching? balance-transfer] :as data} owner opts]
  (component/create
   (if (and fetching? (not balance-transfer))
     [:div.my2.h2 ui/spinner]
     (when balance-transfer
       (case (:type balance-transfer)
         "payout"        (payout-component data)
         "commission"    (commission-component data)
         "award"         (award-component data)
         "voucher_award" (voucher-award-component data))))))

(defn ^:export built-component [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-stylist-dashboard-balance-transfer-details
  [_ event {:keys [balance-transfer-id] :as args} _ app-state]
  (let [user-id    (get-in app-state keypaths/user-id)
        user-token (get-in app-state keypaths/user-token)]
    (when user-token
      (api/get-stylist-balance-transfer user-id user-token balance-transfer-id))))

(defmethod effects/perform-effects events/api-success-stylist-balance-transfer-details
  [_ _ {:keys [data]} _ _]
  (messages/handle-message events/ensure-sku-ids
                           {:sku-ids (->> (:order data)
                                          orders/first-commissioned-shipment
                                          orders/product-items-for-shipment
                                          (map :sku))}))

(defmethod transitions/transition-state events/api-success-stylist-balance-transfer-details [_ _ balance-transfer app-state]
  (-> app-state
      (assoc-in keypaths/stylist-earnings-balance-transfer-details-id (:id balance-transfer))
      (assoc-in (conj keypaths/stylist-earnings-balance-transfers (:id balance-transfer))
                balance-transfer)))
