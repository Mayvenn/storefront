(ns storefront.components.header
  (:require [clojure.string :as string]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.nav :as nav]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.sites :as sites]
            [storefront.accessors.stylists :as stylists]
            [storefront.assets :as assets]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.marquee :as marquee]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [ui.promo-banner :as promo-banner]))

(def blog-url "https://shop.mayvenn.com/blog/")

(def hamburger
  (component/html
   [:a.block.px3.py4.content-box.black
    (assoc (utils/fake-href events/control-menu-expand-hamburger
                            {:keypath keypaths/menu-expanded})
           :style     {:width "50px"}
           ;; only associate data test when cljs registers the click handler
           #?@(:cljs [:data-test "hamburger"]))
    [:div.border-top.border-bottom
     {:style {:height       "17px"
              :border-width "2px"}}
     [:span.hide "MENU"]]]))

(defn drop-down-row
  ([opts content]
   (component/html
    [:a.inherit-color.block.center.h5.flex.items-center.justify-center
     ^:attrs (-> opts
                 (assoc-in [:style :min-width] "200px")
                 (assoc-in [:style :height] "39px"))
     ^:inline content]))
  ([opts content1 content2]
   (component/html
    [:a.inherit-color.block.center.h5.flex.items-center.justify-center
     ^:attrs (-> opts
                 (assoc-in [:style :min-width] "200px")
                 (assoc-in [:style :height] "39px"))
     ^:inline content1
     ^:inline content2]))
  ([opts content1 content2 content3]
   (component/html
    [:a.inherit-color.block.center.h5.flex.items-center.justify-center
     ^:attrs (-> opts
                 (assoc-in [:style :min-width] "200px")
                 (assoc-in [:style :height] "39px"))
     ^:inline content1
     ^:inline content2
     ^:inline content3])))

(defn social-icon [ucare-uuid]
  (ui/ucare-img {:class "ml2"
                 :style {:height "20px"}} ucare-uuid))

(def ^:private gallery-link
  (drop-down-row
   (utils/route-to events/navigate-store-gallery)
   "View gallery"
   (social-icon "fa4eefff-7856-4a1b-8cdb-c8b228b62967")))

(defn ^:private instagram-link [instagram-account]
  (drop-down-row
   {:href (marquee/instagram-url instagram-account)}
   "Follow on"
   (social-icon "1a4a3bd5-0fda-45f2-9bb4-3739b911390f")))

(defn ^:private styleseat-link [styleseat-account]
  (drop-down-row
   {:href (marquee/styleseat-url styleseat-account)}
   "Book on"
   (social-icon "c8f0a4b8-24f7-4de8-9c20-c6634b865bc1")))

(defn store-welcome [signed-in {:keys [store-nickname portrait expanded?]} expandable?]
  (component/html
   [:div.h6.flex.items-center.mt2
    (case (marquee/portrait-status (auth/stylist-on-own-store? signed-in) portrait)
      ::marquee/show-what-we-have [:div.left.pr2 ^:inline (marquee/stylist-portrait portrait)]
      ::marquee/ask-for-portrait  [:div.left.pr2 ^:inline marquee/add-portrait-cta]
      ::marquee/show-nothing      [:div.left {:style {:height (str ui/header-image-size "px")}}])
    [:div "Welcome to " [:span.black.medium {:data-test "nickname"} store-nickname "'s"] " shop"
     (when expandable?
       [:span.ml1 ^:inline (ui/expand-icon expanded?)])]]))

(defn store-info [signed-in {:keys [expanded?] :as store}]
  (component/html
   (if (-> signed-in ::auth/to #{:marketplace :own-store})
     (let [rows (marquee/actions store gallery-link instagram-link styleseat-link)]
       (if (pos? (count rows))
         ^:inline (ui/drop-down
                   expanded?
                   (component/html
                    [:div
                     ^:attrs (utils/fake-href events/control-menu-expand {:keypath keypaths/store-info-expanded})
                     ^:inline (store-welcome signed-in store true)])
                   (component/html
                    [:div.bg-white.absolute.left-0.top-lit
                     (for [[idx row] (map-indexed vector rows)]
                       [:div.border-gray {:key   idx
                                          :class (when-not (zero? idx) "border-top")} row])]))
         ^:inline (store-welcome signed-in store false)))
     [:div])))

(defn account-info
  ;; TODO(jeff): is this overload an error? this fn used to be a multimethod. And JS
  ;;             implicitly allows calling a fn with less args than specified.
  ([signed-in user vouchers?] (account-info signed-in user vouchers? nil))
  ([signed-in {:keys [email expanded?]} vouchers? store]
   (component/html
    (case (::auth/as signed-in)

      :user    (ui/drop-down
                expanded?
                (component/html
                 [:a.inherit-color.h6
                  ^:attrs (utils/fake-href events/control-menu-expand {:keypath keypaths/account-menu-expanded})
                  "Signed in with: " [:span.p-color email]
                  " | Account" [:span.ml1 ^:inline (ui/expand-icon expanded?)]])
                (component/html
                 [:div.bg-white.absolute.right-0.top-lit
                  [:div
                   ^:inline (drop-down-row (utils/route-to events/navigate-account-manage) "Account")]
                  [:div.border-top.border-gray
                   ^:inline (drop-down-row (utils/route-to events/navigate-account-referrals) "Refer a friend")]
                  [:div.border-top.border-gray
                   ^:inline (drop-down-row (utils/fake-href events/control-sign-out) "Sign out")]]))

      :stylist (ui/drop-down
                expanded?
                (component/html
                 [:a.inherit-color.h6
                  ^:attrs (utils/fake-href events/control-menu-expand {:keypath keypaths/account-menu-expanded})
                  "Signed in with: " [:span.p-color email]
                  " | My dashboard" [:span.ml1 ^:inline (ui/expand-icon expanded?)]])
                (component/html
                 [:div.bg-white.absolute.right-0.border.border-gray.top-lit
                  [:div
                   ^:inline (drop-down-row (utils/route-to events/navigate-v2-stylist-dashboard-orders) "My Dashboard")]

                  (when vouchers?
                    [:div.border-top.border-gray
                     ^:inline (drop-down-row (utils/route-to events/navigate-voucher-redeem) "Redeem Client Voucher")])

                  [:div.border-top.border-gray
                   ^:inline (drop-down-row (utils/route-to events/navigate-stylist-share-your-store) "Share Your store")]
                  [:div.border-top.border-gray
                   ^:inline (drop-down-row (utils/route-to events/navigate-gallery-edit) "Edit Gallery")]
                  [:div.border-top.border-gray
                   ^:inline (drop-down-row (utils/route-to events/navigate-stylist-account-profile) "Account Settings")]
                  [:div.border-top.border-gray
                   ^:inline (drop-down-row (utils/fake-href events/control-sign-out) "Sign out")]]))

      :guest [:div
              [:a.inherit-color ^:attrs (utils/route-to events/navigate-sign-in) "Sign in"]
              " | "
              [:a.inherit-color ^:attrs (utils/route-to events/navigate-sign-up) "Create Account"]]

      [:div]))))

(defn ->flyout-handlers [keypath]
  {:on-mouse-enter (utils/expand-menu-callback keypath)
   :on-click       (utils/expand-menu-callback keypath)})

(def close-header-menus (utils/collapse-menus-callback keypaths/header-menus))

(defn header-menu-link [opts text]
  (component/html
   [:a.h5.medium.inherit-color.py2
    ^:attrs (merge opts {:style {:padding-left "24px" :padding-right "24px"}})
    text]))

(defn individual-header-menu-link
  ([opts text]
   (individual-header-menu-link opts text nil))
  ([opts text flyout-content]
   (component/html
    [:div.inline.relative
     [:a.h5.medium.inherit-color.py2
      ^:attrs (merge opts {:style {:padding-left "24px" :padding-right "24px"}})
      text]
     [:div.absolute.left-0.z2
      {:style {:padding-left "24px"}}
      (when flyout-content
        [:div.bg-cool-gray.flex.flex-column.p8.border.border-pale-purple
         (for [{:keys [key nav-message copy new?]} flyout-content]
           [:a.inherit-color.left-align.nowrap.my1.content-1.hover-menu-item
            (merge
             (apply utils/route-to nav-message)
             {:key key
              :id  key})
            [:span copy]])])]])))

(defn individual-flyout-menu
  [{:keys [show-freeinstall-link? show-bundle-sets? salon-services site]
    :as   queried-data}]
  (component/html
   [:div.center
    (when show-freeinstall-link?
      ^:inline (individual-header-menu-link
                (assoc (utils/route-to events/navigate-adventure-match-stylist)
                       :on-mouse-enter close-header-menus)
                (component/html [:span [:span.p-color.pr1 "NEW"] "Get a Mayvenn Install"])))
    (when salon-services
      ^:inline (individual-header-menu-link
                (assoc (utils/route-to events/navigate-category salon-services)
                       :on-mouse-enter close-header-menus)
                (component/html [:span
                                 (when (:category/new? salon-services)
                                   [:span.p-color.pr1 "NEW"])
                                 (:flyout-menu/title salon-services)])))

    (if (= :classic site)
      ^:inline (individual-header-menu-link
                (assoc (utils/route-to events/navigate-shop-by-look {:album-keyword :look})
                       :on-mouse-enter close-header-menus)
                "Shop by look")
      [:div.inline (->flyout-handlers keypaths/shop-looks-menu-expanded)
       ^:inline (individual-header-menu-link
                 (utils/route-to events/navigate-home)
                 "Shop by look"
                 (when (:shop-looks-menu/expanded? queried-data)
                   ;; TODO: refactor data structure to remove be a single level list of items
                   (flatten
                    (:shop-looks-menu/columns queried-data))))])

    (when show-bundle-sets?
      [:div.inline (->flyout-handlers keypaths/shop-bundle-sets-menu-expanded)
       ^:inline (individual-header-menu-link
                 (utils/route-to events/navigate-home)
                 "Shop bundle sets"
                 (when (:shop-bundle-sets-menu/expanded? queried-data)
                   ;; TODO: refactor data structure to remove be a single level list of items
                   (flatten
                    (:shop-bundle-sets-menu/columns queried-data))))])

    [:div.inline (->flyout-handlers keypaths/shop-a-la-carte-menu-expanded)
     ^:inline (individual-header-menu-link
               (utils/route-to events/navigate-home)
               "Shop hair"
               (when (:shop-a-la-carte-menu/expanded? queried-data)
                 ;; TODO: refactor data structure to remove be a single level list of items
                 (flatten (:shop-a-la-carte-menu/columns queried-data))))]
    ^:inline (individual-header-menu-link
              (assoc (utils/route-to events/navigate-content-guarantee)
                     :on-mouse-enter close-header-menus)
              "Our Guarantee")
    ^:inline (individual-header-menu-link
              (assoc (utils/route-to events/navigate-content-our-hair)
                     :on-mouse-enter close-header-menus)
              "Our hair")
    ^:inline (individual-header-menu-link
              {:href           blog-url
               :on-mouse-enter close-header-menus}
              "Blog")]))

(defn flyout-column [options col-count]
  {:pre [(zero? (mod 12 col-count))]}
  (component/html
   [:ul.list-reset.col.px2
    {:class (str "col-" (/ 12 col-count))
     :style {:min-height "1px"}}
    (for [{:keys [key nav-message copy new?]} options]
      [:li {:key key}
       [:a.inherit-color.block.pyp2.titleize
        (apply utils/route-to nav-message)
        (when new?
          [:span.p-color "NEW "])
        (some->> copy string/capitalize)]])]))

(defn flyout [columns expanded?]
  (component/html
   (if expanded?
     [:div.absolute.bg-white.col-12.z3.border-bottom.border-gray
      [:div.mx-auto.clearfix.my6.col-10
       (let [col-count (count columns)]
         (for [[items ix] (map vector columns (range))]
           [:div
            {:key (str "col-" ix)}
            (flyout-column items col-count)]))]]
     [:div])))

;; Produces a mobile-nav layout (no styling)
(defn mobile-nav-header [attrs left center right]
  (let [size {:width "80px" :height "55px"}]
    (component/html
     [:div.flex.items-center
      ^:attrs attrs
      [:div.mx-auto.flex.items-center.justify-around {:style size} ^:inline left]
      [:div.flex-auto.py3 ^:inline center]
      [:div.mx-auto.flex.items-center.justify-around {:style size} ^:inline right]])))

(defn adventure-header
  ([left-target title cart-data]
   (adventure-header {:header.back-navigation/target left-target
                      :header.title/primary          title
                      :header.cart/value             (:quantity cart-data)}))
  ([{:header.back-navigation/keys [target back]
     :header.cart/keys            [value]
     :header.title/keys           [primary]}]
   (mobile-nav-header
    {:class "border-bottom border-gray bg-white black"
     :style {:height "70px"}}
    (component/html
     (if target
       [:div
        {:data-test "header-back"}
        [:a.block.black.p2.flex.justify-center.items-center
         (apply utils/route-back-or-to back target)
         (svg/left-arrow {:width  "20"
                          :height "20"})]]
       [:div]))
    (component/html [:div.content-1.proxima.center primary])
    (ui/shopping-bag {:data-test "mobile-cart"}
                     {:quantity value}))))

(defcomponent component
  [{:as   data
    :keys [store user cart signed-in vouchers?]} _ _]
  [:div
   [:div.hide-on-mb.relative
    {:on-mouse-leave close-header-menus}
    [:div.relative.border-bottom.border-gray
     [:div.flex.justify-between.px8
      [:div {:key "store-info"} ^:inline (store-info signed-in store)]
      [:div {:key "account-info"}
       [:div.my2
        ^:inline (account-info signed-in user vouchers? store)]]]
     [:div.flex.justify-between.px8
      [:div {:style {:width "33px"}}]
      [:div.mb3 {:key "logo"}
       [:div.mb4 ^:inline (ui/clickable-logo {:event     events/navigate-home
                                              :data-test "desktop-header-logo"
                                              :height    "44px"})]
       [:div ^:inline (individual-flyout-menu data)]]
      ^:inline (ui/shopping-bag {:style     {:height "44px"
                                             :width  "33px"}
                                 :data-test "desktop-cart"}
                                cart)]]]
   (mobile-nav-header
    {:class "border-bottom border-gray hide-on-tb-dt"
     :style {:height "70px"}}
    hamburger
    (ui/clickable-logo {:event     events/navigate-home
                        :data-test "header-logo"
                        :height    "29px"})
    (ui/shopping-bag {:style     {:height "70px" :width "80px"}
                      :data-test "mobile-cart"}
                     cart))])

(defn minimal-component
  [logo-nav-event]
  (component/html
   [:div.border-bottom.border-gray.flex.items-center
    [:div.flex-auto.py3
     ^:inline (ui/clickable-logo
               (cond-> {:data-test "header-logo"
                        :height    "40px"}
                 logo-nav-event
                 (merge {:event logo-nav-event})))]]))

(defn category->flyout-option [{:as category :keys [:page/slug header/title category/new?]}]
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

(defn category->icp-flyout-option [{:as category :keys [:page/slug flyout-menu/title category/new?]}]
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
                                         (filter :flyout-menu/order)
                                         (filter (fn [category]
                                                   (or (auth/stylist? (auth/signed-in data))
                                                       (not (-> category
                                                                :catalog/department
                                                                (contains? "stylist-exclusives"))))))
                                         (sort-by :flyout-menu/order)
                                         (map category->icp-flyout-option)
                                         (vector nil))
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

(defn basic-query [data]
  (let [{:keys [match-eligible] :as store} (marquee/query data)
        shop?                              (= "shop" (get-in data keypaths/store-slug))
        aladdin?                           (experiments/aladdin-experience? data)]
    {:signed-in              (auth/signed-in data)
     :on-taxon?              (get-in data keypaths/current-traverse-nav)
     :promo-banner           (promo-banner/query data)
     :user                   {:stylist-portrait (get-in data keypaths/user-stylist-portrait)
                              :email            (get-in data keypaths/user-email)}
     :store                  store
     :show-bundle-sets?      (or aladdin? shop?)
     :vouchers?              (experiments/dashboard-with-vouchers? data)
     :show-freeinstall-link? shop?
     :service-category-page? shop?
     :site                   (sites/determine-site data)}))

(defn salon-services-category [data]
  (let [shop?    (= :shop (sites/determine-site data))
        category (->> (get-in data keypaths/categories)
                      (filter (comp #{"30"} :catalog/category-id))
                      first)]
    (when shop?
      {:salon-services (select-keys category [:page/slug
                                              :catalog/category-id
                                              :flyout-menu/title
                                              :category/new?])})))

(defn query [data]
  (-> (basic-query data)
      (assoc-in [:user :expanded?] (get-in data keypaths/account-menu-expanded))
      (merge (salon-services-category data))
      (merge (shop-a-la-carte-flyout-query data))
      (merge (shop-looks-query data))
      (merge (shop-bundle-sets-query data))
      (assoc-in [:cart :quantity] (orders/displayed-cart-count (get-in data keypaths/order)))
      (assoc :site (sites/determine-site data))))

(defn built-component [data opts]
  (component/html
   [:header.stacking-context.z4
    (when (get-in data keypaths/hide-header?)
      {:class "hide-on-mb-tb"})
    (let [nav-event (get-in data keypaths/navigation-event)]
      (if (nav/show-minimal-header? nav-event)
        ^:inline (minimal-component events/navigate-home)
        ^:inline (component/build component (query data) nil)))]))
