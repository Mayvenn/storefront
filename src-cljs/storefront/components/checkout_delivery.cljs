(ns storefront.components.checkout-delivery
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.accessors.shipping :as shipping]
            [storefront.components.money-formatters
             :refer
             [as-money-without-cents-or-free]]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn ^:private select-shipping-method
  [shipping-method]
  (utils/send-event-callback events/control-checkout-shipping-method-select
                             shipping-method))

(defn component [{:keys [shipping-methods selected-sku]} owner]
  (om/component
   (html
    [:div
     [:.h3 "Shipping Method"]
     [:.py1
      (for [{:keys [sku name price] :as shipping-method} shipping-methods]

        (ui/radio-section
         (merge {:key          sku
                 :name         "shipping-method"
                 :id           (str "shipping-method-" sku)
                 :data-test    "shipping-method"
                 :data-test-id sku
                 :on-click     (select-shipping-method shipping-method)}
                (when (= selected-sku sku) {:checked "checked"}))
         [:.right.ml1.medium {:class (if (pos? price) "navy" "teal")} (as-money-without-cents-or-free price)] 
         [:.overflow-hidden
          [:div (when (= selected-sku sku) {:data-test "selected-shipping-method"}) name]
          [:.h6 (or (shipping/timeframe sku) "")]]))]])))

(defn query [data]
  {:shipping-methods (get-in data keypaths/shipping-methods)
   :selected-sku     (get-in data keypaths/checkout-selected-shipping-method-sku)})

(defn built-component [data opts]
  (om/build component (query data) opts))
