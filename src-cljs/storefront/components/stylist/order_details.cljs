(ns storefront.components.stylist.order-details
  (:require [checkout.cart :as cart]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.sales :as sales]
            [storefront.accessors.shipping :as shipping]
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as component]
            [storefront.components.formatters :as f]
            [storefront.components.stylist.line-items :as line-items]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.transitions :as transitions]
            [storefront.effects :as effects]
            [storefront.request-keys :as request-keys]
            [storefront.api :as api]
            [clojure.string :as string]
            [spice.core :as spice]
            cljs.pprint))

;; TODO Remove handling of underscored keys after storeback has been deployed.

(defn ^:private back-button [back]
  [:a.col-12.dark-gray.flex.items-center.py3
   (merge
     {:data-test "back-link"}
     (utils/route-back-or-to back events/navigate-v2-stylist-dashboard-orders))
   (ui/back-caret "Back" "18px")])

(defn ^:private info-block [header content]
  [:div.align-top.pt2.mb2.h6
   [:span.dark-gray.shout header]
   [:div.medium
    {:data-test (str "info-block-" (-> header string/lower-case (string/replace #" " "-")))}
    (or content "--")]])

(defn ^:private info-columns [[left-header left-content] [right-header right-content]]
  [:div.col-12.pt2.h6
   [:div.col.col-6
    (info-block left-header left-content)]
   [:div.col.col-6
    (info-block right-header right-content)]])

(defn ^:private fmt-with-leading-zero
  "takes number and formats it prepending zero (if n < 10)
  examples: 1 => \"01\"  31 => \"31\""
  [n]
  (cljs.pprint/cl-format nil "~2,'0D" n))

(defn shipment-status-field [shipping-details shipment-count]
  (if (= "shipped" (:state shipping-details))
    [:span
     [:span {:data-test (str "shipment-" shipment-count "-status")} "Shipped"]
     [:span " (" (:name shipping-details) ")"]]
    [:span {:data-test (str "shipment-" shipment-count "-status")} "Processing"]))

(defn ^:private shipment-details [{:as shipment :keys [line-items state shipping-details]} shipment-count]
  [:div
   (info-columns
    ["shipped date" (some-> shipment :shipped-at f/long-date)]
    ["delivery status" (shipment-status-field shipping-details shipment-count)])
   [:div.align-top.mb2
    [:span.dark-gray.shout "order details"]
    (component/build line-items/component
                       {:line-items     line-items
                        :shipment-count shipment-count
                        :show-price?    false}
                       {})]])

(defn ^:private get-user-info [app-state]
  {:user-id    (get-in app-state keypaths/user-id)
   :user-token (get-in app-state keypaths/user-token)})

(defn popup [text]
  [:div.tooltip-popup
   [:div.tooltip-popup-before.border-teal
    [:div.tooltip-popup-after.border-teal]]
   [:div.bg-white.p1.top-lit-light.border.border-teal.dark-gray.light.h6 text]])

(defn voucher-status [sale balance-transfer-id popup-visible?]
  (let [set-popup-visible #(utils/send-event-callback
                             events/control-v2-stylist-dashboard-balance-transfers-voucher-popup-set-visible
                             {:value %1}
                             %2)
        voucher-status (-> sale sales/voucher-status)
        tooltip-text (sales/voucher-status->description voucher-status)]
    [:div
     (when tooltip-text
       {:on-touch-start (set-popup-visible (not popup-visible?) {:prevent-default? false})
        :on-mouse-enter (set-popup-visible true {:prevent-default? true})
        :on-mouse-leave (set-popup-visible false {:prevent-default? true})})
     [:span.titleize (sales/voucher-status->copy voucher-status)]

     (when (and balance-transfer-id (= :voucher/redeemed voucher-status))
       [:a.teal.ml1
        (utils/route-to
          events/navigate-stylist-dashboard-balance-transfer-details
          {:balance-transfer-id balance-transfer-id}) "View"])

     (when tooltip-text
       [:div.border.border-teal.circle.teal.center.inline-block.ml1
        {:style {:width       "18px"
                 :height      "18px"
                 :line-height "15px"}}
        [:div.relative
         [:span.medium "i"]
         (when popup-visible?
           (popup tooltip-text))]])]))

(defn component [{:keys [sale loading? popup-visible? back]} owner opts]
  (let [{:keys [order-number
                placed-at
                order
                voucher
                balance-transfer-id]} sale
        shipments                     (-> sale :order :shipments reverse)
        shipment-count                (-> shipments count fmt-with-leading-zero)]
    (component/create
      (if (or (not order-number) loading?)
        [:div.my6.h2 ui/spinner]
        [:div.container.mb4.px3
         {:style {:display               :grid
                  :grid-template-columns "2em 1fr"
                  :grid-template-areas   (str "'back-btn back-btn'"
                                              "'type-icon title'"
                                              "'spacer fields'")}}
         [:div {:style {:grid-area "back-btn"}}
          (back-button back)]
         [:div {:style {:grid-area "type-icon"}}
          (svg/box-package {:height 18
                            :width  25})]
         [:div
          {:style {:grid-area "title"}}
          [:h4.medium (orders/first-name-plus-last-name-initial order)]]
         [:div {:style {:grid-area "fields"}}
          (if (seq voucher)
            (info-columns
             ["order number" order-number]
             ["voucher type" (get voucher :campaign-name "--")])
            (info-block "order number" order-number))
          (if (seq voucher)
            (info-columns
             ["order date" (some-> placed-at f/long-date)]
             ["voucher status" (voucher-status sale balance-transfer-id popup-visible?)])
            (info-block "order date" (some-> placed-at f/long-date)))
          [:div.col.col-12.pt4
           (for [shipment shipments]
             (let [nth-shipment (some-> shipment :number (subs 1) spice/parse-int fmt-with-leading-zero)]
               [:div.pt3.h6
                {:data-test (str "shipment-" nth-shipment)}
                [:span.bold.shout (when (= nth-shipment shipment-count) "Latest ") "Shipment "]
                [:span nth-shipment
                 " of "
                 shipment-count]
                (shipment-details shipment nth-shipment)]))]]]))))

(defn initialize-shipments-for-returns [shipments]
  (for [shipment shipments]
    (assoc shipment :line-items
           (mapv (merge {:quantity-returned 0})
                (:line-items shipment) ))))

(defn allocate-returned-quantity [returned-quantities {:as line-item :keys [id quantity]}]
  (assoc line-item
         :returned-quantity
         (min quantity
              (get returned-quantities id 0))))


(defn subtract-allocated-returns [returned-line-items returned-quantities]
  (let [indexed-returned-line-items (->> returned-line-items
                                         (spice.maps/index-by :id)
                                         (spice.maps/map-values :returned-quantity))]
    (into {}
          (map
           (fn [[variant-id r-qty]]
             [variant-id (or (- r-qty
                                (get indexed-returned-line-items variant-id 0))
                             0)]))
          returned-quantities)))

(defn return-shipment-line-items [shipment returned-quantities]
  (->> (:line-items shipment)
       (map (partial allocate-returned-quantity returned-quantities))))

(defn add-returns [shipments returned-quantities]
  (loop [index               0
         shipments           shipments
         returned-quantities returned-quantities]
    (if (and (< index (count shipments))
             (seq returned-quantities)
             (some pos? (vals returned-quantities)))
      (let [shipment            (get shipments index)
            returned-line-items (return-shipment-line-items shipment returned-quantities)
            remaining-returns   (subtract-allocated-returns returned-line-items returned-quantities)]
        (recur (inc index)
               (assoc-in shipments [index :line-items] returned-line-items)
               remaining-returns))
      shipments)))

(def no-vouchers? (complement experiments/dashboard-with-vouchers?))

(defn sale-by-order-number
  [app-state order-number]
  (->> (get-in app-state keypaths/v2-dashboard-sales-elements)
       vals
       (filter (fn [sale] (= order-number (:order-number sale))))
       first))

(defn query [app-state]
  (let [order-number           (:order-number (get-in app-state keypaths/navigation-args))
        sale                   (cond-> (sale-by-order-number app-state order-number)
                                 (no-vouchers? app-state)
                                 (dissoc :voucher))
        shipments-enriched     (for [shipment (-> sale :order :shipments)]
                                 (let [product-line-items          (remove (comp #{"waiter"} :source) (:line-items shipment))
                                       enriched-product-line-items (mapv (partial cart/add-product-title-and-color-to-line-item
                                                                                  (get-in app-state keypaths/v2-products)
                                                                                  (get-in app-state keypaths/v2-facets))
                                                                         product-line-items)]
                                   (assoc shipment
                                          :line-items enriched-product-line-items
                                          :shipping-details (shipping/shipping-details shipment))))
        returned-quantities    (orders/returned-quantities (:order sale))
        shipments-with-returns (add-returns (vec shipments-enriched) returned-quantities)]
    {:sale           (assoc-in sale [:order :shipments] shipments-with-returns)
     :loading?       (utils/requesting? app-state request-keys/get-stylist-dashboard-sale)
     :popup-visible? (get-in app-state keypaths/v2-dashboard-balance-transfers-voucher-popup-visible?)
     :back           (first (get-in app-state keypaths/navigation-undo-stack))}))

(defn ^:export built-component [data opts]
  (component/build component (query data) opts))

(defmethod transitions/transition-state events/control-v2-stylist-dashboard-balance-transfers-voucher-popup-set-visible
  [_ event {:keys [value]} app-state]
  (assoc-in app-state keypaths/v2-dashboard-balance-transfers-voucher-popup-visible? value))

(defmethod effects/perform-effects events/navigate-stylist-dashboard-order-details
  [_ event {:keys [order-number] :as args} _ app-state]
  (let [user-info  (get-user-info app-state)
        stylist-id (get-in app-state keypaths/user-store-id)
        handler    #(messages/handle-message events/api-success-v2-stylist-dashboard-sale %)
        params     (merge {:stylist-id   stylist-id
                           :order-number order-number
                           :handler      handler} user-info)]
    (when (:user-token user-info)
      (api/get-stylist-dashboard-sale params))))

(defmethod transitions/transition-state events/api-success-v2-stylist-dashboard-sale
  [_ _ single-sale-map app-state]
  (update-in app-state keypaths/v2-dashboard-sales-elements merge single-sale-map))

(defmethod effects/perform-effects events/api-success-v2-stylist-dashboard-sale
  [_ _ single-sale-map _ _]
  (messages/handle-message events/ensure-sku-ids
                           {:sku-ids (->> single-sale-map
                                          vals
                                          (map :order)
                                          (mapcat orders/all-product-items)
                                          (map :sku))}))
