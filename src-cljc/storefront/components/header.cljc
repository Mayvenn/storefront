(ns storefront.components.header
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
            [clojure.string :as str]))

(def hamburger
  (component/html
   [:a.block.px3.py4 (assoc (utils/fake-href events/control-menu-expand-hamburger
                                             {:keypath keypaths/menu-expanded})
                            :style {:width "70px"}
                            :data-test "hamburger")
    [:div.border-top.border-bottom.border-dark-gray {:style {:height "15px"}} [:span.hide "MENU"]]
    [:div.border-bottom.border-dark-gray {:style {:height "15px"}}]]))

(defn shopping-bag [opts {:keys [quantity]}]
  [:a.relative.pointer.block (merge (utils/route-to events/navigate-cart)
                                    opts)
   (svg/bag {:class (str "absolute overlay m-auto "
                         (if (pos? quantity) "fill-navy" "fill-black"))})
   (when (pos? quantity)
     [:div.absolute.overlay.m-auto {:style {:height "9px"}}
      [:div.center.navy.h6.line-height-1 {:data-test (-> opts :data-test (str  "-populated"))} quantity]])])

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
         [:div.bg-white.absolute.left-0
          (for [[idx row] (map-indexed vector rows)]
            [:div.border-gray {:key   idx
                               :class (when-not (zero? idx) "border-top")} row])])))))

(defmulti account-info (fn [signed-in _] (::auth/as signed-in)))

(defmethod account-info :user [_ {:keys [email expanded?]}]
  (ui/drop-down
   expanded?
   keypaths/account-menu-expanded
   [:a.inherit-color.h6
    "Signed in with: " [:span.teal email]
    " | Manage account" [:span.ml1 (ui/expand-icon expanded?)]]
   [:div.bg-white.absolute.right-0
    [:div
     (drop-down-row (utils/route-to events/navigate-account-manage) "Manage account")]
    [:div.border-top.border-gray
     (drop-down-row (utils/route-to events/navigate-account-referrals) "Refer a friend")]
    [:div.border-top.border-gray
     (drop-down-row (utils/fake-href events/control-sign-out) "Sign out")]]))

(defmethod account-info :stylist [_ {:keys [email expanded?]}]
  (ui/drop-down
   expanded?
   keypaths/account-menu-expanded
   [:a.inherit-color.h6
    "Signed in with: " [:span.teal email]
    " | My dashboard" [:span.ml1 (ui/expand-icon expanded?)]]
   [:div.bg-white.absolute.right-0
    [:div
     (drop-down-row (utils/route-to events/navigate-stylist-dashboard-commissions) "My dashboard")]
    [:div.border-top.border-gray
     (drop-down-row (utils/route-to events/navigate-stylist-share-your-store) "Share your store")]
    [:div.border-top.border-gray
     (drop-down-row stylists/community-url "Community")]
    [:div.border-top.border-gray
     (drop-down-row (utils/route-to events/navigate-stylist-account-profile) "Account settings")]
    [:div.border-top.border-gray
     (drop-down-row (utils/fake-href events/control-sign-out) "Sign out")]]))

(defmethod account-info :guest [_ _]
  [:div.h6
   [:a.inherit-color (utils/route-to events/navigate-sign-in) "Sign in"]
   " | "
   [:a.inherit-color (utils/route-to events/navigate-sign-up) "No account? Sign up"]])

(def open-shopping (utils/expand-menu-callback keypaths/shop-menu-expanded))
(def close-shopping (utils/collapse-menus-callback keypaths/header-menus))

(defn menu-link [opts text]
  [:a.h5.medium.inherit-color.py2
   (merge opts {:style {:padding-left "24px" :padding-right "24px"}})
   text])

(defn menu [bundle-deals?]
  (component/html
   [:div.center
    (when bundle-deals?
      (menu-link (assoc (utils/route-to events/navigate-shop-bundle-deals)
                        :on-mouse-enter close-shopping)
                 "Shop bundle deals"))
    (menu-link (assoc (utils/route-to events/navigate-shop-by-look)
                      :on-mouse-enter close-shopping)
     "Shop looks")
    (menu-link (assoc (utils/route-to events/navigate-home)
                      :on-mouse-enter open-shopping
                      :on-click       open-shopping)
     "Shop hair")
    (menu-link (assoc (utils/route-to events/navigate-content-guarantee)
                      :on-mouse-enter close-shopping)
     "Our Guarantee")
    (menu-link {:href           slideout-nav/blog-url
                :on-mouse-enter close-shopping}
     "Real Beautiful")]))

(defn shopping-column [items col-count]
  {:pre [(zero? (mod 12 col-count))]}
  [:ul.list-reset.col.px2
   {:class (str "col-" (/ 12 col-count))}
   (for [{:keys [page/slug name] :as category} items]
     [:li {:key slug}
      [:a.inherit-color.block.pyp2
       (if (:direct-to-details/id category)
         (utils/route-to events/navigate-product-details
                         {:catalog/product-id (:direct-to-details/id category)
                          :page/slug          (:direct-to-details/slug category)})
         (utils/route-to events/navigate-category category))
       (when (categories/new-category? slug) [:span.teal "NEW "])
       (str/capitalize name)]])])

(defn shopping-flyout [signed-in {:keys [expanded? categories]}]
  (when expanded?
    (let [show?   (fn [category]
                    (or (auth/stylist? signed-in)
                        (not (-> category :criteria :product/department (contains? "stylist-exclusives")))))
          columns (->> (filter :header/order categories)
                       (filter show?)
                       (sort-by :header/order)
                       (group-by :header/group)
                       vals
                       (mapcat (partial partition-all 6)))]
      [:div.absolute.bg-white.col-12.z3.border-bottom.border-gray
       [:div.mx-auto.clearfix.my6
        {:style {:width "580px"}}
        (for [items columns]
          (shopping-column items (count columns)))]])))

(defn component [{:keys [store user cart shopping signed-in bundle-deals?]} _ _]
  (component/create
   [:div
    [:div.hide-on-mb.relative
     {:on-mouse-leave close-shopping}
     [:div.relative.border-bottom.border-gray {:style {:height "150px"}}
      [:div.max-960.mx-auto
       [:div.left (store-info signed-in store)]
       [:div.right
        [:div.h6.my2.flex.items-center
         (account-info signed-in user)
         [:div.pl2 (shopping-bag {:style {:height (str ui/header-image-size "px") :width "28px"}
                                  :data-test "desktop-cart"}
                                 cart)]]]
       [:div.absolute.bottom-0.left-0.right-0
        [:div.mb4 (slideout-nav/logo "desktop-header-logo" "60px")]
        [:div.mb1 (menu bundle-deals?)]]]]
     (shopping-flyout signed-in shopping)]
    [:div.hide-on-tb-dt.border-bottom.border-gray.flex.items-center
     hamburger
     [:div.flex-auto.py3 (slideout-nav/logo "header-logo" "40px")]
     (shopping-bag {:style     {:height "70px" :width "70px"}
                    :data-test "mobile-cart"}
                   cart)]]))

(def minimal-component
  (component/html
   [:div.border-bottom.border-gray.flex.items-center
    [:div.flex-auto.py3 (slideout-nav/logo "header-logo" "40px")]]))

(defn query [data]
  (-> (slideout-nav/basic-query data)
      (assoc-in [:user :expanded?]     (get-in data keypaths/account-menu-expanded))
      (assoc-in [:shopping :expanded?] (get-in data keypaths/shop-menu-expanded))
      (assoc-in [:cart :quantity]      (orders/product-quantity (get-in data keypaths/order)))))

(defn built-component [data opts]
  (if (nav/minimal-events (get-in data keypaths/navigation-event))
    minimal-component
    (component/build component (query data) nil)))
