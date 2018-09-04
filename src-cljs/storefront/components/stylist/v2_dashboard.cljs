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
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.request-keys :as request-keys]
            [storefront.transitions :as transitions]
            [voucher.keypaths :as voucher-keypaths]))

(def tabs
  [{:id :orders
    :title "Orders"
    :navigate events/navigate-v2-stylist-dashboard-orders}
   {:id :payments
    :title "Payments"
    :navigate events/navigate-v2-stylist-dashboard-payments}])

(defn balance-transfer->payment [balance-transfer]
  (let [{:keys [id type data]} balance-transfer
        {:keys [order created-at amount campaign-name reason commission-date payout-method-name]} data]
    (merge {:id id
            :icon "68e6bcb0-a236-46fe-a8e7-f846fff0f464"
            :date created-at
            :subtitle ""
            :amount (mf/as-money amount)
            :amount-description nil
            :styles {:background ""
                     :title-color "black"
                     :amount-color "teal"}}
           (case type
             "commission" {:title (str "Commission Earned" (when-let [name (orders/first-name-plus-last-name-initial order)]
                                                             (str " - " name)))
                           :data-test (str "commission-" id)
                           :date commission-date}
             "award"      {:title reason
                           :data-test (str "award-" id)}
             "voucher_award" {:title (str "Service Payment" (when-let [name (orders/first-name-plus-last-name-initial order)]
                                                              (str " - " name)))
                              :data-test (str "voucher-award-" id)
                              :subtitle campaign-name}
             "payout" {:title "Money Transfer"
                       :icon "4939408b-1ec8-4a47-bb0e-5cdeb15d544d"
                       :data-test (str "payout-" id)
                       :amount-description payout-method-name
                       :styles {:background "bg-too-light-teal"
                                :title-color "teal"
                                :amount-color "teal"}}
             "sales_bonus" {:title "Sales Bonus"
                            :data-test (str "sales-bonus-" id)
                            :icon "56bfbe66-6db0-48c7-9069-f86c6393b15d"}
             {:title "Unknown Payment"}))))

(defn payment-row [item]
  (let [{:keys [id icon title date subtitle amount amount-description styles data-test non-clickable?]} item]
    [:a.block.border-bottom.border-light-gray.px3.py2.flex.items-center
     (merge
       (utils/route-to events/navigate-stylist-dashboard-balance-transfer-details
                       {:balance-transfer-id id})
       {:key       (str "payment" id)
        :data-test data-test
        :class     (:background styles)
        :style     (when non-clickable? {:pointer-events "none"})})
     (ui/ucare-img {:width 20} icon)
     [:div.flex-auto.mx3
      [:h5.medium {:class (:title-color styles)} title]
      [:div.flex.h7.dark-gray
       [:div.mr4 (f/long-date date)]
       subtitle]]
     [:div.right-align
      [:div.bold {:class (:amount-color styles)} amount]
      [:div.h7.dark-gray (or amount-description
                             ui/nbsp)]]]))

(defn pending-voucher-row [{:as pending-voucher :keys [discount date]} service-menu]
  (let [item {:id                 (str "pending-voucher-" 1)
              :icon               "68e6bcb0-a236-46fe-a8e7-f846fff0f464"
              :title              "Service Payment"
              :date               date
              ;; TODO: Future Us: Strictly v2 services (not $100-off)
              :subtitle           (service-menu/discount->campaign-name discount)
              :amount             (service-menu/display-voucher-amount
                                   service-menu
                                   mf/as-money pending-voucher)
              :amount-description "Pending"
              :styles             {:background   ""
                                   :title-color  "black"
                                   :amount-color "orange"}
              :non-clickable?     true}]
    (payment-row item)))

(defn group-payments-by-month [payments]
  (let [year-month           (fn [{:keys [date]}]
                               (let [[year month _] (f/date-tuple date)]
                                 [year month]))
        year-month->payments (group-by year-month (reverse payments))
        sorted-year-months   (reverse (sort (keys year-month->payments)))]
    (for [[year month :as ym] sorted-year-months]
      {:title (str (get f/month-names month) " " year)
       :items (year-month->payments ym)})))

(defn payments-table [pending-voucher service-menu balance-transfers]
  (let [payments (map balance-transfer->payment balance-transfers)
        sections (group-payments-by-month payments)]
    [:div.col-12.mb3
     (when pending-voucher
       (pending-voucher-row pending-voucher service-menu))
     (for [{:keys [title items] :as section} sections]
       [:div {:key (str "payments-table-" title)}
        [:div.h7.bg-silver.px2.py1.medium title]
        ;; ASK: Sales Bonus row
        (for [item (reverse (sort-by :date items))]
          (payment-row item))])]))

(defn status->appearance [status]
  (case status
    :sale/shipped     [1 ["titleize" "teal"] ]
    :sale/returned    [2 ["shout"    "red"]]
    :sale/pending     [2 ["shout"    "yellow"]]
    :sale/unknown     [2 ["shout"    "red"]]
    :voucher/pending  nil
    :voucher/redeemed [1 ["titleize" "teal"]]
    :voucher/expired  [1 ["titleize" "red"]]
    :voucher/active   [1 ["titleize" "purple"]]
    :voucher/none     [1 ["titleize" "light" "gray"]]
    nil               nil))

(defn status-cell [[span classes] text]
  [:td.p2.h6.center.medium {:col-span span}
   [:span {:class classes} text]])

(defn sale-status-cell [sale]
  (let [status     (sales/sale-status sale)
        copy       (sales/status->copy status)
        appearance (status->appearance status)]
    (when (seq appearance)
      (status-cell appearance copy))))

(defn voucher-status-cell [sale]
  (let [status     (sales/voucher-status sale)
        copy       (sales/status->copy status)
        appearance (status->appearance status)]
    (when (seq appearance)
      (status-cell appearance copy))))

(defn sales-table
  [sales sales-pagination]
  [:table.col-12 {:style {:border-collapse "collapse"}}
   [:thead.bg-silver.border-0
    [:tr.h6
     [:th.p2.left-align.medium.col-3 "Order Updated"]
     [:th.p2.left-align.medium "Client"]
     [:th.p2.center.medium.col-1 "Delivery"]
     [:th.p2.center.medium.col-1 "Voucher"]]]
   [:tbody
    (for [sale (map sales (:ordering sales-pagination))
          :let [{:keys [id
                        order-number
                        order
                        order-updated-at]} sale]]
      [:tr.border-bottom.border-gray.py2.pointer.fate-white-hover
       (merge (utils/route-to events/navigate-stylist-dashboard-order-details {:order-number order-number})
              {:key       (str "sales-table-" id)
               :data-test (str "sales-" order-number)})
       [:td.p2.left-align.dark-gray.h6.col-3 (some-> order-updated-at f/abbr-date)]
       [:td.p2.left-align.medium.col-3.h3 (some-> order orders/first-name-plus-last-name-initial)]
       (sale-status-cell sale)
       (voucher-status-cell sale)])]])

(defn ^:private ledger-tabs [active-tab-name]
  [:div.flex.flex-wrap
   (for [{:keys [id title navigate]} tabs]
     [:a.h6.col-6.p2.black
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
  [{:keys [fetching? stats-cards activity-ledger-tab balance-transfers sales sales-pagination pending-voucher service-menu] :as data} owner opts]
  (let [{:keys [active-tab-name]} activity-ledger-tab]
    (component/create
     (if fetching?
       [:div.my2.h2 ui/spinner]
       [:div
        (v2-dashboard-stats/component stats-cards)
        (ledger-tabs active-tab-name)

        (case active-tab-name

          :payments
          (if (or (seq balance-transfers)
                  pending-voucher)
            (payments-table pending-voucher service-menu balance-transfers)
            (empty-ledger activity-ledger-tab))

          :orders
          (if (seq sales)
            (sales-table sales sales-pagination)
            (empty-ledger activity-ledger-tab)))]))))

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
        id->balance-transfers (get-in data keypaths/stylist-earnings-balance-transfers)]
    {:fetching?           (or (utils/requesting? data request-keys/get-stylist-balance-transfers)
                              (utils/requesting? data request-keys/fetch-stylist-service-menu))
     :stats-cards         {:stats                                  (get-in data keypaths/v2-dashboard-stats)
                           :cashing-out?                           (utils/requesting? data request-keys/cash-out-commit)
                           :payout-method                          (get-in data keypaths/stylist-manage-account-chosen-payout-method)
                           :cash-balance-section-expanded?         (get-in data keypaths/v2-ui-dashboard-cash-balance-section-expanded?)
                           :store-credit-balance-section-expanded? (get-in data keypaths/v2-ui-dashboard-store-credit-section-expanded?)
                           :total-available-store-credit           (get-in data keypaths/user-total-available-store-credit)}
     :activity-ledger-tab (determine-active-tab (get-in data keypaths/navigation-event))
     :balance-transfers   (into []
                                (comp
                                 (map get-balance-transfer)
                                 (remove (fn [transfer]
                                           (when-let [status (-> transfer :data :status)]
                                             (not= "paid" status)))))
                                id->balance-transfers)
     :sales               (get-in data keypaths/v2-dashboard-sales-elements)
     :sales-pagination    (get-in data keypaths/v2-dashboard-sales-pagination)
     :service-menu        (get-in data keypaths/stylist-service-menu)
     :pending-voucher     (get-in data voucher-keypaths/voucher-response)}))

(defn built-component [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-v2-stylist-dashboard-payments [_ event args _ app-state]
  (if (experiments/v2-dashboard? app-state)
    (let [stylist-id (get-in app-state keypaths/store-stylist-id)
          user-id    (get-in app-state keypaths/user-id)
          user-token (get-in app-state keypaths/user-token)]
      (messages/handle-message events/v2-stylist-dashboard-stats-fetch)
      (api/get-stylist-dashboard-balance-transfers stylist-id
                                                   user-id
                                                   user-token
                                                   (get-in app-state keypaths/stylist-earnings-pagination)
                                                   #(messages/handle-message events/api-success-v2-stylist-dashboard-balance-transfers
                                                                             (select-keys % [:balance-transfers :pagination]))))
    (effects/redirect events/navigate-stylist-dashboard-earnings)))

(defmethod effects/perform-effects events/navigate-v2-stylist-dashboard-orders [_ event args _ app-state]
  (if (experiments/v2-dashboard? app-state)
    (let [stylist-id (get-in app-state keypaths/store-stylist-id)
          user-id    (get-in app-state keypaths/user-id)
          user-token (get-in app-state keypaths/user-token)]
      (messages/handle-message events/v2-stylist-dashboard-stats-fetch)
      (api/get-stylist-dashboard-sales stylist-id
                                       user-id
                                       user-token
                                       (get-in app-state keypaths/v2-dashboard-sales-pagination)
                                       #(messages/handle-message events/api-success-v2-stylist-dashboard-sales
                                                                 (select-keys % [:sales :pagination]))))
    (effects/redirect events/navigate-stylist-dashboard-earnings)))

(defn most-recent-voucher-award [balance-transfers]
  (->> balance-transfers
       vals
       (filter #(= (:type %) "voucher_award"))
       (sort-by :transfered-at)
       last
       :transfered-at
       date/to-millis))

(defmethod transitions/transition-state events/api-success-v2-stylist-dashboard-balance-transfers
  [_ event {:keys [balance-transfers pagination]} app-state]
  (let [most-recent-voucher-award-date (most-recent-voucher-award balance-transfers)
        voucher-response-date          (-> (get-in app-state voucher-keypaths/voucher-response)
                                           :date
                                           date/to-millis)
        voucher-pending?               (> (or voucher-response-date 0) (or most-recent-voucher-award-date 0))]
    (-> (if voucher-pending?
          app-state
          (update-in app-state voucher-keypaths/voucher dissoc :response))
        (update-in keypaths/stylist-earnings-balance-transfers merge (maps/map-keys (comp spice/parse-int name) balance-transfers))
        (assoc-in keypaths/stylist-earnings-pagination pagination))))

(defmethod transitions/transition-state events/api-success-v2-stylist-dashboard-sales
  [_ event {:keys [sales pagination]} app-state]
  (-> app-state
      (update-in keypaths/v2-dashboard-sales-elements merge (maps/map-keys (comp spice/parse-int name) sales))
      (assoc-in keypaths/v2-dashboard-sales-pagination pagination)))
