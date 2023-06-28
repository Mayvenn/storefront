(ns catalog.ui.molecules
  (:require [storefront.component :as c]
            [storefront.platform.reviews :as review-component]))

(defn price-block
  [{:price-block/keys [primary primary-struck secondary secondary-classes new-primary]}]
  [:div.pt1
   (when (or primary primary-struck)
     [:div.flex
      (when primary
        [:span.proxima.text-base.bold primary])
      (when primary-struck
        [:div.bold
         [:span.proxima.text-base.strike primary-struck]
         [:div.warning-red.right new-primary]])
      [:div.ml1.proxima.text-base {:class secondary-classes}
       secondary]])])

;; FIXME(corey) The product details page uses a fork of this component
(defn yotpo-reviews-summary
  "Renders the yotpo reviews summary"
  [{:yotpo-reviews-summary/keys [product-title product-id data-url]}]
  (when product-id
    [:div.h6
     {:style {:min-height "18px"}}
     (c/build review-component/reviews-summary-dropdown-experiment-component
              {:yotpo-data-attributes {:data-name       product-title
                                       :data-product-id product-id
                                       :data-url        data-url}})]))
