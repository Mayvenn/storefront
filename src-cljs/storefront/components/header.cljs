(ns storefront.components.header
  (:require [storefront.components.utils :as utils]
            [storefront.components.svg :as svg]
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
       [:a.header-menu (utils/fake-href events/control-menu-expand
                                        {:keypath keypaths/menu-expanded})
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
   [:a.block (merge {:style {:width "60px" :padding "17px 12px"}}
                    (utils/fake-href events/control-menu-expand
                                     {:keypath keypaths/menu-expanded}))
    [:.border-top.border-bottom.border-black {:style {:height "13px"}} [:span.hide "MENU"]]
    [:.border-bottom.border-black {:style {:height "13px"}}]]))

(def logo
  (html
   [:a.block.img-logo.bg-no-repeat.bg-center.bg-contain.teal.pp3
    (merge {:style {:height "30px"}
            :title "Mayvenn"}
           (utils/route-to events/navigate-home))]))

(defn shopping-bag [cart-quantity]
  [:.relative (merge {:style {:min-height "60px"
                              :width "60px"}}
                     (utils/route-to events/navigate-cart))
   (svg/bag {:class "absolute overlay m-auto"} cart-quantity)
   [:.center.absolute.overlay.m-auto.f4 {:style {:height "1em"}}
    (when (pos? cart-quantity)
      [:span.teal cart-quantity])]])

(defn triangle-up [width class]
  [:.absolute.inline-block
   {:style {:top                 (str "-" width)
            :margin-left         (str "-" width)
            :border-bottom-width width
            :border-bottom-style "solid"
            :border-left         (str width " solid transparent")
            :border-right        (str width " solid transparent")}
    :class class}])

(defn carrot-top [{:keys [width-px bg-color border-color]}]
  (let [outer-width (str width-px "px")
        inner-width (str (dec width-px) "px")]
    [:div
     (triangle-up outer-width border-color)
     (triangle-up inner-width bg-color)]))

(defn store-dropdown [expanded? {store-name :store_name
                                 instagram-account :instagram_account
                                 store-photo :profile_picture_url
                                 address :address}]
  (utils/drop-down
   expanded?
   keypaths/store-info-expanded
   [:a
    [:.teal.pp3 {:style {:height "30px"}}
     [:.flex.justify-center.items-center
      [:span.f6.line-height-1.gray.nowrap.mrp3 "HAIR BY"]
      [:.truncate.fit.f4 (:firstname address)]] ;;TODO use new field 'nickname'
     [:span.h2 "ˇ"]]]
   [:div.absolute.left-0.right-0.mx-auto.mtp1 {:style {:max-width "240px"}}
    [:.border.border-light-gray.rounded-2.bg-pure-white.center.relative.top-lit
     (carrot-top {:width-px 5 :bg-color "border-pure-white" :border-color "border-light-gray"})
     [:div
      [:.p1.h5
       (when store-photo
         [:.m1 (utils/circle-picture {:class "mx-auto"} store-photo)])
       [:h3.h3.medium store-name]
       [:.gray.line-height-3 (goog.string/format "by %s %s" (:firstname address) (:lastname address)) ]
       (when instagram-account
         [:a.btn.teal {:href (str "http://instagram.com/" instagram-account)}
          [:.flex.justify-center.items-center
           [:.img-instagram.bg-no-repeat.bg-contain.mrp4 {:style {:width "10px" :height "10px"}}]
           [:div "@" instagram-account]]])]
      [:.border.border-silver]
      [:.p2.gray "Located in "
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
