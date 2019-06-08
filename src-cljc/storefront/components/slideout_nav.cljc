(ns storefront.components.slideout-nav
  (:require [catalog.menu :as menu]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.stylists :as stylists]
            [storefront.community :as community]
            [storefront.component :as component]
            [storefront.components.marquee :as marquee]
            [storefront.components.money-formatters :refer [as-money]]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]))

(def blog-url "https://blog.mayvenn.com")

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

(def ^:private social-link :a.inherit-color.h6.underline)

(def ^:private gallery-link
  (component/html
   [social-link
    (utils/route-to events/navigate-gallery)
    "View gallery"]))

(defn ^:private instagram-link [instagram-account]
  [social-link
   {:href (marquee/instagram-url instagram-account)}
   "Follow"])

(defn ^:private styleseat-link [styleseat-account]
  [social-link
   {:href (marquee/styleseat-url styleseat-account)}
   "Book"])

(defn ^:private account-info-marquee [signed-in {:keys [email store-credit]}]
  (when (-> signed-in ::auth/at-all)
    [:div.my3
     [:div.h8.medium "Signed in with:"]
     [:a.teal.h5.bold
      (merge
       {:data-test "signed-in-as"}
       (utils/route-to (if (-> signed-in ::auth/as (= :stylist))
                         events/navigate-stylist-account-profile
                         events/navigate-account-manage)))
      email]
     (when (pos? store-credit)
       [:div
        [:div.h8.medium "Store credit:"]
        [:div.teal.h5.bold (as-money store-credit)]])]))

(defn ^:private stylist-actions
  [vouchers? show-community?]
  (component/html
   [:div
    (when vouchers?
      (ui/teal-button (assoc (utils/route-to events/navigate-voucher-redeem)
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
                         "Account settings")
    (ui/underline-button (utils/route-to events/navigate-account-referrals)
                         "Refer a friend"))))

(def ^:private guest-actions
  (component/html
   (marquee-row
    (ui/underline-button (assoc (utils/route-to events/navigate-sign-in)
                            :data-test "sign-in")
                     "Sign in")
    [:div.h6.col-12.center.dark-gray
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

(defn shopping-rows [{:keys [show-freeinstall-link? v2-experience?]}]
  (let [caret (ui/forward-caret {:width  "23px"
                                 :height "20px"})]
    (concat
     (when show-freeinstall-link?
       [{:link-attrs (utils/fake-href events/initiate-redirect-freeinstall-from-menu {:utm-source "shopHamburger"})
         :data-test  "menu-shop-freeinstall"
         :content    [[:span.teal.pr1 "NEW"]
                      [:span.medium "Get a Free Install"]]}])

     (when-not v2-experience?
       [{:link-attrs (utils/route-to events/navigate-shop-by-look {:album-keyword :deals})
         :data-test  "menu-shop-by-deals"
         :content    [[:span.medium "Deals"]]}])

     [{:link-attrs (utils/route-to events/navigate-shop-by-look {:album-keyword :look})
       :data-test "menu-shop-by-look"
       :content [[:span.medium "Shop Looks"]]}

      {:link-attrs (utils/fake-href events/menu-list
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
       :content [[:span.teal.pr1 "NEW"]
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

(def content-rows
  [{:link-attrs (utils/route-to events/navigate-content-guarantee)
    :data-test  "content-guarantee"
    :content    ["Our Guarantee"]}
   {:link-attrs (utils/route-to events/navigate-content-our-hair)
    :data-test  "content-our-hair"
    :content    ["Our Hair"]}
   {:link-attrs {:href blog-url}
    :data-test  "content-blog"
    :content    ["Real Beautiful blog"]}
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
   (for [row content-rows]
     (menu-row row))])

(def ^:private sign-out-area
  (component/html
   (marquee-row
    (ui/ghost-button (assoc (utils/fake-href events/control-sign-out)
                            :data-test "sign-out")
                     "Sign out")
    [:div])))

(defn ^:private root-menu
  [{:keys [user signed-in show-community? vouchers?] :as data} owner opts]
  (component/create
   [:div
    [:div.px6.border-bottom.border-gray.bg-light-gray.pt3
     (account-info-marquee signed-in user)
     [:div.my3.dark-gray
      (actions-marquee signed-in vouchers? show-community?)]]
    [:div.px6
     (menu-area data)]
    (when (-> signed-in ::auth/at-all)
      [:div.px6.border-top.border-gray
       sign-out-area])]))

(defn component
  [{:keys [cart on-taxon? menu-data] :as data}
   owner
   opts]
  (component/create
   [:div
    [:div.top-0.sticky.z4
     (burger-header cart)]
    (if on-taxon?
      (component/build menu/component menu-data nil)
      (component/build root-menu data nil))]))

(defn basic-query [data]
  (let [shop?                              (= "shop" (get-in data keypaths/store-slug))
        {:keys [match-eligible] :as store} (marquee/query data)]
    {:signed-in              (auth/signed-in data)
     :on-taxon?              (get-in data keypaths/current-traverse-nav-id)
     :user                   {:email (get-in data keypaths/user-email)}
     :store                  store
     :show-community?        (and (not match-eligible)
                                  (stylists/own-store? data))
     :vouchers?              (experiments/vouchers? data)
     :v2-experience?         (experiments/v2-experience? data)
     :shop-homepage?         (and shop?
                                  (= events/navigate-home
                                     (get-in data keypaths/navigation-event)))
     :show-freeinstall-link? shop?
     :shopping               {:categories (get-in data keypaths/categories)}}))

(defn query [data]
  (-> (basic-query data)
      (assoc-in [:user :store-credit] (get-in data keypaths/user-total-available-store-credit))
      (assoc-in [:cart :quantity]  (orders/product-quantity (get-in data keypaths/order)))
      (assoc-in [:menu-data] (menu/query data))))

(defn built-component [data opts]
  (component/build component (query data) nil))
