(ns storefront.components.slideout-nav
  (:require [storefront.platform.component-utils :as utils]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.events :as events]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.platform.messages :as messages]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.accessors.stylists :as stylists]
            [storefront.accessors.auth :as auth]
            [storefront.components.money-formatters :refer [as-money]]
            [storefront.components.marquee :as marquee]
            [clojure.string :as str]
            [storefront.platform.component-utils :as utils]
            [storefront.assets :as assets]
            [storefront.components.promotion-banner :as promotion-banner]
            [storefront.components.ui :as ui]
            [clojure.set :as set]))

(def blog-url "https://blog.mayvenn.com")

(defn promo-bar [promo-data]
  (component/build promotion-banner/component promo-data nil))

(def menu-x
  (component/html
   [:div.absolute {:style {:width "70px"}}
    [:div.relative.rotate-45.p2 {:style     {:height "70px"}
                                 :data-test "close-slideout"
                                 :on-click  #(messages/handle-message events/control-menu-collapse-all)}
     [:div.absolute.border-right.border-dark-gray {:style {:width "25px" :height "50px"}}]
     [:div.absolute.border-bottom.border-dark-gray {:style {:width "50px" :height "25px"}}]]]))

(defn logo [data-test-value height]
  [:a.block.img-logo.bg-no-repeat.bg-center.bg-contain.teal
   (assoc (utils/route-to events/navigate-home)
          :style {:height height}
          :title "Mayvenn"
          :item-prop "logo"
          :data-test data-test-value
          :content (str "https:" (assets/path "/images/header_logo.svg")))])

(def burger-header
  (component/html [:div.bg-white menu-x [:div.center.col-12.p3 (logo "header-logo" "40px")]]))

(defn ^:private marquee-col [content]
  [:div.flex-auto
   {:style {:flex-basis 0}}
   content])

(defn marquee-row [left-content right-content]
  [:div.flex.my3
   (marquee-col left-content)
   [:div.pr3]
   (marquee-col right-content)])

(def social-link :a.inherit-color.h6.underline)

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

(defn store-actions [{:keys [store-nickname] :as store}]
  [:div
   [:div.h7.medium "Welcome to " store-nickname "'s store"]
   [:div.dark-gray
    (interpose " | "
               (marquee/actions store gallery-link instagram-link styleseat-link))]])

(defn portrait [signed-in {:keys [portrait]}]
  (case (marquee/portrait-status (-> signed-in ::auth/as (= :stylist)) portrait)
    ::marquee/show-what-we-have [:div.left.self-center.pr2 (marquee/stylist-portrait portrait)]
    ::marquee/ask-for-portrait  [:div.left.self-center.pr2 marquee/add-portrait-cta]
    ::marquee/show-nothing      nil))

(defn store-info-marquee [signed-in store]
  (when (-> signed-in ::auth/to (= :marketplace))
    [:div.my3.flex
     (portrait signed-in store)
     (store-actions store)]))

(defn account-info-marquee [signed-in {:keys [email store-credit]}]
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

(def stylist-actions
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

(def user-actions
  (component/html
   (marquee-row
    (ui/ghost-button (assoc (utils/route-to events/navigate-account-manage)
                            :data-test "account-settings")
                     "Manage account")
    (ui/ghost-button (utils/route-to events/navigate-account-referrals)
                     "Refer a friend"))))

(def guest-actions
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

(defn actions-marquee [signed-in]
  (case (-> signed-in ::auth/as)
    :stylist stylist-actions
    :user    user-actions
    :guest   guest-actions))

(defn menu-row [& content]
  [:div.border-bottom.border-gray
   {:style {:padding "3px 0 2px"}}
   (into [:a.block.py1.h5.inherit-color] content)])

(defn menu-area [signed-in {:keys [named-searches]}]
  [:ul.list-reset.mb3
   [:li (menu-row (utils/route-to events/navigate-shop-by-look)
                  "Shop looks")]
   (for [{:keys [title items]} (cond-> [{:title "Shop hair"
                                         :items (filter named-searches/is-extension? named-searches)}
                                        {:title "Shop closures & frontals"
                                         :items (filter named-searches/is-closure-or-frontal? named-searches)}]
                                 (-> signed-in ::auth/as (= :stylist))
                                 (conj {:title "Stylist exclusives"
                                        :items (filter named-searches/is-stylist-product? named-searches)}))]
     [:li {:key title}
      (menu-row title)
      [:ul.list-reset.ml6
       (for [{:keys [name slug]} items]
         [:li {:key slug}
          (menu-row (assoc (utils/route-to events/navigate-named-search {:named-search-slug slug})
                           :data-test (str "menu-" slug))
                    (when (named-searches/new-named-search? slug) [:span.teal "NEW "])
                    (str/capitalize name))])]])
   [:li (menu-row (assoc (utils/route-to events/navigate-content-guarantee)
                         :data-test "content-guarantee")
                  "Our guarantee")]
   [:li (menu-row {:href blog-url}
                  "Real Beautiful blog")]
   [:li (menu-row (assoc (utils/route-to events/navigate-content-about-us)
                         :data-test "content-about-us")
                  "About us")]
   [:li (menu-row {:href "https://jobs.mayvenn.com"}
                  "Careers")]
   [:li (menu-row (assoc (utils/route-to events/navigate-content-help)
                         :data-test "content-help")
                  "Contact us")]])

(def sign-out-area
  (component/html
   (marquee-row
    (ui/ghost-button (assoc (utils/fake-href events/control-sign-out)
                            :data-test "sign-out")
                     "Sign out")
    [:div])))

(defn component [{:keys [user store promo-data shopping signed-in] :as data} owner opts]
  (component/create
   [:div
    [:div.top-0.sticky.z4.border-bottom.border-gray
     (promo-bar promo-data)
     burger-header]
    [:div.px6.border-bottom.border-gray
     (store-info-marquee signed-in store)
     (account-info-marquee signed-in user)
     [:div.my3.dark-gray
      (actions-marquee signed-in)]]
    [:div.px6
     (menu-area signed-in shopping)]
    (when (-> signed-in ::auth/at-all)
      [:div.px6.border-top.border-gray
       sign-out-area])]))

(defn basic-query [data]
  {:signed-in (auth/signed-in data)
   :user      {:email (get-in data keypaths/user-email)}
   :store     (marquee/query data)
   :shopping  {:named-searches (named-searches/current-named-searches data)}})

(defn query [data]
  (-> (basic-query data)
      (assoc-in [:user :store-credit] (get-in data keypaths/user-total-available-store-credit))
      (assoc-in [:promo-data] (promotion-banner/query data))))

(defn built-component [data opts]
  (component/build component (query data) nil))
