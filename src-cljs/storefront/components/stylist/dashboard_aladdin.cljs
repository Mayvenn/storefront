(ns storefront.components.stylist.dashboard-aladdin
  (:require [spice.core :as spice]
            [spice.date :as date]
            [spice.maps :as maps]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.payouts :as payouts]
            [storefront.accessors.service-menu :as service-menu]
            [storefront.api :as api]
            [storefront.component :as component]
            [storefront.components.formatters :as f]
            [storefront.components.money-formatters :as mf]
            [storefront.components.stylist.bonus-credit :as bonuses]
            [storefront.components.stylist.earnings :as earnings]
            [storefront.components.stylist.referrals :as referrals]
            [storefront.components.stylist.stats :as stats]
            [storefront.components.svg :as svg]
            [storefront.components.tabs :as tabs]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.platform.numbers :as numbers]
            [storefront.request-keys :as request-keys]
            [storefront.transitions :as transitions]
            [voucher.keypaths :as voucher-keypaths]))

(defn earnings-count [title value]
  [:div.dark-gray.letter-spacing-0
   [:div.shout.h6 title]
   [:div.black.medium.h5 value]])

(defn progress-indicator [{:keys [value maximum]}]
  (let [bar-value (-> value (/ maximum) (* 100.0) (min 100))
        bar-width (str (numbers/round bar-value) "%")
        bar-style {:height "5px"}]
    [:div.bg-gray.flex-auto
     (cond
       (zero? value) [:div.px2 {:style bar-style}]
       (= value maximum) [:div.bg-teal.px2 {:style bar-style}]
       :else [:div.bg-teal.px2 {:style (merge bar-style {:width bar-width})}])]))

(defn ^:private cash-balance-card
  [payout-method
   expanded?
   cashing-out?
   {:as earnings :keys [cash-balance lifetime-earnings monthly-earnings]}
   {:as services :keys [lifetime-services monthly-services]}]
  (let [toggle-expand (utils/fake-href events/control-stylist-v2-dashboard-section-toggle
                                       {:keypath keypaths/aladdin-dashboard-cash-balance-section-expanded?})]
    [:div.h6.bg-too-light-teal.p2

     [:div.letter-spacing-1.shout.dark-gray.mbnp5.flex.items-center
      [:a.black toggle-expand
       "Cash Balance"
       (svg/dropdown-arrow {:class  (str "ml1 stroke-dark-gray "
                                         (when expanded? "rotate-180"))
                            :style  {:stroke-width "2"}
                            :height ".75em"
                            :width  ".75em"})]]

     [:div.flex.items-center.justify-between
      [:a.col-5 toggle-expand
       [:div.h1.black.bold.flex (mf/as-money-without-cents cash-balance)]]
      [:div.col-5
       (if (payouts/cash-out-eligible? payout-method)
         (ui/teal-button
           {:height-class   "py2"
            :data-test      "cash-out-begin-button"
            :on-click       (utils/send-event-callback events/control-stylist-dashboard-cash-out-begin
                                                       {:amount cash-balance
                                                        :payout-method-name payout-method})
            :disabled?      (not (pos? cash-balance))
            :disabled-class "bg-gray"
            :spinning?      cashing-out?}
           [:div.flex.items-center.justify-center.regular.h5
            (ui/ucare-img {:width "28" :class "mr2 flex items-center"} "3d651ddf-b37d-441b-a162-b83728f2a2eb")
            "Cash Out"])
         [:div.h7.right
          "Cash out now with " [:a.teal (utils/fake-href events/navigate-stylist-account-commission) "Mayvenn InstaPay"]])]]
     [:div
      (when-not expanded? {:class "hide"})
      [:div.flex.mt2
       [:div.col-7
        (earnings-count "Monthly Earnings" (mf/as-money-without-cents monthly-earnings))]
       [:div.col-5
        (earnings-count "Lifetime Earnings" (mf/as-money-without-cents lifetime-earnings))]]
      [:div.flex.pt2
       [:div.col-7
        (earnings-count "Monthly Services" monthly-services)]
       [:div.col-5
        (earnings-count "Lifetime Services" lifetime-services)]]]]))

(defn ^:private store-credit-balance-card [total-available-store-credit lifetime-earned expanded?]
  (let [toggle-expand (utils/fake-href events/control-stylist-v2-dashboard-section-toggle
                                       {:keypath keypaths/aladdin-dashboard-store-credit-section-expanded?})]
   [:div.h6.bg-too-light-teal.p2
    [:div.letter-spacing-1.shout.dark-gray.mbnp5.flex.items-center
     [:a.black toggle-expand
      "Store Credit Balance"
      (svg/dropdown-arrow {:class  (str "ml1 stroke-dark-gray "
                                        (when expanded? "rotate-180"))
                           :style  {:stroke-width "2"}
                           :height ".75em"
                           :width  ".75em"})]]

    [:div.flex.items-center
     [:a.col-7 toggle-expand
      [:div.h1.black.bold.flex (mf/as-money-without-cents total-available-store-credit)]]
     [:div.col-5
      ;; TODO(JH + LE) This should really go somewhere.
      (ui/teal-button
        {:height-class "py2"
         :disabled? (zero? total-available-store-credit)}
        [:div.flex.items-center.justify-center.regular.h5
         (ui/ucare-img {:width "28" :class "mr2 flex items-center"} "81775e67-9a83-46b7-b2ae-1cdb5a737876")
         "Shop"])]]
    [:div.flex.pt2 {:class (when-not expanded? "hide")}
     [:div.col-7
      (earnings-count "Lifetime Bonuses" (mf/as-money-without-cents lifetime-earned))]]]
   ))

(defn ^:private sales-bonus-progress [{:keys [previous-level next-level award-for-next-level total-eligible-sales]}]
  [:div.p2
   [:div.h6.letter-spacing-1.shout.dark-gray "Sales Bonus Progress"]
   [:div.h7
    "Sell "
    (mf/as-money-without-cents (- next-level total-eligible-sales))
    " more in non-FREEINSTALL sales to earn your next "
    [:span.bold (mf/as-money-without-cents award-for-next-level)]
    " in credit."]
   [:div.mtp2
    (progress-indicator {:value   (- total-eligible-sales previous-level)
                         :maximum (- next-level previous-level)})]])

(def tabs
  [{:id :orders
    :title "Orders"
    :navigate events/navigate-stylist-v2-dashboard-orders}
   {:id :payments
    :title "Payments"
    :navigate events/navigate-stylist-v2-dashboard-payments}])

(defn balance-transfer->payment [{:keys [id type data] :as balance-transfer}]
  (let [order (:order data)]
    (merge {:id id
            :icon "68e6bcb0-a236-46fe-a8e7-f846fff0f464"
            :date (:created-at data)
            :subtitle ""
            :amount (mf/as-money-without-cents (:amount data))
            :amount-description nil
            :styles {:background ""
                     :title-color "black"
                     :amount-color "teal"}}
           (get {"commission" {:title (str "Commission Earned" (when-let [name (orders/first-name-plus-last-name-initial order)]
                                                                 (str " - " name)))
                               :data-test (str "commission-" id)
                               :date (:commission-date data)}
                 "award"      {:title "Incentive Payment"
                               :data-test (str "award-" id)}
                 "voucher_award" {:title (str "Service Payment" (when-let [name (orders/first-name-plus-last-name-initial order)]
                                                                  (str " - " name)))
                                  :data-test (str "voucher-award-" id)
                                  :subtitle (:campaign-name data)}
                 "payout" {:title "Money Transfer"
                           :icon "4939408b-1ec8-4a47-bb0e-5cdeb15d544d"
                           :data-test (str "payout-" id)
                           :amount-description (:payout-method-name data)
                           :styles {:background "bg-too-light-teal"
                                    :title-color "teal"
                                    :amount-color "teal"}}
                 "sales_bonus" {:title "Sales Bonus"
                                :data-test (str "sales-bonus-" id)
                                :icon "56bfbe66-6db0-48c7-9069-f86c6393b15d"}}
                type
                {:title "Unknown Payment"}))))

(defn payment-row [{:as item :keys [id icon title date subtitle amount amount-description styles data-test]}]
  (spice.core/spy item)
  (let [not-clickable? (= amount-description "Pending")]
   [:a.block.border-bottom.border-light-gray.px3.py2.flex.items-center
    (merge
     (utils/route-to events/navigate-stylist-dashboard-balance-transfer-details
                     {:balance-transfer-id id})
     {:key       id
      :data-test data-test
      :class     (:background styles)
      :style     (when not-clickable? {:pointer-events "none"})})
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
              ;; TODO: Future Us: Strictly aladdin services (not $100-off)
              :subtitle           (service-menu/discount->campaign-name discount)
              :amount             (service-menu/display-voucher-amount
                                   service-menu
                                   mf/as-money-without-cents pending-voucher)
              :amount-description "Pending"
              :styles             {:background   ""
                                   :title-color  "black"
                                   :amount-color "orange"}}]
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
       [:div {:key title}
        [:div.h7.bg-gray.px2.py1.medium title]
        ;; ASK: Sales Bonus row
        (for [item (reverse (sort-by :date items))]
          (payment-row item))])]))

(defn orders-table [orders]
  (for [order (vals orders)]
    [:div (:order-number order)]))

(defn ^:private ledger-tabs [active-tab-name]
  [:div.flex.flex-wrap
   (for [{:keys [id title navigate]} tabs]
     [:a.h6.col-6.p2.black
      (merge (utils/route-to navigate)
             {:key (name id)
              :data-test (str "nav-" (name id))
              :class (if (= id active-tab-name)
                       "bg-gray bold"
                       "bg-light-gray")})
      title])])

(defn ^:private empty-ledger [{:keys [empty-title empty-copy]}]
  [:div.my6.center
   [:h4.gray.bold.p1 empty-title]
   [:h6.dark-gray.col-5.mx-auto.line-height-2 empty-copy]])

(defn component
  [{:keys [fetching? cash-balance-section-expanded? store-credit-balance-section-expanded? stats total-available-store-credit activity-ledger-tab balance-transfers orders payout-method cashing-out? pending-voucher service-menu]} owner opts]
  (let [{:keys [bonuses earnings services]} stats
        {:keys [lifetime-earned]}           bonuses
        {:keys [active-tab-name]} activity-ledger-tab]
    (component/create
     (if fetching?
       [:div.my2.h2 ui/spinner]
       [:div
        [:div.p2
         (cash-balance-card payout-method cash-balance-section-expanded? cashing-out? earnings services)
         [:div.mt2 (store-credit-balance-card total-available-store-credit lifetime-earned store-credit-balance-section-expanded?)]
         (sales-bonus-progress bonuses)]

        (ledger-tabs active-tab-name)

        (case active-tab-name

          :payments
          (if (or (seq balance-transfers)
                  pending-voucher)
            (payments-table pending-voucher service-menu balance-transfers)
            (empty-ledger activity-ledger-tab))

          :orders
          (if (seq orders)
            (orders-table orders)
            (empty-ledger activity-ledger-tab)))]))))

(defn query
  [data]
  (let [get-balance-transfer  second
        id->balance-transfers (get-in data keypaths/stylist-earnings-balance-transfers)]
    {:fetching?                    (or (utils/requesting? data request-keys/get-stylist-balance-transfers)
                                       (utils/requesting? data request-keys/fetch-stylist-service-menu))
     :stats                        (get-in data keypaths/stylist-v2-dashboard-stats)
     :cashing-out?                 (utils/requesting? data request-keys/cash-out-commit)
     :payout-method                (get-in data keypaths/stylist-manage-account-chosen-payout-method)
     :activity-ledger-tab          ({events/navigate-stylist-v2-dashboard-payments {:active-tab-name :payments
                                                                                    :empty-copy      "Payments and bonus activity will appear here."
                                                                                    :empty-title     "No payments yet"}
                                     events/navigate-stylist-v2-dashboard-orders   {:active-tab-name :orders
                                                                                    :empty-copy      "Orders from your store will appear here."
                                                                                    :empty-title     "No orders yet"}}
                                    (get-in data keypaths/navigation-event))
     :cash-balance-section-expanded?         (get-in data keypaths/aladdin-dashboard-cash-balance-section-expanded?)
     :store-credit-balance-section-expanded? (get-in data keypaths/aladdin-dashboard-store-credit-section-expanded?)
     :total-available-store-credit (get-in data keypaths/user-total-available-store-credit)
     :balance-transfers            (into []
                                         (comp
                                          (map get-balance-transfer)
                                          (remove (fn [transfer]
                                                    (when-let [status (-> transfer :data :status)]
                                                      (not= "paid" status)))))
                                         id->balance-transfers)
     :orders                       (get-in data keypaths/stylist-v2-dashboard-sales-elements)
     :service-menu                 (get-in data keypaths/stylist-service-menu)
     :pending-voucher              (get-in data voucher-keypaths/voucher-response)}))

(defn built-component [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-stylist-v2-dashboard-payments [_ event args _ app-state]
  (if (experiments/aladdin-dashboard? app-state)
    (let [stylist-id (get-in app-state keypaths/store-stylist-id)
          user-id    (get-in app-state keypaths/user-id)
          user-token (get-in app-state keypaths/user-token)]
      (messages/handle-message events/stylist-v2-dashboard-stats-fetch)
      (api/get-stylist-dashboard-balance-transfers stylist-id
                                                   user-id
                                                   user-token
                                                   (get-in app-state keypaths/stylist-earnings-pagination)
                                                   #(messages/handle-message events/api-success-stylist-v2-dashboard-balance-transfers
                                                                             (select-keys % [:balance-transfers :pagination]))))
    (effects/redirect events/navigate-stylist-dashboard-earnings)))

(defmethod effects/perform-effects events/navigate-stylist-v2-dashboard-orders [_ event args _ app-state]
  (if (experiments/aladdin-dashboard? app-state)
    (let [stylist-id (get-in app-state keypaths/store-stylist-id)
          user-id    (get-in app-state keypaths/user-id)
          user-token (get-in app-state keypaths/user-token)]
      (messages/handle-message events/stylist-v2-dashboard-stats-fetch)
      (api/get-stylist-dashboard-sales stylist-id
                                       user-id
                                       user-token
                                       (get-in app-state keypaths/stylist-v2-dashboard-sales-pagination)
                                       #(messages/handle-message events/api-success-stylist-v2-dashboard-sales
                                                                 (select-keys % [:sales :pagination]))))
    (effects/redirect events/navigate-stylist-dashboard-earnings)))

(defmethod effects/perform-effects events/stylist-v2-dashboard-stats-fetch [_ event args _ app-state]
  (let [stylist-id (get-in app-state keypaths/store-stylist-id)
        user-id    (get-in app-state keypaths/user-id)
        user-token (get-in app-state keypaths/user-token)]
    (when (and user-id user-token)
      (api/fetch-stylist-service-menu (get-in app-state keypaths/api-cache)
                                      {:user-id    user-id
                                       :user-token user-token
                                       :stylist-id stylist-id})
      (api/get-stylist-account user-id user-token)
      (api/get-stylist-dashboard-stats events/api-success-stylist-v2-dashboard-stats
                                       stylist-id
                                       user-id
                                       user-token))))

(defmethod transitions/transition-state events/api-success-stylist-v2-dashboard-stats
  [_ event {:as stats :keys [stylist earnings services store-credit-balance bonuses]} app-state]
  (-> app-state
      (assoc-in keypaths/stylist-v2-dashboard-stats stats)))

(defn most-recent-voucher-award [balance-transfers]
  (->> balance-transfers
       (filter #(= (:type %) "voucher_award"))
       (sort-by :transfered-at)
       last))

(defmethod transitions/transition-state events/api-success-stylist-v2-dashboard-balance-transfers
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

(defmethod transitions/transition-state events/control-stylist-v2-dashboard-section-toggle
  [_ event {:keys [keypath]} app-state]
  (update-in app-state keypath not))

(defmethod transitions/transition-state events/api-success-stylist-v2-dashboard-sales
  [_ event {:keys [sales pagination]} app-state]
  (-> app-state
      (update-in keypaths/stylist-v2-dashboard-sales-elements merge sales)
      (assoc-in keypaths/stylist-v2-dashboard-sales-pagination pagination)))
