(ns storefront.components.stylist.balance-transfer-details
  (:require [spice.date :as date]
            [storefront.accessors.orders :as orders]
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
  [_ event {:keys [data] :as args} _ app-state]
  (messages/handle-message events/ensure-skus {:skus (->> data
                                                          :order
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
     {:balance-transfer balance-transfer
      :fetching?        (utils/requesting? data request-keys/get-stylist-balance-transfer)}
     (when (= type "commission")
       {:skus      (all-skus-in-balance-transfer (get-in data keypaths/v2-skus)
                                                 balance-transfer)}))))

(def ^:private back-to-earnings
  [:a.left.col-12.dark-gray.flex.items-center.py3
   (merge
    {:data-test "back-link"}
    (utils/route-to events/navigate-stylist-dashboard-earnings))
   (ui/back-caret "back to earnings")])

(defn ^:private info-columns [[left-header left-content] [right-header right-content]]
  [:div.col-12.pt2
   [:div.col-6.inline-block.align-top
    [:span.h5.dark-gray left-header]
    [:div.h5 left-content]]
   [:div.col-6.inline-block.align-top
    [:span.h5.dark-gray right-header]
    [:div.h5 right-content]]])

(defn ^:private commission-component [{:keys [balance-transfer fetching? skus]}]
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
       back-to-earnings
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



(defn ^:private instapay-payout-details [payout-method]
  (info-columns ["Card" (str "xxxx-xxxx-xxxx-" (or (:last-4 payout-method)
                                                   (:last4 payout-method)
                                                   "????"))]
                ["Estimated Arrival" (or (:payout-timeframe payout-method)
                                         (:payout_timeframe payout-method)
                                         "Unknown")]))

(defn ^:private paypal-payout-details [payout-method]
  (info-columns ["Paypal Email Address" (:email payout-method)]
                ["Estimated Arrival" "Instant"]))

(defn ^:private check-payout-details [payout-method]
  (info-columns ["Mailing Address " [:div
                                     [:div (-> payout-method :address :address1)]
                                     [:div (-> payout-method :address :address2)]
                                     [:div (str (-> payout-method :address :city)
                                                ", "
                                                (or (-> payout-method :address :state-name)
                                                    (-> payout-method :address :state_name))
                                                " "
                                                (-> payout-method :address :zipcode))]]]
                ["Estimated Arrival" "7-10 Business Days"]))

(defn ^:private venmo-payout-details [payout-method]
  (info-columns ["Venmo Phone" (:phone payout-method)]
                ["Estimated Arrival" "Instant"]))

(defn ^:private payout-method-details [payout-method]
  (when (:name payout-method)
    (case (:name payout-method)
      "Mayvenn InstaPay" (instapay-payout-details payout-method)
      "Paypal"           (paypal-payout-details payout-method)
      "Check"            (check-payout-details payout-method)
      "Venmo"            (venmo-payout-details payout-method))))

(defn ^:private payout-component [{:keys [balance-transfer]}]
  (let [{:keys [id
                number
                amount
                data]}             balance-transfer
        {:keys [created-at
                payout-method
                payout-method-name
                by-self]} data]
    [:div.container.mb4.px3
     back-to-earnings
     [:h3.my4 "Details - Earnings Transfer - " (or payout-method-name (:payout_method_name data))]
     [:div.flex.justify-between.col-12
      [:div (f/less-year-more-day-date (or created-at (:created_at data)))]
      [:div.pr4 (if by-self "You" "Mayvenn") " transferred " (mf/as-money amount)]]
     (payout-method-details (or payout-method (:payout_method data)))]))

(defn ^:private award-component [{:keys [balance-transfer]}]
  (let [{:keys [id transfered-at amount data]} balance-transfer
        {:keys [reason]}                       data]
    [:div.container.mb4.px3
     back-to-earnings
     [:h3.my4 "Details - Award Received"]
     [:div.flex.justify-between.col-12
      [:div (f/less-year-more-day-date (or transfered-at (:transfered_at data)))]
      [:div.green "+" (mf/as-money amount)]]
     [:div.col-12.inline-block.align-top.mb3
      [:span.h5.dark-gray "Reason"]
      [:div.h5 reason]]
     [:div.h5.center.navy.py3.border-top.border-gray
      (str (mf/as-money amount) " has been added to your next payment.")]]))

(defn component [{:keys [fetching? balance-transfer] :as data} owner opts]
  (component/create
   (if (and fetching? (not balance-transfer))
     [:div.my2.h2 ui/spinner]
     (when balance-transfer
       (case (:type balance-transfer)
         "payout"     (payout-component data)
         "commission" (commission-component data)
         "award"      (award-component data))))))

(defn built-component [data opts]
  (component/build component (query data) opts))
