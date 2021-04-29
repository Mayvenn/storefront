(ns stylist.dashboard
  (:require #?@(:cljs [[storefront.loader :as loader]
                       ;; The following a part of this module, but we need set-loaded! to be called last,
                       ;; which is at the bottom of this page
                       storefront.components.gallery-edit ; For module loading purposes only
                       storefront.components.stylist.account
                       storefront.components.stylist.balance-transfer-details
                       storefront.components.stylist.cash-out
                       storefront.components.stylist.cash-out-pending
                       storefront.components.stylist.cash-out-success
                       storefront.components.stylist.gallery-image-picker
                       storefront.components.stylist.order-details
                       storefront.components.stylist.portrait
                       storefront.components.stylist.share-your-store])
            [storefront.component :as component :refer [defcomponent]]
            stylist.dashboard-stats
            stylist.dashboard-orders-tab
            stylist.dashboard-payments-tab
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.request-keys :as request-keys]
            [voucher.keypaths :as voucher-keypaths]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.service-menu :as service-menu]
            [storefront.components.money-formatters :as mf]))

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
                       "bg-cool-gray bold"
                       "bg-white")})
      title])])

(defn ^:private empty-ledger [{:keys [empty-title empty-copy]}]
  [:div.my6.center
   [:h4.gray.bold.p1 empty-title]
   [:h6.col-5.mx-auto.line-height-2 empty-copy]])

(defcomponent component
  [{:keys [stats-cards activity-ledger-tab
           balance-transfers balance-transfers-pagination fetching-balance-transfers?
           pending-voucher-row
           orders-data] :as data} owner opts]
  (let [{:keys [active-tab-name]} activity-ledger-tab]
    [:div.col-6-on-dt.col-9-on-tb.mx-auto
     (component/build stylist.dashboard-stats/component stats-cards nil)
     (ledger-tabs active-tab-name)

     (case active-tab-name
       :payments
       (stylist.dashboard-payments-tab/payments-table
        pending-voucher-row
        balance-transfers
        balance-transfers-pagination fetching-balance-transfers?)

       :orders
       (component/build stylist.dashboard-orders-tab/component orders-data nil))]))

(def determine-active-tab
  {events/navigate-v2-stylist-dashboard-payments {:active-tab-name :payments
                                                  :empty-copy      "Payments and bonus activity will appear here."
                                                  :empty-title     "No payments yet"}
   events/navigate-v2-stylist-dashboard-orders   {:active-tab-name :orders
                                                  :empty-copy      "Orders from your store will appear here."
                                                  :empty-title     "No orders yet"}})

(defn voucher-response->pending-voucher-row
  "v2-vouchers are vouchers that can potentially have multiple services under a
  :service key"
  [skus service-menu {:as voucher-response :keys [services date redemption-date discount]}]
  (let [v2-voucher?          (nil? discount)
        voucher-service-base (->> (map :sku services)
                                  (select-keys skus )
                                  vals
                                  (filter (comp (partial = #{"base"}) :service/type))
                                  first)]
    {:id                 (str "pending-voucher-" 1)
     :icon               "68e6bcb0-a236-46fe-a8e7-f846fff0f464"
     :title              "Service Payment"
     :date               (if v2-voucher?
                           redemption-date
                           date)
     ;;definition
     :subtitle           (:sku/name voucher-service-base)
     :amount             (if v2-voucher?
                           (some->> services (keep :price) (reduce + 0.0) mf/as-money)
                           (service-menu/display-voucher-amount service-menu mf/as-money voucher-response))
     :amount-description "Pending"
     :styles             {:background   ""
                          :title-color  "black"
                          :amount-color "s-color"}
     :non-clickable?     true}) )

(defn query
  [data]
  (let [get-balance-transfer second
        balance-transfers    (get-in data keypaths/v2-dashboard-balance-transfers-elements)]
    {:stats-cards         (stylist.dashboard-stats/query data)
     :activity-ledger-tab (determine-active-tab (get-in data keypaths/navigation-event))
     :pending-voucher-row (some->> (get-in data voucher-keypaths/voucher-redeemed-response)
                                  (voucher-response->pending-voucher-row (get-in data keypaths/v2-skus)
                                                                         (get-in data keypaths/user-stylist-service-menu)))

     :balance-transfers            (into []
                                         (comp
                                          (map get-balance-transfer)
                                          (remove (fn [transfer]
                                                    (when-let [status (-> transfer :data :status)]
                                                      (not= "paid" status)))))
                                         balance-transfers)
     :fetching-balance-transfers?  (or (utils/requesting? data request-keys/get-stylist-dashboard-balance-transfers)
                                       (utils/requesting? data request-keys/fetch-user-stylist-service-menu))
     :balance-transfers-pagination (get-in data keypaths/v2-dashboard-balance-transfers-pagination)
     :orders-data                  (stylist.dashboard-orders-tab/query data)}))

(defn ^:export built-component [data opts]
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

#?(:cljs (loader/set-loaded! :dashboard))
