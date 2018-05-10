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

(def blog-url "https://blog.mayvenn.com")

(def mobile-hamburger
  (component/html
   [:a.block.px3.py4.flex.items-center
    (assoc (utils/fake-href events/control-menu-expand-hamburger
                            {:keypath keypaths/menu-expanded})
           :data-test "hamburger")
    svg/open-hamburger-menu]))

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
  [:div.h6.flex.items-center
   (case (marquee/portrait-status (-> signed-in ::auth/as (= :stylist)) portrait)
     ::marquee/show-what-we-have [:div.pr2 (marquee/stylist-portrait portrait)]
     ::marquee/ask-for-portrait  [:div.mt1.pr2 marquee/add-portrait-cta]
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
  [:div.dark-gray.h6
   [:a.inherit-color (utils/route-to events/navigate-sign-in) "Sign in"]
   " | "
   [:a.inherit-color (utils/route-to events/navigate-sign-up) "Sign up"]])

(defn ^:private menu-row
  [{:keys [link-attrs data-test content]}]
  [:li {:key data-test}
   [:div.h5.p2.medium
    (into [:a.block.inherit-color.flex.items-center (assoc link-attrs :data-test data-test)] content)]])

(defn drop-down [expanded? menu-keypath [link-tag & link-contents] menu]
  [:div
   (into [link-tag
          (if expanded?
            (utils/fake-href events/control-menu-collapse-all {})
            (utils/fake-href events/control-menu-expand {:keypath menu-keypath}))]
         link-contents)
   (when expanded?
     [:div.relative.z4
      menu])])

(defn root-menu [deals? signed-in]
  [:ul.list-reset.mx2
   (when deals?
     (menu-row slideout-nav/deal-row))
   (for [row (slideout-nav/shopping-rows (ui/forward-caret {:width  "16px"
                                                            :height "13px"}))]
     (menu-row row))
   (when (-> signed-in ::auth/as (= :stylist))
     (menu-row slideout-nav/stylist-exclusive-row))
   (for [row slideout-nav/content-rows]
     (menu-row row))])

(defn non-mobile-hamburger [open?]
  [:div.flex.items-center
   (merge {:style {:width "30px"}
           :data-test "hamburger"})
   (if open?
     svg/close-hamburger-menu
     svg/open-hamburger-menu)])

(defn flyout-menu [{:keys [expanded-flyout-menu? deals? on-taxon? signed-in menu-data]}]
  (drop-down
   expanded-flyout-menu?
   keypaths/shop-menu-expanded
   [:div (non-mobile-hamburger expanded-flyout-menu?)]
   [:div.bg-white.absolute.left-0.pb2.z4
    {:style {:top "17px" :width "245px"}}
    (if on-taxon?
      (component/build menu/new-flyout-submenu-component menu-data nil)
      (root-menu deals? signed-in))]))

(def open-shopping (utils/expand-menu-callback keypaths/shop-menu-expanded))
(def close-shopping (utils/collapse-menus-callback keypaths/header-menus))

(defn header-menu-link [opts text]
  [:a.h5.medium.inherit-color.py2
   (merge opts {:style {:padding-left "24px" :padding-right "24px"}})
   text])

(defn component [{:keys [store user cart signed-in the-ville?] :as data} _ _]
  (component/create
   [:div.border-bottom.border-gray
    [:div.mx-auto.col-11.hide-on-mb.flex.items-center.justify-between {:style {:height "85px"}}

     [:div.flex.items-center.mt4.col-4
      [:div.mr8 (flyout-menu data)]
      (store-info signed-in store)]

     [:div.col-4
      [:a (utils/route-to events/navigate-home)
       [:div.bg-no-repeat.bg-center.img-logo.mb1
        {:style {:height "60px"}}]]]

     [:div.h6.flex.justify-end.items-center.col-4
      [:div.mt4 (account-info signed-in user the-ville?)]
      [:div.mt3.pl6 (ui/shopping-bag-flyout {:style     {:height (str ui/header-image-size "px")
                                                         :width  "35px"}
                                             :data-test "desktop-cart"}
                                     cart)]]]

    [:div.hide-on-tb-dt.flex.items-center
     mobile-hamburger
     (slideout-nav/logo-and-bag cart)]]))

(def minimal-component
  (component/html
   [:div.border-bottom.border-gray.flex.items-center
    [:div.flex-auto.py3 (ui/clickable-logo {:event events/navigate-home
                                            :data-test "header-logo"
                                            :height "40px"})]]))

(defn query [data]
  (-> (slideout-nav/query data)
      (assoc-in [:expanded-flyout-menu?] (get-in data keypaths/shop-menu-expanded))
      (assoc-in [:user :expanded?]       (get-in data keypaths/account-menu-expanded))
      (assoc-in [:cart :quantity]        (orders/product-quantity (get-in data keypaths/order)))))

(defn built-component [data opts]
  (if (nav/minimal-events (get-in data keypaths/navigation-event))
    minimal-component
    (component/build component (query data) nil)))
