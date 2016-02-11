(ns storefront.components.header
  (:require [storefront.components.utils :as utils]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.accessors.orders :as orders]
            [storefront.messages :refer [send]]
            [storefront.keypaths :as keypaths]
            [storefront.hooks.experiments :as experiments]))

(defn header-component [data owner]
  (let [getsat-login? (get-in data keypaths/get-satisfaction-login?)]
    (om/component
     (html
      (let [store (get-in data keypaths/store)]
        [:header#header.header
         (merge (when-not (store :profile_picture_url)
                  {:class "no-picture"})
                (when getsat-login?
                  {:class "comm-login"}))
         (when-not getsat-login?
           [:a.header-menu {:href "#"
                            :on-click (fn [e]
                                        (.preventDefault e)
                                        (send data
                                              events/control-menu-expand
                                              {:keypath keypaths/menu-expanded}))}
            "Menu"])
         (if getsat-login?
           [:div.logo.comm-login]
           [:a.logo (utils/route-to data events/navigate-home)])
         (when-not getsat-login?
           (list
            (let [product-quantity (orders/product-quantity (get-in data keypaths/order))]
              (if (> product-quantity 0)
                [:a.cart.populated
                 (utils/route-to data events/navigate-cart)
                 product-quantity]
                [:a.cart
                 (utils/route-to data events/navigate-cart)]))
            (when (= (get-in data keypaths/navigation-event) events/navigate-home)
              [:div.stylist-bar
               [:div.stylist-bar-img-container
                [:img.stylist-bar-portrait {:src (store :profile_picture_url)}]]
               [:div.stylist-bar-name (store :store_name)]])))])))))
