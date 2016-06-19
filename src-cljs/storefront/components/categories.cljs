(ns storefront.components.categories
  (:require [storefront.platform.component-utils :as utils]
            [storefront.keypaths :as keypaths]
            [storefront.hooks.experiments :as experiments]
            [storefront.accessors.taxons :as taxons]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]))

(defn category [index {:keys [name slug]}]
  [:a.p1.inline-block.col-6
   (merge
    {:key slug
     :data-test (str "taxon-" slug)}
    (utils/route-to events/navigate-category
                   {:taxon-slug slug})
    (when (<= index 5)
      {:class "lg-up-col-4"}))
   [:.bg-no-repeat.bg-top.bg-cover.flex.items-center
    {:class (str "img-" slug)
     :style {:height "200px"}}
    [:.h1.white.center.col-12.titleize.shadow.nowrap
     name]]])

(defn categories-component [data owner]
  (om/component
   (html
    [:div
     [:h1.regular.py2.center.black "Select your favorite style"]
     [:.clearfix.mxn1.center
      (->> (get-in data keypaths/taxons)
           (remove taxons/is-stylist-product?)
           (map-indexed category))]])))

(defn categories-page-component [data owner]
  (om/component
   (html
    ;; More spacing on this page than on home page
    [:.m2
     (om/build categories-component data)])))
