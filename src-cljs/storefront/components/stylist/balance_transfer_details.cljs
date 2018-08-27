(ns storefront.components.stylist.balance-transfer-details
  (:require [spice.date :as date]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as component]
            [storefront.components.formatters :as f]
            [storefront.components.money-formatters :as mf]
            [storefront.components.order-summary :as summary]
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

(defn ^:private all-skus-in-balance-transfer [skus balance-transfer]
  (->> (:order (:data balance-transfer))
       orders/first-commissioned-shipment
       orders/product-items-for-shipment
       (mapv :sku)
       (select-keys skus)))

(defn query [data]
  (let [balance-transfer-id (get-in data keypaths/stylist-earnings-balance-transfer-details-id)
        balance-transfer    (get-in data (conj keypaths/stylist-earnings-balance-transfers
                                               balance-transfer-id))
        type                (:type balance-transfer)]
    (merge
     {:balance-transfer   balance-transfer
      :fetching?          (utils/requesting? data request-keys/get-stylist-balance-transfer)
      :aladdin-dashboard? (experiments/aladdin-dashboard? data)}
     (when (= type "commission")
       {:skus (all-skus-in-balance-transfer (get-in data keypaths/v2-skus)
                                            balance-transfer)}))))

(defn ^:private back-to-earnings [aladdin-dashboard?]
  [:a.col-12.dark-gray.flex.items-center.py3
   (merge
    {:data-test "back-link"}
    (if aladdin-dashboard?
      (utils/route-to events/navigate-stylist-v2-dashboard-payments)
      (utils/route-to events/navigate-stylist-dashboard-earnings)))
   (ui/back-caret "Back")])

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

(defn ^:private commission-component [{:keys [balance-transfer fetching? skus aladdin-dashboard?]}]
  (let [{:keys [id number amount data]} balance-transfer
        {:keys [order
                commission-date
                commissionable-amount]} data
        shipment                        (orders/first-commissioned-shipment order)
        commission-date                 (or commission-date (:commission_date data))
        shipped-at                      (:shipped-at shipment)]
    (if fetching?
      [:div.my2.h2 ui/spinner]

      [:div.container.mb4.px3
       (back-to-earnings aladdin-dashboard?)
       [:h3.my4 "Details - Commission Earned"]
       [:div.flex.justify-between.col-12
        [:div (f/less-year-more-day-date commission-date)]
        [:div (:full-name order)]
        [:div.green "+" (mf/as-money amount)]]

       (info-columns ["Order Number" (:number order)]
                     ["Ship Date" (f/less-year-more-day-date (or shipped-at commission-date))])

       [:div.mt2.mbnp2.mtnp2.border-top.border-gray

        (summary/display-line-items (orders/product-items-for-shipment shipment) skus)]

       (summary/display-order-summary-for-commissions order (or commissionable-amount (:commissionable_amount data)))

       [:div.h5.center.navy
        (str (mf/as-money amount) " has been added to your next payment.")]])))


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

(defn ^:private payout-component [{:keys [balance-transfer aladdin-dashboard?]}]
  (let [{:keys [id
                number
                amount
                data]}             balance-transfer
        {:keys [created-at
                payout-method
                payout-method-name
                by-self]} data]
    [:div.container.mb4.px3
     (back-to-earnings aladdin-dashboard?)
     [:div
      [:div.col.col-1 (svg/stack-o-cash {:height 14
                                             :width  20})]
      [:div.col.col-11.pl1
       [:div.col.col-9
        [:h4.col-12.left.medium "Money Transfer"]]
       [:div.col.col-3.mtp1.right-align
        [:div.h5.medium.green (mf/as-money amount)]]
       [:div.h7.dark-gray.col.col-12.pb4.right-align payout-method-name]
       (payout-method-details
        (f/long-date (or created-at (:transfered_at data)))
        (or payout-method (:payout_method data)))]]]))

(defn ^:private award-component [{:keys [balance-transfer aladdin-dashboard?]}]
  (let [{:keys [id transfered-at amount data]} balance-transfer
        {:keys [reason]}                       data]
    [:div.container.mb4.px3
     (back-to-earnings aladdin-dashboard?)
     [:div
      [:div.col.col-1.px2 (svg/coin-in-slot {:height 14
                                             :width  20})]
      [:div.col.col-9.pl2
       [:h4.col-12.left.medium.pb4 reason]
       [:div.col-12
        (info-block "Deposit Date" (f/long-date (or transfered-at (:transfered_at data))))]]
      [:div.col.col-2.mtp1.right-align
       [:div.h5.medium.green (mf/as-money amount)]
       [:div.h7.dark-gray "Cash"]]]]))

(defn ^:private voucher-award-component [{:keys [balance-transfer aladdin-dashboard?]}]
  (let [{:keys [id transfered-at amount data]}     balance-transfer
        {:keys [order campaign-name order-number]} data
        client-name                                (orders/first-name-plus-last-name-initial order)]
    [:div.container.mb4.px3
     (back-to-earnings aladdin-dashboard?)
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
                      (if aladdin-dashboard?
                        [:a.inherit-color
                         (merge
                          {:data-test "view-order"}
                          (utils/route-to
                           events/navigate-stylist-dashboard-order-details
                           {:order-number order-number}))
                         order-number
                         [:span.teal " View" ]]
                        order-number)))]]
      [:div.col.col-2.mtp1.right-align
       [:div.h5.medium.green (mf/as-money amount)]]]]))

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

(defn built-component [data opts]
  (component/build component (query data) opts))
