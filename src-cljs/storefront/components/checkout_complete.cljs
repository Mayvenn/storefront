(ns storefront.components.checkout-complete
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.utils :as utils]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.hooks.experiments :as experiments]))

(def order-complete-check :.img-check-circle.bg-no-repeat.bg-center.bg-contain.mb3.mt2.m-auto)

(defn redesigned-checkout-complete-component [_ _]
  (om/component
   (html
    (ui/container
     [:.col-10.center.m-auto.py3
      [order-complete-check {:style {:width "70px" :height "70px"}}]]

     [:.h1.col-12.m-auto.center "Thank you for your order!"]

     [:.py3.line-height-3.col-10.m-auto.gray
      "We've received your order and will be processing it right away. Once your order ships we will send you an email confirmation."]

     (ui/button "Return to Homepage" events/navigate-home)))))

(defn old-checkout-complete-component [_ _]
  (om/component
   (html
    [:div.checkout-container
     [:div.order-thank-you
      [:figure.shopping-bag]
      [:p.message "Thank you for your order!"]]

     [:div.solid-line-divider]

     [:p.order-thanks-detail
      "We've received your order and we'll process it right away. Once your order ships we'll send you another e-mail confirmation."]
     [:a.big-button.left-half.button.primary
      (utils/route-to events/navigate-home)
      "Return Home"]])))

(defn checkout-complete-component [data _]
  (om/component
   (html
    (if (experiments/three-steps-redesign? data)
      (om/build redesigned-checkout-complete-component {})
      (om/build old-checkout-complete-component data)))))
