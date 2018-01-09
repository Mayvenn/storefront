(ns storefront.components.slideout-nav
  (:require [catalog.menu :as menu]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.stylists :as stylists]
            [storefront.assets :as assets]
            #?(:clj  [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.components.marquee :as marquee]
            [storefront.components.money-formatters :refer [as-money]]
            [storefront.components.promotion-banner :as promotion-banner]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.experiments :as experiments]
            [spice.date :as date]))

(def blog-url "https://blog.mayvenn.com")

(defn logo [data-test-value height]
  [:a.block.img-logo.bg-no-repeat.bg-center.bg-contain.teal
   (assoc (utils/route-to events/navigate-home)
          :style {:height height}
          :title "Mayvenn"
          :item-prop "logo"
          :data-test data-test-value
          :content (str "https:" (assets/path "/images/header_logo.svg")))])

(defn ^:private promo-bar [promo-data]
  (component/build promotion-banner/component promo-data nil))

(defn burger-header [cart]
  (component/html
   [:div.bg-white.flex.items-center.border-bottom.border-gray
    (ui/big-x {:data-test "close-slideout"
               :attrs {:on-click #(messages/handle-message events/control-menu-collapse-all)}})
    [:div.flex-auto.py3 (logo "header-logo" "40px")]
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

(defn ^:private store-actions [{:keys [store-nickname] :as store}]
  [:div
   [:div.h7.medium "Welcome to " store-nickname "'s store"]
   [:div.dark-gray
    (interpose " | "
               (marquee/actions store gallery-link instagram-link styleseat-link))]])

(defn ^:private portrait [signed-in {:keys [portrait]}]
  (case (marquee/portrait-status (-> signed-in ::auth/as (= :stylist)) portrait)
    ::marquee/show-what-we-have [:div.left.self-center.pr2 (marquee/stylist-portrait portrait)]
    ::marquee/ask-for-portrait  [:div.left.self-center.pr2 marquee/add-portrait-cta]
    ::marquee/show-nothing      nil))

(defn ^:private store-info-marquee [signed-in store]
  (when (-> signed-in ::auth/to (= :marketplace))
    [:div.my3.flex
     (portrait signed-in store)
     (store-actions store)]))

(defn ^:private account-info-marquee [signed-in {:keys [email store-credit]}]
  (when (-> signed-in ::auth/at-all)
    [:div.my3
     [:div.h7.medium "Signed in with:"]
     [:a.teal.h5
      (merge
       {:data-test "signed-in-as"}
       (utils/route-to (if (-> signed-in ::auth/as (= :stylist))
                         events/navigate-stylist-account-profile
                         events/navigate-account-manage)))
      email]
     (when (pos? store-credit)
       [:p.teal.h5 "You have store credit: " (as-money store-credit)])]))

(def ^:private stylist-actions
  (component/html
   [:div
    (marquee-row
     (ui/ghost-button (assoc (utils/route-to events/navigate-stylist-account-profile)
                             :data-test "account-settings")
                      "Account settings")
     (ui/ghost-button (assoc (utils/route-to events/navigate-stylist-share-your-store)
                             :data-test "share-your-store")
                      "Share your store"))
    (marquee-row
     (ui/ghost-button (assoc (utils/route-to events/navigate-stylist-dashboard-earnings)
                             :data-test "dashboard")
                      "Dashboard")
     (ui/ghost-button stylists/community-url
                      "Community"))]))

(def ^:private user-actions
  (component/html
   (marquee-row
    (ui/ghost-button (assoc (utils/route-to events/navigate-account-manage)
                            :data-test "account-settings")
                     "Account settings")
    (ui/ghost-button (utils/route-to events/navigate-account-referrals)
                     "Refer a friend"))))

(def ^:private guest-actions
  (component/html
   (marquee-row
    (ui/ghost-button (assoc (utils/route-to events/navigate-sign-in)
                            :data-test "sign-in")
                     "Sign in")
    [:div.h6.col-12.center.dark-gray
     [:div "No account?"]
     [:a.inherit-color.underline
      (assoc (utils/route-to events/navigate-sign-up)
             :data-test "sign-up")
      "Sign up now, get offers!"]])))

(defn ^:private actions-marquee [signed-in]
  (case (-> signed-in ::auth/as)
    :stylist stylist-actions
    :user    user-actions
    :guest   guest-actions))

(defn ^:private menu-row [& content]
  [:div.h4.border-bottom.border-gray.py3
   (into [:a.block.inherit-color.flex.items-center] content)])

(defn ^:private shopping-area [signed-in human-hair?]
  [:div
   [:li (menu-row (utils/route-to events/navigate-shop-by-look) [:span.medium "Shop Looks"])]
   [:div
    [:li (menu-row (assoc (utils/fake-href events/menu-list
                                           {:page/slug           "virgin-hair"
                                            :catalog/category-id "15"})
                          :data-test "menu-shop-virgin-hair")
                   [:span.medium.flex-auto "Shop Virgin Hair"]
                   ui/forward-caret)]
    [:li (menu-row (assoc (utils/route-to events/navigate-category
                                          {:page/slug           "dyed-virgin-hair"
                                           :catalog/category-id "16"})
                          :data-test "menu-shop-dyed-virgin-hair")
                   (when-not human-hair?
                     [:span.teal.pr1 "NEW"])
                   [:span.medium.flex-auto "Shop Dyed Virgin Hair"])]
    (when human-hair?
      [:li (menu-row (assoc (utils/route-to events/navigate-category
                                            {:page/slug           "dyed-100-human-hair"
                                             :catalog/category-id "19"})
                            :data-test "menu-shop-dyed-100-human-hair")
                     [:span.teal.pr1 "NEW"]
                     [:span.medium.flex-auto "Shop Dyed 100% Human Hair"])])]
   [:li (menu-row (assoc (utils/fake-href events/menu-list
                                          {:page/slug           "closures-and-frontals"
                                           :catalog/category-id "12"})
                         :data-test "menu-shop-closures")
                  [:span.medium.flex-auto "Shop Closures & Frontals"]
                  ui/forward-caret)]
   [:li (menu-row (assoc (utils/route-to events/navigate-category
                                         {:page/slug           "wigs"
                                          :catalog/category-id "13"})
                         :data-test "menu-shop-wigs")
                  (when-not human-hair?
                    [:span.teal.pr1 "NEW"])
                  [:span.medium.flex-auto "Shop Wigs"])]
   [:li (menu-row (assoc (utils/route-to events/navigate-category
                                         {:page/slug           "seamless-clip-ins"
                                          :catalog/category-id "21"})
                         :data-test "menu-shop-seamless-clip-ins")
                  [:span.teal.pr1 "NEW"]
                  [:span.medium.flex-auto "Shop Clip-ins"])]
   [:li (menu-row (assoc (utils/route-to events/navigate-product-details
                                         {:page/slug           "tape-ins"
                                          :catalog/product-id "111"})
                         :data-test "menu-shop-tape-ins")
                  [:span.teal.pr1 "NEW"]
                  [:span.medium.flex-auto "Shop Tape-Ins"])]
   (when (-> signed-in ::auth/as (= :stylist))
     [:li (menu-row (assoc (utils/route-to events/navigate-product-details
                                           {:page/slug          "rings-kits"
                                            :catalog/product-id "49"})
                           :data-test "menu-stylist-products")
                    [:span.medium.flex-auto "Shop Stylist Exclusives"])])])

(defn ^:private menu-area [signed-in human-hair?]
  [:ul.list-reset.mb3
   (shopping-area signed-in human-hair?)
   [:li (menu-row (assoc (utils/route-to events/navigate-content-guarantee)
                         :data-test "content-guarantee")
                  "Our Guarantee")]
   (when human-hair?
     [:li (menu-row (assoc (utils/route-to events/navigate-content-our-hair)
                           :data-test "content-our-hair")
                    "Our Hair")])
   [:li (menu-row {:href blog-url}
                  "Real Beautiful blog")]
   [:li (menu-row (assoc (utils/route-to events/navigate-content-about-us)
                         :data-test "content-about-us")
                  "About Us")]
   [:li (menu-row {:href "https://jobs.mayvenn.com"}
                  "Careers")]
   [:li (menu-row (assoc (utils/route-to events/navigate-content-help)
                         :data-test "content-help")
                  "Contact Us")]])

(def ^:private sign-out-area
  (component/html
   (marquee-row
    (ui/ghost-button (assoc (utils/fake-href events/control-sign-out)
                            :data-test "sign-out")
                     "Sign out")
    [:div])))

(defn ^:private root-menu [{:keys [user signed-in store human-hair?]} owner opts]
  (component/create
   [:div
    [:div.px6.border-bottom.border-gray
     (store-info-marquee signed-in store)
     (account-info-marquee signed-in user)
     [:div.my3.dark-gray
      (actions-marquee signed-in)]]
    [:div.px6
     (menu-area signed-in human-hair?)]
    (when (-> signed-in ::auth/at-all)
      [:div.px6.border-top.border-gray
       sign-out-area])]))

(defn component
  [{:keys [promo-data cart on-taxon? menu-data] :as data}
   owner
   opts]
  (component/create
   [:div
    [:div.top-0.sticky.z4
     (promo-bar promo-data)
     (burger-header cart)]
    (if on-taxon?
      (component/build menu/component menu-data nil)
      (component/build root-menu data nil))]))

(defn basic-query [data]
  {:signed-in               (auth/signed-in data)
   :human-hair?             (experiments/human-hair? data)
   :on-taxon?               (get-in data keypaths/current-traverse-nav-id)
   :user                    {:email (get-in data keypaths/user-email)}
   :store                   (marquee/query data)
   :shopping                {:categories (get-in data keypaths/categories)}})

(defn query [data]
  (-> (basic-query data)
      (assoc-in [:user :store-credit] (get-in data keypaths/user-total-available-store-credit))
      (assoc-in [:promo-data] (promotion-banner/query data))
      (assoc-in [:cart :quantity]  (orders/product-quantity (get-in data keypaths/order)))
      (assoc-in [:menu-data] (menu/query data))))

(defn built-component [data opts]
  (component/build component (query data) nil))
