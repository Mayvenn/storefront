(ns storefront.components.checkout-confirmation
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.components.utils :as utils]
            [storefront.components.checkout-steps :refer [checkout-step-bar]]
            [storefront.components.order-summary :refer [display-order-summary display-line-items]]))

(defn checkout-confirmation-component [data owner]
  (om/component
   (html
    [:div#checkout
     (checkout-step-bar data)
     [:div.row
      [:div.checkout-form-wrapper
       [:form.edit_order
        {:method "POST"
         :on-submit (utils/send-event-callback data events/control-checkout-confirmation-submit)}
        [:div.checkout-container
         (display-line-items data (get-in data keypaths/order))
         (display-order-summary (get-in data keypaths/order))
         [:div.form-buttons.pay-for-order
          [:input.continue.button.primary
           {:type "submit" :name "commit" :value "Pay for order"}]]]]]]])))
