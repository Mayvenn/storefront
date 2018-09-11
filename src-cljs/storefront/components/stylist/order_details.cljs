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
            [spice.core :as spice]))

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
   [:span.dark-gray.shout header]
   [:div.medium (or content "--")]])

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

(defn ^:private delivery-status [shipment]
  (let [shipping-name    (->> shipment
                              :line-items
                              (filter #(= (:source %) "waiter"))
                              first
                              :product-name)]
    [:div
     [:div.titleize (:state shipment)] ;; TODO: Deal with pending (processing) and returned
     [:div "(" shipping-name ")"]]))

(defn ^:private shipment-details [{:as shipment :keys [line-items]}]
  [(info-columns
     ["shipped date" (some-> shipment :shipped-at f/long-date)]
     ["delivery status" (delivery-status shipment)])
   [:div.align-top.mb2
    [:span.dark-gray.shout "order details"]
    (component/build line-items/component
                     {:line-items line-items
                      :show-price? false}
                     {})]])

(defn ^:private get-user-info [app-state]
  {:user-id (get-in app-state keypaths/user-id)
   :user-token (get-in app-state keypaths/user-token)})

(defn popup [text]
  [:div.absolute.mtp5
   {:style {:width "180px"
            :left "-82px"}}
   [:div.mx-auto.mbnp1.border-teal.relative
    {:style {:border-left "8px solid transparent"
             :border-right "8px solid transparent"
             :border-bottom-width "8px"
             :border-bottom-style "solid"
             :width "12px"}}
    [:div.mx-auto.mbnp1.border-teal.absolute
     {:style {:border-left "8px solid transparent"
              :border-right "8px solid transparent"
              :border-bottom "8px solid white"
              :width "12px"
              :top "1px"
              :left "-8px"}}]]
   [:div.bg-white.p1.top-lit-light.border.border-teal text]])

(defn voucher-status [sale popup-visible?]
  (let [tooltip-text (-> sale
                         sales/voucher-status
                         sales/voucher-status->description)
        set-popup-visible #(utils/send-event-callback
                             events/control-v2-stylist-dashboard-balance-transfers-voucher-popup-set-visible
                             {:value %1}
                             %2)]
    [:div
     (when tooltip-text
       {:on-touch-start (set-popup-visible (not popup-visible?) {:prevent-default? false})
        :on-mouse-enter (set-popup-visible true {:prevent-default? true})
        :on-mouse-leave (set-popup-visible false {:prevent-default? true})})
     [:span.titleize (-> sale
                         sales/voucher-status
                         sales/voucher-status->copy)]
     (when tooltip-text
       [:div.border.border-teal.circle.teal.center.inline-block.ml1
        {:style {:width       "18px"
                 :height      "18px"
                 :line-height "15px"}}
        [:div.relative
         [:span.medium "i"]
         (when popup-visible?
           (popup tooltip-text))]])]))

(defn component [{:keys [sale v2-dashboard? loading? popup-visible? back]} owner opts]
  (let [{:keys [order-number
                placed-at
                order
                voucher]} sale
        shipments         (-> sale :order :shipments reverse)
        shipment-count    (-> shipments count fmt-with-leading-zero)]
    (component/create
      (if (or (not order-number) loading?)
        [:div.my6.h2 ui/spinner]
        [:div.container.mb4.px3
         {:style {:display               :grid
                  :grid-template-columns "2em 100%"
                  :grid-template-areas   (str "'back-btn back-btn'"
                                              "'type-icon title'"
                                              "'spacer fields'")}}
         [:div {:style {:grid-area "back-btn"}}
          (back-button back v2-dashboard?)]
         [:div {:style {:grid-area "type-icon"}}
          (svg/box-package {:height 18
                            :width  25})]
         [:div
          {:style {:grid-area "title"}}
          [:h4.medium (orders/first-name-plus-last-name-initial order)]]
         [:div {:style {:grid-area "fields"}}
          (info-columns
            ["order number" order-number]
            ["voucher type" (get voucher :campaign-name "--")])
          (info-columns
            ["order date" (f/long-date placed-at)]
            ["voucher status" (voucher-status sale popup-visible?)])
          (for [shipment shipments]
            (let [nth-shipment (-> shipment :number (subs 1) spice/parse-int fmt-with-leading-zero)]
              [:div.pt4.h6
               [:span.bold.shout (when (= nth-shipment shipment-count) "Latest ") "Shipment "]
               [:span nth-shipment
                " of "
                shipment-count]
               (shipment-details shipment)]))]]))))

(defn assign-returns-to-shipments [shipments returns]
  "Adds :quantity-returned to each line item of each shipment in a sequence of shipments.
Second parameter is of the form {variant-id quantity...}."
  ;; TODO eveything
  shipments
  )

(defn query [app-state]
  (let [order-number (:order-number (get-in app-state keypaths/navigation-args))
        sale (->> (get-in app-state keypaths/v2-dashboard-sales-elements)
                  vals
                  (filter (fn [sale] (= order-number (:order-number sale))))
                  first)
        shipments-enriched (for [shipment (-> sale :order :shipments)]
                             (let [product-line-items (remove (comp #{"waiter"} :source) (:line-items shipment))]
                               (assoc shipment :line-items
                                      (mapv (partial cart/add-product-title-and-color-to-line-item
                                                     (get-in app-state keypaths/v2-products)
                                                     (get-in app-state keypaths/v2-facets))
                                            product-line-items))))
        returned-quantities (orders/returned-quantities (:order sale))
        shipments-with-returns (assign-returns-to-shipments shipments-enriched returned-quantities)]
    {:sale           (assoc-in sale [:order :shipments] shipments-enriched)
     :loading?       (utils/requesting? app-state request-keys/get-stylist-dashboard-sale)
     :v2-dashboard?  (experiments/v2-dashboard? app-state)
     :popup-visible? (get-in app-state keypaths/v2-dashboard-balance-transfers-voucher-popup-visible?)
     :back           (first (get-in app-state keypaths/navigation-undo-stack))}))

(defn built-component [data opts]
  (component/build component (query data) opts))

(defmethod transitions/transition-state events/control-v2-stylist-dashboard-balance-transfers-voucher-popup-set-visible
  [_ event {:keys [value]} app-state]
  (assoc-in app-state keypaths/v2-dashboard-balance-transfers-voucher-popup-visible? value))

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

(defmethod effects/perform-effects events/api-success-v2-stylist-dashboard-sale
  [_ _ single-sale-map _ _]
  (messages/handle-message events/ensure-sku-ids
                           {:sku-ids (->> single-sale-map
                                          vals
                                          first
                                          :order
                                          orders/product-items
                                          (map :sku))}))
