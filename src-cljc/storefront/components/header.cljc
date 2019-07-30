(ns storefront.components.header
  (:require [clojure.string :as string]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.nav :as nav]
            [storefront.accessors.orders :as orders]
            [storefront.community :as community]
            [storefront.assets :as assets]
            [storefront.component :as component]
            [storefront.components.marquee :as marquee]
            [storefront.components.slideout-nav :as slideout-nav]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.routes :as routes]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(def hamburger
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
   (case (marquee/portrait-status (auth/stylist-on-own-store? signed-in) portrait)
     ::marquee/show-what-we-have [:div.left.pr2 (marquee/stylist-portrait portrait)]
     ::marquee/ask-for-portrait  [:div.left.pr2 marquee/add-portrait-cta]
     ::marquee/show-nothing      [:div.left {:style {:height (str ui/header-image-size "px")}}])
   [:div.dark-gray
    "Welcome to " [:span.black.medium {:data-test "nickname"} store-nickname "'s"] " shop"
    (when expandable?
      [:span.ml1 (ui/expand-icon expanded?)])]])

(defn store-info [signed-in {:keys [expanded?] :as store}]
  (when (-> signed-in ::auth/to #{:marketplace :own-store})
    (let [rows (marquee/actions store gallery-link instagram-link styleseat-link)]
      (if-not (boolean (seq rows))
        (store-welcome signed-in store false)
        (ui/drop-down
         expanded?
         keypaths/store-info-expanded
         [:div (store-welcome signed-in store true)]
         [:div.bg-white.absolute.left-0.top-lit
          (for [[idx row] (map-indexed vector rows)]
            [:div.border-gray {:key   idx
                               :class (when-not (zero? idx) "border-top")} row])])))))

(defmulti account-info (fn [signed-in _ _ _] (::auth/as signed-in)))

(defmethod account-info :user [_ {:keys [email expanded?]} _ _]
  (ui/drop-down
   expanded?
   keypaths/account-menu-expanded
   [:a.inherit-color.h6
    "Signed in with: " [:span.teal email]
    " | Account settings" [:span.ml1 (ui/expand-icon expanded?)]]
   [:div.bg-white.absolute.right-0.top-lit
    [:div
     (drop-down-row (utils/route-to events/navigate-account-manage) "Account settings")]
    [:div.border-top.border-gray
     (drop-down-row (utils/route-to events/navigate-account-referrals) "Refer a friend")]
    [:div.border-top.border-gray
     (drop-down-row (utils/fake-href events/control-sign-out) "Sign out")]]))

(defmethod account-info :stylist [_ {:keys [email expanded?]} vouchers? store]
  (ui/drop-down
   expanded?
   keypaths/account-menu-expanded
   [:a.inherit-color.h6
    "Signed in with: " [:span.teal email]
    " | My dashboard" [:span.ml1 (ui/expand-icon expanded?)]]
   [:div.bg-white.absolute.right-0.border.border-gray.dark-gray.top-lit
    [:div
     (drop-down-row (utils/route-to events/navigate-v2-stylist-dashboard-orders) "My Dashboard")]

    (when vouchers?
      [:div.border-top.border-gray
       (drop-down-row (utils/route-to events/navigate-voucher-redeem) "Redeem Client Voucher")])

    [:div.border-top.border-gray
     (drop-down-row (utils/route-to events/navigate-stylist-share-your-store) "Share Your store")]
    (when-not (:match-eligible store)
      [:div.border-top.border-gray
       (drop-down-row community/community-url "Community")])
    [:div.border-top.border-gray
     (drop-down-row (utils/route-to events/navigate-stylist-account-profile) "Account Settings")]
    [:div.border-top.border-gray
     (drop-down-row (utils/fake-href events/control-sign-out) "Sign out")]]))

(defmethod account-info :guest [_ _ _ _]
  [:div.h6
   [:a.inherit-color (utils/route-to events/navigate-sign-in) "Sign in"]
   " | "
   [:a.inherit-color (utils/route-to events/navigate-sign-up) "No account? Sign up"]])

(def open-shopping (utils/expand-menu-callback keypaths/shop-menu-expanded))
(def close-shopping (utils/collapse-menus-callback keypaths/header-menus))

(defn header-menu-link [opts text]
  [:a.h5.medium.inherit-color.py2
   (merge opts {:style {:padding-left "24px" :padding-right "24px"}})
   text])

(defn menu [{:keys [show-freeinstall-link? v2-experience?]}]
  (component/html
   [:div.center
    (when show-freeinstall-link?
      (header-menu-link
       (assoc (utils/fake-href events/initiate-redirect-freeinstall-from-menu {:utm-source "shopFlyout"})
              :on-mouse-enter close-shopping)
       [:span [:span.teal.pr1 "NEW"] "Get a free install"]))

    (when-not v2-experience?
      (header-menu-link (assoc (utils/route-to events/navigate-shop-by-look {:album-keyword :deals})
                               :on-mouse-enter close-shopping)
                        "Deals"))
    (header-menu-link (assoc (utils/route-to events/navigate-shop-by-look {:album-keyword :look})
                             :on-mouse-enter close-shopping)
                      "Shop looks")
    (header-menu-link (assoc (utils/route-to events/navigate-home)
                             :on-mouse-enter open-shopping
                             :on-click       open-shopping)
                      "Shop hair")
    (header-menu-link (assoc (utils/route-to events/navigate-content-guarantee)
                             :on-mouse-enter close-shopping)
                      "Our Guarantee")
    (header-menu-link (assoc (utils/route-to events/navigate-content-our-hair)
                             :on-mouse-enter close-shopping)
                      "Our hair")
    (header-menu-link {:href           slideout-nav/blog-url
                       :on-mouse-enter close-shopping}
                      "Real Beautiful")]))

(defn shopping-column [items col-count]
  {:pre [(zero? (mod 12 col-count))]}
  [:ul.list-reset.col.px2
   {:class (str "col-" (/ 12 col-count))}
   (for [{:keys [page/slug copy/title category/new?] :as category} items]
     [:li {:key slug}
      [:a.inherit-color.block.pyp2.titleize
       (if (:direct-to-details/id category)
         (utils/route-to events/navigate-product-details
                         (merge
                          {:catalog/product-id (:direct-to-details/id category)
                           :page/slug          (:direct-to-details/slug category)}
                          (when-let [sku-id (:direct-to-details/sku-id category)]
                            {:query-params {:SKU sku-id}})))
         (utils/route-to events/navigate-category category))
       (when new?
         [:span.teal "NEW "])
       (string/capitalize title)]])])

(defn shopping-flyout [signed-in {:keys [expanded? categories]}]
  (when expanded?
    (let [show?   (fn [category]
                    (or (auth/stylist? signed-in)
                        (not (-> category :catalog/department (contains? "stylist-exclusives")))))
          columns (->> (filter :header/order categories)
                       (filter show?)
                       (sort-by :header/group)
                       (group-by :header/group)
                       vals
                       (map (partial sort-by :header/order))
                       (mapcat (partial partition-all 11)))]
      [:div.absolute.bg-white.col-12.z3.border-bottom.border-gray
       [:div.mx-auto.clearfix.my6.col-10
        (for [items columns]
          (shopping-column items (count columns)))]])))

(defn component [{:as data :keys [store user cart shopping signed-in vouchers?]} _ _]
  (component/create
   [:div
    [:div.hide-on-mb.relative
     {:on-mouse-leave close-shopping}
     [:div.relative.border-bottom.border-gray {:style {:height "150px"}}
      [:div.max-960.mx-auto
       [:div.left (store-info signed-in store)]
       [:div.right
        [:div.h6.my2.flex.items-center
         (account-info signed-in user vouchers? store)
         [:div.pl2
          (ui/shopping-bag {:style     {:height (str ui/header-image-size "px")
                                        :width  "28px"}
                            :data-test "desktop-cart"}
                           cart)]]]
       [:div.absolute.bottom-0.left-0.right-0
        [:div.mb4 (ui/clickable-logo {:event     events/navigate-home
                                      :data-test "desktop-header-logo"
                                      :height    "60px"})]
        [:div.mb1 (menu data)]]]]
     (shopping-flyout signed-in shopping)]
    [:div.hide-on-tb-dt.border-bottom.border-gray.flex.items-center
     hamburger
     [:div.flex-auto.py3 (ui/clickable-logo {:event     events/navigate-home
                                             :data-test "header-logo"
                                             :height    "40px"})]
     (ui/shopping-bag {:style     {:height "70px" :width "70px"}
                       :data-test "mobile-cart"}
                      cart)]]))

(defn minimal-component
  [logo-nav-event]
  (component/html
   [:div.border-bottom.border-gray.flex.items-center
    [:div.flex-auto.py3
     (ui/clickable-logo
      (cond-> {:data-test "header-logo"
               :height    "40px"}
        logo-nav-event
        (merge {:event logo-nav-event})))]]))

(defn query [data]
  (-> (slideout-nav/basic-query data)
      (assoc-in [:user :expanded?]     (get-in data keypaths/account-menu-expanded))
      (assoc-in [:shopping :expanded?] (get-in data keypaths/shop-menu-expanded))
      (assoc-in [:cart :quantity]      (orders/product-quantity (get-in data keypaths/order)))))

(defn built-component [data opts]
  [:header.stacking-context.z4
   (when (get-in data keypaths/hide-header?)
     {:class "hide-on-mb-tb"})
   (let [nav-event              (get-in data keypaths/navigation-event)
         freeinstall-subdomain? (= "freeinstall" (get-in data keypaths/store-slug))
         info-page?             (routes/sub-page? [nav-event] [events/navigate-info])]
     (if (nav/show-minimal-header? nav-event freeinstall-subdomain?)
       (minimal-component (cond info-page?                   events/navigate-adventure-home
                                (not freeinstall-subdomain?) events/navigate-home
                                :else                        nil))
       (component/build component (query data) nil)))])

(defn adventure-minimal-component [sign-in?]
  (component/html
   [:div.border-bottom.border-gray.flex.items-center.flex-wrap
    (if sign-in?
      [:a.block.inherit-color.col-3.flex.items.center
       (merge {:data-test "adventure-back-to-checkout"}
              (utils/route-back {:navigation-message [events/navigate-checkout-returning-or-guest]}))
       [:div.flex.items-center.justify-center {:style {:height "60px" :width "60px"}}
        (svg/back-arrow {:width "24px" :height "24px"})]]
      [:div.col-3])
    [:div.flex-auto.py3.col-6 (ui/clickable-logo
                         {:data-test "header-logo"
                          :height    "40px"})]
    [:div.col-3]]))

(defn adventure-built-component [data opts]
  [:header.stacking-context.z4
   (when (get-in data keypaths/hide-header?)
     {:class "hide-on-mb-tb"})
   (let [navigation-event (get-in data keypaths/navigation-event)
         sign-in?         (#{events/navigate-checkout-sign-in} navigation-event)]
     (if (nav/show-minimal-header? navigation-event true)
         (adventure-minimal-component sign-in?)
         (component/build component (query data) nil)))])
