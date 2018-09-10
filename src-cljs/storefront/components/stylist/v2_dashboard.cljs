(ns storefront.components.stylist.v2-dashboard
  (:require [spice.core :as spice]
            [spice.date :as date]
            [spice.maps :as maps]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.sales :as sales]
            [storefront.accessors.service-menu :as service-menu]
            [storefront.api :as api]
            [storefront.component :as component]
            [storefront.components.formatters :as f]
            [storefront.components.money-formatters :as mf]
            [storefront.components.stylist.v2-dashboard-stats :as v2-dashboard-stats]
            [storefront.components.stylist.v2-dashboard-payments-tab :as payments-tab]
            [storefront.components.stylist.v2-dashboard-orders-tab :as orders-tab]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.request-keys :as request-keys]
            [storefront.transitions :as transitions]
            [voucher.keypaths :as voucher-keypaths]
            [storefront.components.stylist.pagination :as pagination]))

(def tabs
  [{:id :orders
    :title "Orders"
    :navigate events/navigate-v2-stylist-dashboard-orders}
   {:id :payments
    :title "Payments"
    :navigate events/navigate-v2-stylist-dashboard-payments}])

(defn ^:private ledger-tabs [active-tab-name]
  [:div.flex.flex-wrap
   (for [{:keys [id title navigate]} tabs]
     [:a.h5.col-6.p2.black
      (merge (utils/route-to navigate)
             {:key (str "ledger-tabs-" id)
              :data-test (str "nav-" (name id))
              :class (if (= id active-tab-name)
                       "bg-silver bold"
                       "bg-fate-white")})
      title])])

(defn ^:private empty-ledger [{:keys [empty-title empty-copy]}]
  [:div.my6.center
   [:h4.gray.bold.p1 empty-title]
   [:h6.dark-gray.col-5.mx-auto.line-height-2 empty-copy]])

(defn component
  [{:keys [stats-cards activity-ledger-tab
           balance-transfers balance-transfers-pagination fetching-balance-transfers?
           sales sales-pagination fetching-sales?
           pending-voucher service-menu] :as data} owner opts]
  (let [{:keys [active-tab-name]} activity-ledger-tab]
    (component/create
     [:div
      (v2-dashboard-stats/component stats-cards)
      (ledger-tabs active-tab-name)

      (case active-tab-name

        :payments
        (payments-tab/payments-table pending-voucher service-menu balance-transfers balance-transfers-pagination fetching-balance-transfers?)

        :orders
        (orders-tab/sales-table sales sales-pagination fetching-sales?))])))

(def determine-active-tab
  {events/navigate-v2-stylist-dashboard-payments {:active-tab-name :payments
                                                  :empty-copy      "Payments and bonus activity will appear here."
                                                  :empty-title     "No payments yet"}
   events/navigate-v2-stylist-dashboard-orders   {:active-tab-name :orders
                                                  :empty-copy      "Orders from your store will appear here."
                                                  :empty-title     "No orders yet"}})

(defn query
  [data]
  (let [get-balance-transfer  second
        balance-transfers (get-in data keypaths/v2-dashboard-balance-transfers-elements)]
    {:stats-cards         {:stats                                  (get-in data keypaths/v2-dashboard-stats)
                           :cashing-out?                           (utils/requesting? data request-keys/cash-out-commit)
                           :payout-method                          (get-in data keypaths/stylist-manage-account-chosen-payout-method)
                           :cash-balance-section-expanded?         (get-in data keypaths/v2-ui-dashboard-cash-balance-section-expanded?)
                           :store-credit-balance-section-expanded? (get-in data keypaths/v2-ui-dashboard-store-credit-section-expanded?)
                           :total-available-store-credit           (get-in data keypaths/user-total-available-store-credit)}
     :activity-ledger-tab (determine-active-tab (get-in data keypaths/navigation-event))
     :service-menu        (get-in data keypaths/stylist-service-menu)
     :pending-voucher     (get-in data voucher-keypaths/voucher-response)

     :balance-transfers           (into []
                                        (comp
                                         (map get-balance-transfer)
                                         (remove (fn [transfer]
                                                   (when-let [status (-> transfer :data :status)]
                                                     (not= "paid" status)))))
                                        balance-transfers)
     :fetching-balance-transfers? (or (utils/requesting? data request-keys/get-stylist-dashboard-balance-transfers)
                                      (utils/requesting? data request-keys/fetch-stylist-service-menu))
     :balance-transfers-pagination     (get-in data keypaths/v2-dashboard-balance-transfers-pagination)

     :sales            (get-in data keypaths/v2-dashboard-sales-elements)
     :fetching-sales?  (utils/requesting? data request-keys/get-stylist-dashboard-sales)
     :sales-pagination (get-in data keypaths/v2-dashboard-sales-pagination)}))

(defn built-component [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-v2-stylist-dashboard [_ event args _ app-state]
  (if (experiments/v2-dashboard? app-state)
    (messages/handle-message events/v2-stylist-dashboard-stats-fetch)
    (effects/redirect events/navigate-stylist-dashboard-earnings)))


