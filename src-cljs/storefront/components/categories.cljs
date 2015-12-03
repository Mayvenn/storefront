(ns storefront.components.categories
  (:require [storefront.components.utils :as utils]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.taxons :refer [filter-nav-taxons
                                                 taxon-class-name
                                                 taxon-path-for]]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]))

(defn category [data taxon index]
  (let [taxon-name (taxon :name)
        taxon-path (taxon-path-for taxon)]
    [:a.hair-category-container
     (merge
      (utils/route-to data
                      events/navigate-category
                      {:taxon-path taxon-path})
      (when (> index 5)
        {:class "extra-wide"}))
     [:p.hair-category-label taxon-name]
     [:div.hair-category-image {:class taxon-path}]]))

(defn categories-component [data owner]
  (om/component
   (html
    [:div.bundle-builder
     [:header
      [:h1
       [:div "Select Your Favorite Style"]
       [:.category-header-sub "Mayvenn hair is available in six" [:br] "different styles for every occasion"]]]
     [:div
      [:div.category-list
       (map (partial category data)
            (filter-nav-taxons (get-in data keypaths/taxons))
            (range))
       [:div {:style {:clear "both"}}]]]])))
