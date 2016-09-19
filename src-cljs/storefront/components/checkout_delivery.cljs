(ns storefront.components.checkout-delivery
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.accessors.shipping :as shipping]
            [storefront.components.money-formatters
             :refer
             [as-money-without-cents-or-free]]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn ^:private select-shipping-method [shipping-method]
  (utils/send-event-callback events/control-checkout-shipping-method-select shipping-method))

(defn component [{:keys [shipping-methods selected-sku]} owner]
  (om/component
   (html
    [:div
     [:.h3 "Shipping Method"]
     [:.py1
      (for [{:keys [sku name price] :as shipping-method} shipping-methods]
        [:label.flex.items-center.col-12.py1
         {:key sku}
         [:input.mx2.h2
          {:type         "radio"
           :name         "shipping-method"
           :id           (str "shipping-method-" sku)
           :data-test    "shipping-method"
           :data-test-id sku
           :checked      (= selected-sku sku)
           :on-change    (select-shipping-method shipping-method)}]
         [:.clearfix.col-12
          [:.right.medium {:class (if (pos? price) "navy" "teal")} (as-money-without-cents-or-free price)]
          [:.overflow-hidden
           [:.mb1 (when (= selected-sku sku) {:data-test "selected-shipping-method"}) name]
           [:.h6 (shipping/timeframe sku)]]]])]])))

(defn query [data]
  {:shipping-methods (get-in data keypaths/shipping-methods)
   :selected-sku     (get-in data keypaths/checkout-selected-shipping-method-sku)})

(defn built-component [data opts]
  (om/build component (query data) opts))
