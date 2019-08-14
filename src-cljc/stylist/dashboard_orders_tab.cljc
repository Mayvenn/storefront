(ns stylist.dashboard-orders-tab
  (:require [spice.core :as spice]
            [spice.maps :as maps]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.sales :as sales]
            #?@(:cljs
               [[storefront.api :as api]
                [storefront.components.stylist.pagination :as pagination]])
            [storefront.component :as component]
            [storefront.components.formatters :as f]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.request-keys :as request-keys]
            [storefront.transitions :as transitions]
            [storefront.accessors.experiments :as experiments]))

(defn status-cell [[span classes] text]
  [:td.p2.h8.center.medium {:col-span span}
   [:span {:class classes} text]])

(defn status->appearance [status]
  (case status
    :sale/shipped     [1 ["titleize" "teal"] ]
    :sale/returned    [2 ["shout"    "error"]]
    :sale/pending     [2 ["shout"    "yellow"]]
    :sale/unknown     [2 ["shout"    "error"]]
    :voucher/pending  nil
    :voucher/returned nil
    :voucher/redeemed [1 ["titleize" "teal"]]
    :voucher/expired  [1 ["titleize" "error"]]
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

(def empty-ledger
  [:div.my6.center
   [:h4.gray.bold.p1 "No orders yet"]
   [:h6.dark-gray.col-5.mx-auto.line-height-2 "Orders from your store will appear here."]])

(def show-program-terms
  [:div.mx-auto
   [:div.border-top.border-gray.mx-auto.my2 {:style {:width "100px"}}]
   [:div.center.my2.h6
    [:a.dark-gray (utils/route-to events/navigate-content-program-terms) "Mayvenn Program Terms"]]])

(defn component
  [{:keys [sales-ui pagination-ui fetching-data? header-ui]}]
  (component/create
   (cond
     sales-ui
     [:div
      {:data-test "orders-tab"}
      [:table.col-12 {:style {:border-collapse "collapse"}}
       header-ui
       [:tbody
        sales-ui]]
      pagination-ui
      show-program-terms]

     fetching-data?
     [:div.my2.h2 ui/spinner]

     :else empty-ledger)))

(defn sale-row [{:keys [order order-number id order-updated-at] :as sale}]
  [:tr.border-bottom.border-gray.py2.pointer.fate-white-hover
   (merge (utils/route-to events/navigate-stylist-dashboard-order-details {:order-number order-number})
          {:key       (str "sales-table-" id)
           :data-test (str "sales-" order-number)})
   [:td.p2.left-align.dark-gray.h8 (some-> order-updated-at #?(:cljs f/abbr-date))]
   [:td.p2.left-align.medium.h5.nowrap
    {:style {:overflow-x :hidden :max-width 120 :text-overflow :ellipsis}}  ; For really long first names
    (some-> order orders/first-name-plus-last-name-initial)]
   (sale-status-cell sale)])

(defn sale-row-with-voucher [{:keys [order order-number id order-updated-at] :as sale}]
  [:tr.border-bottom.border-gray.py2.pointer.fate-white-hover
   (merge (utils/route-to events/navigate-stylist-dashboard-order-details {:order-number order-number})
          {:key       (str "sales-table-" id)
           :data-test (str "sales-" order-number)})
   [:td.p2.left-align.dark-gray.h8 (some-> order-updated-at #?(:cljs f/abbr-date))]
   [:td.p2.left-align.medium.h5.nowrap
    {:style {:overflow-x :hidden :max-width 120 :text-overflow :ellipsis}}  ; For really long first names
    (some-> order orders/first-name-plus-last-name-initial)]
   (sale-status-cell sale)
   (voucher-status-cell sale)])

(defn header-ui [show-voucher-elements?]
  [:thead.bg-silver.border-0
   [:tr.h8.medium
    [:th.px2.py1.left-align.medium.col-2.nowrap
     {:data-test "header-order-updated"}
     "Order Updated"]
    [:th.px2.py1.left-align.medium
     {:data-test "header-client"
      :class (if show-voucher-elements? "col-8" "col-5")}
     "Client"]
    [:th.px2.py1.center.medium
     {:data-test "header-delivery"
      :class (if show-voucher-elements? "col-1" "col-4")}
     "Delivery"]
    (when show-voucher-elements?
      [:th.px2.py1.center.medium.col-1
       {:data-test "header-voucher"}
       "Voucher"])]])

(defn query [data]
  (let [fetching-data?                (utils/requesting? data request-keys/get-stylist-dashboard-sales)
        {:keys [page total ordering]} (get-in data keypaths/v2-dashboard-sales-pagination)
        sales-sorted                  (map (get-in data keypaths/v2-dashboard-sales-elements) ordering)
        show-voucher-elements?        (experiments/dashboard-with-vouchers? data)
        sale-row-fn                   (if show-voucher-elements? sale-row-with-voucher sale-row)]
    {:sales-ui       (mapv sale-row-fn sales-sorted)
     :fetching-data? fetching-data?
     :pagination-ui  #?(:cljs (pagination/fetch-more events/control-v2-stylist-dashboard-sales-load-more
                                                     fetching-data? page total)
                        :clj nil)
     :header-ui      (header-ui show-voucher-elements?)}))

(defmethod effects/perform-effects events/navigate-v2-stylist-dashboard-orders [_ event args _ app-state]
  (let [no-orders-loaded? (empty? (get-in app-state keypaths/v2-dashboard-sales-pagination))]
    (when (and no-orders-loaded?
               (->> app-state auth/signed-in auth/stylist?))
      (messages/handle-message events/v2-stylist-dashboard-sales-fetch))))

(defmethod effects/perform-effects events/v2-stylist-dashboard-sales-fetch [_ event args _ app-state]
  #?(:cljs
     (let [stylist-id (get-in app-state keypaths/user-store-id)
           user-id    (get-in app-state keypaths/user-id)
           user-token (get-in app-state keypaths/user-token)]
       (api/get-stylist-dashboard-sales stylist-id
                                        user-id
                                        user-token
                                        (get-in app-state keypaths/v2-dashboard-sales-pagination)
                                        #(messages/handle-message events/api-success-v2-stylist-dashboard-sales
                                                                  (select-keys % [:sales :pagination]))))))

(defmethod transitions/transition-state events/api-success-v2-stylist-dashboard-sales
  [_ event {:keys [sales pagination]} app-state]
  (let [old-ordering (get-in app-state keypaths/v2-dashboard-sales-pagination-ordering)
        new-pagination (update pagination :ordering #(concat old-ordering %))]
    (-> app-state
        (update-in keypaths/v2-dashboard-sales-elements merge (maps/map-keys (comp spice/parse-int name) sales))
        (assoc-in keypaths/v2-dashboard-sales-pagination new-pagination))))

(defmethod effects/perform-effects events/control-v2-stylist-dashboard-sales-load-more [_ _ args _ app-state]
  (messages/handle-message events/v2-stylist-dashboard-sales-fetch))

(defmethod transitions/transition-state events/control-v2-stylist-dashboard-sales-load-more [_ args _ app-state]
  (update-in app-state keypaths/v2-dashboard-sales-pagination-page inc))
