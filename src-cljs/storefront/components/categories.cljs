(ns storefront.components.categories
  (:require [storefront.components.utils :as utils]
            [storefront.keypaths :as keypaths]
            [storefront.hooks.experiments :as experiments]
            [storefront.accessors.taxons :refer [filter-nav-taxons
                                                 taxon-path-for]]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]))

(defn category [frontals? index taxon]
  (let [taxon-name (taxon :name)
        taxon-path (taxon-path-for taxon)]
    [:a.col.p1
     (merge
      {:key taxon-path}
      (utils/route-to events/navigate-category
                      {:taxon-path taxon-path})
      (if (> index 5)
        {:class (str "col-12" (when frontals? " lg-col-6"))}
        {:class "col-6 lg-col-4"}))
     [:.bg-no-repeat.bg-top.bg-cover.flex.items-center.col-12
      {:class (str "img-" taxon-path)
       :style {:height "200px"}}
      [:.h1.white.center.col-12.titleize.shadow
       taxon-name]]]))

(defn categories-component [data owner]
  (om/component
   (html
    [:div
     [:.center.black
      [:h1.regular.py2
       [:div "Select your favorite style"]]]
     [:div.clearfix.mxn1.category-grid
      (let [frontals? (experiments/frontals? data)
            taxons (map-indexed (partial category frontals?) (filter-nav-taxons (get-in data keypaths/taxons)))]
        (if frontals?
          taxons
          (drop-last taxons)))
      [:div {:style {:clear "both"}}]]])))

(defn categories-page-component [data owner]
  (om/component
   (html
    [:.m2
     (om/build categories-component data)])))
