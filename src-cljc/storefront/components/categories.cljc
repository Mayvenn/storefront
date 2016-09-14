(ns storefront.components.categories
  (:require [storefront.platform.component-utils :as utils]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.named-searches :as named-searches]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.events :as events]))

(defn link-to-search [index {:keys [name slug]}]
  [:li.p1.inline-block.col-6
   (merge {:key slug}
          (when (<= index 5) {:class "lg-up-col-4"}))
   [:a (merge
        {:data-test (str "named-search-" slug)}
        (utils/route-to events/navigate-category
                        {:named-search-slug slug}))
    [:div.bg-no-repeat.bg-top.bg-cover
     {:class (str "img-" slug)
      :style {:height "200px"}}
     [:div.bg-darken-3.flex.items-center.col-12 {:style {:height "100%"}}
      [:div.h1.white.center.col-12.titleize.shadow.nowrap
       name]]]]])

(defn component [{:keys [named-searches]} owner opts]
  (component/create
   [:nav.m2 {:role "navigation" :aria-labelledby "select-style-header"}
    [:h1#select-style-header.py2.center.black "Select your favorite style"]
    [:ul.list-reset.clearfix.mxn1.center
     (map-indexed link-to-search named-searches)]]))

(defn query [data]
  {:named-searches (remove named-searches/is-stylist-product? (named-searches/current-named-searches data))})

(defn built-component [data opts]
  (component/build component (query data) nil))
