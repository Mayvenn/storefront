(ns storefront.components.checkout-confirmation
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.events :as events]
            [storefront.hooks.experiments :as experiments]
            [storefront.utils.query :as query]
            [storefront.components.utils :as utils]
            [storefront.components.checkout-steps :refer [checkout-step-bar]]
            [storefront.components.checkout-delivery :refer [checkout-confirm-delivery-component]]
            [storefront.components.order-summary :refer [display-order-summary display-line-items]]))

(defn checkout-confirmation-component [data owner]
  (let [placing-order? (query/get {:request-key request-keys/place-order}
                                  (get-in data keypaths/api-requests))
        updating-shipping? (query/get {:request-key request-keys/update-shipping-method}
                                      (get-in data keypaths/api-requests))
        saving? (or placing-order? updating-shipping?)]
    (om/component
     (html
      [:div#checkout
       (checkout-step-bar data)
       [:div.row
        [:div.checkout-form-wrapper
         [:form.edit_order
          [:div.checkout-container
           (display-line-items data (get-in data keypaths/order))
           (when (experiments/three-steps? data)
             (om/build checkout-confirm-delivery-component data))
           (display-order-summary data (get-in data keypaths/order))
           [:div.form-buttons.pay-for-order
            [:a.large.continue.button.primary
             (merge
              {:on-click (when-not saving?
                           (utils/send-event-callback events/control-checkout-confirmation-submit))
               :class (when saving? "saving")})
             "Complete my Purchase"]]]]]]]))))
