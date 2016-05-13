(ns storefront.components.checkout-delivery
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.utils.query :as query]
            [storefront.events :as events]
            [storefront.messages :as messages]
            [storefront.hooks.experiments :as experiments]
            [storefront.accessors.shipping :as shipping]
            [storefront.components.checkout-steps :refer [checkout-step-bar]]
            [storefront.components.validation-errors :refer [validation-errors-component]]
            [storefront.components.formatters :refer [as-money as-money-or-free as-money-without-cents-or-free]]
            [storefront.components.utils :as utils]))

(defn display-shipping-method [{:keys [sku name price]} {:keys [selected-sku saving? on-click]}]
  (let [selected? (= selected-sku sku)]
    [:li.shipping-method
     {:key sku
      :on-click on-click
      :class (when selected?
               (str "selected" (when saving? " saving")))}
     [:label
      [:input.ship-method-radio {:type "radio"}]
      [:div.checkbox-container
       [:figure.large-checkbox]]
      [:div.shipping-method-container
       [:div.rate-name name]
       [:div.rate-timeframe (shipping/timeframe sku)]]
      [:div.rate-cost (as-money-or-free price)]]]))

(defn select-and-submit-shipping-method [shipping-method]
  (fn [e]
    (.preventDefault e)
    (messages/handle-message events/control-checkout-shipping-method-select shipping-method)
    (messages/handle-later events/control-checkout-shipping-method-submit)
    nil))

(defn select-shipping-method [shipping-method]
  (utils/send-event-callback events/control-checkout-shipping-method-select
                             shipping-method))

(defn checkout-confirm-delivery-component [data owner]
  (let [saving? (query/get {:request-key request-keys/update-shipping-method}
                           (get-in data keypaths/api-requests))]
    (om/component
     (html
      [:div.checkout-container.delivery
       [:h2.checkout-header "Delivery Options"]
       [:div#methods
        [:div.shipment
         [:ul.field.radios.shipping-methods
          (for [shipping-method (get-in data keypaths/shipping-methods)]
            (display-shipping-method shipping-method
                                     {:selected-sku (get-in data keypaths/checkout-selected-shipping-method-sku)
                                      :saving?      saving?
                                      :on-click     (select-and-submit-shipping-method shipping-method)}))]]]]))))

(defn checkout-delivery-component [data owner]
  (om/component
   (html
    [:div#checkout
     (om/build validation-errors-component data)
     (checkout-step-bar data)
     [:div.checkout-form-wrapper
      [:form.edit_order
       [:div.checkout-container.delivery
        [:h2.checkout-header "Delivery Options"]
        [:div#methods
         [:div.shipment
          [:ul.field.radios.shipping-methods
           (for [shipping-method (get-in data keypaths/shipping-methods)]
             (display-shipping-method shipping-method
                                      {:selected-sku (get-in data keypaths/checkout-selected-shipping-method-sku)
                                       :saving?      false
                                       :on-click     (select-shipping-method shipping-method)}))]]]
        [:div.form-buttons
         (let [saving (query/get {:request-key request-keys/update-shipping-method}
                                 (get-in data keypaths/api-requests))]
           [:a.large.continue.button.primary
            {:on-click (when-not saving (utils/send-event-callback events/control-checkout-shipping-method-submit))
             :class    (when saving "saving")}
            "Continue to Payment"])]]]]])))


(defn redesigned-confirm-delivery-component [{:keys [saving? shipping-methods selected-sku]} owner]
  (om/component
   (html
    [:.bg-white
     [:.h3 "Shipping Method"]
     [:.py1
      (for [{:keys [sku name price] :as shipping-method} shipping-methods]
        [:.flex.items-center.col-12.my2.px2
         {:key sku
          :class (str (when (= selected-sku sku) "selected")
                      " "
                      (when saving? "saving"))}
         [:input.mr2.h1
          {:type "radio"
           :name "shipping-method"
           :id (str "shipping-method-" sku)
           :checked (= selected-sku sku)
           :on-change (fn [e]
                        (messages/handle-message events/control-checkout-shipping-method-select shipping-method)
                        (messages/handle-later events/control-checkout-shipping-method-submit))}]
         [:label.flex.flex-column.col-12 {:for (str "shipping-method-" sku)}
          [:.h4.flex
           [:.flex-auto.mb1 name]
           [:div (as-money-without-cents-or-free price)]]
          [:.h5 (shipping/timeframe sku)]]])]])))

(defn query [data]
  {:saving?          (query/get {:request-key request-keys/update-shipping-method}
                                (get-in data keypaths/api-requests))
   :shipping-methods (get-in data keypaths/shipping-methods)
   :selected-sku     (get-in data keypaths/checkout-selected-shipping-method-sku)})
