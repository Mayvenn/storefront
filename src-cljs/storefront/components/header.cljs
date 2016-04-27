(ns storefront.components.header
  (:require [storefront.components.utils :as utils]
            [storefront.components.svg :as svg]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.taxons :refer [new-taxon? slug->name is-closure? is-extension? is-stylist-product?]]
            [storefront.accessors.stylists :refer [own-store?]]
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
       [:a.header-menu (utils/fake-href-menu-expand keypaths/menu-expanded)
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
   [:a.block (merge {:style {:width "60px" :padding "18px 12px"}}
                    (utils/fake-href-menu-expand keypaths/menu-expanded))
    [:.border-top.border-bottom.border-black {:style {:height "12px"}} [:span.hide "MENU"]]
    [:.border-bottom.border-black {:style {:height "12px"}}]]))

(defn logo [menu-height]
  (html
   [:div
    [:a.block.img-logo.bg-no-repeat.bg-center.bg-contain.teal.pp3
     (merge {:style {:height (str (/ menu-height 2) "px")}
             :title "Mayvenn"}
            (utils/route-to events/navigate-home))]]))

(defn shopping-bag [cart-quantity]
  [:.relative.pointer (merge {:style {:min-height "60px"
                              :width "60px"}}
                     (utils/route-to events/navigate-cart))
   (svg/bag {:class "absolute overlay m-auto"} cart-quantity)
   [:.center.absolute.overlay.m-auto.f5.teal {:style {:height "1em"}}
    (when (pos? cart-quantity)
      [:.mtp3 cart-quantity])]])

(defn triangle-up [width class]
  [:.absolute.inline-block
   {:style {:top                 (str "-" width)
            :margin-left         (str "-" width)
            :border-bottom-width width
            :border-bottom-style "solid"
            :border-left         (str width " solid transparent")
            :border-right        (str width " solid transparent")}
    :class class}])

(defn triangle-down [width class]
  [:.absolute.inline-block
   {:style {:bottom              (str "-" width)
            :margin-left         (str "-" width)
            :border-top-width width
            :border-top-style "solid"
            :border-left         (str width " solid transparent")
            :border-right        (str width " solid transparent")}
    :class class}])

(defn carrot-top [{:keys [width-px bg-color border-color]}]
  (let [outer-width (str width-px "px")
        inner-width (str (dec width-px) "px")]
    [:div
     (triangle-up outer-width border-color)
     (triangle-up inner-width bg-color)]))

(defn carrot-down [{:keys [width-px bg-color border-color]}]
  (let [outer-width (str width-px "px")
        inner-width (str (dec width-px) "px")]
    [:div
     (triangle-down outer-width border-color)
     (triangle-down inner-width bg-color)]))

(def header-navigation-selected-link "border-teal border-bottom")
(def header-menu-selected-link "border-teal border-bottom pyp3")

(defn selectable
  ([current-navigation-message class-str event-name]
   (selectable current-navigation-message class-str event-name {}))
  ([current-navigation-message class-str event-name args]
   (when (utils/current-page? current-navigation-message event-name args)
     {:class class-str})))

(defn store-dropdown [expanded?
                      {store-name :store_name
                       nickname :store_nickname
                       instagram-account :instagram_account
                       store-photo :profile_picture_url
                       address :address}]
  [:div
   (utils/drop-down
    expanded?
    keypaths/store-info-expanded
    [:a
     [:.teal {:style {:margin-bottom "10px"}}
      [:.flex.justify-center.items-center.pyp3
       [:span.line-height-1.gray.nowrap.mrp3 {:style {:font-size "8px"}} "HAIR BY"]
       [:.truncate.fit.f3 nickname]]
      [:.relative
       (carrot-down {:width-px 4 :bg-color "border-white" :border-color "border-teal"})]]]
    [:div.absolute.left-0.right-0.mx-auto {:style {:max-width "240px"}}
     [:.border.border-light-gray.rounded-2.bg-pure-white.center.relative.top-lit
      (carrot-top {:width-px 5 :bg-color "border-pure-white" :border-color "border-light-gray"})
      [:div
       [:.p1.f5
        (when store-photo
          [:.m1 (utils/circle-picture {:class "mx-auto"} store-photo)])
        [:h3.f3.medium store-name]
        [:.gray.line-height-3 (goog.string/format "by %s %s" (:firstname address) (:lastname address)) ]
        (when instagram-account
          [:a.btn.teal {:href (str "http://instagram.com/" instagram-account)}
           [:.flex.justify-center.items-center
            [:.img-instagram.bg-no-repeat.bg-contain.mrp4 {:style {:width "10px" :height "10px"}}]
            [:div "@" instagram-account]]])]
       [:.border.border-silver]
       [:.p2.f4.gray "Located in "
        [:span.black (:city address) ", " (:state address)]]]]])])

(defn stylist-dropdown [expanded?
                        selected-link?
                        {store-photo :profile_picture_url
                         address :address}]
  [:div
   (utils/drop-down
    expanded?
    keypaths/account-menu-expanded
    [:a.flex.items-center
     [:.black.flex-auto.text-right.right-align.h5.pt1
      [:.flex.justify-end.items-center
       (when store-photo
         [:.mx1.inline-block (utils/circle-picture {:class "mx-auto" :width "20px"} store-photo)])
       (:firstname address)]]
     [:.relative.ml1 {:style {:height "4px"}} (carrot-down {:width-px 4 :bg-color "border-white" :border-color "border-teal"})]]
    [:div.absolute.right-0 {:style {:max-width "140px"}}
     [:.border.border-light-gray.rounded-2.bg-pure-white.center.relative.top-lit {:style {:margin-right "-1em" :top "5px"}}
      [:.absolute {:style {:right "15px"}}
       (carrot-top {:width-px 5 :bg-color "border-pure-white" :border-color "border-light-gray"})]
      [:.h6.bg-pure-white.rounded-2.flex.flex-column.left-align
       [:div.px2.py1.line-height-4
        (selected-link? header-navigation-selected-link events/navigate-stylist-dashboard-commissions)
        [:a.teal.block (utils/route-to events/navigate-stylist-dashboard-commissions)
         [:span
          (selected-link? header-menu-selected-link events/navigate-stylist-dashboard)
          "Dashboard"]]
        [:a.teal.block (utils/navigate-community) "Community"]
        [:a.teal.block (utils/route-to events/navigate-stylist-manage-account)
         [:span (selected-link? header-menu-selected-link events/navigate-stylist-manage-account)
          "Account Settings"]]]
       [:.border-bottom.border-silver]
       [:a.teal.block.py1.center.bg-white.rounded-bottom-2 (utils/fake-href events/control-sign-out) "Logout"]]]])])

(defn customer-dropdown [expanded? selected-link? user-email]
  [:div
   (utils/drop-down
    expanded?
    keypaths/account-menu-expanded
    [:a.flex.items-center
     [:.black.flex-auto.text-right.right-align.h5.pt1 user-email]
     [:.relative.ml1 {:style {:height "4px"}} (carrot-down {:width-px 4 :bg-color "border-white" :border-color "border-teal"})]]
    [:div.absolute.right-0 {:style {:max-width "140px"}}
     [:.border.border-light-gray.rounded-2.bg-pure-white.center.relative.top-lit {:style {:margin-right "-1em"}}
      [:.absolute {:style {:right "15px"}}
       (carrot-top {:width-px 5 :bg-color "border-pure-white" :border-color "border-light-gray"})]
      [:.h6.bg-pure-white.flex.flex-column.left-align
       [:div.px2.py1.line-height-4
        [:a.teal.block.pyp1 (utils/route-to events/navigate-account-manage)
         [:span (selected-link? header-menu-selected-link events/navigate-account-manage)
          "Account Settings"]]
        [:a.teal.block (utils/route-to events/navigate-account-referrals)
         [:span (selected-link? header-menu-selected-link events/navigate-account-referrals)
          "Refer a Friend"]]]
       [:.border.border-silver]
       [:a.teal.block.py1.center.bg-white (utils/fake-href events/control-sign-out) "Logout"]]]])])

(defn guest-component []
  [:.right-align.h6.sans-serif
   [:a.inline-block.black (utils/route-to events/navigate-sign-in) "Sign In"]
   [:.inline-block.pxp4.black "|"]
   [:a.inline-block.black (utils/route-to events/navigate-sign-up) "Sign Up"]])

(defn row
  ([right] (row nil right))
  ([left right]
   [:.clearfix.pyp1
    [:.col.col-2 [:.px1 (or left utils/nbsp)]]
    [:.col.col-10.line-height-3 right]]))

(defn products-section [selected-link? title taxons]
  [:div
   (row [:.border-bottom.border-light-gray.black.h4 title])
   [:.my1
    (for [{:keys [name slug]} taxons]
      [:a.h5 (merge {:key slug} (utils/route-to events/navigate-category {:taxon-slug slug}))
       (row
        (when (new-taxon? slug) utils/new-flag)
        [:.teal.titleize
         [:span
          (selected-link? header-menu-selected-link events/navigate-category {:taxon-slug slug})
          (get slug->name slug name)]])])]])

(defn shop-dropdown [stylist? expanded? selected-link? taxons]
  [:.absolute.col-12.bg-top-white.bg-lighten-5.to-sm-hide.z1
   (when-not expanded? {:class "hide"})
   [:.flex.justify-center.items-start {:style {:padding "1em 6em 2em"}}
    [:div.col-4 (products-section selected-link? "Hair Extensions" (filter is-extension? taxons))]
    [:div.col-4 (products-section selected-link? "Closures" (filter is-closure? taxons))]
    (when stylist?
      [:div.col-4 (products-section selected-link? "Stylist Products" (filter is-stylist-product? taxons))])]])


(defn mobile-header [store cart-quantity store-expanded? class-str]
  [:.flex.bg-white {:style {:min-height "60px"}
                               :class class-str}
   hamburger
   [:.flex-auto.center
    [:.flex.flex-column.justify-between {:style {:height "60px"}}
     (logo 60)
     (store-dropdown store-expanded? store)]]
   (shopping-bag cart-quantity)])

(defn desktop-header [account-expanded?
                      selected-link?
                      shop-expanded?
                      store-expanded?
                      stylist?
                      cart-quantity
                      store
                      taxons
                      user-email
                      class-str]
  [:.clearfix {:on-mouse-leave (utils/collapse-all-menus-callback)}
   [:.bg-white.clearfix {:style {:min-height "80px"}
                         :class class-str}
    [:.col.col-4
     [:div {:style {:height "48px"}}]
     [:.right.h5.sans-serif.extra-light
      [:div.col.py1
       (selected-link? header-navigation-selected-link events/navigate-category)
       [:a.black
        {:href "/categories"
         :on-mouse-enter (utils/expand-menu-callback keypaths/menu-expanded)
         :on-click (utils/expand-menu-callback keypaths/menu-expanded)}
        "Shop"]]

      [:a.black.col.py1.ml4
       (merge
        (selected-link? header-navigation-selected-link events/navigate-guarantee)
        {:on-mouse-enter (utils/collapse-all-menus-callback)}
        (utils/route-to events/navigate-guarantee)) "Guarantee"]]]
    [:.col.col-4.center
     [:.flex.flex-column.justify-between {:style {:height "75px"}}
      (logo 80)
      (store-dropdown store-expanded? store)]]
    [:.col.col-4
     [:div
      [:.flex.justify-between.items-center.pt1 {:style {:height "48px"}}
       [:.flex-auto
        (cond
          stylist? (stylist-dropdown account-expanded? selected-link? store)
          user-email (customer-dropdown account-expanded? selected-link? user-email)
          :else (guest-component))]
       [:.pl2.self-bottom (shopping-bag cart-quantity)]]]
     [:.h5.sans-serif.extra-light
      [:a.black.col.py1.mr4 {:on-mouse-enter (utils/collapse-all-menus-callback)
                             :href "https://blog.mayvenn.com"} "Blog"]
      [:a.black.col.py1
       (merge
        (selected-link? header-navigation-selected-link events/navigate-help)
        {:on-mouse-enter (utils/collapse-all-menus-callback)}
        (utils/route-to events/navigate-help)) "Contact Us"]]]]
   (shop-dropdown stylist? shop-expanded? selected-link? taxons)])



(defn new-nav-component [{:keys [store
                                 cart-quantity
                                 store-expanded?
                                 account-expanded?
                                 stylist?
                                 shop-expanded?
                                 selected-link?
                                 store
                                 user-email
                                 taxons]} _]
  (om/component
   (html
    [:div
     (mobile-header store cart-quantity store-expanded? "sm-up-hide")
     (desktop-header account-expanded?
                     selected-link?
                     shop-expanded?
                     store-expanded?
                     stylist?
                     cart-quantity
                     store
                     taxons
                     user-email
                     "to-sm-hide")])))

(defn new-nav-query [data]
  {:store             (get-in data keypaths/store)
   :store-expanded?   (get-in data keypaths/store-info-expanded)
   :account-expanded? (get-in data keypaths/account-menu-expanded)
   :shop-expanded?    (get-in data keypaths/menu-expanded)
   :cart-quantity     (orders/product-quantity (get-in data keypaths/order))
   :stylist?          (own-store? data)
   :selected-link?    (partial selectable (get-in data keypaths/navigation-message))
   :user-email        (get-in data keypaths/user-email)
   :taxons            (cond->> (get-in data keypaths/taxons)
                        (not (experiments/frontals? data)) (remove (comp #{"frontals"} :slug)))})

(defn built-new-component [data]
  (om/build new-nav-component (new-nav-query data)))
