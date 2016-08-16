(ns storefront.components.categories
  (:require [storefront.platform.component-utils :as utils]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.taxons :as taxons]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
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

(defn component [{:keys [taxons]} owner opts]
  (component/create
   [:div.m2
    [:h1.py2.center.black "Select your favorite style"]
    [:div.clearfix.mxn1.center
     (map-indexed category taxons)]]))

(defn query [data]
  {:taxons (remove taxons/is-stylist-product? (taxons/current-taxons data))})

(defn built-component [data opts]
  (component/build component (query data) nil))
