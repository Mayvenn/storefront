(ns storefront.components.categories
  (:require [storefront.platform.component-utils :as utils]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.named-searches :as named-searches]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.events :as events]))

(defn category [index {:keys [name slug]}]
  [:a.p1.inline-block.col-6
   (merge
    {:key slug
     :data-test (str "named-search-" slug)}
    (utils/route-to events/navigate-category
                   {:named-search-slug slug})
    (when (<= index 5)
      {:class "lg-up-col-4"}))
   [:div.bg-no-repeat.bg-top.bg-cover
    {:class (str "img-" slug)
     :style {:height "200px"}}
    [:div.bg-darken-3.flex.items-center.col-12 {:style {:height "100%"}}
     [:div.h1.white.center.col-12.titleize.shadow.nowrap
      name]]]])

(defn component [{:keys [named-searches]} owner opts]
  (component/create
   [:div.m2
    [:h1.py2.center.black "Select your favorite style"]
    [:div.clearfix.mxn1.center
     (map-indexed category named-searches)]]))

(defn query [data]
  {:named-searches (remove named-searches/is-stylist-product? (named-searches/current-named-searches data))})

(defn built-component [data opts]
  (component/build component (query data) nil))
