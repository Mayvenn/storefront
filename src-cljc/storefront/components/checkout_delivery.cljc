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
  [{:keys [show-priority-shipping-method? shipping-methods selected-sku]} owner _]
  [:div.pb2.pt4
   [:.proxima.title-2 "Shipping Method"]
   [:.py1
    (for [{:keys [sku name price] :as shipping-method} shipping-methods]
      [:div.my2
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
         [:.content-3 (or (if show-priority-shipping-method?
                            (shipping/priority-shipping-experimental-longform-timeframe sku)
                            (shipping/longform-timeframe sku)) "")]])])]])

(def priority-shipping-method?
  (comp boolean #{"WAITER-SHIPPING-7"} :sku))

(defn query
  [data]
  (let [show-priority-shipping-method? (experiments/show-priority-shipping-method? data)
        current-shipping-method        (-> data
                                           (get-in keypaths/order)
                                           (orders/shipping-item))
        shipping-methods               (cond->> (get-in data keypaths/shipping-methods)
                                         (and
                                          (not (priority-shipping-method? current-shipping-method))
                                          (not show-priority-shipping-method?))
                                         (remove priority-shipping-method?))
        selected-sku                   (get-in data keypaths/checkout-selected-shipping-method-sku)]
    {:shipping-methods               shipping-methods
     :show-priority-shipping-method? show-priority-shipping-method?
     :selected-sku                   selected-sku}))
