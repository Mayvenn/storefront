(ns storefront.components.header
  (:require [storefront.components.utils :as utils]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn header-component [data owner]
  (om/component
   (html
    (let [store (get-in data keypaths/store-path)]
      [:header#header.header (when-not (store :profile_picture_url)
                               {:class "no-picture"})
       [:a.header-menu {:href "#" :on-click (utils/enqueue-event data events/control-menu-expand)} "Menu"]
       [:a.logo (utils/route-to data events/navigate-home)]
       (let [item-count (get-in data (conj keypaths/order-path :total_quantity))]
         (if (> item-count 0)
           [:a.cart.populated
            (utils/route-to data events/navigate-cart)
            item-count]
           [:a.cart
            (utils/route-to data events/navigate-cart)]))
       (when (= (get-in data keypaths/navigation-event-path) events/navigate-home)
         [:div.stylist-bar
          [:div.stylist-bar-img-container
           [:img.stylist-bar-portrait {:src (store :profile_picture_url)}]]
          [:div.stylist-bar-name (store :store_name)]
          (when-let [instagram-account (store :instagram_account)]
            [:div.social-icons
             [:i.instagram-icon
              [:a.full-link {:href (str "http://instagram.com/" instagram-account) :target "_blank"}]]])])]))))
