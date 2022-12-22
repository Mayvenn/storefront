(ns catalog.reviews
  "Product reviews, possibly more in the future"
  (:require #?@(:cljs [[storefront.hooks.reviews :as yotpo]])
            [storefront.component :as c] 
            [storefront.events :as e]
            [storefront.effects :as fx]
            [storefront.platform.reviews :as review-component]))

;; Browsing reviews
(c/defcomponent browser
  [{:keys [data-name data-product-id data-url]} _ _]
  (c/build review-component/reviews-component
           {:yotpo-data-attributes
            {:data-name       data-name
             :data-product-id data-product-id
             :data-url        data-url}}))

(c/defcomponent summary
  [{:keys [data-name data-product-id data-url]} _ _]
  (when data-product-id
    [:div.h6
     {:style {:min-height "18px"}}
     (c/build review-component/reviews-summary-dropdown-experiment-component
              {:yotpo-data-attributes
               {:data-name       data-name
                :data-product-id data-product-id
                :data-url        data-url}})]))

(defmethod fx/perform-effects e/reviews|reset
  [_ _ _ _ _]
  #?(:cljs 
     (do
       (yotpo/insert-reviews)
       (yotpo/start))))