(ns storefront.components.header
  (:require [storefront.components.utils :as utils]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.state :as state]))

(defn header-component [data owner]
  (om/component
   (html
    (let [store (get-in data state/store-path)]
      [:header#header.header (when-not (store :profile_picture_url)
                               {:class "no-picture"})
       [:a.header-menu {:href "#"} "Menu"]
       [:a.logo (utils/route-to data events/navigate-home)]
       [:a.cart {:href "#"}]
       (when (= (get-in data state/navigation-event-path) events/navigate-home)
         [:div.stylist-bar
          [:div.stylist-bar-img-container
           [:img.stylist-bar-portrait {:src (store :profile_picture_url)}]]
          [:div.stylist-bar-name (store :store_name)]
          (when-let [instagram-account (store :instagram_account)]
            [:div.social-icons
             [:i.instagram-icon
              [:a.full-link {:href (str "http://instagram.com/" instagram-account) :target "_blank"}]]])])]))))
