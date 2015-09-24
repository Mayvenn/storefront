(ns storefront.components.reviews
  (:require [sablono.core :refer-macros [html]]
            [om.core :as om]
            [storefront.accessors.taxons :as taxons]
            [storefront.events :as events]
            [storefront.hooks.experiments :as experiments]
            [storefront.messages :refer [send]]
            [storefront.routes :as routes]
            [storefront.keypaths :as keypaths]
            [storefront.utils.query :as query]))

(defn reviews-component [data owner {product-id :product-id}]
  (reify
    om/IDidMount
    (did-mount [_] (send data events/reviews-component-mounted))
    om/IWillUnmount
    (will-unmount [_] (send data events/reviews-component-will-unmount))
    om/IRender
    (render [_]
      (html
       [:div.product-reviews
        [:div.yotpo.yotpo-main-widget
         {:data-product-id product-id
          :data-url (apply routes/path-for @data
                           (get-in data keypaths/navigation-message))}]]))))

(defn reviews-summary-component [data owner {product-id :product-id}]
  (reify
    om/IDidMount
    (did-mount [_] (send data events/reviews-component-mounted))
    om/IWillUnmount
    (will-unmount [_] (send data events/reviews-component-will-unmount))
    om/IRender
    (render [_]
      (html
       [:div.product-reviews-summary
        [:div.yotpo.bottomLine.star-summary
         {:data-product-id product-id
          :data-url (apply routes/path-for @data
                           (get-in data keypaths/navigation-message))}]
        [:div.yotpo.QABottomLine.question-summary
         {:data-product-id product-id}]]))))
