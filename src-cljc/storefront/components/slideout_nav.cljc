(ns storefront.components.slideout-nav
  (:require [catalog.hamburger-drill-down :as drill-down]
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
            [storefront.accessors.experiments :as experiments]))

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

(def ^:private menu-x
  (component/html
   [:div.absolute {:style {:width "70px"}}
    [:div.relative.rotate-45.p2 {:style     {:height "70px"}
                                 :data-test "close-slideout"
                                 :on-click  #(messages/handle-message events/control-menu-collapse-all)}
     [:div.absolute.border-right.border-dark-gray {:style {:width "25px" :height "50px"}}]
     [:div.absolute.border-bottom.border-dark-gray {:style {:width "50px" :height "25px"}}]]]))

(def ^:private forward-caret
  (component/html
   (svg/dropdown-arrow {:class  "stroke-black"
                        :width  "23px"
                        :height "20px"
                        :style  {:transform "rotate(-90deg)"}})))

(def ^:private burger-header
  (component/html
   [:div.bg-white
    menu-x
    [:div.center.col-12.p3 (logo "header-logo" "40px")]]))

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
      (utils/route-to (if (-> signed-in ::auth/as (= :stylist))
                        events/navigate-stylist-account-profile
                        events/navigate-account-manage))
      email]
     (when (pos? store-credit)
       [:p.teal.h5 "You have store credit: " (as-money store-credit)])]))

(def ^:private stylist-actions
  (component/html
   [:div
    (marquee-row
     (ui/ghost-button (assoc (utils/route-to events/navigate-stylist-account-profile)
                             :data-test "account-settings")
                      "Manage account")
     (ui/ghost-button (assoc (utils/route-to events/navigate-stylist-share-your-store)
                             :data-test "share-your-store")
                      "Share your store"))
    (marquee-row
     (ui/ghost-button (assoc (utils/route-to events/navigate-stylist-dashboard-commissions)
                             :data-test "dashboard")
                      "Dashboard")
     (ui/ghost-button stylists/community-url
                      "Community"))]))

(def ^:private user-actions
  (component/html
   (marquee-row
    (ui/ghost-button (assoc (utils/route-to events/navigate-account-manage)
                            :data-test "account-settings")
                     "Manage account")
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

(defn ^:private minor-menu-row [& content]
  [:div.border-bottom.border-gray
   {:style {:padding "3px 0 2px"}}
   (into [:a.block.py1.h5.inherit-color.flex.items-center] content)])

(defn ^:private major-menu-row [& content]
  [:div.h4.border-bottom.border-gray.py3
   (into [:a.block.inherit-color.flex.items-center] content)])

(defn ^:private shopping-area [signed-in bundle-deals?]
  [:div
   (when bundle-deals?
     [:li (major-menu-row (utils/route-to events/navigate-shop-bundle-deals) [:span.medium "Shop Bundle Deals"])])
   [:li (major-menu-row (utils/route-to events/navigate-shop-by-look) [:span.medium "Shop Looks"])]
   [:li (major-menu-row (assoc (utils/fake-href events/menu-traverse-descend
                                                {:page/slug           "bundles"
                                                 :catalog/category-id "11"})
                               :data-test "menu-shop-bundles")
                        [:span.medium.flex-auto "Shop Hair"]
                        forward-caret)]
   [:li (major-menu-row (assoc (utils/fake-href events/menu-traverse-descend
                                                {:page/slug           "closures-and-frontals"
                                                 :catalog/category-id "12"})
                               :data-test "menu-shop-closures")
                        [:span.medium.flex-auto "Shop Closures & Frontals"]
                        forward-caret)]
   [:li (major-menu-row (assoc (utils/route-to events/navigate-category
                                                {:page/slug           "wigs"
                                                 :catalog/category-id "13"})
                               :data-test "menu-shop-wigs")
                        [:span.teal.pr1 "NEW"]
                        [:span.medium.flex-auto "Shop Wigs"])]
   (when (-> signed-in ::auth/as (= :stylist))
     [:li (major-menu-row (assoc (utils/route-to events/navigate-product-details
                                                 {:page/slug          "rings-kit"
                                                  :catalog/product-id "49"})
                                 :data-test "menu-stylist-products")
                          [:span.medium.flex-auto "Shop Stylist Exclusives"])])])

(defn ^:private menu-area [signed-in bundle-deals?]
  [:ul.list-reset.mb3
   (shopping-area signed-in bundle-deals?)
   [:li (minor-menu-row (assoc (utils/route-to events/navigate-content-guarantee)
                               :data-test "content-guarantee")
                        "Our guarantee")]
   [:li (minor-menu-row {:href blog-url}
                        "Real Beautiful blog")]
   [:li (minor-menu-row (assoc (utils/route-to events/navigate-content-about-us)
                               :data-test "content-about-us")
                        "About us")]
   [:li (minor-menu-row {:href "https://jobs.mayvenn.com"}
                        "Careers")]
   [:li (minor-menu-row (assoc (utils/route-to events/navigate-content-help)
                               :data-test "content-help")
                        "Contact us")]])

(def ^:private sign-out-area
  (component/html
   (marquee-row
    (ui/ghost-button (assoc (utils/fake-href events/control-sign-out)
                            :data-test "sign-out")
                     "Sign out")
    [:div])))

(defn ^:private root-menu [{:keys [signed-in store user shopping bundle-deals?]} owner opts]
  (component/create
   [:div
    [:div.px6.border-bottom.border-top.border-gray
     (store-info-marquee signed-in store)
     (account-info-marquee signed-in user)
     [:div.my3.dark-gray
      (actions-marquee signed-in)]]
    [:div.px6
     (menu-area signed-in bundle-deals?)]
    (when (-> signed-in ::auth/at-all)
      [:div.px6.border-top.border-gray
       sign-out-area])]))

(defn component
  [{:keys [user store promo-data shopping signed-in on-taxon? drill-down-data] :as data}
   owner
   opts]
  (component/create
   [:div
    [:div.top-0.sticky.z4.border-gray
     (promo-bar promo-data)
     burger-header]
    (if on-taxon?
      (component/build drill-down/component drill-down-data nil)
      (component/build root-menu data nil))]))

(defn basic-query [data]
  {:signed-in     (auth/signed-in data)
   :on-taxon?     (get-in data keypaths/current-traverse-nav-id)
   :bundle-deals? (experiments/bundle-deals? data)
   :user          {:email (get-in data keypaths/user-email)}
   :store         (marquee/query data)
   :shopping      {:categories (get-in data keypaths/categories)}})

(defn query [data]
  (-> (basic-query data)
      (assoc-in [:user :store-credit] (get-in data keypaths/user-total-available-store-credit))
      (assoc-in [:promo-data] (promotion-banner/query data))
      (assoc-in [:drill-down-data] (drill-down/query data))))

(defn built-component [data opts]
  (component/build component (query data) nil))
