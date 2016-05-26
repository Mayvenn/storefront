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
       [:.h1.center "Thank you for your order!"]

       [:.py3.line-height-3.gray
        "We've received your order and will be processing it right away. Once your order ships we will send you an email confirmation."]]]

     (ui/button "Return to Homepage" events/navigate-home)))))

(defn built-component [data _]
  (om/component (html (om/build component {}))))
