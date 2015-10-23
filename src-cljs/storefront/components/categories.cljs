(ns storefront.components.categories
  (:require [storefront.components.utils :as utils]
            ;; [storefront.hooks.analytics :as analytics]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.taxons :refer [filter-nav-taxons
                                                 taxon-class-name
                                                 taxon-path-for]]
            [om.core :as om]
            ;; [clojure.string :as string]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.hooks.experiments :as experiments]))

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

(defn bundle-builder-categories-component [data]
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
     [:div {:style {:clear "both"}}]]]])

(defn single-category [data category]
  (let [class (taxon-path-for category)]
    [:a.hair-category-link (utils/route-to data
                                           events/navigate-category
                                           {:taxon-path class})
     [:.hair-container
      [:.hair-details
       [:.hair-taxon {:class class}
        [:p.hair-taxon-name (:name category)]]]
      [:.hair-image-container
       [:.hair-model-image {:class class}]
       [:.hair-image {:class class}]
       [:.hair-shop-image]]]]))

(defn original-categories-component [data]
  [:div
   (list
    [:.categories-banner]
    [:.hair-categories
     (map (partial single-category data) (filter-nav-taxons (:taxons data)))]
    [:.gold-features
     [:figure.guarantee-feature]
     [:figure.free-shipping-feature]
     [:figure.triple-bundle-feature]])])

(defn categories-component [data owner]
  (om/component
   (html
    (if (experiments/bundle-builder? data)
      (bundle-builder-categories-component data)
      (original-categories-component data)))))
