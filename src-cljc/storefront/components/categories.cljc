(ns storefront.components.categories
  #?@(:cljs [(:require-macros [storefront.component-macros :as component])])
  (:require [storefront.platform.component-utils :as utils]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.taxons :as taxons]
            #?@(:clj [[storefront.component :as component]])
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
   [:div.bg-no-repeat.bg-top.bg-cover.flex.items-center
    {:class (str "img-" slug)
     :style {:height "200px"}}
    [:div.h1.white.center.col-12.titleize.shadow.nowrap
     name]]])

(defn categories-component [data owner opts]
  (component/create
   [:div
    [:h1.regular.py2.center.black "Select your favorite style"]
    [:div.clearfix.mxn1.center
     (->> (get-in data keypaths/taxons)
          (remove taxons/is-stylist-product?)
          (map-indexed category))]]))

(defn categories-page-component [data owner opts]
  (component/create
   ;; More spacing on this page than on home page
   [:div.m2
    (component/build categories-component data nil)]))
