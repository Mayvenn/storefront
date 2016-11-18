(ns storefront.components.checkout-complete
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.platform.date :as date]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]))

(def ^:private order-complete-check
  (svg/circled-check {:class "stroke-navy"
                      :style {:width "80px" :height "80px"}}))

(defn component [_ _]
  (om/component
   (html
    (ui/narrow-container
     [:.my2.center order-complete-check]

     [:.mx3
      [:.h2.center
       {:data-test "checkout-success-message"}
       "Thank you for your order!"]

      [:.py2.line-height-3
       [:p.my2.gray
        "We've received your order and will be processing it right away. Once your order ships we will send you an email confirmation."]]

      (when (date/after date/black-friday)
        [:div.pb2.line-height-3
         [:p.mb2.teal
          "Due to the high volume of orders we are receiving, your order may take longer to process. We are working hard to deliver your order as quickly as possible. Happy Holidays!"]])]

     [:div.mb2
      (ui/teal-button (utils/route-to events/navigate-home) "Return to Homepage")]))))

(defn built-component [data _]
  (om/build component {}))
