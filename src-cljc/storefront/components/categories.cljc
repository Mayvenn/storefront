(ns storefront.components.categories
  (:require [storefront.platform.component-utils :as utils]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.named-searches :as named-searches]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.events :as events]))

(defn link-to-search [index {:keys [name slug representative-images]}]
  [:li.p1.inline-block.col-6
   (merge {:key slug}
          (when (<= index 5) {:class "col-4-on-tb-dt"}))
   [:a (merge
        {:data-test (str "named-search-" slug)}
        (utils/route-to events/navigate-category
                        {:named-search-slug slug}))
    (ui/aspect-ratio
     ;; TODO: Should we add new assets that are the top 2/3 of these images? Would double the downloads.
     ;; Or: should we make this more like the new home grid?
     3 2
     [:img.col-12.block (utils/img-attrs (:model-full representative-images) :large)]
     [:div.absolute.overlay.bg-darken-2
      [:div.flex.items-center.container-height
       [:div.h2.medium.white.col-12.titleize.shadow.nowrap name]]])]])

(defn component [{:keys [named-searches]} owner opts]
  (component/create
   [:div.container
    [:nav.m2 {:aria-labelledby "select-style-header"}
     [:h1#select-style-header.h2.py2.center.navy.regular "Select your favorite style"]
     [:ul.list-reset.clearfix.mxn1.center
      (map-indexed link-to-search named-searches)]]]))

(defn query [data]
  {:named-searches (remove named-searches/is-stylist-product? (named-searches/current-named-searches data))})

(defn built-component [data opts]
  (component/build component (query data) nil))
