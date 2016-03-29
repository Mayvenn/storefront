(ns storefront.components.categories
  (:require [storefront.components.utils :as utils]
            [storefront.keypaths :as keypaths]
            [storefront.hooks.experiments :as experiments]
            [storefront.accessors.taxons :refer [filter-nav-taxons
                                                 taxon-class-name
                                                 taxon-path-for]]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]))

(defn category [taxon index]
  (let [taxon-name (taxon :name)
        taxon-path (taxon-path-for taxon)]
    [:a.col.mn1.p1
     (merge
      {:key taxon-path}
      (utils/route-to events/navigate-category
                      {:taxon-path taxon-path})
      (if (> index 5)
        {:class "col-12"}
        {:class "sm-col-6 md-col-6 lg-col-4"}))
     [:.bg-no-repeat.bg-top.bg-cover.flex.items-center
      {:class (str "img-" taxon-path)
       :style {:width "100%"
               :height "200px"}}
      [:.h1.white.center.col-12.titleize.shadow
       taxon-name]]]))

(defn frontal-category [taxon index]
  (let [taxon-name (taxon :name)
        taxon-path (taxon-path-for taxon)]
    [:a.col.mn1.p1
     (merge
      (utils/route-to events/navigate-category
                      {:taxon-path taxon-path})
      (if (> index 5)
        {:class "sm-col-12 lg-col-6"}
        {:class "sm-col-6 md-col-6 lg-col-4"}))
     [:.bg-no-repeat.bg-top.bg-cover.flex.items-center
      {:class (str "img-" taxon-path)
       :style {:width "100%"
               :height "200px"}}
      [:.h1.white.center.col-12.titleize.shadow
       taxon-name]]]))

(defn categories-component [data owner]
  (om/component
   (html
    [:div.bundle-builder
     [:.center.black
      [:h1.regular
       [:div "Select your favorite style"]]]
     [:div.clearfix.px2.py1.category-grid
      (if (experiments/frontals? data)
        (map frontal-category (filter-nav-taxons (get-in data keypaths/taxons)) (range))
        (drop-last (map category (filter-nav-taxons (get-in data keypaths/taxons)) (range))))
      [:div {:style {:clear "both"}}]]])))
