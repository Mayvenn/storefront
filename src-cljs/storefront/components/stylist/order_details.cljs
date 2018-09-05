(ns storefront.components.stylist.order-details
  (:require [spice.date :as date]
            [checkout.cart :as cart]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.sales :as sales]
            [storefront.accessors.experiments :as experiments]
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

;; TODO Remove handling of underscored keys after storeback has been deployed.

(defn ^:private back-button [back v2-dashboard?]
  [:a.col-12.dark-gray.flex.items-center.py3
   (merge
    {:data-test "back-link"}
    (utils/route-back-or-to back
                            (if v2-dashboard?
                              events/navigate-v2-stylist-dashboard-orders
                              events/navigate-stylist-dashboard-earnings)))
   (ui/back-caret "Back")])

(defn ^:private info-block [header content]
  [:div.align-top.mb2
   [:span.h6.dark-gray.shout header]
   [:div.h6.medium (or content "--")]])

(defn ^:private info-columns [[left-header left-content] [right-header right-content]]
  [:div.col-12.pt2
   [:div.inline-block.col-6
    (info-block left-header left-content)]
   [:div.inline-block.col-6
    (info-block right-header right-content)]])

(defn ^:private fmt-with-leading-zero
  "takes number and formats it prepending zero (if n < 10)
  examples: 1 => \"01\"  31 => \"31\""
  [n]
  (cljs.pprint/cl-format nil "~2,'0D" n))

(defn ^:private display-line-item
  ([line-item] (display-line-item line-item true))
  ([{:keys [product-title color-name unit-price quantity sku id legacy/variant-id variant-attrs]} show-price?]
   [:div.h6.pb2 {:key (or variant-id id)}
    [:div.medium {:data-test (str "line-item-title-" sku)} product-title]
    [:div {:data-test (str "line-item-color-" sku)} color-name]
    (when show-price?
      [:div {:data-test (str "line-item-price-ea-" sku)} "Price: " (mf/as-money-without-cents unit-price) " ea"])
    [:div
     (when-let [length (:length variant-attrs)]
       [:span {:data-test (str "line-item-length-" sku)} length "” " ])
     [:span {:data-test (str "line-item-quantity-" sku)} "(Qty: " quantity ")"]]]))

(defn ^:private shipment-details [order line-items]
  (let [{:keys [shipments]}        order
        n                          (fmt-with-leading-zero (count shipments))
        {:keys [shipped-at state]} (last shipments)]
    [:div
     [:div "Latest Shipment "
      [:span (str n " of " n)]]
     (info-columns
      ["shipped date" (some-> shipped-at f/long-date)]
      ["status" state])
     [:div.align-top.mb2
      [:span.h6.dark-gray.shout "order details"]
      (for [line-item line-items]
        (display-line-item line-item false))]]))

(defn ^:private get-user-info [app-state]
  {:user-id (get-in app-state keypaths/user-id)
   :user-token (get-in app-state keypaths/user-token)})

(defmethod effects/perform-effects events/navigate-stylist-dashboard-order-details
  [_ event {:keys [order-number] :as args} _ app-state]
  (let [user-info  (get-user-info app-state)
        stylist-id (get-in app-state keypaths/store-stylist-id)
        handler    #(messages/handle-message events/api-success-v2-stylist-dashboard-sale %)
        params     (merge {:stylist-id   stylist-id
                           :order-number order-number
                           :handler      handler} user-info)]
    (when (:user-token user-info)
      (api/get-stylist-dashboard-sale params))))

(defmethod transitions/transition-state events/api-success-v2-stylist-dashboard-sale
  [_ _ single-sale-map app-state]
  (update-in app-state keypaths/v2-dashboard-sales-elements merge single-sale-map))

(defn query [data]
  (let [order-number (:order-number (get-in data keypaths/navigation-args))
        sale         (->> (get-in data keypaths/v2-dashboard-sales-elements)
                          vals
                          (filter (fn [sale] (= order-number (:order-number sale))))
                          first)
        line-items   (->> (:order sale)
                          orders/first-commissioned-shipment
                          orders/product-items-for-shipment)]
    {:sale          sale
     :loading?      (utils/requesting? data request-keys/get-stylist-dashboard-sale)
     :v2-dashboard? (experiments/v2-dashboard? data)
     :back          (first (get-in data keypaths/navigation-undo-stack))
     :line-items    (mapv (partial cart/add-product-title-and-color-to-line-item
                                   (get-in data keypaths/v2-products)
                                   (get-in data keypaths/v2-facets))
                          line-items)}))

(defn component [{:keys [sale v2-dashboard? loading? back line-items]} owner opts]
  (let [{:keys [order-number
                voucher-type
                placed-at
                order]} sale]
    (component/create
     (if (or (not order-number) loading?)
       [:div.my6.h2 ui/spinner]
       [:div.container.mb4.px3
        (back-button back v2-dashboard?)
        [:div
         [:div.col.col-1.px2 (svg/box-package {:height 18
                                               :width  25})]
         [:div.col.col-9.pl2
          [:h4.col-12.left.medium.pb4 (orders/first-name-plus-last-name-initial order)]
          (info-columns
           ["order number" order-number]
           ["voucher type" voucher-type])
          (info-columns
           ["order date" (f/long-date placed-at)]
           ["voucher status" (-> sale
                                 sales/voucher-status
                                 sales/voucher-status->copy)])
          (shipment-details order line-items)]]])) ))

(defn built-component [data opts]
  (component/build component (query data) opts))
