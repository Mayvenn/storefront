(ns storefront.components.category
  (:require [storefront.components.utils :as utils]
            [storefront.taxons :refer [taxon-path-for]]
            [clojure.string :as string]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.state :as state]))

(defn display-taxon [taxon]
  [:div.hair-taxon.decorated.small-width {:class (taxon-path-for taxon)}
   [:a.taxon-link
    [:p.hair-taxon-name (:name taxon)]]])

(defn display-product [product]
  (let [collection-name (product :collection_name)]
    [:a {:href "#FIXME: product#show"}
     [:div.taxon-product-container
      (js/console.log (->> product
                                  :variants
                                  (map :images)

                                  (clj->js)))
      (when-let [first-image (->> product
                                  :variants
                                  (map :images)
                                  (filter (comp not empty?))
                                  ffirst)]
        [:div.taxon-product-image-container
         [:div.corner-ribbon {:class collection-name}
          collection-name]
         [:img (:product_url first-image)]])
      [:div.taxon-product-info-container
       [:div.taxon-product-description-container
        [:div.taxon-product-collection
         [:img {:class collection-name
                :src (str "/images/products/squiggle-categories-" collection-name "@2x.png")}]
         collection-name]
        [:div.taxon-product-title (:name product)]]
       [:div.taxon-from-price
        [:span "From: "]
        [:br]
        "$"
        (js/Math.floor (product :from_price))]]]]))

(defn category-component [data owner]
  (om/component
   (html
    (if-let [taxon (get-in data state/browse-taxon-path)]
      (let [taxon-permalink-class (string/replace (:permalink taxon) #"/" "-")]
        [:div
         [:div.taxon-products-banner {:class taxon-permalink-class}]
         [:div.taxon-products-container
          (when (:stylist_only? taxon)
            [:h2.header-bar-heading "Stylist Products"]
            [:div.taxon-nav
             (map display-taxon (get-in data state/taxons-path))
             [:div {:style {:clear "both"}}]])

          [:div.taxon-products-list-container
           (map display-product
                (get-in data (conj state/products-for-taxons-path (:id taxon))))]]

         [:div.free-shipping.taxon-lower-banner
          [:figure.guarantee-feature]
          [:feature.fs-feature]]])))))
