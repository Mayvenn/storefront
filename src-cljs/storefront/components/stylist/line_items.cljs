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

(defn ^:private display-line-item
  ([line-item] (display-line-item line-item true))
  ([{:keys [product-title color-name unit-price quantity returned-quantity sku id legacy/variant-id variant-attrs]} show-price?]
   [:div.h6.pb2 {:key (or variant-id id)}
    [:div.medium {:data-test (str "line-item-title-" sku)} product-title]
    [:div {:data-test (str "line-item-color-" sku)} color-name]
    (when show-price?
      [:div {:data-test (str "line-item-price-ea-" sku)} "Price: " (mf/as-money-without-cents unit-price) " ea"])
    [:div
     (when-let [length (:length variant-attrs)]
       [:span {:data-test (str "line-item-length-" sku)} length "” " ])
     [:span {:data-test (str "line-item-quantity-" sku)} ]
     (case returned-quantity
       0 [:span "(Qty: " quantity ")"]
       1 [:span "(Qty: " [:span.strike quantity] " " (- quantity returned-quantity) ") "
          [:span.red (str returned-quantity " Item Returned")]]
       [:span "(Qty: " [:span.strike quantity] " " (- quantity returned-quantity) ") "
        [:span.red (str returned-quantity " Items Returned")]])]]))

(defn component [{:keys [line-items show-price?]} owner opts]
  (component/create
   [:div (for [line-item line-items]
           (display-line-item line-item show-price?))]))

(defn query [app-state]
  (let [order-number        (:order-number (get-in app-state keypaths/navigation-args))
        order               (->> (get-in app-state keypaths/v2-dashboard-sales-elements)
                                 vals
                                 (filter (fn [sale] (= order-number (:order-number sale))))
                                 first
                                 :order)
        returned-quantities (orders/returned-quantities order)
        line-items          (->> order
                                 orders/product-items
                                 (mapv #(assoc % :returned-quantity (get returned-quantities (:id %) 0))))
]
    (mapv (partial cart/add-product-title-and-color-to-line-item
                   (get-in app-state keypaths/v2-products)
                   (get-in app-state keypaths/v2-facets))
          line-items)))

(defn built-component [data opts]
  (component/build component data opts))
