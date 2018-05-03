(ns storefront.components.header-new-flyout
  (:require [catalog.categories :as categories]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.stylists :as stylists]
            [storefront.accessors.nav :as nav]
            [storefront.accessors.auth :as auth]
            [storefront.assets :as assets]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.components.slideout-nav :as slideout-nav]
            [storefront.components.marquee :as marquee]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [clojure.string :as string]
            [storefront.accessors.experiments :as experiments]
            [catalog.menu :as menu]))

(def non-mobile-hamburger
  (component/html
   [:div.left.block.mr6.py4 {:style {:width "25px"}
                             :data-test "hamburger"}
    [:div.border-top.border-bottom.border-dark-gray.border-width-2 {:style {:height "9px"}} [:span.hide "MENU"]]
    [:div.border-bottom.border-dark-gray.border-width-2 {:style {:height "7px"}}]]))

(def mobile-hamburger
  (component/html
   [:a.block.px3.py4 (assoc (utils/fake-href events/control-menu-expand-hamburger
                                             {:keypath keypaths/menu-expanded})
                            :style {:width "70px"}
                            :data-test "hamburger")
    [:div.border-top.border-bottom.border-dark-gray {:style {:height "15px"}} [:span.hide "MENU"]]
    [:div.border-bottom.border-dark-gray {:style {:height "15px"}}]]))

(defn drop-down-row [opts & content]
  (into [:a.inherit-color.block.center.h5.flex.items-center.justify-center
         (-> opts
             (assoc-in [:style :min-width] "200px")
             (assoc-in [:style :height] "39px"))]
        content))

(defn social-icon [path]
  [:img.ml2 {:style {:height "20px"}
             :src   path}] )

(def ^:private gallery-link
  (component/html
   (drop-down-row
    (utils/route-to events/navigate-gallery)
    "View gallery"
    (social-icon (assets/path "/images/share/stylist-gallery-icon.png")))))

(defn ^:private instagram-link [instagram-account]
  (drop-down-row
   {:href (marquee/instagram-url instagram-account)}
   "Follow on"
   (social-icon (assets/path "/images/share/instagram-icon.png"))))

(defn ^:private styleseat-link [styleseat-account]
  (drop-down-row
   {:href (marquee/styleseat-url styleseat-account)}
   "Book on"
   (social-icon (assets/path "/images/share/styleseat-logotype.png"))))

(defn store-welcome [signed-in {:keys [store-nickname portrait expanded?]} expandable?]
  [:div.h6.flex.items-center.mt2
   (case (marquee/portrait-status (-> signed-in ::auth/as (= :stylist)) portrait)
     ::marquee/show-what-we-have [:div.left.pr2 (marquee/stylist-portrait portrait)]
     ::marquee/ask-for-portrait  [:div.left.pr2 marquee/add-portrait-cta]
     ::marquee/show-nothing      [:div.left {:style {:height (str ui/header-image-size "px")}}])
   [:div.dark-gray
    "Welcome to " [:span.black.medium {:data-test "nickname"} store-nickname "'s"] " shop"
    (when expandable?
      [:span.ml1 (ui/expand-icon expanded?)])]])

(defn store-info [signed-in {:keys [expanded?] :as store}]
  (when (-> signed-in ::auth/to (= :marketplace))
    (let [rows (marquee/actions store gallery-link instagram-link styleseat-link)]
      (if-not (boolean (seq rows))
        (store-welcome signed-in store false)
        (ui/drop-down
         expanded?
         keypaths/store-info-expanded
         [:div (store-welcome signed-in store true)]
         [:div.bg-white.absolute.mt1.top-lit
          {:style {:left "50px"}}
          (for [[idx row] (map-indexed vector rows)]
            [:div.border-gray {:key   idx
                               :class (when-not (zero? idx) "border-top")} row])])))))

(defmulti account-info (fn [signed-in _ _] (::auth/as signed-in)))

(defmethod account-info :user [_ {:keys [email expanded?]} the-ville?]
  (ui/drop-down
   expanded?
   keypaths/account-menu-expanded
   [:a.inherit-color.h6
    "Account settings" [:span.ml1 (ui/expand-icon expanded?)]]
   [:div.bg-white.absolute.left-0.top-lit
    [:div
     (drop-down-row (utils/route-to events/navigate-account-manage) "Account settings")]
    [:div.border-top.border-gray
     (drop-down-row (if the-ville?
                      (utils/route-to events/navigate-friend-referrals-freeinstall)
                      (utils/route-to events/navigate-account-referrals)) "Refer a friend")]
    [:div.border-top.border-gray
     (drop-down-row (utils/fake-href events/control-sign-out) "Sign out")]]))

(defmethod account-info :stylist [_ {:keys [email expanded?]} _]
  (ui/drop-down
   expanded?
   keypaths/account-menu-expanded
   [:a.inherit-color.h6
    "My dashboard" [:span.ml1 (ui/expand-icon expanded?)]]
   [:div.bg-white.absolute.right-0.mt3.top-lit
    [:div
     (drop-down-row (utils/route-to events/navigate-stylist-dashboard-earnings) "My dashboard")]
    [:div.border-top.border-gray
     (drop-down-row (utils/route-to events/navigate-stylist-share-your-store) "Share your store")]
    [:div.border-top.border-gray
     (drop-down-row stylists/community-url "Community")]
    [:div.border-top.border-gray
     (drop-down-row (utils/route-to events/navigate-stylist-account-profile) "Account settings")]
    [:div.border-top.border-gray
     (drop-down-row (utils/fake-href events/control-sign-out) "Sign out")]]))

(defmethod account-info :guest [_ _ _]
  [:div.h6
   [:a.inherit-color (utils/route-to events/navigate-sign-in) "Sign in"]
   " | "
   [:a.inherit-color (utils/route-to events/navigate-sign-up) "No account? Sign up"]])

(defn flyout-menu [expanded?]
  (ui/drop-down
   expanded?
   keypaths/shop-menu-expanded
   [:div non-mobile-hamburger]
   [:div.bg-white.absolute.left-0.top-lit
    {:style {:top "50px"}}
    (for [[text & msg] [["Deals" events/navigate-shop-by-look {:album-slug "deals"}]
                        ["Shop Looks" events/navigate-shop-by-look {:album-slug "look"}]
                        ["Shop hair" events/navigate-home]
                        ["Shop Guarantee" events/navigate-content-guarantee]
                        ["Our hair" events/navigate-content-our-hair]
                        ["Our Real Beautiful" slideout-nav/blog-url]]]
      [:div
       (drop-down-row (apply utils/route-to msg) text)])]))

(def open-shopping (utils/expand-menu-callback keypaths/shop-menu-expanded))
(def close-shopping (utils/collapse-menus-callback keypaths/header-menus))

(defn header-menu-link [opts text]
  [:a.h5.medium.inherit-color.py2
   (merge opts {:style {:padding-left "24px" :padding-right "24px"}})
   text])

(defn component [{:keys [store user cart shopping signed-in deals? the-ville? expanded-flyout-menu?]} _ _]
  (component/create
   [:div
    [:div.hide-on-mb
     [:div.relative.border-bottom.border-gray.pt3 {:style {:height "85px"}}
      [:div.absolute.bottom-0.left-0.right-0
       [:div.mb2
        (ui/clickable-logo {:event events/navigate-home
                            :data-test "desktop-header-logo"
                            :height "60px"})]]
      [:div.max-960.mx-auto.pt2.relative
       [:div.left.col-5
        (flyout-menu expanded-flyout-menu?)
        [:div.mr4.pr2 (store-info signed-in store)]]
       [:div.right.col-4
        [:div.h6.my2.flex.items-center.right
         [:div.mt1 (account-info signed-in user the-ville?)]
         [:div.pl6 (ui/shopping-bag {:style {:height (str ui/header-image-size "px") :width "28px"}
                                     :data-test "desktop-cart"}
                                    cart)]]]]]]
    [:div.hide-on-tb-dt.border-bottom.border-gray.flex.items-center
     mobile-hamburger
     [:div.flex-auto.py3 (ui/clickable-logo {:event events/navigate-home
                                             :data-test "header-logo"
                                             :height "40px"})]
     (ui/shopping-bag {:style     {:height "70px" :width "70px"}
                       :data-test "mobile-cart"}
                      cart)]]))

(def minimal-component
  (component/html
   [:div.border-bottom.border-gray.flex.items-center
    [:div.flex-auto.py3 (ui/clickable-logo {:event events/navigate-home
                                            :data-test "header-logo"
                                            :height "40px"})]]))

(defn query [data]
  (-> (slideout-nav/basic-query data)
      (assoc-in [:expanded-flyout-menu?] (get-in data keypaths/shop-menu-expanded))
      (assoc-in [:user :expanded?]       (get-in data keypaths/account-menu-expanded))
      (assoc-in [:shopping :expanded?]   (get-in data keypaths/shop-menu-expanded))
      (assoc-in [:cart :quantity]        (orders/product-quantity (get-in data keypaths/order)))))

(defn built-component [data opts]
  (if (nav/minimal-events (get-in data keypaths/navigation-event))
    minimal-component
    (component/build component (query data) nil)))
