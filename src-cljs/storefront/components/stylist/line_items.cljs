(ns storefront.components.stylist.line-items
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

(defn ^:private returned-quantity-element [shipment-count sku qt returned-qt]
  (let [base-dt (str "shipment-" shipment-count "-line-item-" sku)]
    [:span
     "(Qty: "
     [:span.strike {:data-test (str base-dt "-struck-out-quantity")} qt]
     " "
     [:span {:data-test (str base-dt "-remaining-quantity")} (- qt returned-qt)]
     ") "
     [:span.red (str returned-qt " Item" (when (< 1 returned-qt) "s") " Returned")]]))

(defn ^:private display-line-item
  ([shipment-count line-item] (display-line-item shipment-count line-item true))
  ([shipment-count {:keys [product-title color-name unit-price quantity returned-quantity sku id legacy/variant-id variant-attrs]} show-price?]
   (let [base-dt (str "shipment-" shipment-count "-line-item-" sku)]
     [:div.h6.pb2 {:key (or variant-id id)}
      [:div.medium {:data-test (str base-dt "-title")} product-title]
      [:div {:data-test (str base-dt "-color")} color-name]
      (when show-price?
        [:div {:data-test (str base-dt "-price-ea")} "Price: " (mf/as-money-without-cents unit-price) " ea"])
      [:div
       (when-let [length (:length variant-attrs)]
         [:span {:data-test (str base-dt "-length")} length "” " ])
       [:span {:data-test (str base-dt "-quantity")} ]
       (if (pos? (or returned-quantity 0))
         (returned-quantity-element shipment-count sku quantity returned-quantity)
         [:span "(Qty: " quantity ")"])]])))

(defn component [{:keys [line-items shipment-count show-price?]} owner opts]
  (component/create
   [:div (for [line-item line-items]
           (display-line-item shipment-count line-item show-price?))]))

(defn built-component [data opts]
  (component/build component data opts))
