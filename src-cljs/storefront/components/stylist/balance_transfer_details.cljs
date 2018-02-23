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

(defmethod effects/perform-effects events/navigate-stylist-dashboard-balance-transfer-details
  [_ event {:keys [balance-transfer-id] :as args} _ app-state]
  (let [user-id    (get-in app-state keypaths/user-id)
        user-token (get-in app-state keypaths/user-token)]
    (when user-token
      (api/get-stylist-balance-transfer user-id user-token balance-transfer-id))))

(defmethod effects/perform-effects events/api-success-stylist-balance-transfer-details
  [_ event {:keys [data] :as args} _ app-state]
  (messages/handle-message events/ensure-skus {:skus (map :sku (orders/product-items (:order data)))}))

(defmethod transitions/transition-state events/api-success-stylist-balance-transfer-details [_ _ balance-transfer app-state]
  (-> app-state
      (assoc-in keypaths/stylist-earnings-balance-transfer-details-id (:id balance-transfer))
      (assoc-in (conj keypaths/stylist-earnings-balance-transfers (:id balance-transfer))
                balance-transfer)))

(defn ^:private all-skus-in-balance-transfer [skus balance-transfer]
  (->> (:order (:data balance-transfer))
       orders/product-items
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
   (utils/route-to events/navigate-stylist-dashboard-earnings)
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
  (let [{:keys [id number amount]} balance-transfer
        {:keys [order
                commission_date
                commissionable_amount
                number]}           (:data balance-transfer)]
    (if fetching?
      [:div.my2.h2 ui/spinner]

      [:div.container.mb4.px3
       back-to-earnings
       [:h3.my4 "Details - Commission Earned"]
       [:div.flex.justify-between.col-12
        [:div (f/less-year-more-day-date commission_date)]
        [:div (:full-name order)]
        [:div.green "+" (mf/as-money amount)]]

       (info-columns ["Order Number" (:number order)]
                     ["Ship Date" (f/less-year-more-day-date (->> (:order balance-transfer)
                                                                  :shipments
                                                                  first
                                                                  :shipped-at))])

       [:div.mt2.mbnp2.mtnp2.border-top.border-gray
        (summary/display-line-items (orders/product-items order) skus)]

       (summary/display-order-summary-for-commissions order commissionable_amount)

       [:div.h5.center.navy
        (str (mf/as-money amount) " has been added to your next payment.")]])))



(defn ^:private instapay-payout-details [payout_method]
  (info-columns ["Card" (str "xxxx-xxxx-xxxx-" (or (:last4 payout_method) "????"))]
                ["Estimated Arrival" (or (:payout_timeframe payout_method) "Unknown")]))

(defn ^:private paypal-payout-details [payout_method]
  (info-columns ["Paypal Email Address" (:email payout_method)]
                ["Estimated Arrival" "Instant"]))

(defn ^:private check-payout-details [payout_method]
  (info-columns ["Mailing Address " [:div
                                     [:div (-> payout_method :address :address1)]
                                     [:div (-> payout_method :address :address2)]
                                     [:div (str (-> payout_method :address :city)
                                                ", "
                                                (-> payout_method :address :state_name)
                                                " "
                                                (-> payout_method :address :zipcode))]]]
                ["Estimated Arrival" "7-10 Business Days"]))

(defn ^:private venmo-payout-details [payout_method]
  (info-columns ["Venmo Phone" (:phone payout_method)]
                ["Estimated Arrival" "Instant"]))

(defn ^:private payout-method-details [payout_method]
  (when (:name payout_method)
    (case (:name payout_method)
      "Mayvenn InstaPay" (instapay-payout-details payout_method)
      "Paypal"           (paypal-payout-details payout_method)
      "Check"            (check-payout-details payout_method)
      "Venmo"            (venmo-payout-details payout_method))))

(defn ^:private payout-component [{:keys [balance-transfer]}]
  (let [{:keys [id
                number
                amount]}             balance-transfer
        {:keys [created_at
                payout_method
                payout_method_name]} (:data balance-transfer)]
    [:div.container.mb4.px3
     [:a.left.col-12.dark-gray.flex.items-center.py3
      (utils/route-to events/navigate-stylist-dashboard-earnings)
      (ui/back-caret "back to earnings")]
     [:h3.my4 "Details - Earnings Transfer - " payout_method_name]
     [:div.flex.justify-between.col-12
      [:div (f/less-year-more-day-date created_at)]
      [:div.pr4 "You transferred " (mf/as-money amount)]]
     (payout-method-details payout_method)]))

(defn component [data owner opts]
  (component/create
   (if (and (:fetching? data)
            (not (:balance-transfer data)))
     [:div.my2.h2 ui/spinner]
     (when (:balance-transfer data)
       (case (:type (:balance-transfer data))
         "payout" (payout-component data)
         "commission" (commission-component data))))))

(defn built-component [data opts]
  (component/build component (query data) opts))
