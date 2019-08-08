(ns catalog.ui.molecules
  (:require [storefront.component :as component]
            [storefront.components.money-formatters :as mf]
            [storefront.platform.reviews :as review-component]))

(defn product-title
  "TODO empty state"
  [{:title/keys [id primary secondary]}]
  [:div {:data-test id}
   [:h3.black.medium.titleize {:item-prop "name"} primary]
   [:div.medium secondary]])

(defn ^:private item-price
  [price]
  (when price
    [:span {:item-prop "price"} (mf/as-money price)]))

(defn price-block
  [{:price-block/keys [primary secondary]}]
  [:div.right-align
   {:item-prop  "offers"
    :item-scope ""
    :item-type  "http://schema.org/Offer"}
   (when-let [primary-formatted (item-price primary)]
     [:div
      [:div.bold primary-formatted]
      [:div.h8.dark-gray secondary]])])

(defn yotpo-reviews-summary
  [{:yotpo-reviews-summary/keys [product-title product-id data-url]}]
  [:div
   (when product-id
     [:div.h6
      {:style {:min-height "18px"}}
      (component/build review-component/reviews-summary-dropdown-experiment-component
                       {:yotpo-data-attributes
                        {:data-name       product-title
                         :data-product-id product-id
                         :data-url        data-url}})])])
