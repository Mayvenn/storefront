(ns storefront.components.stylist.earnings
  (:require [clojure.string :as str]
            goog.string
            [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.accessors.images :as images]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.stylist-urls :as stylist-urls]
            [storefront.components.formatters :as f]
            [storefront.components.money-formatters :as mf]
            [storefront.components.stylist.pagination :as pagination]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [storefront.platform.messages :as messages]
            [storefront.transitions :as transitions]
            [storefront.effects :as effects]
            [spice.maps :as maps]
            [storefront.api :as api]))

(defn commission-row
  [row-number orders balance-transfer]
  (let [{:keys [id]} balance-transfer
        {:keys [amount commission-date commissionable-amount order-number]} (:data balance-transfer)
        order (get orders (keyword order-number))]
    [:tr.pointer (merge {:key (str "balance-transfer-" id)
                         :data-test (str "commission-" order-number)}
                        (utils/route-to events/navigate-stylist-dashboard-balance-transfer-details {:balance-transfer-id id})
                        (when (odd? row-number)
                          {:class "bg-too-light-teal"}))
     [:td.px3.py2 (f/less-year-more-day-date commission-date)]
     [:td.py2 (:full-name order) [:div.h6 "Commission Earned"]]
     [:td.pr3.py2.green.right-align "+" (mf/as-money amount)]]))

(defn award-row [row-number balance-transfer]
  (let [{:keys [id amount created-at reason] :as award} (:data balance-transfer)]
    [:tr.pointer (merge {:key (str "award-" id)}
                        (utils/route-to events/navigate-stylist-dashboard-balance-transfer-details {:balance-transfer-id id})
                        (when (odd? row-number)
                          {:class "bg-too-light-teal"}))
     [:td.px3.py2 (f/less-year-more-day-date created-at)]
     [:td.py2 "Account Correction" [:div.h6 reason]]
     [:td.pr3.py2.green.right-align "+" (mf/as-money amount)]]))

(defn payout-row [balance-transfer]
  (let [{:keys [id]} balance-transfer
        {:keys [amount created-at payout-method-name]} (:data balance-transfer)]
    [:tr.pointer.bg-light-gray
     (merge {:key (str "payout-" id)}
            (utils/route-to events/navigate-stylist-dashboard-balance-transfer-details {:balance-transfer-id id}))
     [:td.px3.py2 (f/less-year-more-day-date created-at)]
     [:td.py2 {:col-span 2} "You transferred " [:span.medium (mf/as-money amount)]
      [:div.h6 (str "Earnings Transfer - " payout-method-name)]]]))

(defn earnings-table [orders balance-transfers]
  [:table.col-12.mb3 {:style {:border-spacing 0}}
   [:tbody
    (map-indexed
     (fn [i {:keys [type data] :as balance-transfer}]
       (case type
         "commission" (commission-row i orders balance-transfer)
         "award"      (award-row i balance-transfer)
         "payout"     (payout-row balance-transfer)))
     balance-transfers)]])

(def empty-commissions
  (html
   [:div.center
    [:div.p2.border-bottom.border-light-gray
     [:div.img-receipt-icon.bg-no-repeat.bg-center {:style {:height "8em"}}]
     [:p.h3.gray "Looks like you don't have any commissions yet."]]
    [:.py3.h4
     [:p.mx4.pb2 "Get started by sharing your store with your clients:"]
     [:p.medium stylist-urls/store-url]]]))

(defn show-commission-rate [rate]
  (let [message (list "Earn " rate "% commission on all sales. (tax and store credit excluded)")]
    [:div.h6.dark-gray
     [:div.p2.hide-on-mb
      [:div.mb1.center svg/micro-dollar-sign]
      [:div message]]
     [:div.my3.hide-on-tb-dt
      [:div.center message]]]))

(def show-program-terms
  [:div.col-on-tb-dt.col-12-on-tb-dt
   [:div.border-top.border-gray.mx-auto.my2 {:style {:width "100px"}}]
   [:div.center.my2.h6
    [:a.dark-gray (utils/route-to events/navigate-content-program-terms) "Mayvenn Program Terms"]]])

(defn component [{:keys [balance-transfers orders pagination stylist fetching?]}]
  (om/component
   (let [{current-page :page total-pages :total} pagination]
     (html
      (if (and (empty? balance-transfers)
               fetching?)
        [:div.my2.h2 ui/spinner]
        [:div.clearfix
         {:data-test "earnings-panel"}
         [:div.col-on-tb-dt.col-9-on-tb-dt
          (when (seq balance-transfers)
            (earnings-table orders balance-transfers))
          (pagination/fetch-more events/control-stylist-balance-transfers-load-more
                                 fetching?
                                 current-page
                                 total-pages)
          (when (zero? total-pages)
            empty-commissions)]

         [:div.col-on-tb-dt.col-3-on-tb-dt
          (when (:commission-rate stylist)
            (show-commission-rate (:commission-rate stylist)))
          show-program-terms]])))))

(defn query [data]
  (let [transfer-index    (mapcat val (get-in data keypaths/stylist-earnings-balance-transfers-index))
        balance-transfers (get-in data keypaths/stylist-earnings-balance-transfers)
        orders            (get-in data keypaths/stylist-earnings-orders)]
    {:balance-transfers (into []
                              (comp
                               (map (partial get balance-transfers))
                               (remove (fn [transfer]
                                         (when-let [status (-> transfer :data :status)]
                                           (not= "paid" status)))))
                              transfer-index)
     :orders            orders
     :pagination        (get-in data keypaths/stylist-earnings-pagination)
     :stylist           (get-in data keypaths/stylist)
     :fetching?         (utils/requesting? data request-keys/get-stylist-balance-transfers)}))

(defmethod effects/perform-effects events/api-success-stylist-balance-transfers [_ event {:keys [orders]} _ app-state]
  (messages/handle-message events/ensure-skus {:skus (->> orders
                                                          (mapcat orders/product-items)
                                                          (map :sku)
                                                          set)}))

(defmethod effects/perform-effects events/control-stylist-balance-transfers-load-more [_ _ args _ app-state]
  (messages/handle-message events/stylist-balance-transfers-fetch args))

(defmethod transitions/transition-state events/control-stylist-balance-transfers-load-more [_ args _ app-state]
  (update-in app-state keypaths/stylist-earnings-pagination-page inc))

(defmethod effects/perform-effects events/stylist-balance-transfers-fetch [_ _ args _ app-state]
  (let [stylist-id (get-in app-state keypaths/store-stylist-id)
        user-id    (get-in app-state keypaths/user-id)
        user-token (get-in app-state keypaths/user-token)]
    (when (and user-id user-token)
      (api/get-stylist-balance-transfers stylist-id
                                         user-id
                                         user-token
                                         (get-in app-state keypaths/stylist-earnings-pagination)
                                         #(messages/handle-message events/api-success-stylist-balance-transfers
                                                                   (select-keys % [:stylist
                                                                                   :balance-transfers
                                                                                   :orders
                                                                                   :pagination]))))))

(defmethod effects/perform-effects events/navigate-stylist-dashboard-earnings [_ event args _ app-state]
  (messages/handle-message events/stylist-balance-transfers-fetch))

(defmethod transitions/transition-state events/navigate-stylist-dashboard-earnings
  [_ event {:keys [stylist balance-transfers orders pagination]} app-state]
  (-> app-state
      (assoc-in keypaths/stylist-earnings-pagination {:page 1 :per 15})))

(defmethod transitions/transition-state events/api-success-stylist-balance-transfers
  [_ event {:keys [stylist balance-transfers orders pagination]} app-state]
  (let [page (:page pagination)]
    (-> app-state
        (update-in keypaths/stylist merge stylist)
        (update-in keypaths/stylist-earnings-orders merge orders)
        (update-in keypaths/stylist-earnings-balance-transfers merge (maps/index-by :id balance-transfers))
        (assoc-in (conj keypaths/stylist-earnings-balance-transfers-index page) (map :id balance-transfers))
        (assoc-in keypaths/stylist-earnings-pagination pagination))))
