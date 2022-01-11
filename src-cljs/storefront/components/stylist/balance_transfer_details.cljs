(ns storefront.components.stylist.balance-transfer-details
  (:require checkout.classic-cart
            [ui.molecules :as ui-molecules]
            [storefront.accessors.adjustments :as adjustments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.line-items :as accessors.line-items]
            [storefront.component :as component :refer [defcomponent]]
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
   [:span.h6.shout.nowrap header]
   [:div.h6.medium (or content "--")]])

(defn ^:private info-columns [[left-header left-content] [right-header right-content]]
  [:div.col-12.pt2
   [:div.inline-block.col-6
    (info-block left-header left-content)]
   [:div.inline-block.col-6
    (info-block right-header right-content)]])

(defn ^:private display-line-item
  ([line-item] (display-line-item line-item true))
  ([{:keys [product-title unit-price quantity sku id legacy/variant-id variant-attrs]
     :join/keys [facets]} show-price?]
   [:div.h6.pb2 {:key (or variant-id id)}
    [:div.medium {:data-test (str "line-item-title-" sku)} product-title]
    [:div {:data-test (str "line-item-color-" sku)} (-> facets :hair/color :option/name)]
    [:div {:data-test (str "line-item-base-material-" sku)} (-> facets :hair/base-material :option/name)]
    (when show-price?
      [:div {:data-test (str "line-item-price-ea-" sku)} "Price: " (mf/as-money unit-price) " ea"])
    [:div
     (when-let [length (:length variant-attrs)]
       [:span {:data-test (str "line-item-length-" sku)} length "” "])
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
       (when (adjustments/non-zero-adjustment? coupon-code)
         (summary-row {:key name} [:div (adjustments/display-adjustment-name name)] price)))

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
       [:div.py3 (ui-molecules/return-link (:payout/return-link queried-data))]
       [:div.col.col-1 (svg/coin-in-slot {:height 14
                                          :width  20})]
       [:div.col.col-11.pl1
        [:div.col.col-9.pb2
         [:h5.col-12.left.medium (str "Commission Earned" (when-let [name (orders/first-name-plus-last-name-initial order)]
                                                            (str " - " name)))]]
        [:div.col.col-3.mtp1.right-align.pb2
         [:div.h5.medium.p-color (mf/as-money amount)]]

        (info-columns
         ["Deposit Date" (f/long-date commission-date)]
         ["Order Number" (:number order)])

        [:h6.shout.bold.pt2 "Payment Details"]
        (info-block
         "Shipped Date" (f/long-date (or shipped-at commission-date)))

        [:div.align-top
         [:span.h6.shout.nowrap "order details"]
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

(defn ^:private info-on-payout-molecule
  [{:keys [id title copy]}]
  (when id
    [:div.p2.col-6
     [:div.content-3 title]
     [:div copy]]))

(defn ^:private transfer-confirmation-molecule
  [{:keys [id title earnings-copy earnings-amount
           fee-copy fee-amount fee-dt earnings-dt
           total-cashout-copy total-cashout total-dt]}]
  (when id
    [:div.p4
     [:div.col-12.content-3.pb2 title]
     [:div.flex.items-center.justify-between.pb1
      {:data-test earnings-dt}
      [:div earnings-copy]
      [:div earnings-amount]]
     (when fee-dt
       [:div.flex.items-center.justify-between
        {:data-test fee-dt}
        [:div fee-copy]
        [:div fee-amount]])
     [:div.border-bottom.border-width-2.my2]
     [:div.flex.items-center.justify-between
      {:data-test total-dt}
      [:div.content-3.bold.shout total-cashout-copy]
      [:div total-cashout]]]))

(defn ^:private payout-component
  [{:payout/keys [return-link date-sent card transfer-method arrival-time transfer-confirmation-data] :as data}]
  [:div.stretch.max-580.mx-auto
   [:div.hide-on-tb-dt
    [:div.border-bottom.border-cool-gray.border-width-2.m-auto.col-7-on-dt
     [:div.px2.my2 (ui-molecules/return-link return-link)]]]
   [:div.hide-on-mb
    [:div.m-auto.container
     [:div.px2.my2 (ui-molecules/return-link return-link)]]]
   [:div.bg-cool-gray
    [:span.flex.items-center.pl1.pt1
     (info-on-payout-molecule date-sent)
     (info-on-payout-molecule card)]
    [:span.flex.items-center.pl1.pb2
     (info-on-payout-molecule transfer-method)
     (info-on-payout-molecule arrival-time)]]
   (transfer-confirmation-molecule transfer-confirmation-data)])

(defn ^:private award-component
  [{:keys [balance-transfer] :as queried-data}]
  (let [{:keys [id transfered-at amount data]} balance-transfer
        {:keys [reason]}                       data]
    [:div.container.mb4.px3
     [:div.py3.pl1 (ui-molecules/return-link (:payout/return-link queried-data))]
     [:div
      [:div.col.col-1.px2 (svg/coin-in-slot {:height 14
                                             :width  20})]
      [:div.col.col-9.pl2
       [:h4.col-12.left.medium.pb4 reason]
       [:div.col-12
        (info-block "Deposit Date" (f/long-date (or transfered-at (:transfered_at data))))]]
      [:div.col.col-2.mtp1.right-align
       [:div.h5.medium.s-color (mf/as-money amount)]
       [:div.h8 "Cash"]]]]))

(defn ^:private payment-award-component
  [{:keys [balance-transfer] :as queried-data}]
  (let [{:keys [id transfered-at amount data]} balance-transfer
        {:keys [payee-name note payment-id]}   data]
    [:div.container.mb4.px3
     [:div.py3 (ui-molecules/return-link (:payout/return-link queried-data))]
     [:div
      [:div.col.col-1.px2 (svg/coin-in-slot {:height 14
                                             :width  20})]
      [:div.col.col-9.pl2
       [:h4.col-12.left.medium.pb4 "Stylist Payment"
        (when payee-name (str " - " payee-name))]
       [:div.col-12
        (info-block "deposit date" (f/long-date (or transfered-at (:transfered_at data))))
        (info-block "from" payee-name)
        (info-block "payment id" payment-id)
        (info-block "note" note)]]
      [:div.col.col-2.mtp1.right-align
       [:div.h5.medium.s-color (mf/as-money amount)]]]]))

;; TODO: Refactor the query in this namespace to not be as one-to-one with the
;; model representation
(defn ^:private voucher-award-component
  [{:keys [balance-transfer service-skus voucher-services] :as queried-data}]
  (let [{:keys [id transfered-at amount data]}             balance-transfer
        {:keys [order campaign-name order-number voucher]} data
        client-name                                        (orders/first-name-plus-last-name-initial order)]
    [:div.container.mb4.px3
     [:div.py3 (ui-molecules/return-link (:payout/return-link queried-data))]
     [:div
      [:div.col.col-1.px2 (svg/coin-in-slot {:height 14
                                             :width  20})]
      [:div.col.col-9.pl2
       [:h4.col-12.left.medium.pb4 "Service Payment"
        (when client-name (str " - " client-name))]
       [:div.col-12
        (info-block "deposit date" (f/long-date (or transfered-at (:transfered_at data))))
        (info-block "client" client-name)
        (info-block "services" (if-let [services (:services voucher)]
                                 [:div
                                  (mapv (fn [{sku-name :sku/name
                                              sku-id   :catalog/sku-id}]
                                          (let [id (str "service-" sku-id)]
                                            [:div
                                             {:data-test id
                                              :key       id}
                                             sku-name]))
                                        service-skus)]
                                 campaign-name))

        (when order-number
          (info-block "order number"
                      [:a.inherit-color
                       (merge
                        {:data-test "view-order"}
                        (utils/route-to
                         events/navigate-stylist-dashboard-order-details
                         {:order-number order-number}))
                       order-number
                       [:span.p-color " View"]]))]]
      [:div.col.col-2.mtp1.right-align
       [:div.h5.medium.s-color (mf/as-money amount)]]]]))

(def ^:private return-link<-
  #:return-link{:id            "back-link"
                :event-message [events/navigate-v2-stylist-dashboard-payments]
                :copy          "Back"})

(defn ^:private payout-info-query
  [id title copy]
  {:id id
   :title title
   :copy copy})

(defn ^:private transfer-confirmation-data-query
  [amount fee total]
  (let [fee (js/parseFloat (or fee 0))]
    {:id                 "transfer-confirmation-id"
    :title              "Transfer Confirmation"
    :earnings-copy      "Your Earnings"
    :earnings-amount    (mf/as-money (+ (js/parseFloat amount) fee))
    :earnings-dt        "earnings-row"
    :fee-dt             (when (> fee 0) "instapay-fee-row")
    :fee-copy           "Instapay Fee"
    :fee-amount         (mf/as-money (- fee))
    :total-dt           "total-row"
    :total-cashout-copy "Cashout Amount"
    :total-cashout      (mf/as-money total)}))

(defn query [app-state]
  (let [balance-transfer-id      (get-in app-state keypaths/stylist-earnings-balance-transfer-details-id)
        {:keys [id type amount data fee]
         :as   balance-transfer} (get-in app-state (conj keypaths/stylist-earnings-balance-transfers
                                                    balance-transfer-id))
        skus                     (get-in app-state keypaths/v2-skus)
        total                    (js/parseFloat amount)]
    (merge
     {:payout/return-link                return-link<-
      :payout/date-sent                  (payout-info-query "date-sent-info-id" "Date Sent" "January 16, 2019")
      :payout/card                       (payout-info-query "card-info-id" "Card" (str "xxxx-xxxx-xxxx-" "last"))
      :payout/transfer-method            (payout-info-query "transfer-method-id" "Transfer Method" "Instapay")
      :payout/arrival-time               (payout-info-query "arrival-time-id" "Estimated Arrival" "Instant")
      :payout/transfer-confirmation-data (transfer-confirmation-data-query amount fee total)
      :service-skus                      (when (= "voucher_award" type)
                                           (->> balance-transfer
                                                :data
                                                :voucher
                                                :services
                                                (mapv :sku)
                                                (select-keys skus)
                                                vals
                                                (sort-by :order.view/addon-sort)))
      :balance-transfer                  balance-transfer
      :fetching?                         (utils/requesting? app-state request-keys/get-stylist-balance-transfer)}
     (when (= type "commission")
       (let [line-items (->> (:order (:data balance-transfer))
                             orders/first-commissioned-shipment
                             orders/product-items-for-shipment)]
         {:line-items (mapv (partial accessors.line-items/prep-for-display
                                     (get-in app-state keypaths/v2-products)
                                     (get-in app-state keypaths/v2-skus)
                                     (get-in app-state keypaths/v2-facets))
                            line-items)})))))

(defcomponent component [{:keys [fetching? balance-transfer] :as data} owner opts]
  (if (and fetching? (not balance-transfer))
    [:div.my2.h2 ui/spinner]
    (when balance-transfer
      (case (:type balance-transfer)
        "payout"        (payout-component data)
        "commission"    (commission-component data)
        "award"         (award-component data)
        "payment_award" (payment-award-component data)
        "voucher_award" (voucher-award-component data)))))

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
                                          :line-items
                                          (map :sku))}))

(defmethod transitions/transition-state events/api-success-stylist-balance-transfer-details [_ _ balance-transfer app-state]
  (-> app-state
      (assoc-in keypaths/stylist-earnings-balance-transfer-details-id (:id balance-transfer))
      (assoc-in (conj keypaths/stylist-earnings-balance-transfers (:id balance-transfer))
                balance-transfer)))
