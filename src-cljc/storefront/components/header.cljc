(ns storefront.components.header
  (:require [catalog.categories :as categories]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.nav :as nav]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.sites :as sites]
            [storefront.component :as c]
            [storefront.components.marquee :as marquee]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.transitions :as transitions]
            [ui.promo-banner :as promo-banner]))

(def blog-url "https://shop.mayvenn.com/blog/")
(def wigs-101-url "https://shop.mayvenn.com/info/wigs-101-guide")

(def hamburger
  (c/html
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
   (c/html
    [:a.inherit-color.block.center.h5.flex.items-center.justify-center
     ^:attrs (-> opts
                 (assoc-in [:style :min-width] "200px")
                 (assoc-in [:style :height] "39px"))
     ^:inline content]))
  ([opts content1 content2]
   (c/html
    [:a.inherit-color.block.center.h5.flex.items-center.justify-center
     ^:attrs (-> opts
                 (assoc-in [:style :min-width] "200px")
                 (assoc-in [:style :height] "39px"))
     ^:inline content1
     ^:inline content2]))
  ([opts content1 content2 content3]
   (c/html
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
  (c/html
   [:div.h6.flex.items-center.mt2
    (case (marquee/portrait-status (auth/stylist-on-own-store? signed-in) portrait)
      ::marquee/show-what-we-have [:div.left.pr2 ^:inline (marquee/stylist-portrait portrait)]
      ::marquee/ask-for-portrait  [:div.left.pr2 ^:inline marquee/add-portrait-cta]
      ::marquee/show-nothing      [:div.left {:style {:height (str ui/header-image-size "px")}}])
    [:div "Welcome to " [:span.black.medium {:data-test "nickname"} store-nickname "'s"] " shop"
     (when expandable?
       [:span.ml1 ^:inline (ui/expand-icon expanded?)])]]))

(defn store-info [signed-in {:keys [expanded?] :as store}]
  (c/html
   (if (-> signed-in ::auth/to #{:marketplace :own-store})
     (let [rows (marquee/actions store gallery-link instagram-link styleseat-link)]
       (if (pos? (count rows))
         ^:inline (ui/drop-down
                   expanded?
                   (c/html
                    [:div
                     ^:attrs (utils/fake-href events/control-menu-expand {:keypath keypaths/store-info-expanded})
                     ^:inline (store-welcome signed-in store true)])
                   (c/html
                    [:div.bg-white.absolute.left-0.top-lit
                     (for [[idx row] (map-indexed vector rows)]
                       [:div.border-gray {:key   idx
                                          :class (when-not (zero? idx) "border-top")} row])]))
         ^:inline (store-welcome signed-in store false)))
     [:div])))

(defn account-info
  ;; TODO(jeff): is this overload an error? this fn used to be a multimethod. And JS
  ;;             implicitly allows calling a fn with less args than specified.
  ;; ([signed-in user vouchers?] (account-info signed-in user vouchers? nil ))
  [signed-in {:keys [email expanded?]} vouchers? stylist-experience past-appointments?]
  (c/html
   (case (::auth/as signed-in)

     :user    (ui/drop-down
               expanded?
               (c/html
                [:a.inherit-color.h6
                 ^:attrs (utils/fake-href events/control-menu-expand {:keypath keypaths/account-menu-expanded})
                 "Signed in with: " [:span.p-color email]
                 " | Account" [:span.ml1 ^:inline (ui/expand-icon expanded?)]])
               (c/html
                [:div.bg-white.absolute.right-0.top-lit
                 [:div
                  ^:inline (drop-down-row (utils/route-to events/navigate-account-manage) "Account")]
                 [:div.border-top.border-gray
                  ^:inline (drop-down-row (utils/fake-href events/control-sign-out) "Sign out")]]))

     :stylist (ui/drop-down
               expanded?
               (c/html
                [:a.inherit-color.h6
                 ^:attrs (utils/fake-href events/control-menu-expand {:keypath keypaths/account-menu-expanded})
                 "Signed in with: " [:span.p-color email]
                 " | My dashboard" [:span.ml1 ^:inline (ui/expand-icon expanded?)]])
               (c/html
                [:div.bg-white.absolute.right-0.border.border-gray.top-lit
                 [:div
                  ^:inline (drop-down-row (utils/route-to events/navigate-v2-stylist-dashboard-orders) "My Dashboard")]

                 (when vouchers?
                   [:div.border-top.border-gray
                    ^:inline (drop-down-row (utils/route-to events/navigate-voucher-redeem) "Redeem Client Voucher")])

                 [:div.border-top.border-gray
                  ^:inline (drop-down-row
                            (if (and past-appointments? (= stylist-experience "aladdin"))
                              (utils/route-to events/navigate-gallery-appointments)
                              (utils/route-to events/navigate-gallery-edit))
                            "Edit Gallery")]
                 [:div.border-top.border-gray
                  ^:inline (drop-down-row (utils/route-to events/navigate-stylist-account-profile) "Account Settings")]
                 [:div.border-top.border-gray
                  ^:inline (drop-down-row (utils/fake-href events/control-sign-out) "Sign out")]]))

     :guest [:div
             [:a.inherit-color ^:attrs (utils/route-to events/navigate-sign-in) "Sign in"]
             " | "
             [:a.inherit-color ^:attrs (utils/route-to events/navigate-sign-up) "Create Account"]]

     [:div])))


(defmethod transitions/transition-state events/stick-flyout
  [_ _ _ app-state]
  (assoc-in app-state keypaths/flyout-stuck-open? true))

(defmethod transitions/transition-state events/unstick-flyout
  [_ _ _ app-state]
  (assoc-in app-state keypaths/flyout-stuck-open? false))

(defmethod effects/perform-effects events/flyout-mouse-enter
  [_ _ {:keys [menu-keypath]} _ app-state]
  (when-not (get-in app-state keypaths/flyout-stuck-open?)
    (messages/handle-message events/control-menu-expand {:keypath menu-keypath})))

(defmethod effects/perform-effects events/flyout-on-click
  [_ _ {:keys [menu-keypath]} _ app-state]
  (messages/handle-message events/stick-flyout)
  (messages/handle-message events/control-menu-collapse-all {:menus (disj (get-in app-state keypaths/header-menus)
                                                                           menu-keypath)})
  (messages/handle-message events/control-menu-expand {:keypath menu-keypath}))

(defmethod effects/perform-effects events/flyout-mouse-away
  [_ _ _ _ app-state]
  (when-not (get-in app-state keypaths/flyout-stuck-open?)
    (messages/handle-message events/control-menu-collapse-all {:menus (get-in app-state keypaths/header-menus)})))

(defmethod effects/perform-effects events/flyout-click-away
  [_ _ _ _ app-state]
  (when (get-in app-state keypaths/flyout-stuck-open?)
    (messages/handle-message events/unstick-flyout)
    (messages/handle-message events/control-menu-collapse-all {:menus (get-in app-state keypaths/header-menus)})))

(defn ->flyout-handlers [keypath]
  {:on-mouse-enter (utils/send-event-callback events/flyout-mouse-enter {:menu-keypath keypath})
   :on-click       (utils/send-event-callback events/flyout-on-click {:menu-keypath keypath})})

(def close-header-menus (utils/send-event-callback events/flyout-mouse-away))

(c/defcomponent flyout-content
  [{:flyout/keys [items id]} _ _]
  (if id
    [:div.stacking-context.bg-cool-gray.flex.flex-column.p8.border.border-pale-purple
     (for [{:keys [key nav-message copy]} items]
       [:a.inherit-color.left-align.nowrap.my1.content-1.hover-menu-item
        (merge
         (apply utils/route-to nav-message)
         {:key key
          :id  key})
        [:span copy]])]
    (c/html [:div])) )

(c/defcomponent header-menu-item
  [{:header-menu-item/keys [navigation-target
                            href
                            content
                            new-label?
                            flyout-menu-path]
    :as data} _ _]
  [:div.inline.relative.flyout
   (merge
    {:on-mouse-leave close-header-menus}
    (when flyout-menu-path
      (->flyout-handlers flyout-menu-path)))
   [:a.h5.medium.inherit-color.py2
    ^:attrs
    (cond-> {:style {:padding-left "24px" :padding-right "24px"}}

      navigation-target
      (merge (apply utils/route-to navigation-target))

      href
      (merge {:href href}))
    (when new-label?
      [:span.p-color.pr1 "NEW"])
    content]
   [:div.absolute.left-0.z2
    {:style {:padding-left "24px"}}
    (c/build flyout-content data)]])

(c/defcomponent desktop-menu
  [{:desktop-menu/keys [items]
    :as                queried-data} _ _]
  [:div.center
   (for [item items]
     (c/build header-menu-item item (c/component-id (:header-menu-item/id item))))])

;; Produces a mobile-nav layout (no styling)
(defn mobile-nav-header [attrs left center right]
  (let [size {:width "80px" :height "55px"}]
    (c/html
     [:div.flex.items-center
      ^:attrs attrs
      [:div.mx-auto.flex.items-center.justify-around {:style size} ^:inline left]
      [:div.flex-auto.py3 ^:inline center]
      [:div.mx-auto.flex.items-center.justify-around {:style size} ^:inline right]])))

(defn adventure-header
  [{:header.back-navigation/keys [target back]
    :header.cart/keys            [value]
    :header.title/keys           [primary]}]
  (mobile-nav-header
   {:class "border-bottom border-gray bg-white black"
    :style {:height "70px"}}
   (c/html
    (if target
      [:div
       {:data-test "header-back"}
       [:a.block.black.p2.flex.justify-center.items-center
        (apply utils/route-back-or-to back target)
        (svg/left-arrow {:width  "20"
                         :height "20"})]]
      [:div]))
   (c/html [:div.content-1.proxima.center primary])
   (ui/shopping-bag {:data-test "mobile-cart"}
                    {:quantity value})))

(c/defcomponent mobile-nav-header-component
  [{:keys [forced-mobile-layout?] :as data} _ _]
  (mobile-nav-header
   {:class (str "border-bottom border-gray "
                (when-not forced-mobile-layout?
                  "hide-on-dt"))
    :style {:height "70px"}}
   hamburger
   (ui/clickable-logo {:event     events/navigate-home
                       :data-test "header-logo"
                       :height    "29px"})
   (ui/shopping-bag {:style     {:height "70px" :width "80px"}
                     :data-test "mobile-cart"}
                    data)))

(c/defcomponent component
  [{:as   data
    :keys [store user cart signed-in vouchers? stylist-experience past-appointments?]} _ _]
  [:div
   [:div.hide-on-mb-tb.relative
    [:div.relative.border-bottom.border-gray
     [:div.flex.justify-between.px8
      [:div {:key "store-info"} ^:inline (store-info signed-in store)]
      [:div {:key "account-info"}
       [:div.my2
        ^:inline (account-info signed-in user vouchers? stylist-experience past-appointments?)]]]
     [:div.flex.justify-between.px8
      [:div {:style {:width "33px"}}]
      [:div.mb3 {:key "logo"}
       [:div.mb4 ^:inline (ui/clickable-logo {:event     events/navigate-home
                                              :data-test "desktop-header-logo"
                                              :height    "44px"})]
       [:div ^:inline (c/build desktop-menu data)]]
      ^:inline (ui/shopping-bag {:style     {:height "44px"
                                             :width  "33px"}
                                 :data-test "desktop-cart"}
                                cart)]]]
   (c/build mobile-nav-header-component cart)])

(defn minimal-component
  [logo-nav-event]
  (c/html
   [:div.border-bottom.border-gray.flex.items-center
    [:div.flex-auto.py3
     ^:inline (ui/clickable-logo
               (cond-> {:data-test "header-logo"
                        :height    "40px"}
                 logo-nav-event
                 (merge {:event logo-nav-event})))]]))

;; NOTE: This a-la-carte does not mean services, it refers to not using shared-carts/looks
(defn shop-a-la-carte-flyout-query [data]
  {:header-menu-item/flyout-menu-path keypaths/shop-a-la-carte-menu-expanded
   :header-menu-item/content          "Shop hair"
   :header-menu-item/id               "desktop-shop-hair"
   :flyout/items                      (->> (get-in data keypaths/categories)
                                           (filter :flyout-menu/order)
                                           (filter (fn [category]
                                                     (or (auth/stylist? (auth/signed-in data))
                                                         (not (-> category
                                                                  :catalog/department
                                                                  (contains? "stylist-exclusives"))))))
                                           (sort-by :flyout-menu/order)
                                           (mapv (fn category->icp-flyout-option
                                                   [{:as category :keys [:page/slug flyout-menu/title category/new?]}]
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
                                                    :new?        new?})))
   :flyout/id                         (when (get-in data keypaths/shop-a-la-carte-menu-expanded)
                                        "shop-a-la-carte-menu-expanded")})

(defn shop-looks-query [data]
  {:header-menu-item/navigation-target [events/navigate-shop-by-look {:album-keyword :look}]
   :header-menu-item/id                "desktop-shop-by-look"
   :header-menu-item/content           "Shop by look"})

(defn shop-bundle-sets-query [data]
  {:header-menu-item/flyout-menu-path keypaths/shop-bundle-sets-menu-expanded
   :header-menu-item/id               "desktop-shop-bundle-sets"
   :header-menu-item/content          "Shop bundle sets"
   :flyout/items                      [{:key         "all"
                                        :nav-message [events/navigate-shop-by-look {:album-keyword :all-bundle-sets}]
                                        :new?        false
                                        :copy        "All Bundle Sets"}
                                       {:key         "straight"
                                        :nav-message [events/navigate-shop-by-look {:album-keyword :straight-bundle-sets}]
                                        :new?        false
                                        :copy        "Straight Bundle Sets"}
                                       {:key         "curly"
                                        :nav-message [events/navigate-shop-by-look {:album-keyword :wavy-curly-bundle-sets}]
                                        :new?        false
                                        :copy        "Wavy & Curly Bundle Sets"}]
   :flyout/id                         (when (get-in data keypaths/shop-bundle-sets-menu-expanded)
                                        "shop-bundle-sets-menu-expanded")})

(defn basic-query [data]
  (let [store       (marquee/query data)
        site        (sites/determine-site data)
        shop?       (= :shop site)
        classic?    (= :classic site)
        signed-in   (auth/signed-in data)
        unified-fi? (experiments/shopping-quiz-unified-fi? data)]
    {:signed-in                   signed-in
     :on-taxon?                   (get-in data keypaths/current-traverse-nav)
     :promo-banner                (promo-banner/query data)
     :user                        {:stylist-portrait (get-in data keypaths/user-stylist-portrait)
                                   :email            (get-in data keypaths/user-email)}
     :store                       store
     :vouchers?                   (experiments/dashboard-with-vouchers? data)
     :stylist-experience          (get-in data keypaths/user-stylist-experience)
     :past-appointments?          (experiments/past-appointments? data)
     :site                        site
     :slide-out-nav/content-items [{:slide-out-nav-content-item/target  [events/navigate-content-guarantee]
                                    :slide-out-nav-content-item/id      "content-guarantee"
                                    :slide-out-nav-content-item/primary "Our Guarantee"}
                                   {:slide-out-nav-content-item/target  [events/navigate-content-our-hair]
                                    :slide-out-nav-content-item/id      "content-our-hair"
                                    :slide-out-nav-content-item/primary "Our Hair"}
                                   {:slide-out-nav-content-item/target  {:href wigs-101-url}
                                    :slide-out-nav-content-item/id      "content-wigs-101"
                                    :slide-out-nav-content-item/primary "Wigs 101"}
                                   {:slide-out-nav-content-item/target  {:href blog-url}
                                    :slide-out-nav-content-item/id      "content-blog"
                                    :slide-out-nav-content-item/primary "Blog"}
                                   {:slide-out-nav-content-item/target  {:href "https://info.mayvenn.com/info/walmart"}
                                    :slide-out-nav-content-item/id      "content-our-locations"
                                    :slide-out-nav-content-item/primary "Our Locations"}
                                   {:slide-out-nav-content-item/target  [events/navigate-content-about-us]
                                    :slide-out-nav-content-item/id      "content-about-us"
                                    :slide-out-nav-content-item/primary "About Us"}
                                   {:slide-out-nav-content-item/target  {:href "https://jobs.mayvenn.com"}
                                    :slide-out-nav-content-item/id      "content-jobs"
                                    :slide-out-nav-content-item/primary "Careers"}
                                   {:slide-out-nav-content-item/target  [events/navigate-content-help]
                                    :slide-out-nav-content-item/id      "content-help"
                                    :slide-out-nav-content-item/primary "Contact Us"}]

     :slide-out-nav/menu-items (cond-> []
                                 (and shop? (not unified-fi?))
                                 (concat
                                  [{:slide-out-nav-menu-item/target      [events/navigate-adventure-find-your-stylist]
                                    :slide-out-nav-menu-item/id          "menu-shop-find-stylist"
                                    :slide-out-nav-menu-item/new-primary "NEW"
                                    :slide-out-nav-menu-item/primary     "Get a Mayvenn Install"}])

                                 (and shop? unified-fi?)
                                 (concat
                                  [{:slide-out-nav-menu-item/target      [events/navigate-shopping-quiz-unified-freeinstall-intro]
                                    :slide-out-nav-menu-item/id          "menu-shop-quiz-unified-fi"
                                    :slide-out-nav-menu-item/new-primary "NEW"
                                    :slide-out-nav-menu-item/primary     "Start Hair Quiz"}])

                                 shop?
                                 (concat
                                  [{:slide-out-nav-menu-item/target  [events/navigate-shop-by-look {:album-keyword :look}]
                                    :slide-out-nav-menu-item/nested? false
                                    :slide-out-nav-menu-item/id      "menu-shop-by-look"
                                    :slide-out-nav-menu-item/primary "Shop By Look"}
                                   {:slide-out-nav-menu-item/target  [events/menu-list {:menu-type :shop-bundle-sets}]
                                    :slide-out-nav-menu-item/nested? true
                                    :slide-out-nav-menu-item/id      "menu-shop-by-bundle-sets"
                                    :slide-out-nav-menu-item/primary "Shop Bundle Sets"}])

                                 classic?
                                 (concat
                                  [{:slide-out-nav-menu-item/target  [events/navigate-shop-by-look {:album-keyword :look}]
                                    :slide-out-nav-menu-item/nested? false
                                    :slide-out-nav-menu-item/id      "menu-shop-by-look"
                                    :slide-out-nav-menu-item/primary "Shop By Look"}])

                                 :always
                                 (concat
                                  [{:slide-out-nav-menu-item/target      [events/navigate-category {:page/slug "wigs" :catalog/category-id "13"}]
                                    :slide-out-nav-menu-item/id          "menu-shop-wigs"
                                    :slide-out-nav-menu-item/new-primary "NEW"
                                    :slide-out-nav-menu-item/nested?     false
                                    :slide-out-nav-menu-item/primary     "Wigs"}
                                   {:slide-out-nav-menu-item/target  [events/navigate-category {:page/slug "human-hair-bundles" :catalog/category-id "27"}]
                                    :slide-out-nav-menu-item/nested? false
                                    :slide-out-nav-menu-item/id      "menu-shop-human-hair-bundles"
                                    :slide-out-nav-menu-item/primary "Hair Bundles"}
                                   {:slide-out-nav-menu-item/target  [events/navigate-category {:page/slug "virgin-closures" :catalog/category-id "0"}]
                                    :slide-out-nav-menu-item/id      "menu-shop-virgin-closures"
                                    :slide-out-nav-menu-item/nested? false
                                    :slide-out-nav-menu-item/primary "Closures"}
                                   {:slide-out-nav-menu-item/target  [events/navigate-category {:page/slug "virgin-frontals" :catalog/category-id "1"}]
                                    :slide-out-nav-menu-item/id      "menu-shop-virgin-frontals"
                                    :slide-out-nav-menu-item/nested? false
                                    :slide-out-nav-menu-item/primary "Frontals"}
                                   {:slide-out-nav-menu-item/target  [events/navigate-category {:page/slug "hair-extensions" :catalog/category-id "28"}]
                                    :slide-out-nav-menu-item/nested? false
                                    :slide-out-nav-menu-item/id      "menu-shop-hair-extensions"
                                    :slide-out-nav-menu-item/primary "Hair Extensions"}])

                                 (-> signed-in ::auth/as (= :stylist))
                                 (concat
                                  [{:slide-out-nav-menu-item/target  [events/navigate-product-details
                                                                      {:page/slug          "rings-kits"
                                                                       :catalog/product-id "49"
                                                                       :query-params       {:SKU (:direct-to-details/sku-id categories/the-only-stylist-exclusive)}}]
                                    :slide-out-nav-menu-item/nested? false
                                    :slide-out-nav-menu-item/id      "menu-stylist-products"
                                    :slide-out-nav-menu-item/primary "Stylist Exclusives"}]))
     :desktop-menu/items (cond-> []

                           (and shop? (not unified-fi?))
                           (concat
                            [{:header-menu-item/navigation-target [events/navigate-adventure-find-your-stylist]
                              :header-menu-item/id                "desktop-shop-find-stylist"
                              :header-menu-item/new-label?        true
                              :header-menu-item/content           "Get a Mayvenn Install"}])

                           (and shop? unified-fi?)
                           (concat
                            [{:header-menu-item/navigation-target [events/navigate-shopping-quiz-unified-freeinstall-intro]
                              :header-menu-item/id                "desktop-shop-quiz-unified-fi"
                              :header-menu-item/new-label?        true
                              :header-menu-item/content           "Start Hair Quiz"}])

                           shop?
                           (concat
                            [(shop-looks-query data)
                             (shop-bundle-sets-query data)])

                           classic?
                           (concat [{:header-menu-item/navigation-target [events/navigate-shop-by-look {:album-keyword :look}]
                                     :header-menu-item/id                "desktop-shop-by-look"
                                     :header-menu-item/content           "Shop by look"}])

                           :always
                           (concat
                            [(shop-a-la-carte-flyout-query data)
                             {:header-menu-item/navigation-target [events/navigate-content-guarantee]
                              :header-menu-item/id                "desktop-our-guarantee"
                              :header-menu-item/content           "Our Guarantee"}
                             {:header-menu-item/navigation-target [events/navigate-content-our-hair]
                              :header-menu-item/id                "desktop-our-hair"
                              :header-menu-item/content           "Our hair"}
                             {:header-menu-item/href    wigs-101-url
                              :header-menu-item/id      "desktop-wigs-101"
                              :header-menu-item/content "Wigs 101"}
                             {:header-menu-item/href    blog-url
                              :header-menu-item/id      "desktop-blog"
                              :header-menu-item/content "Blog"}]))}))

(defn query [data]
  (-> (basic-query data)
      (assoc-in [:user :expanded?] (get-in data keypaths/account-menu-expanded))
      (assoc-in [:cart :quantity] (orders/displayed-cart-count (get-in data keypaths/order)))))

(defn built-component [data opts]
  (c/html
   [:header.stacking-context.z4
    (when (get-in data keypaths/hide-header?)
      {:class "hide-on-mb-tb"})
    (let [nav-event (get-in data keypaths/navigation-event)]
      (if (nav/show-minimal-header? data nav-event)
        ^:inline (minimal-component events/navigate-home)
        ^:inline (c/build component (query data) nil)))]))
