(ns storefront.components.categories
  (:require [storefront.components.utils :as utils]
            [storefront.keypaths :as keypaths]
            [storefront.hooks.experiments :as experiments]
            [storefront.accessors.taxons :refer [filter-nav-taxons]]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]))

(defn category [frontals? index {:keys [name slug]}]
  [:a.p1.inline-block
   (merge
    {:key slug
     :data-test (str "taxon-" slug)}
    (utils/route-to events/navigate-category
                   {:taxon-slug slug})
    (if (> index 5)
      {:class (if frontals?
                "col-6"
                "col-12 lg-col-6")}
      {:class "col-6 lg-col-4"}))
   [:.bg-no-repeat.bg-top.bg-cover.flex.items-center
    {:class (str "img-" slug)
     :style {:height "200px"}}
    [:.h1.white.center.col-12.titleize.shadow.nowrap
     name]]])

(defn categories-component [data owner]
  (om/component
   (html
    [:div
     [:.center.black
      [:h1.regular.py2
       [:div "Select your favorite style"]]]
     [:.clearfix.mxn1.center
      (let [frontals? (experiments/frontals? data)
            taxons (filter-nav-taxons (get-in data keypaths/taxons))
            taxons (if frontals? taxons (drop-last taxons))]
        (map-indexed (partial category frontals?) taxons))]])))

(defn categories-page-component [data owner]
  (om/component
   (html
    [:.m2
     (om/build categories-component data)])))
