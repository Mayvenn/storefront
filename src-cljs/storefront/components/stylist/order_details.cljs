(ns storefront.components.stylist.order-details
  (:require checkout.classic-cart
            [clojure.string :as string]
            [spice.core :as spice]
            [spice.maps :as maps]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.line-items :as line-items-accessors]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.sales :as sales]
            [storefront.accessors.shipping :as shipping]
            [storefront.api :as api]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.formatters :as f]
            [storefront.components.money-formatters :as mf]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.request-keys :as request-keys]
            [storefront.transitions :as transitions]
            [ui.molecules :as ui-molecules]))

(defn ^:private info-block [header content]
  [:div.align-top.pt2.mb2.h6
   [:span.shout header]
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
  (str (when (< n 10) \0) n))

(defn shipment-status-field [shipping-details shipment-count]
  (if (= "shipped" (:state shipping-details))
    [:span
     [:span {:data-test (str "shipment-" shipment-count "-status")} "Shipped"]
     [:span " (" (:name shipping-details) ")"]]
    [:span {:data-test (str "shipment-" shipment-count "-status")} "Processing"]))

(defcomponent ^:private line-item-molecule
  [{:line-item/keys [id primary secondary-information quantity-information]} _ _]
  [:div.h6.pb2 {:key id}
   [:div.medium {:data-test id} primary]

   (for [{:line-item.secondary-information/keys [id value]} secondary-information]
     [:div {:key id :data-test id} value])

   [:div
    (for [{:quantity-information/keys [id value attrs]} quantity-information]
      [:span (merge {:key id :data-test id} attrs) value])]])

(defn popup [text]
  [:div.tooltip-popup
   [:div.tooltip-popup-before.border-p-color
    [:div.tooltip-popup-after.border-p-color]]
   [:div.bg-white.p1.top-lit-light.border.border-p-color.light.h6 text]])

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
       [:a.p-color.ml1
        (utils/route-to
         events/navigate-stylist-dashboard-balance-transfer-details
         {:balance-transfer-id balance-transfer-id}) "View"])

     (when tooltip-text
       [:div.border.border-p-color.circle.p-color.center.inline-block.ml1
        {:style {:width       "18px"
                 :height      "18px"
                 :line-height "15px"}}
        [:div.relative
         [:span.medium "i"]
         (when popup-visible?
           (popup tooltip-text))]])]))


(defn line-item<-
  [shipment-number
   show-price?
   {:keys [product-title color-name unit-price quantity returned-quantity product-name sku variant-attrs]}]
  (let [base-dt (str "shipment-" shipment-number "-line-item-" sku)]
    {:line-item/id      (str base-dt "-title")
     :line-item/primary (or product-title product-name)
     :line-item/secondary-information
     (keep identity
           [(when color-name
              {:line-item.secondary-information/id    (str base-dt "-color")
               :line-item.secondary-information/value color-name})

            (when show-price?
              {:line-item.secondary-information/id    (str base-dt "-price-ea")
               :line-item.secondary-information/value (str "Price:" (mf/as-money unit-price) " ea")})

            (when-let [length (:length variant-attrs)]
              {:line-item.secondary-information/id    (str base-dt "-length")
               :line-item.secondary-information/value (str length "” ")})])

     :line-item/quantity-information
     (keep identity
           [{:quantity-information/id    (str base-dt "-open-bracket")
             :quantity-information/value "(Qty: "}

            {:quantity-information/id    (str base-dt "-struck-out-quantity")
             :quantity-information/value quantity
             :quantity-information/attrs (when returned-quantity {:class "strike"})}

            (when returned-quantity
              {:quantity-information/id    (str base-dt "-remaining-quantity")
               :quantity-information/value (str " " (- quantity (or returned-quantity 0)))})

            {:quantity-information/id    (str base-dt "-close-bracket")
             :quantity-information/value ") "}

            (when returned-quantity
              {:quantity-information/id    (str base-dt "-returned-message")
               :quantity-information/value (str returned-quantity (ui/pluralize returned-quantity " Item") " Returned")
               :quantity-information/attrs {:class "error"}})])}))

(defn fanout-addons
  [addon-facets {:keys [addon-facet-ids sku] :as line-item}]
  (if addon-facet-ids
    (into [line-item]
          (->> addon-facet-ids
               (map (fnil keyword str))
               (keep (partial get addon-facets))
               (map (fn [facet]
                      {:quantity      1
                       :unit-price    (:service/price facet)
                       :product-title (:facet/name facet)
                       :sku           (-> facet
                                          :facet/name
                                          (string/replace #" " "-")
                                          string/lower-case)}))))
    [line-item]))

(defcomponent line-item-display-component [{:keys [line-items]} _ _]
  [:div (for [line-item line-items]
          (component/build line-item-molecule line-item))])

(defcomponent component
  [{:keys [sale loading? popup-visible?] :as queried-data} _ _]
  (let [{:keys [order-number
                placed-at
                order
                voucher
                balance-transfer-id]} sale
        shipments                     (-> order :shipments reverse)
        shipment-count                (-> shipments count fmt-with-leading-zero)]
    (if (or (not order-number) loading?)
      [:div.my6.h2 ui/spinner]
      [:div.container.mb4.px3
       {:style {:display               :grid
                :grid-template-columns "2em 1fr"
                :grid-template-areas   (str "'back-btn back-btn'"
                                            "'type-icon title'"
                                            "'spacer fields'")}}
       [:div {:style {:grid-area "back-btn"}}
        [:div.py2.pl (ui-molecules/return-link queried-data)]]
       [:div {:style {:grid-area "type-icon"}}
        ^:inline (svg/box-package {:height 18
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
              (let [{:keys [addon-facets line-items shipping-details]} shipment]
                [:div
                 (info-columns
                  ["shipped date" (some-> shipment :shipped-at f/long-date)]
                  ["delivery status" (shipment-status-field shipping-details nth-shipment)])
                 [:div.align-top.mb2
                  [:span.shout "order details"]
                  (component/build line-item-display-component
                                   {:line-items (->> line-items
                                                     (mapcat (partial fanout-addons addon-facets))
                                                     (mapv (partial line-item<- nth-shipment false)))}
                                   {})]])]))]]])))

(defn initialize-shipments-for-returns [shipments]
  (for [shipment shipments]
    (assoc shipment :line-items
           (mapv (merge {:quantity-returned 0})
                 (:line-items shipment)))))

(defn allocate-returned-quantity [returned-quantities {:as line-item :keys [id quantity]}]
  (assoc line-item
         :returned-quantity
         (min quantity
              (get returned-quantities id 0))))

(defn subtract-allocated-returns [returned-line-items returned-quantities]
  ;; TODO: service line items may be showing up here
  (let [indexed-returned-line-items (->> returned-line-items
                                         (maps/index-by :id)
                                         (maps/map-values :returned-quantity))]
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
                                       enriched-product-line-items (mapv (partial checkout.classic-cart/add-product-title-and-color-to-line-item
                                                                                  (get-in app-state keypaths/v2-products)
                                                                                  (get-in app-state keypaths/v2-facets))
                                                                         product-line-items)]
                                   (assoc shipment
                                          :line-items enriched-product-line-items
                                          :shipping-details (shipping/shipping-details shipment))))
        returned-quantities    (orders/returned-quantities (:order sale))
        shipments-with-returns (add-returns (vec shipments-enriched) returned-quantities)]
    {:sale                      (assoc-in sale [:order :shipments] shipments-with-returns)
     :loading?                  (utils/requesting? app-state request-keys/get-stylist-dashboard-sale)
     :popup-visible?            (get-in app-state keypaths/v2-dashboard-balance-transfers-voucher-popup-visible?)
     :return-link/back          (first (get-in app-state keypaths/navigation-undo-stack))
     :return-link/event-message [events/navigate-v2-stylist-dashboard-orders]
     :return-link/copy          "Back"
     :return-link/id            "back-link"}))

(defn ^:export built-component [data opts]
  (component/build component (query data) opts))

(defmethod transitions/transition-state events/control-v2-stylist-dashboard-balance-transfers-voucher-popup-set-visible
  [_ event {:keys [value]} app-state]
  (assoc-in app-state keypaths/v2-dashboard-balance-transfers-voucher-popup-visible? value))

(defn ^:private get-user-info [app-state]
  {:user-id    (get-in app-state keypaths/user-id)
   :user-token (get-in app-state keypaths/user-token)})

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
  (update-in app-state keypaths/v2-dashboard-sales-elements merge
             (maps/map-keys (comp spice/parse-int name) single-sale-map)))

(defmethod effects/perform-effects events/api-success-v2-stylist-dashboard-sale
  [_ _ single-sale-map _ _]
  (messages/handle-message events/ensure-sku-ids
                           {:sku-ids (->> single-sale-map
                                          vals
                                          (map :order)
                                          (mapcat :shipments)
                                          (mapcat :line-items)
                                          (filter line-items-accessors/product-or-service?)
                                          (map :sku))}))
