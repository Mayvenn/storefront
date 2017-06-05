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
            [storefront.components.money-formatters :refer [as-money]]
            [storefront.routes :as routes]
            [clojure.string :as str]
            [storefront.platform.component-utils :as utils]
            [storefront.components.header :as header]
            [storefront.assets :as assets]
            [storefront.components.promotion-banner :as promotion-banner]
            [storefront.components.ui :as ui]
            [clojure.set :as set]))

(def menu-x
  (component/html
   [:div.absolute {:style {:width "70px"}}
    [:div.relative.rotate-45.p2 {:style {:height "70px"}
                                 :on-click #(messages/handle-message events/control-menu-collapse-all)}
     [:div.absolute.border-right.border-dark-gray {:style {:width "25px" :height "50px"}}]
     [:div.absolute.border-bottom.border-dark-gray {:style {:width "50px" :height "25px"}}]]]))

(def logo
  (component/html
   [:a.block.img-logo.bg-no-repeat.bg-center.bg-contain.teal
    (merge {:style {:height "40px"}
            :title "Mayvenn"
            :item-prop "logo"
            :data-test "header-logo"
            :content (str "https:" (assets/path "/images/header_logo.svg"))}
           (utils/route-to events/navigate-home))]))

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

(defn ^:private gallery-link [text]
  [social-link
   (utils/route-to events/navigate-gallery)
   text])

(defn ^:private instagram-link [text instagram-account]
  [social-link
   {:href (str "http://instagram.com/" instagram-account)}
   text])

(defn ^:private styleseat-link [text styleseat-account]
  [social-link
   {:href (str "https://www.styleseat.com/v/" styleseat-account)}
   text])

(defn ^:private stylist-portrait [portrait size]
  (ui/circle-picture {:class "mx-auto"
                      :width (str size "px")}
                     (ui/square-image portrait size)))

(defn store-actions [{:keys [welcome-message store-slug instagram-account styleseat-account portrait gallery?]}]
  [:div
   [:div.h7.bold welcome-message]
   [:div.dark-gray
    (interpose " | " (cond-> []
                       gallery?          (conj (gallery-link "See my gallery"))
                       instagram-account (conj (instagram-link "Follow me" instagram-account))
                       styleseat-account (conj (styleseat-link "Book me" styleseat-account))))]])

(defn store-info-marquee [store]
  [:div.my3.flex
   (when (:portrait store)
     [:div.left.self-center.pr2
      (stylist-portrait (:portrait store) 36)])
   (store-actions store)])

(defn account-info-marquee [{:keys [email store-credit signed-in-state]}]
  (when (#{::signed-in-as-user ::signed-in-as-stylist} signed-in-state)
    [:div.my3
     [:div.h7.bold "Signed in with:"]
     [:a.teal.h5
      (utils/route-to events/navigate-account-manage)
      email]
     (when (pos? store-credit)
       [:p.teal.h5 "You have store credit: " (as-money store-credit)])]))

(defmulti actions-marquee :signed-in-state)
(defmethod actions-marquee ::signed-in-as-stylist [_]
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
    (ui/ghost-button (utils/fake-href events/control-stylist-community)
                     "Community"))])

(defmethod actions-marquee ::signed-in-as-user [_]
  (marquee-row
   (ui/ghost-button (assoc (utils/route-to events/navigate-account-manage)
                           :data-test "account-settings")
                    "Manage account")
   (ui/ghost-button (utils/route-to events/navigate-account-referrals)
                    "Refer a friend")))

(defmethod actions-marquee ::signed-out [_]
  (marquee-row
   (ui/ghost-button (assoc (utils/route-to events/navigate-sign-in)
                           :data-test "sign-in")
                    "Sign in")
   [:div.h6.col-12.center.dark-gray
    [:div "No account?"]
    [:a.inherit-color.underline
     (assoc (utils/route-to events/navigate-sign-up)
            :data-test "sign-up")
     "Sign up now, get offers!"]]))

(defn component [{:keys [user store promo-data] :as data} owner opts]
  (component/create
   (let [store-slug (:store-slug store)]
     [:div
      [:div.top-0.sticky.z4
       (component/build promotion-banner/component promo-data opts)
       [:div.border-bottom.border-gray
        menu-x
        [:div.center.col-12.p3 logo]]]
      [:div.px6.border-bottom.border-gray
       (when-not (#{"shop" "store"} store-slug)
         (store-info-marquee store))
       (account-info-marquee user)
       [:div.my3.dark-gray
        (actions-marquee user)]]
      [:div.px6
       (marquee-row
        (ui/ghost-button (assoc (utils/fake-href events/control-sign-out)
                                :data-test "sign-out")
                         "Sign out")
        [:div])]])))

(defn signed-in-state [data]
  (if (stylists/own-store? data)
    ::signed-in-as-stylist
    (if (get-in data keypaths/user-email)
      ::signed-in-as-user
      ::signed-out)))

(defn query [data]
  (merge
   (let [user  {:email           (get-in data keypaths/user-email)
                :store-credit    (get-in data keypaths/user-total-available-store-credit)
                :signed-in-state (signed-in-state data)}
         store (-> (get-in data keypaths/store)
                   (set/rename-keys {:store_slug        :store-slug
                                     :instagram_account :instagram-account
                                     :styleseat_account :styleseat-account})
                   (assoc :gallery? (stylists/gallery? data)))]
     {:promo-data (promotion-banner/query data)
      :user       user
      :store      (-> store
                      (assoc :welcome-message (if (= ::signed-in-as-stylist (:signed-in-state user))
                                                (str "Hi, " (:store-slug store) ". Welcome to your shop.")
                                                (str "Welcome to " (:store-slug store) "'s shop."))))})))

(defn built-component [data opts]
  (component/build component (query data) nil))
