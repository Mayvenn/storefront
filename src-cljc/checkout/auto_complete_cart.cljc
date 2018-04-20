(ns checkout.auto-complete-cart
  (:require [storefront.accessors.images :as images]
            [storefront.components.money-formatters :refer [as-money-without-cents]]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]))


(defn display-adjustable-line-items
  [line-items skus update-line-item-requests delete-line-item-requests]
  (for [{sku-id :sku variant-id :id :as line-item} line-items
        :let [sku               (get skus sku-id)
              legacy-variant-id (or (:legacy/variant-id line-item) (:id line-item))
              price             (or (:sku/price line-item)         (:unit-price line-item))
              thumbnail         (merge
                                 (images/cart-image sku)
                                 {:data-test (str "line-item-img-" (:catalog/sku-id sku))})
              removing?         (get delete-line-item-requests variant-id)
              updating?         (get update-line-item-requests sku-id)]]
    [:div.py4 {:key legacy-variant-id}
     [:a.left.pr1
      (when-let [length (-> sku :hair/length first)]
        [:div.right.z1.circle.stacking-context.border.border-white.flex.items-center.justify-center.medium.h5.bg-too-light-teal
         {:style {:margin-left "-19px"
                  :margin-top  "-14px"
                  :width       "32px"
                  :height      "32px"}} (str length "\"")])
      [:img.block.border.border-light-gray
       (assoc thumbnail :style {:width "75px" :height "75px"})]]
     [:div {:style {:margin-top "-14px"}}
      [:a.medium.titleize.h5
       {:data-test (str "line-item-title-" sku-id)}
       (:product-title line-item)]
      [:div.h6
       [:div.flex.justify-between ;; Color & Remove
        [:div
         {:data-test (str "line-item-color-" sku-id)}
         (:color-name line-item)]
        [:div.flex.items-center.justify-between
         (if removing?
           [:div.h3 {:style {:width "1.2em"}} ui/spinner]
           [:a.gray.medium
            (merge {:data-test (str "line-item-remove-" sku-id)}
                   (utils/fake-href events/control-cart-remove (:id line-item))) "T"])]]
       [:div.flex.justify-between ;; Quantity & Price
        [:div.h3
         {:data-test (str "line-item-quantity-" sku-id)}
         (ui/auto-complete-counter {:spinning? updating?
                                    :data-test sku-id}
                                   (:quantity line-item)
                                   (utils/send-event-callback events/control-cart-line-item-dec {:variant line-item})
                                   (utils/send-event-callback events/control-cart-line-item-inc {:variant line-item}))]
        [:div {:data-test (str "line-item-price-ea-" sku-id)} (as-money-without-cents price) " ea"]]]]]))
