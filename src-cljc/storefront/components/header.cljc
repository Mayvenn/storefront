(ns storefront.components.header
  (:require [clojure.string :as string]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.nav :as nav]
            [storefront.accessors.orders :as orders]
            [storefront.assets :as assets]
            [storefront.community :as community]
            [storefront.component :as component]
            [storefront.components.marquee :as marquee]
            [storefront.components.slideout-nav :as slideout-nav]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
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
  (component/html
   [:img.ml2 {:style {:height "20px"}
              :src   path}]) )

(def ^:private gallery-link
  (component/html
   (drop-down-row
    (utils/route-to events/navigate-store-gallery)
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
  (component/html
   [:div.h6.flex.items-center.mt2
    (case (marquee/portrait-status (auth/stylist-on-own-store? signed-in) portrait)
      ::marquee/show-what-we-have [:div.left.pr2 (marquee/stylist-portrait portrait)]
      ::marquee/ask-for-portrait  [:div.left.pr2 marquee/add-portrait-cta]
      ::marquee/show-nothing      [:div.left {:style {:height (str ui/header-image-size "px")}}])
    [:div.dark-gray
     "Welcome to " [:span.black.medium {:data-test "nickname"} store-nickname "'s"] " shop"
     (when expandable?
       [:span.ml1 (ui/expand-icon expanded?)])]]))

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
    " | Account" [:span.ml1 (ui/expand-icon expanded?)]]
   [:div.bg-white.absolute.right-0.top-lit
    [:div
     (drop-down-row (utils/route-to events/navigate-account-manage) "Account")]
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
    [:div.border-top.border-gray
     (drop-down-row (utils/route-to events/navigate-gallery-edit) "Edit Gallery")]
    (when-not (:match-eligible store)
      [:div.border-top.border-gray
       (drop-down-row community/community-url "Community")])
    [:div.border-top.border-gray
     (drop-down-row (utils/route-to events/navigate-stylist-account-profile) "Account Settings")]
    [:div.border-top.border-gray
     (drop-down-row (utils/fake-href events/control-sign-out) "Sign out")]]))

(defmethod account-info :guest [_ _ _ _]
  (component/html
   [:div.h6
    [:a.inherit-color (utils/route-to events/navigate-sign-in) "Sign in"]
    " | "
    [:a.inherit-color (utils/route-to events/navigate-sign-up) "No account? Sign up"]]))

(defn ->flyout-handlers [keypath]
  {:on-mouse-enter (utils/expand-menu-callback keypath)
   :on-click       (utils/expand-menu-callback keypath)})

(def close-header-menus (utils/collapse-menus-callback keypaths/header-menus))

(defn header-menu-link [opts text]
  (component/html
   [:a.h5.medium.inherit-color.py2
    (merge opts {:style {:padding-left "24px" :padding-right "24px"}})
    text]))

(defn menu
  [{:keys [show-freeinstall-link? show-bundle-sets-and-hide-deals? blog?]}]
  (component/html
   [:div.center
    (when show-freeinstall-link?
      (header-menu-link
       (assoc (utils/route-to events/navigate-adventure-match-stylist)
              :on-mouse-enter close-header-menus)
       [:span [:span.teal.pr1 "NEW"] "Get a Mayvenn Install"]))

    (when-not show-bundle-sets-and-hide-deals?
      (header-menu-link (assoc (utils/route-to events/navigate-shop-by-look {:album-keyword :deals})
                               :on-mouse-enter close-header-menus)
                        "Deals"))

    (header-menu-link (merge (utils/route-to events/navigate-home)
                             (->flyout-handlers keypaths/shop-looks-menu-expanded))
                      "Shop looks")

    (when show-bundle-sets-and-hide-deals?
      (header-menu-link (merge (utils/route-to events/navigate-home)
                               (->flyout-handlers keypaths/shop-bundle-sets-menu-expanded))
                        "Shop bundle sets"))

    (header-menu-link (merge (utils/route-to events/navigate-home)
                             (->flyout-handlers keypaths/shop-a-la-carte-menu-expanded))
                      "Shop hair")
    (header-menu-link (assoc (utils/route-to events/navigate-content-guarantee)
                             :on-mouse-enter close-header-menus)
                      "Our Guarantee")
    (header-menu-link (assoc (utils/route-to events/navigate-content-our-hair)
                             :on-mouse-enter close-header-menus)
                      "Our hair")
    (if blog?
      (header-menu-link {:href           slideout-nav/new-blog-url
                         :on-mouse-enter close-header-menus}
                        "Blog")
      (header-menu-link {:href           slideout-nav/blog-url
                         :on-mouse-enter close-header-menus}
                        "Real Beautiful"))]))

(defn flyout-column [options col-count]
  {:pre [(zero? (mod 12 col-count))]}
  (component/html
   [:ul.list-reset.col.px2
    {:class (str "col-" (/ 12 col-count))}
    (for [{:keys [key nav-message copy new?]} options]
      [:li {:key key}
       [:a.inherit-color.block.pyp2.titleize
        (apply utils/route-to nav-message)
        (when new?
          [:span.teal "NEW "])
        (string/capitalize copy)]])]))

(defn flyout [columns expanded?]
  (when expanded?
    (component/html
     [:div.absolute.bg-white.col-12.z3.border-bottom.border-gray
      [:div.mx-auto.clearfix.my6.col-10
       (let [col-count (count columns)]
         (for [[items ix] (map vector columns (range))]
           [:div
            {:key (str "col-" ix)}
            (flyout-column items col-count)]))]])))

(defn component [{:as data :keys [store user cart signed-in vouchers?]} _ _]
  (component/create
   [:div
    [:div.hide-on-mb.relative
     {:on-mouse-leave close-header-menus}
     [:div.relative.border-bottom.border-gray {:style {:height "180px"}}
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
     (flyout (:shop-a-la-carte-menu/columns data)
             (:shop-a-la-carte-menu/expanded? data))
     (flyout (:shop-looks-menu/columns data)
             (:shop-looks-menu/expanded? data))
     (flyout (:shop-bundle-sets-menu/columns data)
             (:shop-bundle-sets-menu/expanded? data))]
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

(defn category->flyout-option [{:as category :keys [:page/slug copy/title category/new?]}]
  {:key         slug
   :nav-message (let [{:direct-to-details/keys [id slug sku-id]} category]
                  (if id
                    [events/navigate-product-details
                     (merge
                      {:catalog/product-id id
                       :page/slug          slug}
                      (when sku-id {:query-params {:SKU sku-id}}))]
                    [events/navigate-category category]))
   :copy        title
   :new?        new?})

(defn shop-a-la-carte-flyout-query [data]
  {:shop-a-la-carte-menu/columns   (->>  (get-in data keypaths/categories)
                                         (filter :header/order)
                                         (filter (fn [category]
                                                   (or (auth/stylist? (auth/signed-in data))
                                                       (not (-> category
                                                                :catalog/department
                                                                (contains? "stylist-exclusives"))))))
                                         (sort-by :header/group)
                                         (group-by :header/group)
                                         vals
                                         (map (partial sort-by :header/order))
                                         (map (partial map category->flyout-option))
                                         (mapcat (partial partition-all 11)))
   :shop-a-la-carte-menu/expanded? (get-in data keypaths/shop-a-la-carte-menu-expanded)})

(defn shop-looks-query [data]
  {:shop-looks-menu/columns   [[{:key         "all"
                                 :nav-message [events/navigate-shop-by-look {:album-keyword :look}]
                                 :new?        false
                                 :copy        "All Looks"}]
                               [{:key         "straight"
                                 :nav-message [events/navigate-shop-by-look {:album-keyword :straight-looks}]
                                 :new?        false
                                 :copy        "Straight Looks"}]
                               [{:key         "curly"
                                 :nav-message [events/navigate-shop-by-look {:album-keyword :wavy-curly-looks}]
                                 :new?        false
                                 :copy        "Wavy & Curly Looks"}]]
   :shop-looks-menu/expanded? (get-in data keypaths/shop-looks-menu-expanded)})

(defn shop-bundle-sets-query [data]
  {:shop-bundle-sets-menu/columns   [[{:key         "all"
                                       :nav-message [events/navigate-shop-by-look {:album-keyword :all-bundle-sets}]
                                       :new?        false
                                       :copy        "All Bundle Sets"}]
                                     [{:key         "straight"
                                       :nav-message [events/navigate-shop-by-look {:album-keyword :straight-bundle-sets}]
                                       :new?        false
                                       :copy        "Straight Bundle Sets"}]
                                     [{:key         "curly"
                                       :nav-message [events/navigate-shop-by-look {:album-keyword :wavy-curly-bundle-sets}]
                                       :new?        false
                                       :copy        "Wavy & Curly Bundle Sets"}]]
   :shop-bundle-sets-menu/expanded? (get-in data keypaths/shop-bundle-sets-menu-expanded)})

(defn query [data]
  (-> (slideout-nav/basic-query data)
      (assoc-in [:user :expanded?] (get-in data keypaths/account-menu-expanded))
      (merge (shop-a-la-carte-flyout-query data))
      (merge (shop-looks-query data))
      (merge (shop-bundle-sets-query data))
      (assoc-in [:cart :quantity] (orders/product-quantity (get-in data keypaths/order)))))

(defn built-component [data opts]
  (component/html
   [:header.stacking-context.z4
    (when (get-in data keypaths/hide-header?)
      {:class "hide-on-mb-tb"})
    (let [nav-event (get-in data keypaths/navigation-event)]
      (if (nav/show-minimal-header? nav-event)
        (minimal-component events/navigate-home)
        (component/build component (query data) nil)))]))
