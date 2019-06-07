(ns storefront.components.stylist.v2-dashboard
  (:require [storefront.component :as component]
            [storefront.components.stylist.v2-dashboard-stats :as v2-dashboard-stats]
            [storefront.components.stylist.v2-dashboard-payments-tab :as payments-tab]
            [storefront.components.stylist.v2-dashboard-orders-tab :as orders-tab]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.request-keys :as request-keys]
            [voucher.keypaths :as voucher-keypaths]
            [storefront.accessors.auth :as auth]))

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
           pending-voucher service-menu
           orders-data] :as data} owner opts]
  (let [{:keys [active-tab-name]} activity-ledger-tab]
    (component/create
     [:div.col-6-on-dt.col-9-on-tb.mx-auto
      (component/build v2-dashboard-stats/component stats-cards nil)
      (ledger-tabs active-tab-name)

      (case active-tab-name
        :payments
        (payments-tab/payments-table pending-voucher service-menu balance-transfers balance-transfers-pagination fetching-balance-transfers?)

        :orders
        (component/build orders-tab/component orders-data nil))])))

(def determine-active-tab
  {events/navigate-v2-stylist-dashboard-payments {:active-tab-name :payments
                                                  :empty-copy      "Payments and bonus activity will appear here."
                                                  :empty-title     "No payments yet"}
   events/navigate-v2-stylist-dashboard-orders   {:active-tab-name :orders
                                                  :empty-copy      "Orders from your store will appear here."
                                                  :empty-title     "No orders yet"}})
(defn query
  [data]
  (let [get-balance-transfer second
        balance-transfers    (get-in data keypaths/v2-dashboard-balance-transfers-elements)]
    {:stats-cards         (v2-dashboard-stats/query data)
     :activity-ledger-tab (determine-active-tab (get-in data keypaths/navigation-event))
     :service-menu        (get-in data keypaths/stylist-service-menu)
     :pending-voucher     (get-in data voucher-keypaths/voucher-response)

     :balance-transfers            (into []
                                         (comp
                                          (map get-balance-transfer)
                                          (remove (fn [transfer]
                                                    (when-let [status (-> transfer :data :status)]
                                                      (not= "paid" status)))))
                                         balance-transfers)
     :fetching-balance-transfers?  (or (utils/requesting? data request-keys/get-stylist-dashboard-balance-transfers)
                                       (utils/requesting? data request-keys/fetch-stylist-service-menu))
     :balance-transfers-pagination (get-in data keypaths/v2-dashboard-balance-transfers-pagination)
     :orders-data (orders-tab/query data)}))

(defn built-component [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-v2-stylist-dashboard [_ event args _ app-state]
  (let [signed-in (auth/signed-in app-state)]
    (cond
      (not (::auth/at-all signed-in))
      (effects/redirect events/navigate-sign-in)

      (not (= :stylist (::auth/as signed-in)))
      (effects/redirect events/navigate-home)

      :else
      (messages/handle-message events/v2-stylist-dashboard-stats-fetch))))

(defmethod effects/perform-effects events/navigate-stylist-dashboard-earnings
  [_ event args _ app-state]
  (effects/redirect events/navigate-v2-stylist-dashboard-orders))

(defmethod effects/perform-effects events/navigate-stylist-dashboard-bonus-credit
  [_ event args _ app-state]
  (effects/redirect events/navigate-v2-stylist-dashboard-orders))

(defmethod effects/perform-effects events/navigate-stylist-dashboard-referrals
  [_ event args _ app-state]
  (effects/redirect events/navigate-v2-stylist-dashboard-orders))
