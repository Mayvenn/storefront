(ns storefront.components.header
  (:require [storefront.accessors.named-searches :as named-searches]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.stylists :as stylists]
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
            [storefront.routes :as routes]
            [clojure.set :as set]
            [clojure.string :as str]))

(def sans-stylist? #{"store" "shop"})

(def signed-in? #{::signed-in-as-user ::signed-in-as-stylist})

(defn fake-href-menu-expand [keypath]
  (utils/fake-href events/control-menu-expand {:keypath keypath}))

(def hamburger
  (component/html
   [:a.block.px3.py4 (assoc (fake-href-menu-expand keypaths/menu-expanded)
                            :style {:width "70px"}
                            :data-test "hamburger")
    [:div.border-top.border-bottom.border-dark-gray {:style {:height "15px"}} [:span.hide "MENU"]]
    [:div.border-bottom.border-dark-gray {:style {:height "15px"}}]]))

(defn logo [data-test-value height]
  (component/html
   [:a.block.img-logo.bg-no-repeat.bg-center.bg-contain.teal
    (assoc (utils/route-to events/navigate-home)
           :style {:height height}
           :title "Mayvenn"
           :item-prop "logo"
           :data-test data-test-value
           :content (str "https:" (assets/path "/images/header_logo.svg")))]))

(defn shopping-bag [opts cart-quantity]
  [:a.relative.pointer.block (merge (utils/route-to events/navigate-cart)
                                    opts)
   (svg/bag {:class (str "absolute overlay m-auto "
                         (if (pos? cart-quantity) "fill-navy" "fill-black"))})
   (when (pos? cart-quantity)
     [:div.absolute.overlay.m-auto {:style {:height "9px"}}
      [:div.center.navy.h6.line-height-1 {:data-test (-> opts :data-test (str  "-populated"))} cart-quantity]])])

(def header-image-size 36)

(defn ^:private stylist-portrait [portrait]
  (ui/circle-picture {:class "mx-auto"
                      :width (str header-image-size "px")}
                     (ui/square-image portrait header-image-size)))

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
   {:href (str "http://instagram.com/" instagram-account)}
   "Follow on"
   (social-icon (assets/path "/images/share/instagram-icon.png"))))

(defn ^:private styleseat-link [styleseat-account]
  (drop-down-row
   {:href (str "https://www.styleseat.com/v/" styleseat-account)}
   "Book on"
   (social-icon (assets/path "/images/share/styleseat-logotype.png"))))

(defn expand-icon [expanded?]
  [:img.ml1 {:style {:width "8px"}
             :src   (if expanded?
                      (assets/path "/images/icons/collapse.png")
                      (assets/path "/images/icons/expand.png"))}])

(defn store-welcome [{:keys [store-nickname portrait expanded?]} expandable?]
  [:div.h6.flex.items-center.mt2
   (when portrait
     [:div.left.pr2
      (stylist-portrait portrait)])
   [:div.dark-gray
    "Welcome to " [:span.black.medium store-nickname "'s"] " shop"
    (when expandable?
      (expand-icon expanded?))]])

(defn store-info [{:keys [store-slug store-nickname portrait expanded? gallery? instagram-account styleseat-account] :as store}]
  (when-not (sans-stylist? store-slug)
    (let [rows (cond-> []
                 gallery?          (conj gallery-link)
                 styleseat-account (conj (styleseat-link styleseat-account))
                 instagram-account (conj (instagram-link instagram-account)))]
      (if-not (boolean (seq rows))
        (store-welcome store false)
        (ui/drop-down
         expanded?
         keypaths/store-info-expanded
         [:a (store-welcome store true)]
         [:div.bg-white.absolute.left-0
          (for [[idx row] (map-indexed vector rows)]
            [:div.border-gray {:key   idx
                               :class (when-not (zero? idx) "border-top")} row])])))))

(defmulti account-info :signed-in-state)

(defmethod account-info ::signed-in-as-user [{:keys [email expanded?]}]
  (ui/drop-down
   expanded?
   keypaths/account-menu-expanded
   [:a.inherit-color.h6
    "Signed in with: " [:span.teal email] " | "
    "Manage account" (expand-icon expanded?)]
   [:div.bg-white.absolute.right-0
    [:div
     (drop-down-row (utils/route-to events/navigate-account-manage) "Manage account")]
    [:div.border-top.border-gray
     (drop-down-row (utils/route-to events/navigate-account-referrals) "Refer a friend")]
    [:div.border-top.border-gray
     (drop-down-row (utils/fake-href events/control-sign-out) "Sign out")]]))

(defmethod account-info ::signed-in-as-stylist [{:keys [email expanded?]}]
  (ui/drop-down
   expanded?
   keypaths/account-menu-expanded
   [:a.inherit-color.h6
    "Signed in with: " [:span.teal email] " | "
    "My dashboard" (expand-icon expanded?)]
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

(defmethod account-info ::signed-out [_]
  [:div.h6
   [:a.inherit-color (utils/route-to events/navigate-sign-in)
    "Sign in"]
   " | No account? "
   [:a.inherit-color (utils/route-to events/navigate-sign-up)
    "Sign up"]])

(defn menu-link [opts text]
  [:a.h5.medium.inherit-color.py2
   (merge opts {:style {:padding-left "24px" :padding-right "24px"}})
   text])

(def menu
  (component/html
   [:div.center
    (menu-link (assoc (utils/route-to events/navigate-shop-by-look)
                      :on-mouse-enter (utils/collapse-menus-callback keypaths/header-menus))
     "Shop looks")
    (menu-link (assoc (utils/route-to events/navigate-categories)
                      :on-mouse-enter (utils/expand-menu-callback keypaths/shop-menu-expanded)
                      :on-click       (utils/expand-menu-callback keypaths/shop-menu-expanded))
     "Shop hair")
    (menu-link (assoc (utils/route-to events/navigate-content-guarantee)
                      :on-mouse-enter (utils/collapse-menus-callback keypaths/header-menus))
     "Our Guarantee")
    (menu-link {:on-mouse-enter (utils/collapse-menus-callback keypaths/header-menus)
                :href           "https://blog.mayvenn.com"}
     "Real Beauty")]))

(defn shop-section [{:keys [expanded? sections] :as shop}]
  (when expanded?
    [:div.absolute.bg-white.col-12.z3.border-bottom.border-gray
     [:ul.list-reset.clearfix.max-960.center.mx-auto.my6
      (for [{:keys [title shop-items]} sections]
        [:li.align-top.left-align.inline-block.col-4.px2 {:key title}
         [:div.mb6.medium title]
         [:ul.list-reset
          (for [{:keys [slug name]} shop-items]
            [:li {:key slug}
             [:a.inherit-color.block.pyp2 (utils/route-to events/navigate-category {:named-search-slug slug})
              (when (named-searches/new-named-search? slug) [:span.teal "NEW "])
              (str/capitalize name)]])]])]]))

(defn component [{:keys [store user cart shop]} _ _]
  (component/create
   [:div
    [:div.hide-on-mb.relative
     {:on-mouse-leave (utils/collapse-menus-callback keypaths/header-menus)}
     [:div.relative.border-bottom.border-gray {:style {:height "150px"}}
      [:div.max-960.mx-auto
       [:div.left (store-info store)]
       [:div.right
        [:div.h6.my2.flex.items-center
         (account-info user)
         [:div.pl2 (shopping-bag {:style {:height (str header-image-size "px") :width "28px"}
                                  :data-test "desktop-cart"}
                                 (:cart-quantity cart))]]]
       [:div.absolute.bottom-0.left-0.right-0
        [:div.mb4 (logo "desktop-header-logo" "60px")]
        [:div.mb1 menu]]]]
     (shop-section shop)]
    [:div.hide-on-tb-dt.border-bottom.border-gray.flex.items-center
     hamburger
     [:div.flex-auto.py3 (logo "mobile-header-logo" "40px")]
     (shopping-bag {:style     {:height "70px" :width "70px"}
                    :data-test "mobile-cart"}
                   (:cart-quantity cart))]]))

(defn minimal-component [_ _ _]
  (component/create
   [:div.border-bottom.border-gray.flex.items-center
    [:div.flex-auto.py3 (logo "minimal-header-logo" "40px")]]))

(defn signed-in-state [data]
  (if (stylists/own-store? data)
    ::signed-in-as-stylist
    (if (get-in data keypaths/user-email)
      ::signed-in-as-user
      ::signed-out)))

(defn query [data]
  ;; TODO: these are very similar to slideout nav queries... unify them somehow?
  (let [named-searches  (named-searches/current-named-searches data)
        signed-in-state (signed-in-state data)]
    {:store (-> (get-in data keypaths/store)
                (set/rename-keys {:store_slug        :store-slug
                                  :store_nickname    :store-nickname
                                  :instagram_account :instagram-account
                                  :styleseat_account :styleseat-account})
                (assoc :gallery? (stylists/gallery? data)
                       :expanded? (get-in data keypaths/store-info-expanded)))
     :user  {:expanded?       (get-in data keypaths/account-menu-expanded)
             :email           (get-in data keypaths/user-email)
             :signed-in-state signed-in-state}
     :cart  {:cart-quantity (orders/product-quantity (get-in data keypaths/order))}
     :shop  {:expanded? (get-in data keypaths/shop-menu-expanded)
             :sections  (cond-> [{:title "Shop hair"
                                  :shop-items (filter named-searches/is-extension? named-searches)}
                                 {:title "Shop closures & frontals"
                                  :shop-items (filter named-searches/is-closure-or-frontal? named-searches)}]
                          (= ::signed-in-as-stylist signed-in-state)
                          (conj {:title      "Stylist exclusives"
                                 :shop-items (filter named-searches/is-stylist-product? named-searches)}))}}))

(defn built-component [data opts]
  (if (nav/minimal-events (get-in data keypaths/navigation-event))
    (component/build minimal-component {} nil)
    (component/build component (query data) nil)))
