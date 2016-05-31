(ns storefront.components.checkout-complete
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]))

(def ^:private order-complete-check
  (html (svg/adjustable-check {:class "stroke-navy" :width "80px" :height "80px"})))

(defn component [_ _]
  (om/component
   (html
    (ui/narrow-container
     [:.col-12
      [:.py2.center order-complete-check]

      [:.px3
       [:.h1.center
        {:data-test "checkout-success-message"}
        "Thank you for your order!"]

       [:.py2.line-height-3
        [:p.my2.gray
         "We've received your order and will be processing it right away. Once your order ships we will send you an email confirmation."]]]]

     (ui/button "Return to Homepage" events/navigate-home)))))

(defn built-component [data _]
  (om/component (html (om/build component {}))))
