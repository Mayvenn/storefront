(ns storefront.components.stylist.line-items
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.money-formatters :as mf]))

(defn ^:private returned-quantity-element [shipment-count sku qt returned-qt]
  (let [base-dt (str "shipment-" shipment-count "-line-item-" sku)]
    [:span
     "(Qty: "
     [:span.strike {:data-test (str base-dt "-struck-out-quantity")} qt]
     " "
     [:span {:data-test (str base-dt "-remaining-quantity")} (- qt returned-qt)]
     ") "
     [:span.error (str returned-qt " Item" (when (< 1 returned-qt) "s") " Returned")]]))

(defn ^:private display-line-item
  ([shipment-count line-item] (display-line-item shipment-count line-item true))
  ([shipment-count {:keys [product-title unit-price quantity returned-quantity product-name sku id legacy/variant-id variant-attrs]
                    :join/keys [facets]} show-price?]
   (let [base-dt (str "shipment-" shipment-count "-line-item-" sku)]
     [:div.h6.pb2 {:key (or variant-id id)}
      [:div.medium {:data-test (str base-dt "-title")} (or product-title product-name)]
      [:div {:data-test (str "line-item-color-" sku)} (-> facets :hair/color :option/name)]
      [:div {:data-test (str "line-item-base-material-" sku)} (-> facets :hair/base-material :option/name)]
      (when show-price?
        [:div {:data-test (str base-dt "-price-ea")} "Price: " (mf/as-money unit-price) " ea"])
      [:div
       (when-let [length (:length variant-attrs)]
         [:span {:data-test (str base-dt "-length")} length "” "])
       [:span {:data-test (str base-dt "-quantity")}]
       (if (pos? (or returned-quantity 0))
         (returned-quantity-element shipment-count sku quantity returned-quantity)
         [:span "(Qty: " quantity ")"])]])))

(defcomponent component [{:keys [line-items shipment-count show-price?]} owner opts]
  [:div (for [line-item line-items]
          (display-line-item shipment-count line-item show-price?))])

(defn built-component [data opts]
  (component/build component data opts))
