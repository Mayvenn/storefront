(ns storefront.components.slideout-nav
  (:require [catalog.menu :as menu]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.stylists :as stylists]
            [storefront.assets :as assets]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.marquee :as marquee]
            [storefront.components.header :as header]
            [storefront.components.money-formatters :refer [as-money]]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [ui.promo-banner :as promo-banner]
            [catalog.categories :as categories]))

(defn burger-header [cart]
  (component/html
   (header/mobile-nav-header
    {:class "border-bottom border-gray bg-white black"
     :style {:height "70px"}}
    ;; HACKY(jeff): b/c of relative+absolute position of big-x, padding-left also increases y-offset, so we use negative margin to correct it
    (component/html
     [:div.mtn2.pl4
      {:style {:width  "100%"
               :height "100%"}}
      (ui/big-x {:data-test "close-slideout"
                 :attrs     {:on-click #(messages/handle-message events/control-menu-collapse-all)}})])
    (ui/clickable-logo {:event     events/navigate-home
                        :data-test "header-logo"
                        :height    "29px"})
    (ui/shopping-bag {:style     {:height "70px" :width "80px"}
                      :data-test "mobile-cart"}
                     cart))))

(defn ^:private marquee-col [content]
  (component/html
   [:div.flex-auto
    {:style {:flex-basis 0}}
    content]))

(defn ^:private marquee-row [left-content right-content]
  (component/html
   [:div.flex.my3
    (marquee-col left-content)
    [:div.pr3]
    (marquee-col right-content)]))

(defn ^:private stylist-portrait [{:keys [stylist-portrait]}]
  (component/html
   (let [header-image-size 40
         portrait-status   (:status stylist-portrait)]
     (if (#{"approved" "pending"} portrait-status)
       (ui/circle-picture {:class "mr2 flex items-center"
                           :width (str header-image-size "px")}
                          (ui/square-image stylist-portrait header-image-size))
       [:a.mr2.flex.items-center (utils/route-to events/navigate-stylist-account-profile)
        (ui/ucare-img {:width           header-image-size
                       :picture-classes "flex"}
                      "81bd063f-56ba-4e9c-9aef-19a1207fd422")]))))

(defn ^:private account-info-marquee [signed-in {:keys [email store-credit]}]
  (component/html
   (when (-> signed-in ::auth/at-all)
     [:div.my3.flex.flex-wrap
      (when false #_(pos? store-credit)
            [:div.mr4.mb2
             [:div.title-3.proxima.shout "Credit"]
             [:div.content-2.proxima (as-money store-credit)]])
      [:div
       [:div.title-3.proxima.shout "Signed in with"]
       [:a.inherit-color.content-2.proxima
        (merge
         {:data-test "signed-in-as"}
         (utils/route-to (if (-> signed-in ::auth/as (= :stylist))
                           events/navigate-stylist-account-profile
                           events/navigate-account-manage)))
        email]]])))

(defn ^:private stylist-actions
  [vouchers?]
  (component/html
   [:div
    (when vouchers?
      (ui/button-medium-primary (assoc (utils/route-to events/navigate-voucher-redeem)
                                       :data-test    "redeem-voucher"
                                       :class "mb2")
                                "Redeem Client Voucher"))
    [:div.flex.flex-wrap
     (ui/button-small-secondary (assoc (utils/route-to events/navigate-stylist-account-profile)
                                       :data-test "account-settings"
                                       :class "mr2 mt2")
                                "Settings")
     (ui/button-small-secondary (assoc (utils/route-to events/navigate-stylist-share-your-store)
                                       :data-test "share-your-store"
                                       :class "mr2 mt2")
                                "Share your store")
     (ui/button-small-secondary (assoc (utils/route-to events/navigate-v2-stylist-dashboard-orders)
                                       :data-test "dashboard"
                                       :class "mr2 mt2")
                                "Dashboard")]]))

(def ^:private user-actions
  (component/html
   (marquee-row
    (ui/button-large-secondary (assoc (utils/route-to events/navigate-account-manage)
                                      :data-test "account-settings")
                               "Account")
    (ui/button-large-secondary (utils/route-to events/navigate-account-referrals)
                               "Refer a friend"))))

(def ^:private guest-actions
  (component/html
   [:div.flex.items-center.justify-between
    [:div.col-3
     (ui/button-small-secondary (assoc (utils/route-to events/navigate-sign-in)
                                       :data-test "sign-in")
                                "Sign in")]
    [:div.col-8
     (ui/button-small-underline-primary
      (assoc (utils/route-to events/navigate-sign-up)
             :data-test "sign-up")
      "Or sign up now, get offers!")]]))

(defn ^:private actions-marquee
  [signed-in vouchers?]
  (case (-> signed-in ::auth/as)
    :stylist (stylist-actions vouchers?)
    :user    user-actions
    :guest   guest-actions))

(defn ^:private menu-row [{:keys [link-attrs data-test new-content content]}]
  (component/html
   [:li {:key data-test}
    [:div.py3
     (into [:a.block.inherit-color.flex.items-center.content-1.proxima
            (assoc link-attrs :data-test data-test)
            [:span.col-2.title-3.proxima.center (when-let [c new-content] c)]]
           content)]]))

(defn ^:private content-row [{:keys [link-attrs data-test content]}]
  (component/html
   [:li {:key data-test}
    [:div.py3
     (into [:a.block.inherit-color.flex.items-center.content-2.proxima
            (assoc link-attrs :data-test data-test)
            [:span.col-2]]
           content)]]))

(defn ^:private caretize-content
  [content]
  (component/html
   [:div.col-8.flex.justify-between.items-center
    [:span.medium.flex-auto content]
    ^:inline (ui/forward-caret {:width  16
                                :height 16})]))

(defn shopping-rows
  [{:keys [show-freeinstall-link? show-bundle-sets? site] :as data}]
  (concat
   (when show-freeinstall-link?
     [{:link-attrs  (utils/route-to events/navigate-adventure-match-stylist)
       :data-test   "menu-shop-freeinstall"
       :new-content "NEW"
       :content     [[:span.medium "Get a Mayvenn Install"]]}])

   (when (experiments/service-category-page? data)
     [{:link-attrs  (utils/route-to events/navigate-category
                                    {:page/slug           "salon-services"
                                     :catalog/category-id "30"})
       :data-test   "menu-shop-salon-services"
       :new-content "NEW"
       :content     [[:span.medium "Shop Salon Services"]]}])
   (if (= :classic site)
     [{:link-attrs (utils/route-to events/navigate-shop-by-look {:album-keyword :look})
       :data-test  "menu-shop-by-look"
       :content    [[:span.medium "Shop By Look"]]}]
     [{:link-attrs (utils/fake-href events/menu-list
                                    {:menu-type :shop-looks})
       :data-test  "menu-shop-by-look"
       :content    [(caretize-content "Shop By Look")]}])

   (when show-bundle-sets?
     [{:link-attrs (utils/fake-href events/menu-list {:menu-type :shop-bundle-sets})
       :data-test  "menu-shop-by-bundle-sets"
       :content    [(caretize-content "Shop Bundle Sets")]}])

   [{:link-attrs (utils/route-to events/navigate-category
                                 {:page/slug           "human-hair-bundles"
                                  :catalog/category-id "27"})
     :data-test  "menu-shop-human-hair-bundles"
     :content    [[:span.medium.flex-auto "Hair Bundles"]]}
    {:link-attrs (utils/route-to events/navigate-category
                                 {:page/slug           "virgin-closures"
                                  :catalog/category-id "0"})
     :data-test  "menu-shop-virgin-closures"
     :content    [[:span.medium.flex-auto "Closures"]]}
    {:link-attrs (utils/route-to events/navigate-category
                                 {:page/slug           "virgin-frontals"
                                  :catalog/category-id "1"})
     :data-test  "menu-shop-virgin-frontals"
     :content    [[:span.medium.flex-auto "Frontals"]]}
    {:link-attrs  (utils/route-to events/navigate-category
                                  {:page/slug           "wigs"
                                   :catalog/category-id "13"})
     :data-test   "menu-shop-wigs"
     :new-content "NEW"
     :content     [[:span.medium.flex-auto "Wigs"]]}
    {:link-attrs  (utils/route-to events/navigate-category
                                  {:page/slug           "hair-extensions"
                                   :catalog/category-id "28"})
     :data-test   "menu-shop-hair-extensions"
     :content     [[:span.medium.flex-auto "Hair Extensions"]]}]))

(def stylist-exclusive-row
  {:link-attrs (utils/route-to events/navigate-product-details
                               {:page/slug          "rings-kits"
                                :catalog/product-id "49"
                                :query-params       {:SKU (:direct-to-details/sku-id categories/the-only-stylist-exclusive)}})
   :data-test  "menu-stylist-products"
   :content    [[:span.medium.flex-auto "Stylist Exclusives"]]})

(defn content-rows [_]
  [{:link-attrs (utils/route-to events/navigate-content-guarantee)
    :data-test  "content-guarantee"
    :content    ["Our Guarantee"]}
   {:link-attrs (utils/route-to events/navigate-content-our-hair)
    :data-test  "content-our-hair"
    :content    ["Our Hair"]}
   {:link-attrs {:href header/blog-url}
    :data-test  "content-blog"
    :content    ["Blog"]}
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
  (component/html
   [:ul.list-reset.mb3.mt5
    (for [row (shopping-rows data)]
      (menu-row row))
    (when (-> signed-in ::auth/as (= :stylist))
      (menu-row stylist-exclusive-row))
    [:div.mt5
     (for [[i row] (map-indexed vector (content-rows data))]
       [:div
        {:key (str i)}
        (when-not (zero? i)
          [:div.border-bottom.border-cool-gray.col-8.m-auto])
        (content-row row)])]]))

(def ^:private sign-out-area
  (component/html
   (marquee-row
    (ui/button-large-secondary (assoc (utils/fake-href events/control-sign-out)
                                      :data-test "sign-out")
                               "Sign out")
    [:div])))

(def ^:private gallery-link
  (component/html
   (ui/button-small-underline-primary
    (utils/route-to events/navigate-gallery-edit)
    "Edit Gallery")))

(defcomponent ^:private root-menu
  [{:keys [user signed-in vouchers?] :as data} owner opts]
  [:div
   [:div.bg-cool-gray.p4
    (when (auth/stylist? signed-in)
      [:div.flex.items-center (stylist-portrait user) gallery-link])
    (account-info-marquee signed-in user)
    [:div.my3
     (actions-marquee signed-in vouchers?)]]
   [:div.px3
    (menu-area data)]
   (when (-> signed-in ::auth/at-all)
     [:div.px6.border-top.border-gray
      sign-out-area])])

(defcomponent component
  [{:keys [cart on-taxon? menu-data promo-banner] :as data}
   owner
   opts]
  [:div
   (promo-banner/static-organism promo-banner nil nil)
   [:div.top-0.sticky.z4
    (burger-header cart)]
   (if on-taxon?
     (component/build menu/component menu-data nil)
     (component/build root-menu data nil))])

(defn query [data]
  (-> (header/basic-query data)
      (assoc-in [:user :store-credit] (get-in data keypaths/user-total-available-store-credit))
      (assoc-in [:cart :quantity] (orders/displayed-cart-count (get-in data keypaths/order)))
      (assoc-in [:menu-data] (case (get-in data keypaths/current-traverse-nav-menu-type)
                               :category         (menu/category-query data)
                               :shop-looks       (menu/shop-looks-query data)
                               :shop-bundle-sets (menu/shop-bundle-sets-query data)
                               nil))))

(defn built-component [data opts]
  (component/build component (query data) nil))
