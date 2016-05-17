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
            [storefront.components.utils :as utils]
            [storefront.components.ui :as ui]
            [clojure.string :as str]))

(defn fake-href-menu-expand [keypath]
  {:href "#"
   :on-click
   (utils/send-event-callback events/control-menu-expand {:keypath keypath})})

(def sans-stylist? #{"store" "shop"})

(def hamburger
  (html
   [:a.block (merge {:style {:width "60px" :padding "18px 12px"}}
                    {:data-test "hamburger"}
                    (fake-href-menu-expand keypaths/menu-expanded))
    [:.border-top.border-bottom.border-black {:style {:height "12px"}} [:span.hide "MENU"]]
    [:.border-bottom.border-black {:style {:height "12px"}}]]))

(defn logo [height]
  (html
   [:a.block.img-logo.bg-no-repeat.bg-center.bg-contain.green.pp3
    (merge {:style {:height height}
            :title "Mayvenn"}
           (utils/route-to events/navigate-home))]))

(defn shopping-bag [cart-quantity]
  [:.relative.pointer (merge {:style {:height "60px" :width "60px"}}
                             {:data-test "cart"}
                             (utils/route-to events/navigate-cart))
   (svg/bag {:class "absolute overlay m-auto"} cart-quantity)
   (when (pos? cart-quantity)
     [:.absolute.overlay.m-auto {:style {:height "10px"}}
      [:.center.navy.f5.mtp3 {:data-test "populated-cart"} cart-quantity]])])

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
   {:style {:bottom           (str "-" width)
            :margin-left      (str "-" width)
            :border-top-width width
            :border-top-style "solid"
            :border-left      (str width " solid transparent")
            :border-right     (str width " solid transparent")}
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

(def navy-carrot-bottom
  (html
   (carrot-down {:width-px 4 :bg-color "border-white" :border-color "border-navy"})))

(def notch-up
  (html
   (carrot-top {:width-px 5 :bg-color "border-pure-white" :border-color "border-light-silver"})))

(def selected-link        "border-navy border-bottom border-width-2")
(def padded-selected-link "border-navy border-bottom border-width-2 pyp3")

(defn social-link [img-attrs href title]
  [:a.f4.navy.block.p1.rounded-bottom-2.border-top.border-bottom.border-dark-white.bg-white {:href href}
   [:.flex.items-center
    [:.mr1 {:style {:width "15px"}}
     [:.bg-no-repeat.bg-contain img-attrs]]
    [:.pp2 title]]])

(defn store-dropdown [expanded?
                      {store-name :store_name
                       nickname :store_nickname
                       instagram-account :instagram_account
                       styleseat-account :styleseat_account
                       store-photo :profile_picture_url
                       address :address}]
  [:div.center
   (ui/drop-down
    expanded?
    keypaths/store-info-expanded
    [:a
     [:div {:style {:margin-bottom "10px"}}
      [:.flex.justify-center.items-center.mtp3
       [:span.line-height-1.gray.nowrap.mrp3.f6 "HAIR BY"]
       [:.truncate.fit.f3.navy {:data-test "nickname"} nickname]]
      [:.relative navy-carrot-bottom]]]
    [:.absolute.left-0.right-0.mx-auto {:style {:width "188px"}}
     [:.relative.border.border-light-silver.rounded-2.bg-pure-white.top-lit
      notch-up
      [:div
       [:.py1.f5
        (when store-photo
          [:.m1 (ui/circle-picture {:class "mx-auto"} store-photo)])
        [:h3.f3.medium store-name]
        [:.gray.line-height-3 "by " (:firstname address) " " (:lastname address) ]
        [:.mt1.f5.gray.muted "Located in " (:city address) ", " (:state address)]]
       (when instagram-account
         (social-link {:class "img-instagram mlp1" :style {:width "12px" :height "12px"}}
                      (str "http://instagram.com/" instagram-account)
                      "Follow me on Instagram"))
       (when styleseat-account
         (social-link {:class "img-styleseat" :style {:width "15px" :height "14px"}}
                      (str "https://www.styleseat.com/v/" styleseat-account)
                      "Book me on StyleSeat"))]]])])

(defn account-dropdown [expanded? link & menu]
  (ui/drop-down
   expanded?
   keypaths/account-menu-expanded
   [:a.flex.items-center
    [:.black.flex-auto.right-align.h5 link]
    [:.relative.ml1.mtn1 {:style {:height "4px"}} navy-carrot-bottom]]
   [:.absolute.right-0 {:style {:max-width "140px"}}
    [:.relative.border.border-light-silver.rounded-2.bg-pure-white.top-lit {:style {:margin-right "-1em" :top "5px"}}
     [:.absolute {:style {:right "15px"}} notch-up]
     [:.h6.bg-pure-white.rounded-2
      (into [:.px2.py1.line-height-4] menu)
      [:.border-bottom.border-dark-white]
      [:a.navy.block.py1.center.bg-white.rounded-bottom-2 (utils/fake-href events/control-sign-out) "Logout"]]]]))

(defn account-link [current-page? nav-event title]
  [:a.green.block (utils/route-to nav-event)
   [:span (when current-page? {:class padded-selected-link}) title]])

(defn stylist-account [expanded?
                       current-page?
                       {store-photo :profile_picture_url
                        address :address}]
  (account-dropdown
   expanded?
   [:.flex.justify-end.items-center
    (when store-photo [:.mr1 (ui/circle-picture {:class "mx-auto" :width "20px"} store-photo)])
    [:.truncate (:firstname address)]]
   (account-link (current-page? events/navigate-stylist-dashboard) events/navigate-stylist-dashboard-commissions "Dashboard")
   [:a.green.block (utils/navigate-community) "Community"]
   (account-link (current-page? events/navigate-stylist-manage-account) events/navigate-stylist-manage-account "Account Settings")))

(defn customer-account [expanded? current-page? user-email]
  (account-dropdown
   expanded?
   [:.truncate user-email]
   (account-link (current-page? events/navigate-account-manage) events/navigate-account-manage "Account Settings")
   (account-link (current-page? events/navigate-account-referrals) events/navigate-account-referrals "Refer a Friend")))

(def guest-account
  (html
   [:.right-align.h6.sans-serif
    [:a.inline-block.black (utils/route-to events/navigate-sign-in) "Sign In"]
    [:.inline-block.pxp4.black "|"]
    [:a.inline-block.black (utils/route-to events/navigate-sign-up) "Sign Up"]]))

(defn row
  ([right] (row nil right))
  ([left right]
   [:.clearfix.pyp1
    [:.col.col-2 [:.px1 (or left ui/nbsp)]]
    [:.col.col-10.line-height-3 right]]))

(defn products-section [current-page? title taxons]
  [:div
   (row [:.border-bottom.border-light-silver.black.h4 title])
   [:.my1
    (for [{:keys [name slug]} taxons]
      [:a.h5 (merge {:key slug} (utils/route-to events/navigate-category {:taxon-slug slug}))
       (row
        (when (new-taxon? slug) ui/new-flag)
        [:span.green.titleize
         (when (current-page? events/navigate-category {:taxon-slug slug}) {:class padded-selected-link})
         (get slug->name slug name)])])]])

(defn shop-panel [stylist? expanded? current-page? taxons]
  [:.absolute.col-12.bg-white.to-lg-hide.z1.top-lit
   (when-not expanded? {:class "hide"})
   [:.flex.items-start {:style {:padding "1em 10% 2em"}}
    [:.col-4 (products-section current-page? "Hair Extensions" (filter is-extension? taxons))]
    [:.col-4 (products-section current-page? "Closures" (filter is-closure? taxons))]
    (when stylist?
      [:.col-4 (products-section current-page? "Stylist Products" (filter is-stylist-product? taxons))])]])

(defn desktop-nav-link-options [current-page? nav-event]
  (merge
   {:on-mouse-enter (utils/collapse-menus-callback keypaths/header-menus)}
   (when (current-page? nav-event) {:class selected-link})
   (utils/route-to nav-event)))

(defn lower-left-desktop-nav [current-page?]
  [:.to-lg-hide {:style {:margin-top "-12px"}}
   [:.right.h5.sans-serif.extra-light
    [:a.black.col.py1 (merge
                       {:href           "/categories"
                        :on-mouse-enter (utils/expand-menu-callback keypaths/shop-menu-expanded)
                        :on-click       (utils/expand-menu-callback keypaths/shop-menu-expanded)}
                       (when (current-page? events/navigate-category) {:class selected-link}))
     "Shop"]
    [:a.black.col.py1.ml4 (desktop-nav-link-options current-page? events/navigate-guarantee)
     "Guarantee"]]])

(defn lower-right-desktop-nav [current-page?]
  [:.to-lg-hide {:style {:margin-top "-12px"}}
   [:.h5.sans-serif.extra-light
    [:a.black.col.py1.mr4 {:on-mouse-enter (utils/collapse-menus-callback keypaths/header-menus)
                           :href           "https://blog.mayvenn.com"}
     "Blog"]
    [:a.black.col.py1 (desktop-nav-link-options current-page? events/navigate-help)
     "Contact Us"]]])

(defn component [{:keys [nav-message
                                 account-expanded?
                                 shop-expanded?
                                 store-expanded?
                                 stylist?
                                 cart-quantity
                                 store
                                 taxons
                                 user-email]} _]
  (om/component
   (html
    (let [current-page? (partial utils/current-page? nav-message)]
      [:.clearfix {:on-mouse-leave (utils/collapse-menus-callback keypaths/header-menus)}
       [:.flex.items-stretch.bg-white.clearfix {:style {:min-height "60px"}}
        [:.col-4
         [:div {:style {:height "60px"}} [:.lg-up-hide hamburger]]
         (lower-left-desktop-nav current-page?)]
        (into [:.col-4.flex.flex-column.justify-center {:style {:min-width "188px"}}]
              (if (sans-stylist? (:store_slug store))
                (list (logo "40px"))
                (list
                 (logo "30px")
                 (store-dropdown store-expanded? store))))
        [:.col-4
         [:.flex.justify-end.items-center
          [:.flex-auto.to-lg-hide.pr2
           (cond
             stylist?   (stylist-account account-expanded? current-page? store)
             user-email (customer-account account-expanded? current-page? user-email)
             :else      guest-account)]
          (shopping-bag cart-quantity)]
         (lower-right-desktop-nav current-page?)]]
       (shop-panel stylist? shop-expanded? current-page? taxons)]))))

(defn query [data]
  {:store             (get-in data keypaths/store)
   :store-expanded?   (get-in data keypaths/store-info-expanded)
   :account-expanded? (get-in data keypaths/account-menu-expanded)
   :shop-expanded?    (get-in data keypaths/shop-menu-expanded)
   :cart-quantity     (orders/product-quantity (get-in data keypaths/order))
   :stylist?          (own-store? data)
   :nav-message       (get-in data keypaths/navigation-message)
   :user-email        (get-in data keypaths/user-email)
   :taxons            (get-in data keypaths/taxons)})

(defn built-component [data]
  (om/build component (query data)))
