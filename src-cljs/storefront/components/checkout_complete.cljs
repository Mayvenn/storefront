(ns storefront.components.checkout-complete
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.keypaths :as keypaths]
            [storefront.hooks.experiments :as experiments]
            [storefront.components.utils :as utils]
            [storefront.accessors.black-friday :as bf]
            [storefront.events :as events]))

(defn checkout-complete-component [data owner]
  (om/component
   (html
    [:div.checkout-container
     [:div.order-thank-you
      [:figure.shopping-bag]
      [:p.message "Thank you for your order!"]]

     [:div.solid-line-divider]

     [:p.order-thanks-detail
      "We've received your order and will being processing it right away. Once your order ships we will send you another e-mail confirmation."]

     (when (bf/after-black-friday-start?)
       [:p.order-thanks-detail.emphasis
        "Due to the high volume of orders, shipping may be delayed. We are working hard to deliver as quickly as possible. Happy Holidays!"])
     [:a.big-button.left-half.button.primary
      (merge (utils/route-to data events/navigate-home)
             (when (experiments/simplify-funnel? data)
               {:class "bright"}))
      "Return Home"]])))
