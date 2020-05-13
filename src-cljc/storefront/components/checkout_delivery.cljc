(ns storefront.components.checkout-delivery
  (:require [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.shipping :as shipping]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.money-formatters :as mf]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(defn ^:private select-shipping-method
  [shipping-method]
  (utils/send-event-callback events/control-checkout-shipping-method-select
                             shipping-method))

(defcomponent component
  [{:keys [shipping-methods selected-sku]} owner _]
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
       [:.right.ml1.medium {:class (if (pos? price) "black" "p-color")} (mf/as-money-or-free price)]
       [:.overflow-hidden
        [:div (when (= selected-sku sku) {:data-test "selected-shipping-method"}) name]
        [:.h6 (or (shipping/longform-timeframe sku) "")]]))]])

(defn query
  [data]
  (let [shipping-methods               (get-in data keypaths/shipping-methods)
        selected-sku                   (get-in data keypaths/checkout-selected-shipping-method-sku)]
    {:shipping-methods               shipping-methods
     :selected-sku                   selected-sku}))
