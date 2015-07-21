(ns storefront.components.category
  (:require [storefront.components.utils :as utils]
            [storefront.taxons :refer [taxon-path-for taxon-class-name]]
            [clojure.string :as string]
            [storefront.products :refer [collection->grade strip-origin-and-collection]]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.experiments :as experiments]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.query :as query]))

(defn display-taxon [data selected-taxon taxon]
  (let [taxon-path (taxon-path-for taxon)
        selected-class (if (= selected-taxon taxon) "selected" nil)
        taxon-classes (string/join " " (conj [taxon-path] selected-class))]
    [:div.hair-taxon.decorated.small-width {:class taxon-classes}
     [:a.taxon-link (utils/route-to data events/navigate-category {:taxon-path taxon-path})
      [:p.hair-taxon-name (:name taxon)]]]))

(defn display-product [data taxon product]
  (let [collection-name (:collection_name product)]
   [:a {:href (utils/href-to data events/navigate-product {:product-path (:slug product) :query-params {:taxon_id (taxon :id)}})
        :on-click (utils/click-to data events/control-click-category-product {:target product :taxon taxon})}
     [:div.taxon-product-container
      (when-let [first-image (->> product
                                  :master
                                  :images
                                  first
                                  :product_url)]
        [:div.taxon-product-image-container
         {:style {:background-image (str "url(" first-image ")")}}
         (when-not (experiments/display-variation data "add-grades")
           (when-not (= collection-name "premier")
             [:div.corner-ribbon {:class collection-name}
              collection-name]))
         [:img {:src first-image}]])
      [:div.taxon-product-info-container
       [:div.taxon-product-description-container
        [:div.taxon-product-collection
         [:div.taxon-product-collection-indicator
          {:class collection-name}]
         (if (experiments/display-variation data "add-grades")
           (collection->grade collection-name (:name product))
           collection-name)]
        [:div.taxon-product-title
         (if (experiments/display-variation data "add-grades")
           (strip-origin-and-collection (:name product))
           (:name product))]]
       [:div.taxon-from-price
        [:span "From: "]
        [:br]
        "$"
        (js/Math.floor (product :from_price))]]]]))

(defn category-component [data owner]
  (om/component
   (html
    (if-let [taxon (query/get (get-in data keypaths/browse-taxon-query)
                              (get-in data keypaths/taxons))]
      [:div
       [:div.taxon-products-banner {:class (taxon-class-name taxon)}]
       [:div.taxon-products-container
        [:div.taxon-nav
         (map (partial display-taxon data taxon)
              (get-in data keypaths/taxons))
         [:div {:style {:clear "both"}}]]

        [:div.taxon-products-list-container
         (let [products (->> (get-in data keypaths/products)
                             vals
                             (sort-by :index)
                             (filter #(contains? (set (:taxon_ids %)) (:id taxon))))]
           (if (get-in data (concat keypaths/api-requests
                                    request-keys/get-products
                                    [(taxon-path-for taxon)]))
             [:.spinner]
             (map (partial display-product data taxon) products)))]]

       [:div.gold-features
        [:figure.guarantee-feature]
        [:figure.free-shipping-feature]
        [:figure.triple-bundle-feature]
        [:feature.fs-feature]]]))))
