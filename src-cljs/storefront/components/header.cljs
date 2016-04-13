(ns storefront.components.header
  (:require [storefront.components.utils :as utils]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.accessors.orders :as orders]
            [storefront.keypaths :as keypaths]
            [storefront.hooks.experiments :as experiments]
            [clojure.string :as str]))

(defn header-component [{:keys [store order home-page?]} _]
  (om/component
   (html
    (let [product-quantity (orders/product-quantity order)
          {store-photo-url :profile_picture_url store-name :store_name} store]
      [:header#header.header
       (when-not store-photo-url
         {:class "no-picture"})
       [:a.header-menu {:href "#"
                        :on-click
                        (utils/send-event-callback events/control-menu-expand
                                                   {:keypath keypaths/menu-expanded})}
        "Menu"]
       [:a.logo (utils/route-to events/navigate-home)]
       (if (> product-quantity 0)
         [:a.cart.populated (utils/route-to events/navigate-cart) product-quantity]
         [:a.cart (utils/route-to events/navigate-cart)])
       (when home-page?
         [:div.stylist-bar
          [:div.stylist-bar-img-container
           [:img.stylist-bar-portrait {:src store-photo-url}]]
          [:div.stylist-bar-name store-name]])]))))

(defn header-query [data]
  {:store      (get-in data keypaths/store)
   :home-page? (= (get-in data keypaths/navigation-event) events/navigate-home)
   :order      (get-in data keypaths/order)})

(def hamburger
  (html
   [:div {:style {:width "60px" :padding "17px 12px"}}
    [:.border-top.border-bottom.border-black {:style {:height "13px"}} [:span.hide "MENU"]]
    [:.border-bottom.border-black {:style {:height "13px"}}]]))

(def logo
  (html
   [:a.block.img-logo.bg-no-repeat.bg-center.bg-contain.teal
    (merge {:style {:height "30px" :padding "0.25rem"}
            :alt "Mayvenn"}
           (utils/route-to events/navigate-home))]))

(defn shopping-bag [cart-quantity]
  [:.relative {:style {:min-height "60px" :width "60px"}}
   [:.center.absolute.overlay.m-auto {:style {:font-size "12px" :height "1em"}}
    (if (pos? cart-quantity)
      [:span.teal cart-quantity]
      [:span "no"])]])

(defn carrot-top [{:keys [width-px bg-color border-color]}]
  (let [outer-width (str width-px "px")
        inner-width (str (dec width-px) "px")]
    [:div
     [:.absolute.inline-block
      {:style {:top (str "-" outer-width) :margin-left (str "-" outer-width) :border (str outer-width " solid transparent") :border-top "none" :border-bottom-color border-color}}]
     [:.absolute.inline-block
      {:style {:top (str "-" inner-width) :margin-left (str "-" inner-width) :border (str inner-width " solid transparent") :border-top "none" :border-bottom-color bg-color}}]]))

(defn store-dropdown [expanded? {store-name :store_name
                                 instagram-account :instagram_account
                                 store-photo :profile_picture_url
                                 address :address}]
  (utils/drop-down
   expanded?
   keypaths/store-info-expanded
   [:a
    [:.teal {:style {:height "30px" :padding-top "0.25rem"}}
     [:.flex.justify-center.items-center
      [:span.line-height-1.gray.nowrap {:style {:font-size "7px" :margin-right "0.25rem"}} "HAIR BY"]
      [:.truncate.fit {:style {:font-size "12px"}} (:firstname address)]] ;;TODO use new field 'nickname'
     [:span.h2 "ˇ"]]]
   [:div.absolute.left-0.right-0.mx-auto {:style {:max-width "240px" :margin-top "0.125rem"}}
    [:.border.rounded-2.bg-pure-white.center.relative
     {:style {:border-color "#d8d8d8"
              :box-shadow "0 2px 4px 0 rgba(0, 0, 0, 0.15)"}}
     (carrot-top {:width-px 5 :bg-color "#fff" :border-color "#d8d8d8"})
     [:div
      [:.p1
       (when store-photo
         [:.m1 (utils/circle-picture {:class "mx-auto"} store-photo)])
       [:h3.medium store-name]
       [:.h5.gray.line-height-3 (goog.string/format "by %s %s" (:firstname address) (:lastname address)) ]
       (when instagram-account
         [:a.block.p1.h5.teal {:href (str "http://instagram.com/" instagram-account)} "@" instagram-account])]
      [:.border.border-silver]
      [:.p2.h5.gray "Located in "
       [:span.black (:city address) ", " (:state address)]]]]]))

(defn new-nav-component [{:keys [store cart-quantity store-expanded?]} _]
  (om/component
   (html
    [:.flex {:style {:min-height "60px"}}
     hamburger
     [:.flex-auto.center
      logo
      (store-dropdown store-expanded? store)]
     (shopping-bag cart-quantity)])))

(defn new-nav-query [data]
  {:store         (get-in data keypaths/store)
   :store-expanded? (get-in data keypaths/store-info-expanded)
   :cart-quantity (orders/product-quantity (get-in data keypaths/order))})

(defn built-new-component [data]
  (om/build new-nav-component (new-nav-query data)))
