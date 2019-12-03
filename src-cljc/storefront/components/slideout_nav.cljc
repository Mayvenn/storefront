(ns storefront.components.slideout-nav
  (:require [catalog.menu :as menu]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.stylists :as stylists]
            [storefront.assets :as assets]
            [storefront.community :as community]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.marquee :as marquee]
            [storefront.components.money-formatters :refer [as-money]]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]))

(def blog-url "https://blog.mayvenn.com")

;; TODO: Make this the main blog-url when blog experiment is 100%
(def new-blog-url "https://shop.mayvenn.com/blog/")

(defn burger-header [cart]
  (component/html
   [:div.bg-white.flex.items-center.border-bottom.border-gray
    (ui/big-x {:data-test "close-slideout"
               :attrs     {:on-click #(messages/handle-message events/control-menu-collapse-all)}})
    [:div.flex-auto.py3 (ui/clickable-logo {:event     events/navigate-home
                                            :data-test "header-logo"
                                            :height    "40px"})]
    (ui/shopping-bag {:style     {:height "70px" :width "70px"}
                      :data-test "mobile-cart"}
                     cart)]))

(defn ^:private marquee-col [content]
  [:div.flex-auto
   {:style {:flex-basis 0}}
   content])

(defn ^:private marquee-row [left-content right-content]
  [:div.flex.my3
   (marquee-col left-content)
   [:div.pr3]
   (marquee-col right-content)])

(defn ^:private stylist-portrait [{:keys [stylist-portrait]}]
  (let [header-image-size 36
        portrait-status   (:status stylist-portrait)]
    [:div.h6.flex.items-center.left.mr2
     (if (#{"approved" "pending"} portrait-status)
       (ui/circle-picture {:class "mx-auto"
                           :width (str header-image-size "px")}
                          (ui/square-image stylist-portrait header-image-size))
       [:a (utils/route-to events/navigate-stylist-account-profile)
        [:img {:width (str header-image-size "px")
               :src   "//ucarecdn.com/81bd063f-56ba-4e9c-9aef-19a1207fd422/-/format/auto/stylist-bug-no-pic-fallback"}]])]))

(defn ^:private account-info-marquee [signed-in {:keys [email store-credit]}]
  (when (-> signed-in ::auth/at-all)
    [:div.my3
     [:div.h8.medium "Signed in with:"]
     [:a.p-color.h5.bold
      (merge
       {:data-test "signed-in-as"}
       (utils/route-to (if (-> signed-in ::auth/as (= :stylist))
                         events/navigate-stylist-account-profile
                         events/navigate-account-manage)))
      email]
     (when (pos? store-credit)
       [:div
        [:div.h8.medium "Store credit:"]
        [:div.p-color.h5.bold (as-money store-credit)]])]))

(defn ^:private stylist-actions
  [vouchers? show-community?]
  (component/html
   [:div
    (when vouchers?
      (ui/p-color-button (assoc (utils/route-to events/navigate-voucher-redeem)
                                :height-class "py2"
                                :data-test    "redeem-voucher")
                         "Redeem Client Voucher"))
    [:div
     (marquee-row
      (ui/underline-button (assoc (utils/route-to events/navigate-stylist-account-profile)
                                  :data-test "account-settings")
                           "Settings")
      (ui/underline-button (assoc (utils/route-to events/navigate-stylist-share-your-store)
                                  :data-test "share-your-store")
                           "Share your store"))
     (marquee-row
      (ui/underline-button (assoc (utils/route-to events/navigate-v2-stylist-dashboard-orders)
                                  :data-test "dashboard")
                           "Dashboard")
      (when show-community?
        (ui/underline-button community/community-url
                             "Community")))]]))

(def ^:private user-actions
  (component/html
   (marquee-row
    (ui/underline-button (assoc (utils/route-to events/navigate-account-manage)
                                :data-test "account-settings")
                         "Account")
    (ui/underline-button (utils/route-to events/navigate-account-referrals)
                         "Refer a friend"))))

(def ^:private guest-actions
  (component/html
   (marquee-row
    (ui/underline-button (assoc (utils/route-to events/navigate-sign-in)
                                :data-test "sign-in")
                         "Sign in")
    [:div.h6.col-12.center
     [:div "No account?"]
     [:a.inherit-color.underline
      (assoc (utils/route-to events/navigate-sign-up)
             :data-test "sign-up")
      "Sign up now, get offers!"]])))

(defn ^:private actions-marquee
  [signed-in vouchers? show-community?]
  (case (-> signed-in ::auth/as)
    :stylist (stylist-actions vouchers? show-community?)
    :user    user-actions
    :guest   guest-actions))

(defn ^:private menu-row [{:keys [link-attrs data-test content]}]
  [:li {:key data-test}
   [:div.h4.border-bottom.border-gray.py3
    (into [:a.block.inherit-color.flex.items-center (assoc link-attrs :data-test data-test)] content)]])

(defn shopping-rows
  [{:keys [show-freeinstall-link? show-bundle-sets-and-hide-deals? site]}]
  (let [^:inline caret (ui/forward-caret {:width 16 :height 16 :color "gray"})]
    (concat
     (when show-freeinstall-link?
       [{:link-attrs (utils/route-to events/navigate-adventure-match-stylist)
         :data-test  "menu-shop-freeinstall"
         :content    [[:span.p-color.pr1 "NEW"]
                      [:span.medium "Get a Mayvenn Install"]]}])

     (when-not show-bundle-sets-and-hide-deals?
       [{:link-attrs (utils/route-to events/navigate-shop-by-look {:album-keyword :deals})
         :data-test  "menu-shop-by-deals"
         :content    [[:span.medium "Deals"]]}])

     (if (= :classic site)
       [{:link-attrs (utils/route-to events/navigate-shop-by-look {:album-keyword :look})
         :data-test  "menu-shop-by-look"
         :content    [[:span.medium "Shop Looks"]]}]
       [{:link-attrs (utils/fake-href events/menu-list
                                      {:menu-type :shop-looks})
         :data-test  "menu-shop-by-look"
         :content    [[:span.medium.flex-auto "Shop Looks"]
                      caret]}])

     (when show-bundle-sets-and-hide-deals?
       [{:link-attrs (utils/fake-href events/menu-list {:menu-type :shop-bundle-sets})
         :data-test  "menu-shop-by-bundle-sets"
         :content    [[:span.medium.flex-auto "Shop Bundle Sets"]
                      caret]}])

     [{:link-attrs (utils/fake-href events/menu-list
                                    {:page/slug           "virgin-hair"
                                     :catalog/category-id "15"})
       :data-test  "menu-shop-virgin-hair"
       :content    [[:span.medium.flex-auto "Virgin Hair"]
                    caret]}

      {:link-attrs (utils/route-to events/navigate-category
                                   {:page/slug           "dyed-virgin-hair"
                                    :catalog/category-id "16"})
       :data-test  "menu-shop-dyed-virgin-hair"
       :content    [[:span.medium.flex-auto "Dyed Virgin Hair"]]}
      {:link-attrs (utils/fake-href events/menu-list
                                    {:page/slug           "closures-and-frontals"
                                     :catalog/category-id "12"})
       :data-test "menu-shop-closures"
       :content [[:span.medium.flex-auto "Closures & Frontals"]
                 caret]}
      {:link-attrs (utils/route-to events/navigate-category
                                   {:page/slug           "wigs"
                                    :catalog/category-id "13"})
       :data-test "menu-shop-wigs"
       :content [[:span.p-color.pr1 "NEW"]
                 [:span.medium.flex-auto "Wigs"]]}
      {:link-attrs (utils/route-to events/navigate-category
                                   {:page/slug           "seamless-clip-ins"
                                    :catalog/category-id "21"})
       :data-test "menu-shop-seamless-clip-ins"
       :content [[:span.medium.flex-auto "Clip-Ins"]]}
      {:link-attrs (utils/route-to events/navigate-product-details
                                   {:page/slug          "50g-straight-tape-ins"
                                    :catalog/product-id "111"})
       :data-test "menu-shop-tape-ins"
       :content [[:span.medium.flex-auto "Tape-Ins"]]}])))

(def stylist-exclusive-row
  {:link-attrs (utils/route-to events/navigate-product-details
                               {:page/slug          "rings-kits"
                                :catalog/product-id "49"})
   :data-test "menu-stylist-products"
   :content [[:span.medium.flex-auto "Stylist Exclusives"]]})

(defn content-rows [{:keys [blog?]}]
  [{:link-attrs (utils/route-to events/navigate-content-guarantee)
    :data-test  "content-guarantee"
    :content    ["Our Guarantee"]}
   {:link-attrs (utils/route-to events/navigate-content-our-hair)
    :data-test  "content-our-hair"
    :content    ["Our Hair"]}
   (if blog?
     {:link-attrs {:href new-blog-url}
      :data-test  "content-blog"
      :content    ["Blog"]}
     {:link-attrs {:href blog-url}
      :data-test  "content-blog"
      :content    ["Real Beautiful blog"]})
   {:link-attrs (utils/route-to events/navigate-content-about-us)
    :data-test  "content-about-us"
    :content    ["About Us"]}
   {:link-attrs {:href "https://jobs.mayvenn.com"}
    :data-test  "content-jobs"
    :content    ["Careers"]}
   {:link-attrs (utils/route-to events/navigate-content-help)
    :data-test  "content-help"
    :content    ["Contact Us"]}])

(defn ^:private menu-area [{:keys [signed-in] :as data}]
  [:ul.list-reset.mb3
   (for [row (shopping-rows data)]
     (menu-row row))
   (when (-> signed-in ::auth/as (= :stylist))
     (menu-row stylist-exclusive-row))
   (for [row (content-rows data)]
     (menu-row row))])

(def ^:private sign-out-area
  (component/html
   (marquee-row
    (ui/ghost-button (assoc (utils/fake-href events/control-sign-out)
                            :data-test "sign-out")
                     "Sign out")
    [:div])))

(def ^:private gallery-link
  (component/html
   [:a.inherit-color.h6.underline
    (utils/route-to events/navigate-gallery-edit)
    "Edit Gallery"]))

(defcomponent ^:private root-menu
  [{:keys [user signed-in show-community? vouchers?] :as data} owner opts]
  [:div
   [:div.px6.border-bottom.border-gray.bg-cool-gray.pt3
    (when (auth/stylist? signed-in)
      [:div.flex.items-center (stylist-portrait user) gallery-link])
    (account-info-marquee signed-in user)
    [:div.my3
     (actions-marquee signed-in vouchers? show-community?)]]
   [:div.px6
    (menu-area data)]
   (when (-> signed-in ::auth/at-all)
     [:div.px6.border-top.border-gray
      sign-out-area])])

(defcomponent component
  [{:keys [cart on-taxon? menu-data] :as data}
   owner
   opts]
  [:div
   [:div.top-0.sticky.z4
    (burger-header cart)]
   (if on-taxon?
     (component/build menu/component menu-data nil)
     (component/build root-menu data nil))])

(defn determine-site
  [app-state]
  (cond
    (= "mayvenn-classic" (get-in app-state keypaths/store-experience)) :classic
    (= "aladdin" (get-in app-state keypaths/store-experience))         :aladdin
    (= "shop" (get-in app-state keypaths/store-slug))                  :shop))

(defn basic-query [data]
  (let [{:keys [match-eligible] :as store} (marquee/query data)
        shop?                              (= "shop" (get-in data keypaths/store-slug))
        aladdin?                           (experiments/aladdin-experience? data)]
    {:signed-in                        (auth/signed-in data)
     :on-taxon?                        (get-in data keypaths/current-traverse-nav)
     :user                             {:stylist-portrait (get-in data keypaths/user-stylist-portrait)
                                        :email            (get-in data keypaths/user-email)}
     :store                            store
     :show-community?                  (and (not match-eligible)
                                            (stylists/own-store? data))
     :show-bundle-sets-and-hide-deals? (or aladdin? shop?)
     :vouchers?                        (experiments/dashboard-with-vouchers? data)
     :show-freeinstall-link?           shop?
     :blog?                            (experiments/blog? data)
     :site                             (determine-site data)}))

(defn query [data]
  (-> (basic-query data)
      (assoc-in [:user :store-credit] (get-in data keypaths/user-total-available-store-credit))
      (assoc-in [:cart :quantity]  (orders/product-quantity (get-in data keypaths/order)))
      (assoc-in [:menu-data] (case (get-in data keypaths/current-traverse-nav-menu-type)
                               :category         (menu/category-query data)
                               :shop-looks       (menu/shop-looks-query data)
                               :shop-bundle-sets (menu/shop-bundle-sets-query data)
                               nil))))

(defn built-component [data opts]
  (component/build component (query data) nil))
