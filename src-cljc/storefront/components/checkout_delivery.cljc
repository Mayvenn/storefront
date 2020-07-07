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
  [{:delivery/keys [primary note-box options]} owner _]
  [:div.pb2.pt4.mx3
   [:.proxima.title-2 primary]
   [:.py1
    (for [option options]
      [:div.my2 {:key (:react/key option)}
       (ui/radio-section
        (let [{:control/keys [data-test id data-test-id target selected?]} option]
          (merge {:name data-test
                  :id id
                  :data-test data-test
                  :data-test-id data-test-id
                  :on-click     (apply utils/send-event-callback target)}
                 (when selected? {:checked "checked"})))
        [:div.right.ml1.medium (:detail/value option)]
        [:div.overflow-hidden
         [:div {:data-test (:primary/data-test option)} (:primary/copy option)]
         [:div.content-3 (:secondary/copy option)]])])]])

(defn query
  [data]
  (let [shipping-methods (get-in data keypaths/shipping-methods)
        selected-sku     (get-in data keypaths/checkout-selected-shipping-method-sku)]
    {:delivery/primary "Shipping Method"
     :delivery/options (map (fn [{:keys [sku name price] :as shipping-method}]
                              {:react/key            sku
                               :primary/data-test    (when (= selected-sku sku) "selected-shipping-method")
                               :primary/copy         name
                               :secondary/copy       (or (shipping/longform-timeframe sku) "")
                               :control/id           (str "shipping-method-" sku)
                               :control/data-test    "shipping-method"
                               :control/data-test-id sku
                               :control/target       [events/control-checkout-shipping-method-select shipping-method]
                               :control/selected?    (= selected-sku sku)
                               :detail/value         [:span {:class (if (pos? price) "black" "p-color")}
                                                       (mf/as-money-or-free price)]}) shipping-methods)}))
