(ns stylist.dashboard-payments-tab
  (:require [spice.core :as spice]
            [spice.date :as date]
            [spice.maps :as maps]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.service-menu :as service-menu]
            #?@(:cljs
                [[storefront.api :as api]
                 [storefront.components.stylist.pagination :as pagination]])
            [storefront.components.formatters :as f]
            [storefront.components.money-formatters :as mf]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.transitions :as transitions]
            [voucher.keypaths :as voucher-keypaths]
            [clojure.string :as string]))

(defn balance-transfer->payment [balance-transfer]
  (let [{:keys [id type data]} balance-transfer

        {:keys [order created-at amount
                campaign-name reason commission-date
                payout-method-name
                voucher-uuid voucher
                payee-name]}
        data]
    (merge {:id                 id
            :icon               "68e6bcb0-a236-46fe-a8e7-f846fff0f464"
            :date               created-at
            :subtitle           ""
            :amount             (mf/as-money amount)
            :amount-description nil
            :styles             {:background   ""
                                 :title-color  "black"
                                 :amount-color "p-color"}}
           (case type
             "commission"    {:title     (str "Commission Earned"
                                              (when-let [name (orders/first-name-plus-last-name-initial order)]
                                                (str " - " name)))
                              :data-test (str "commission-" (:number order))
                              :date      commission-date}
             "award"         {:title     reason
                              :data-test (str "award-" id)}
             "payment_award" {:title     (str "Stylist Payment"
                                              (when payee-name
                                                (str " - " payee-name)))
                              :data-test (str "award-" id)}
             "voucher_award" {:title     (str "Service Payment"
                                              (when-let [name (orders/first-name-plus-last-name-initial order)]
                                                (str " - " name)))
                              :data-test (str "voucher-award-" id)
                              :subtitle  (let [fix-uuid-key #(string/replace (name %) #"-" "")]
                                           (or (->> voucher
                                                    ;; This beaut is to deal with storeback kabobify weirdness
                                                    (filter (fn [[k _]] (= (fix-uuid-key k)
                                                                           (fix-uuid-key voucher-uuid))))
                                                    first
                                                    last
                                                    :services
                                                    (filter #(or (= "base" (:service/type %))
                                                                 (= "base" (:type %))))
                                                    first
                                                    :product-name)
                                               campaign-name))}
             "payout"        {:title              "Money Transfer"
                              :icon               "4939408b-1ec8-4a47-bb0e-5cdeb15d544d"
                              :data-test          (str "payout-" id)
                              :amount-description payout-method-name
                              :styles             {:background   "bg-cool-gray"
                                                   :title-color  "p-color"
                                                   :amount-color "p-color"}}
             "sales_bonus"   {:title     "Sales Bonus"
                              :data-test (str "sales-bonus-" id)
                              :icon      "56bfbe66-6db0-48c7-9069-f86c6393b15d"}
             {:title "Unknown Payment"}))))

(defn payment-row [item]
  (let [{:keys [id icon title date subtitle amount amount-description styles data-test non-clickable?]} item]
    [:a.block.border-bottom.border-cool-gray.px3.py2.flex.items-center
     (merge
      (utils/route-to events/navigate-stylist-dashboard-balance-transfer-details
                      {:balance-transfer-id id})
      {:key       (str "payment" id)
       :data-test data-test
       :class     (:background styles)
       :style     (when non-clickable? {:pointer-events "none"})})
     (ui/ucare-img {:width 20 :alt ""} icon)
     [:div.flex-auto.mx3
      [:div.title-3.canela {:class (:title-color styles)} title]
      [:div.flex.h8
       [:div.mr4 #?(:cljs (f/long-date date))]
       subtitle]]
     [:div.right-align
      [:div.bold {:class (:amount-color styles)} amount]
      [:div.h8 (or amount-description ui/nbsp)]]]))

(defn group-payments-by-month [payments]
  #?(:cljs
     (let [year-month           (fn [{:keys [date]}]
                                  (let [[year month _] (f/date-tuple date)]
                                    [year month]))
           year-month->payments (group-by year-month (reverse payments))
           sorted-year-months   (reverse (sort (keys year-month->payments)))]
       (for [[year month :as ym] sorted-year-months]
         {:title (str (get f/month-names month) " " year)
          :items (year-month->payments ym)}))))

(def empty-payments
  [:div.my6.center
   [:div.canela.title-3.gray-700.bold.p1 "No payments yet"]
   [:div.content-3.col-5.mx-auto.line-height-2 "Payments and bonus activity will appear here"]])

(defn payments-table [pending-voucher-row balance-transfers pagination fetching?]
  (let [payments (map balance-transfer->payment balance-transfers)
        sections (group-payments-by-month payments)
        {current-page :page total-pages :total} pagination]
    [:div
     {:data-test "payments-tab"}
     (cond
       (and (nil? pending-voucher-row) (empty? payments)) empty-payments

       fetching?
       [:div.my2.h2 ui/spinner]

       :else
       [:div
        [:div.col-12.mb3
         (when pending-voucher-row
           (payment-row pending-voucher-row))
         (for [{:keys [title items] :as section} sections]
           [:div {:key (str "payments-table-" title)}
            [:div.h8.bg-cool-gray.px2.py1.medium title]
            ;; ASK: Sales Bonus row
            (for [item (reverse (sort-by :date items))]
              (payment-row item))])]
        #?(:cljs
           (pagination/fetch-more events/control-v2-stylist-dashboard-balance-transfers-load-more
                                  fetching?
                                  current-page
                                  total-pages))])]))

(defmethod effects/perform-effects events/navigate-v2-stylist-dashboard-payments [_ event args _ app-state]
  (let [no-balance-transfers-loaded? (empty? (get-in app-state keypaths/v2-dashboard-balance-transfers-pagination-ordering))]
    (when (and no-balance-transfers-loaded?
               (->> app-state auth/signed-in auth/stylist?))
      (messages/handle-message events/v2-stylist-dashboard-balance-transfers-fetch))))

(defmethod effects/perform-effects events/v2-stylist-dashboard-balance-transfers-fetch [_ event args _ app-state]
  #?(:cljs
     (let [stylist-id (get-in app-state keypaths/user-store-id)
           user-id    (get-in app-state keypaths/user-id)
           user-token (get-in app-state keypaths/user-token)]
       (api/get-stylist-dashboard-balance-transfers stylist-id
                                                    user-id
                                                    user-token
                                                    (get-in app-state keypaths/v2-dashboard-balance-transfers-pagination)
                                                    #(messages/handle-message events/api-success-v2-stylist-dashboard-balance-transfers
                                                                              (select-keys % [:balance-transfers :pagination]))))))

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
        voucher-response-date          (some->> (get-in app-state voucher-keypaths/voucher-redeemed-response)
                                                ((some-fn :date :redemption-date))
                                                date/to-millis)
        voucher-pending?               (> (or voucher-response-date 0) (or most-recent-voucher-award-date 0))]
    (-> (if voucher-pending? app-state (update-in app-state voucher-keypaths/voucher dissoc :response))
        (update-in keypaths/v2-dashboard-balance-transfers-elements merge (maps/map-keys (comp spice/parse-int name) balance-transfers))
        (assoc-in keypaths/v2-dashboard-balance-transfers-pagination pagination))))

(defmethod effects/perform-effects events/control-v2-stylist-dashboard-balance-transfers-load-more [_ _ args _ app-state]
  (messages/handle-message events/v2-stylist-dashboard-balance-transfers-fetch))

(defmethod transitions/transition-state events/control-v2-stylist-dashboard-balance-transfers-load-more [_ args _ app-state]
  (update-in app-state keypaths/v2-dashboard-balance-transfers-pagination-page inc))
