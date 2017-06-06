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
            [storefront.assets :as assets]
            [storefront.components.promotion-banner :as promotion-banner]
            [storefront.components.ui :as ui]
            [clojure.set :as set]))

(def signed-in? #{::signed-in-as-user ::signed-in-as-stylist})

(defn promo-bar [promo-data]
  (component/build promotion-banner/component promo-data nil))

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
    (assoc (utils/route-to events/navigate-home)
           :style {:height "40px"}
           :title "Mayvenn"
           :item-prop "logo"
           :data-test "header-logo"
           :content (str "https:" (assets/path "/images/header_logo.svg")))]))

(def burger-header
  (component/html [:div.bg-white menu-x [:div.center.col-12.p3 logo]]))

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

(defn store-actions [{:keys [welcome-message instagram-account styleseat-account gallery?]}]
  [:div
   [:div.h7.bold welcome-message]
   [:div.dark-gray
    (interpose " | " (cond-> []
                       gallery?          (conj (gallery-link "See my gallery"))
                       instagram-account (conj (instagram-link "Follow me" instagram-account))
                       styleseat-account (conj (styleseat-link "Book me" styleseat-account))))]])

(defn store-info-marquee [{:keys [store-slug portrait] :as store}]
  (when-not (#{"shop" "store"} store-slug)
    [:div.my3.flex
     (when portrait
       [:div.left.self-center.pr2
        (stylist-portrait portrait 36)])
     (store-actions store)]))

(defn account-info-marquee [{:keys [email store-credit signed-in-state]}]
  (when (signed-in? signed-in-state)
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
    (ui/ghost-button stylists/community-url
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

(defn menu-row [& content]
  [:div.border-bottom.border-gray
   {:style {:padding "3px 0 2px"}}
   (into [:a.block.py1.h5.inherit-color] content)])

(defn menu-area [shop-sections]
  [:ul.list-reset.mb3
   [:li (menu-row (utils/route-to events/navigate-shop-by-look)
                  "Shop looks")]
   (for [{:keys [title shop-items]} shop-sections]
     [:li {:key title}
      (menu-row title)
      [:ul.list-reset.ml6
       (for [{:keys [name slug]} shop-items]
         [:li {:key slug}
          (menu-row (assoc (utils/route-to events/navigate-category {:named-search-slug slug})
                           :data-test (str "menu-" slug))
                    (when (named-searches/new-named-search? slug) [:span.teal "NEW "])
                    (str/capitalize name))])]])
   [:li (menu-row (assoc (utils/route-to events/navigate-content-guarantee)
                         :data-test "content-guarantee")
                  "Our guarantee")]
   [:li (menu-row {:href "https://blog.mayvenn.com"}
                  "Real Beauty blog")]
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

(defn component [{:keys [user store promo-data shop-sections] :as data} owner opts]
  (component/create
   [:div
    [:div.top-0.sticky.z4.border-bottom.border-gray
     (promo-bar promo-data)
     burger-header]
    [:div.px6.border-bottom.border-gray
     (store-info-marquee store)
     (account-info-marquee user)
     [:div.my3.dark-gray
      (actions-marquee user)]]
    [:div.px6
     (menu-area shop-sections)]
    (when (-> user :signed-in-state signed-in?)
      [:div.px6.border-top.border-gray
       sign-out-area])]))

(defn signed-in-state [data]
  (if (stylists/own-store? data)
    ::signed-in-as-stylist
    (if (get-in data keypaths/user-email)
      ::signed-in-as-user
      ::signed-out)))

(defn query [data]
  (let [signed-in-state (signed-in-state data)
        user            {:email           (get-in data keypaths/user-email)
                         :store-credit    (get-in data keypaths/user-total-available-store-credit)
                         :signed-in-state signed-in-state}
        store           (-> (get-in data keypaths/store)
                            (set/rename-keys {:store_slug        :store-slug
                                              :store_nickname    :store-nickname
                                              :instagram_account :instagram-account
                                              :styleseat_account :styleseat-account})
                            (assoc :gallery? (stylists/gallery? data)))
        named-searches  (named-searches/current-named-searches data)]
    {:promo-data    (promotion-banner/query data)
     :user          user
     :store         (-> store
                        (assoc :welcome-message (if (= ::signed-in-as-stylist signed-in-state)
                                                  (str "Hi, " (:store-slug store) ". Welcome to your shop.")
                                                  (str "Welcome to " (:store-nickname store) "'s shop."))))
     :shop-sections (cond-> [{:title "Shop hair"
                              :shop-items (filter named-searches/is-extension? named-searches)}
                             {:title "Shop closures & frontals"
                              :shop-items (filter named-searches/is-closure-or-frontal? named-searches)}]
                      (= ::signed-in-as-stylist signed-in-state)
                      (conj {:title      "Stylist exclusives"
                             :shop-items (filter named-searches/is-stylist-product? named-searches)}))}))

(defn built-component [data opts]
  (component/build component (query data) nil))
