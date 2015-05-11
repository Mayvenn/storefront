(ns storefront.components.category
  (:require [storefront.components.utils :as utils]
            [storefront.taxons :refer [taxon-path-for]]
            [clojure.string :as string]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.state :as state]))

(defn display-taxon [data selected-taxon taxon]
  (let [taxon-path (taxon-path-for taxon)
        selected-class (if (= selected-taxon taxon) "selected" nil)
        taxon-classes (string/join " " (conj [taxon-path] selected-class))]
    [:div.hair-taxon.decorated.small-width {:class taxon-classes}
     [:a.taxon-link (utils/route-to data events/navigate-category {:taxon-path taxon-path})
      [:p.hair-taxon-name (:name taxon)]]]))

(defn display-product [data product]
  (let [collection-name (product :collection_name)]
    [:a (utils/route-to data events/navigate-product {:product-path (:slug product)})
     [:div.taxon-product-container
      (when-let [first-image (->> product
                                  :master
                                  :images
                                  first
                                  :product_url)]
        [:div.taxon-product-image-container
         (when-not (= collection-name "premier")
           [:div.corner-ribbon {:class collection-name}
            collection-name])
         [:img {:src first-image}]])
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
    (let [taxon-path (get-in data state/browse-taxon-path)]
      (if-let [taxon (->> (get-in data state/taxons-path)
                          (filter #(= taxon-path (taxon-path-for %)))
                          first)]
        (let [taxon-permalink-class (string/replace (:permalink taxon) #"/" "-")]
          [:div
           [:div.taxon-products-banner {:class taxon-permalink-class}]
           [:div.taxon-products-container
            [:div.taxon-nav
             (map (partial display-taxon data taxon)
                  (get-in data state/taxons-path))
             [:div {:style {:clear "both"}}]]

            [:div.taxon-products-list-container
             (map (partial display-product data)
                  (get-in data (conj state/products-for-taxons-path (taxon-path-for taxon))))]]

           [:div.gold-features
            [:figure.guarantee-feature]
            [:figure.free-shipping-feature]
            [:figure.triple-bundle-feature]
            [:feature.fs-feature]]]))))))
