(ns storefront.components.header
  (:require [storefront.components.utils :as utils]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]))

(defn header-component [data owner]
  (om/component
   (html
    [:header#header.header.no-picture
     [:a.header-menu {:href "#"} "Menu"]
     [:a.logo (utils/route-to data events/navigate-home)]
     [:a.cart {:href "#"}]
     [:div.stylist-bar
      [:div.stylist-bar-img-container
       [:img.stylist-bar-portrait {:src "#FIXME"}]]
      [:div.stylist-bar-name "SomeStylist#FIXME"]
      [:div.social-icons
       [:i.instagram-icons
        [:a.full-link {:href "http://instagram.com/#FIXME" :target "_blank"}]]]]])))
