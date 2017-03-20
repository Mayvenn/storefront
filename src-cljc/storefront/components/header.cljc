(ns storefront.components.header
  (:require [storefront.accessors.named-searches :as named-searches]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.stylists :refer [community-url own-store?]]
            [storefront.accessors.nav :as nav]
            [storefront.accessors.experiments :as experiments]
            [storefront.assets :as assets]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.routes :as routes]))

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
            :data-test "header-logo"
            :content (str "https:" (assets/path "/images/header_logo.svg"))}
           (utils/route-to events/navigate-home))]))

(defn shopping-bag [cart-quantity]
  [:a.relative.pointer.block.mtp1 (merge {:style {:height "60px" :width "60px"}}
                                         {:data-test "cart"}
                                         (utils/route-to events/navigate-cart))
   (svg/bag {:class (str "absolute overlay m-auto "
                         (if (pos? cart-quantity) "fill-navy" "fill-black"))})
   (when (pos? cart-quantity)
     [:div.absolute.overlay.m-auto {:style {:height "9px"}}
      [:div.center.navy.h6.line-height-1 {:data-test "populated-cart"} cart-quantity]])])

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
   (carrot-down {:width-px 4 :bg-color "border-white" :border-color "border-navy"})))

(def notch-up
  (component/html
   (carrot-top {:width-px 5 :bg-color "border-white" :border-color "border-gray"})))

(def selected-link        "border-navy border-bottom border-width-2")
(def padded-selected-link "border-navy border-bottom border-width-2 pyp3")

;; Sharing this width ensure the popup is centered on mobile
(def popup-width "188px")

(defn store-dropdown-link [link-attrs title last?]
  [:a.h5.navy.block.p1.border-top.border-gray.bg-light-gray
   (merge link-attrs (when last?
                       {:class "rounded-bottom-1"}))
   [:div.pp2.center title]])

(defn store-dropdown [expanded?
                      gallery?
                      {store-name :store_name
                       nickname :store_nickname
                       instagram-account :instagram_account
                       styleseat-account :styleseat_account
                       portrait :portrait}]
  [:div.center {:height "30px"}
   (ui/drop-down
    expanded?
    keypaths/store-info-expanded
    [:a
     [:div {:style {:margin-bottom "6px"}}
      [:div.flex.justify-center.items-center
       [:span.line-height-1.dark-gray.nowrap.mrp3.h7 "HAIR BY"]
       [:div.truncate.fit.navy.h5 {:data-test "nickname"} nickname]]
      [:div.relative navy-carrot-bottom]]]
    [:div.absolute.left-0.right-0.mx-auto {:style {:width popup-width}}
     [:div.relative.border.border-gray.rounded-1.bg-white.top-lit
      notch-up
      [:div.dark-gray
       [:div.p1.h6
        [:div.m1 (ui/circle-picture
                  {:class "mx-auto"}
                  (ui/square-image portrait 48))]
        [:h4.regular store-name]]
       (when instagram-account
         (store-dropdown-link
          {:href (str "http://instagram.com/" instagram-account)}
          "Follow me on Instagram"
          (not (or styleseat-account
                   gallery?))))
       (when styleseat-account
         (store-dropdown-link
          {:href  (str "https://www.styleseat.com/v/" styleseat-account)}
          "Book me on StyleSeat"
          (not gallery?)))
       (when gallery?
         (store-dropdown-link
          (utils/route-to events/navigate-gallery)
          "See my gallery"
          true))]]])])

(defn account-dropdown [expanded? link & menu]
  (ui/drop-down
   expanded?
   keypaths/account-menu-expanded
   [:a.flex.items-center
    [:div.dark-gray.flex-auto.right-align.h5 link]
    [:div.relative.ml1.mtn1 {:style {:height "4px"}} navy-carrot-bottom]]
   [:div.absolute.right-0 {:style {:max-width "140px"}}
    [:div.relative.border.border-gray.rounded-1.bg-white.top-lit {:style {:margin-right "-1em" :top "5px"}}
     [:div.absolute {:style {:right "15px"}} notch-up]
     [:div.h5.bg-white.rounded-1
      (into [:div.px2.py1] menu)
      [:div.border-bottom.border-gray]
      [:a.navy.block.py1.center.bg-light-gray.rounded-bottom-1
       (utils/fake-href events/control-sign-out) "Logout"]]]]))

(defn account-link [current-page? nav-event title]
  [:a.py1.teal.block (utils/route-to nav-event)
   [:span (when current-page? {:class padded-selected-link}) title]])

(defn stylist-account [expanded?
                       current-page?
                       {:keys [portrait store_nickname]}]
  (account-dropdown
   expanded?
   [:div.flex.justify-end.items-center
    (when (:resizable_url portrait)
      [:div.mr1 (ui/circle-picture {:class "mx-auto" :width "20px"}
                                   (ui/square-image portrait 48))])
    [:div.truncate store_nickname]]
   (account-link (current-page? [events/navigate-stylist-dashboard]) events/navigate-stylist-dashboard-commissions "Dashboard")
   [:a.teal.block community-url "Community"]
   (account-link (current-page? [events/navigate-stylist-account]) events/navigate-stylist-account-profile "Account Settings")))

(defn customer-account [expanded? current-page? user-email]
  (account-dropdown
   expanded?
   [:div.truncate user-email]
   (account-link (current-page? [events/navigate-account-manage]) events/navigate-account-manage "Account Settings")
   (account-link (current-page? [events/navigate-account-referrals]) events/navigate-account-referrals "Refer a Friend")))

(def guest-account
  (component/html
   [:div.right-align.h5
    [:a.inline-block.dark-gray (utils/route-to events/navigate-sign-in) "Sign In"]
    [:div.inline-block.pxp4.dark-gray "|"]
    [:a.inline-block.dark-gray (utils/route-to events/navigate-sign-up) "Sign Up"]]))

(defn row
  ([right] (row nil right))
  ([left right]
   [:div.flex.items-center.pyp1
    [:div.col-2 [:div.px1 (or left ui/nbsp)]]
    [:div.col-10.pyp1 right]]))

(defn products-section [current-page? title named-searches]
  [:nav {:aria-label (str "Shop " title)}
   (row [:div.border-bottom.border-gray.dark-gray title])
   [:ul.my1.list-reset
    (for [{:keys [name slug]} named-searches]
      [:li {:key slug}
       [:a.h5.medium (utils/route-to events/navigate-category {:named-search-slug slug})
        (row
         (when (named-searches/new-named-search? slug) ui/new-flag)
         [:span.teal.titleize
          (when (current-page? [events/navigate-category {:named-search-slug slug}])
            {:class padded-selected-link})
          name])]])]])

(defn shop-panel [stylist? expanded? current-page? named-searches]
  [:div.absolute.col-12.bg-white.hide-on-mb.z4.top-lit
   (when-not expanded? {:class "hide"})
   [:div.flex.items-start {:style {:padding "1em 10% 2em"}}
    [:div.col-4 (products-section current-page? "Hair Extensions" (filter named-searches/is-extension? named-searches))]
    [:div.col-4 (products-section current-page? "Closures" (filter named-searches/is-closure-or-frontal? named-searches))]
    (when stylist?
      [:div.col-4 (products-section current-page? "Stylist Products" (filter named-searches/is-stylist-product? named-searches))])]])

(defn nav-link-options [current-page? nav-event]
  (merge
   {:on-mouse-enter (utils/collapse-menus-callback keypaths/header-menus)}
   (when (current-page? [nav-event]) {:class selected-link})
   (utils/route-to nav-event)))

(def upper-left
  [:div {:style {:height "60px"}} [:div.hide-on-tb-dt hamburger]])

(defn upper-right [{:keys [store auth cart context]}]
  (let [{:keys [expanded? user-email]}   auth
        {:keys [cart-quantity]}          cart
        {:keys [store]}                  store
        {:keys [stylist? current-page?]} context]
    [:div.flex.justify-end.items-center
     [:div.flex-auto.hide-on-mb.pr2
      (cond
        stylist?   (stylist-account expanded? current-page? store)
        user-email (customer-account expanded? current-page? user-email)
        :else      guest-account)]
     (shopping-bag cart-quantity)]))

(defn lower-left [current-page?]
  [:div.right
   [:div.h4.hide-on-mb {:style {:margin-top "-12px"}}
    [:a.black.col.py1.mr4 (merge
                           {:href           "/categories"
                            :on-mouse-enter (utils/expand-menu-callback keypaths/shop-menu-expanded)
                            :on-click       (utils/expand-menu-callback keypaths/shop-menu-expanded)}
                           (when (current-page? [events/navigate-category]) {:class selected-link}))
     "Shop"]
    [:a.black.col.py1.ml4 (nav-link-options current-page? events/navigate-shop-by-look)
     "Shop By Look"]]])

(defn lower-right [current-page?]
  [:div.h4.hide-on-mb {:style {:margin-top "-12px"}}
   [:a.black.col.py1.mr4 (nav-link-options current-page? events/navigate-content-guarantee)
    "Guarantee"]
   [:a.black.col.py1.ml4 {:on-mouse-enter (utils/collapse-menus-callback keypaths/header-menus)
                          :href           "https://blog.mayvenn.com"}
    "Blog"]])

(defn header [left middle right flyout]
  [:div.clearfix.relative.border-bottom.border-gray {:on-mouse-leave (utils/collapse-menus-callback keypaths/header-menus)}
   [:div.flex.items-stretch.justify-center.bg-white.clearfix {:style {:min-height "60px"}}
    (into [:div.flex-auto.col-4] left)
    (into [:div.flex-auto.col-4.flex.flex-column.justify-center {:style {:min-width popup-width}}] middle)
    (into [:div.flex-auto.col-4] right)]
   flyout])

(defn left [{:keys [current-page?]}]
  [upper-left
   (lower-left current-page?)])

(defn middle [{:keys [store expanded? gallery?]}]
  (if (sans-stylist? (:store_slug store))
    (list (logo "40px"))
    (list
     (logo "30px")
     (store-dropdown expanded? gallery? store))))

(defn right [{{:keys [current-page?]} :context :as data}]
  [(upper-right data)
   (lower-right current-page?)])

(defn flyout [{:keys [shop context]}]
  (let [{:keys [expanded? named-searches]} shop
        {:keys [stylist? current-page?]}   context]
    (shop-panel stylist? expanded? current-page? named-searches)))

(defn component [data _ _]
  (component/create
   (let [data (assoc-in data [:context :current-page?] (partial routes/sub-page? (-> data :context :nav-message)))]
     (header
      (left (:context data))
      (middle (:store data))
      (right (select-keys data [:store :auth :cart :context]))
      (flyout (select-keys data [:shop :context]))))))

(defn minimal-component [store _ _]
  (component/create
   (header
    [[:div.hide-on-mb {:style {:min-height "80px"}} ui/nbsp]]
    (middle store)
    nil
    nil)))

(defn store-query [data]
  {:expanded? (get-in data keypaths/store-info-expanded)
   :gallery?  (experiments/gallery? data)
   :store     (get-in data keypaths/store)})

(defn auth-query [data]
  {:expanded?  (get-in data keypaths/account-menu-expanded)
   :user-email (get-in data keypaths/user-email)})

(defn cart-query [data]
  {:cart-quantity (orders/product-quantity (get-in data keypaths/order))})

(defn shop-query [data]
  {:expanded?      (get-in data keypaths/shop-menu-expanded)
   :named-searches (named-searches/current-named-searches data)})

(defn context-query [data]
  {:stylist?    (own-store? data)
   :nav-message (get-in data keypaths/navigation-message)})

(defn query [data]
  {:store   (store-query data)
   :auth    (auth-query data)
   :cart    (cart-query data)
   :shop    (shop-query data)
   :context (context-query data)})

(defn built-component [data opts]
  (if (nav/minimal-events (get-in data keypaths/navigation-event))
    (component/build minimal-component (store-query data) nil)
    (component/build component (query data) nil)))
