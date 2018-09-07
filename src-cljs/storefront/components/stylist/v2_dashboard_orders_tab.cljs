(ns storefront.components.stylist.v2-dashboard-orders-tab
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
            [voucher.keypaths :as voucher-keypaths]
            [storefront.components.stylist.pagination :as pagination]))

(defn status-cell [[span classes] text]
  [:td.p2.h7.center.medium {:col-span span}
   [:span {:class classes} text]])

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

(defn sales-table [sales sales-pagination fetching-sales?]
  (let [{current-page :page
         total-pages  :total
         ordering     :ordering} sales-pagination]
    [:div
     [:table.col-12 {:style {:border-collapse "collapse"}}
      [:thead.bg-silver.border-0
       [:tr.h7.medium
        [:th.px2.py1.left-align.medium.col-3.nowrap "Order Updated"]
        [:th.px2.py1.left-align.medium "Client"]
        [:th.px2.py1.center.medium.col-1 "Delivery"]
        [:th.px2.py1.center.medium.col-1 "Voucher"]]]
      [:tbody
       (for [sale (map sales ordering)
             :let [{:keys [id
                           order-number
                           order
                           order-updated-at]} sale]]
         [:tr.border-bottom.border-gray.py2.pointer.fate-white-hover
          (merge (utils/route-to events/navigate-stylist-dashboard-order-details {:order-number order-number})
                 {:key       (str "sales-table-" id)
                  :data-test (str "sales-" order-number)})
          [:td.p2.left-align.dark-gray.h7.col-3 (some-> order-updated-at f/abbr-date)]
          [:td.p2.left-align.medium.col-3.h5
           {:style {:overflow-x :hidden :max-width 120 :text-overflow :ellipsis}}  ; For real long first names
           (some-> order orders/first-name-plus-last-name-initial)]
          (sale-status-cell sale)
          (voucher-status-cell sale)])]]
     (pagination/fetch-more events/control-stylist-sales-load-more
                            fetching-sales?
                            current-page
                            total-pages)]))

(defmethod effects/perform-effects events/navigate-v2-stylist-dashboard-orders [_ event args _ app-state]
  (messages/handle-message events/stylist-sales-fetch))

(defmethod effects/perform-effects events/stylist-sales-fetch [_ event args _ app-state]
  (let [stylist-id (get-in app-state keypaths/store-stylist-id)
        user-id    (get-in app-state keypaths/user-id)
        user-token (get-in app-state keypaths/user-token)]
    (api/get-stylist-dashboard-sales stylist-id
                                     user-id
                                     user-token
                                     (get-in app-state keypaths/v2-dashboard-sales-pagination)
                                     #(messages/handle-message events/api-success-v2-stylist-dashboard-sales
                                                               (select-keys % [:sales :pagination])))))

(defmethod transitions/transition-state events/api-success-v2-stylist-dashboard-sales
  [_ event {:keys [sales pagination]} app-state]
  (let [old-ordering (:ordering (get-in app-state keypaths/v2-dashboard-sales-pagination))
        new-ordering (concat old-ordering (:ordering pagination))
        new-pagination (assoc pagination :ordering new-ordering)]
    (-> app-state
        (update-in keypaths/v2-dashboard-sales-elements merge (maps/map-keys (comp spice/parse-int name) sales))
        ;; TODO: Do this with an update-in and without so much letting
        (assoc-in keypaths/v2-dashboard-sales-pagination new-pagination))))

(defmethod effects/perform-effects events/control-stylist-sales-load-more [_ _ args _ app-state]
  (messages/handle-message events/stylist-sales-fetch))

(defmethod transitions/transition-state events/control-stylist-sales-load-more [_ args _ app-state]
  (update-in app-state keypaths/v2-dashboard-sales-pagination-page inc))
