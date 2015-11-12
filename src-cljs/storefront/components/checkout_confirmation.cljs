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
            [storefront.components.order-summary :refer [display-order-summary display-line-items]]))

(defn checkout-confirmation-component [data owner]
  (om/component
   (html
    [:div#checkout
     (checkout-step-bar data)
     [:div.row
      [:div.checkout-form-wrapper
       [:form.edit_order
        [:div.checkout-container
         (display-line-items data (get-in data keypaths/order))
         (display-order-summary (get-in data keypaths/order))
         [:div.form-buttons.pay-for-order
          (let [placing-order (query/get {:request-key request-keys/place-order}
                                         (get-in data keypaths/api-requests))]
            [:a.large.continue.button.primary
             {:on-click (when-not placing-order (utils/send-event-callback data events/control-checkout-confirmation-submit))
              :class [(when placing-order "saving")
                      (when (experiments/simplify-funnel? data) "bright")]}
             (if (experiments/simplify-funnel? data)
               "Complete my Purchase"
               "Pay for order")])]]]]]])))
