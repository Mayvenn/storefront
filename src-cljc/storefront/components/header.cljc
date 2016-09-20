(ns storefront.components.header
  (:require [storefront.platform.component-utils :as utils]
            [storefront.assets :as assets]
            [storefront.components.svg :as svg]
            [storefront.events :as events]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.accessors.orders :as orders]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.accessors.stylists :refer [own-store?]]
            [storefront.keypaths :as keypaths]
            [storefront.app-routes :as app-routes]
            [storefront.platform.component-utils :as utils]
            [storefront.components.ui :as ui]
            [clojure.string :as str]))

(defn fake-href-menu-expand [keypath]
  {:href "#"
   :on-click
   (utils/send-event-callback events/control-menu-expand {:keypath keypath})})

(def sans-stylist? #{"store" "shop"})

(def hamburger
  (component/html
   [:a.block (merge {:style {:width "60px" :padding "18px 12px"}}
                    {:data-test "hamburger"}
                    (fake-href-menu-expand keypaths/menu-expanded))
    [:div.border-top.border-bottom.border-dark-gray {:style {:height "12px"}} [:span.hide "MENU"]]
    [:div.border-bottom.border-dark-gray {:style {:height "12px"}}]]))

(defn logo [height]
  (component/html
   [:a.block.img-logo.bg-no-repeat.bg-center.bg-contain.teal.pp3
    (merge {:style {:height height}
            :title "Mayvenn"
            :item-prop "logo"
            :content (str "https:" (assets/path "/images/header_logo.png"))}
           (utils/route-to events/navigate-home))]))

(defn shopping-bag [cart-quantity]
  [:a.relative.pointer.block (merge {:style {:height "60px" :width "60px"}}
                                    {:data-test "cart"}
                                    (utils/route-to events/navigate-cart))
   (svg/bag {:class "absolute overlay m-auto"} cart-quantity)
   (when (pos? cart-quantity)
     [:div.absolute.overlay.m-auto {:style {:height "10px"}}
      [:div.center.navy.f6.mtp3 {:data-test "populated-cart"} cart-quantity]])])

(defn triangle-up [width class]
  [:div.absolute.inline-block
   {:style {:top                 (str "-" width)
            :margin-left         (str "-" width)
            :border-bottom-width width
            :border-bottom-style "solid"
            :border-left         (str width " solid transparent")
            :border-right        (str width " solid transparent")}
    :class class}])

(defn triangle-down [width class]
  [:div.absolute.inline-block
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
  (component/html
   (carrot-down {:width-px 4 :bg-color "border-light-silver" :border-color "border-navy"})))

(def notch-up
  (component/html
   (carrot-top {:width-px 5 :bg-color "border-white" :border-color "border-dark-silver"})))

(def selected-link        "border-navy border-bottom border-width-2")
(def padded-selected-link "border-navy border-bottom border-width-2 pyp3")

;; Sharing this width ensure the popup is centered on mobile
(def popup-width          "188px")

(defn social-link [img href title]
  [:a.f5.navy.block.p1.rounded-bottom-1.border-top.border-dark-silver.bg-light-silver {:href href}
   [:div.flex.items-center
    [:div.mr1 {:style {:width "15px"}} img]
    [:div.pp2 title]]])

(defn store-dropdown [expanded?
                      {store-name :store_name
                       nickname :store_nickname
                       instagram-account :instagram_account
                       styleseat-account :styleseat_account
                       store-photo :profile_picture_url}]
  [:div.center
   (ui/drop-down
    expanded?
    keypaths/store-info-expanded
    [:a
     [:div {:style {:margin-bottom "10px"}}
      [:div.flex.justify-center.items-center.mtp3
       [:span.line-height-1.gray.nowrap.mrp3.f6 "HAIR BY"]
       [:div.truncate.fit.f4.navy {:data-test "nickname"} nickname]]
      [:div.relative navy-carrot-bottom]]]
    [:div.absolute.left-0.right-0.mx-auto {:style {:width popup-width}}
     [:div.relative.border.border-dark-silver.rounded-1.bg-white.top-lit
      notch-up
      [:div
       [:div.p1.f6
        [:div.m1 (ui/circle-picture {:class "mx-auto"} store-photo)]
        [:h4.f4 store-name]]
       (when instagram-account
         (social-link
          [:div.mlp1.fill-light-gray {:style {:width "15px" :height "15px"}} svg/instagram]
          (str "http://instagram.com/" instagram-account)
          "Follow me on Instagram"))
       (when styleseat-account
         (social-link
          [:div.bg-no-repeat.bg-contain.img-styleseat {:style {:width "15px" :height "14px"}}]
          (str "https://www.styleseat.com/v/" styleseat-account)
          "Book me on StyleSeat"))]]])])

(defn account-dropdown [expanded? link & menu]
  (ui/drop-down
   expanded?
   keypaths/account-menu-expanded
   [:a.flex.items-center
    [:div.dark-gray.flex-auto.right-align.h5 link]
    [:div.relative.ml1.mtn1 {:style {:height "4px"}} navy-carrot-bottom]]
   [:div.absolute.right-0 {:style {:max-width "140px"}}
    [:div.relative.border.border-dark-silver.rounded-1.bg-white.top-lit {:style {:margin-right "-1em" :top "5px"}}
     [:div.absolute {:style {:right "15px"}} notch-up]
     [:div.h6.bg-white.rounded-1
      (into [:div.px2.py1.line-height-4] menu)
      [:div.border-bottom.border-dark-silver]
      [:a.navy.block.py1.center.bg-light-silver.rounded-bottom-1
       (utils/fake-href events/control-sign-out) "Logout"]]]]))

(defn account-link [current-page? nav-event title]
  [:a.teal.block (utils/route-to nav-event)
   [:span (when current-page? {:class padded-selected-link}) title]])

(defn stylist-account [expanded?
                       current-page?
                       {store-photo :profile_picture_url
                        store-nickname :store_nickname}]
  (account-dropdown
   expanded?
   [:div.flex.justify-end.items-center
    (when store-photo [:div.mr1 (ui/circle-picture {:class "mx-auto" :width "20px"} store-photo)])
    [:div.truncate store-nickname]]
   (account-link (current-page? events/navigate-stylist-dashboard) events/navigate-stylist-dashboard-commissions "Dashboard")
   [:a.teal.block (utils/navigate-community) "Community"]
   (account-link (current-page? events/navigate-stylist-account) events/navigate-stylist-account-profile "Account Settings")))

(defn customer-account [expanded? current-page? user-email]
  (account-dropdown
   expanded?
   [:div.truncate user-email]
   (account-link (current-page? events/navigate-account-manage) events/navigate-account-manage "Account Settings")
   (account-link (current-page? events/navigate-account-referrals) events/navigate-account-referrals "Refer a Friend")))

(def guest-account
  (component/html
   [:div.right-align.h6
    [:a.inline-block.dark-gray (utils/route-to events/navigate-sign-in) "Sign In"]
    [:div.inline-block.pxp4.dark-gray "|"]
    [:a.inline-block.dark-gray (utils/route-to events/navigate-sign-up) "Sign Up"]]))

(defn row
  ([right] (row nil right))
  ([left right]
   [:div.clearfix.pyp1
    [:div.col.col-2 [:div.px1 (or left ui/nbsp)]]
    [:div.col.col-10.line-height-3 right]]))

(defn products-section [current-page? title named-searches]
  [:nav {:role "navigation" :aria-label (str "Shop " title)}
   (row [:div.border-bottom.border-dark-silver.dark-gray.h4 title])
   [:ul.my1.list-reset
    (for [{:keys [name slug]} named-searches]
      [:li {:key slug}
       [:a.h5 (utils/route-to events/navigate-category {:named-search-slug slug})
        (row
         (when (named-searches/new-named-search? slug) ui/new-flag)
         [:span.teal.titleize
          (when (current-page? events/navigate-category {:named-search-slug slug})
            {:class padded-selected-link})
          name])]])]])

(defn shop-panel [stylist? expanded? current-page? named-searches]
  [:div.absolute.col-12.bg-light-silver.to-lg-hide.z1.top-lit
   (when-not expanded? {:class "hide"})
   [:div.flex.items-start {:style {:padding "1em 10% 2em"}}
    [:div.col-4 (products-section current-page? "Hair Extensions" (filter named-searches/is-extension? named-searches))]
    [:div.col-4 (products-section current-page? "Closures" (filter named-searches/is-closure-or-frontal? named-searches))]
    (when stylist?
      [:div.col-4 (products-section current-page? "Stylist Products" (filter named-searches/is-stylist-product? named-searches))])]])

(defn desktop-nav-link-options [current-page? nav-event]
  (merge
   {:on-mouse-enter (utils/collapse-menus-callback keypaths/header-menus)}
   (when (current-page? nav-event) {:class selected-link})
   (utils/route-to nav-event)))

(defn lower-left-desktop-nav [current-page?]
  [:div.to-lg-hide {:style {:margin-top "-12px"}}
   [:div.right.h5.light
    [:a.dark-gray.col.py1 (merge
                       {:href           "/categories"
                        :on-mouse-enter (utils/expand-menu-callback keypaths/shop-menu-expanded)
                        :on-click       (utils/expand-menu-callback keypaths/shop-menu-expanded)}
                       (when (current-page? events/navigate-category) {:class selected-link}))
     "Shop"]
    [:a.dark-gray.col.py1.ml4 (desktop-nav-link-options current-page? events/navigate-shop-by-look)
     "Shop By Look"]]])

(defn lower-right-desktop-nav [current-page?]
  [:div.to-lg-hide {:style {:margin-top "-12px"}}
   [:div.h5.light
    [:a.dark-gray.col.py1.mr4 (desktop-nav-link-options current-page? events/navigate-content-guarantee)
     "Guarantee"]
    [:a.dark-gray.col.py1 {:on-mouse-enter (utils/collapse-menus-callback keypaths/header-menus)
                       :href           "https://blog.mayvenn.com"}
     "Blog"]]])

(defn component [{:keys [nav-message
                         account-expanded?
                         shop-expanded?
                         store-expanded?
                         stylist?
                         cart-quantity
                         store
                         named-searches
                         user-email]} _ _]
  (component/create
   (let [current-page? (partial app-routes/current-page? nav-message)]
     [:div.clearfix.relative.border-bottom.border-dark-silver {:on-mouse-leave (utils/collapse-menus-callback keypaths/header-menus)}
      [:div.flex.items-stretch.justify-center.bg-white.clearfix {:style {:min-height "60px"}}
       [:div.flex-auto.col-4
        [:div {:style {:height "60px"}} [:div.lg-up-hide hamburger]]
        (lower-left-desktop-nav current-page?)]
       (into [:div.flex.flex-column.justify-center.flex-auto.col-4 {:style {:min-width popup-width}}]
             (if (sans-stylist? (:store_slug store))
               (list (logo "40px"))
               (list
                (logo "30px")
                (store-dropdown store-expanded? store))))
       [:div.flex-auto.col-4
        [:div.flex.justify-end.items-center
         [:div.flex-auto.to-lg-hide.pr2
          (cond
            stylist?   (stylist-account account-expanded? current-page? store)
            user-email (customer-account account-expanded? current-page? user-email)
            :else      guest-account)]
         (shopping-bag cart-quantity)]
        (lower-right-desktop-nav current-page?)]]
      (shop-panel stylist? shop-expanded? current-page? named-searches)])))

(defn query [data]
  {:store             (get-in data keypaths/store)
   :store-expanded?   (get-in data keypaths/store-info-expanded)
   :account-expanded? (get-in data keypaths/account-menu-expanded)
   :shop-expanded?    (get-in data keypaths/shop-menu-expanded)
   :cart-quantity     (orders/product-quantity (get-in data keypaths/order))
   :stylist?          (own-store? data)
   :nav-message       (get-in data keypaths/navigation-message)
   :user-email        (get-in data keypaths/user-email)
   :named-searches    (named-searches/current-named-searches data)})

(defn built-component [data opts]
  (component/build component (query data) nil))
