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

(def blog-url "https://blog.mayvenn.com")

(defn instagram-url [instagram-account]
  (str "http://instagram.com/" instagram-account))

(defn styleseat-url [styleseat-account]
  (str "https://www.styleseat.com/v/" styleseat-account))

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
   {:href (instagram-url instagram-account)}
   "Follow"])

(defn ^:private styleseat-link [styleseat-account]
  [social-link
   {:href (styleseat-url styleseat-account)}
   "Book"])

(def header-image-size 36)

(defn stylist-portrait [portrait]
  (ui/circle-picture {:class "mx-auto"
                      :width (str header-image-size "px")}
                     (ui/square-image portrait header-image-size)))

(def add-portrait-cta
  (component/html
   [:a (utils/route-to events/navigate-stylist-account-profile)
    [:img {:width (str header-image-size "px")
           :src   (assets/path "/images/icons/stylist-bug-no-pic-fallback.png")}]]))

(defn store-actions [{:keys [store-nickname instagram-account styleseat-account gallery?]}]
  [:div
   [:div.h7.medium "Welcome to " store-nickname "'s store"]
   [:div.dark-gray
    (interpose " | " (cond-> []
                       gallery?          (conj gallery-link)
                       instagram-account (conj (instagram-link instagram-account))
                       styleseat-account (conj (styleseat-link styleseat-account))))]])

(defn portrait-status [signed-in portrait]
  (let [stylist? (-> signed-in ::as (= ::stylist))
        status   (:status portrait)]
    (cond
      (or (= "approved" status)
          (and (= "pending" status)
               stylist?))
      ::show-what-we-have

      stylist?
      ::ask-for-portrait

      :else
      ::show-nothing)))

(defn store-info-marquee [signed-in {:keys [store-slug portrait] :as store}]
  (when (-> signed-in ::to (= ::marketplace))
    [:div.my3.flex
     (case (portrait-status signed-in portrait)
       ::show-what-we-have [:div.left.self-center.pr2 (stylist-portrait portrait)]
       ::ask-for-portrait  [:div.left.self-center.pr2 add-portrait-cta]
       ::show-nothing      nil)
     (store-actions store)]))

(defn account-info-marquee [signed-in {:keys [email store-credit]}]
  (when (-> signed-in ::at-all)
    [:div.my3
     [:div.h7.medium "Signed in with:"]
     [:a.teal.h5
      (utils/route-to events/navigate-account-manage)
      email]
     (when (pos? store-credit)
       [:p.teal.h5 "You have store credit: " (as-money store-credit)])]))

(defmulti actions-marquee ::as)
(defmethod actions-marquee ::stylist [_]
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

(defmethod actions-marquee ::user [_]
  (marquee-row
   (ui/ghost-button (assoc (utils/route-to events/navigate-account-manage)
                           :data-test "account-settings")
                    "Manage account")
   (ui/ghost-button (utils/route-to events/navigate-account-referrals)
                    "Refer a friend")))

(defmethod actions-marquee ::guest [_]
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

(defn menu-area [shopping]
  [:ul.list-reset.mb3
   [:li (menu-row (utils/route-to events/navigate-shop-by-look)
                  "Shop looks")]
   (for [{:keys [title items]} (:sections shopping)]
     [:li {:key title}
      (menu-row title)
      [:ul.list-reset.ml6
       (for [{:keys [name slug]} items]
         [:li {:key slug}
          (menu-row (assoc (utils/route-to events/navigate-category {:named-search-slug slug})
                           :data-test (str "menu-" slug))
                    (when (named-searches/new-named-search? slug) [:span.teal "NEW "])
                    (str/capitalize name))])]])
   [:li (menu-row (assoc (utils/route-to events/navigate-content-guarantee)
                         :data-test "content-guarantee")
                  "Our guarantee")]
   [:li (menu-row {:href blog-url}
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
     (menu-area shopping)]
    (when (-> signed-in ::at-all)
      [:div.px6.border-top.border-gray
       sign-out-area])]))

(defn signed-in [data]
  (let [as-stylist? (stylists/own-store? data)
        as-user?    (get-in data keypaths/user-email)
        store-slug  (get-in data (conj keypaths/store :store_slug))]
    {::at-all (or as-stylist? as-user?)
     ::as     (cond
                as-stylist? ::stylist
                as-user?    ::user
                :else       ::guest)
     ::to     (if (contains? #{"store" "shop"} store-slug)
                ::dtc
                ::marketplace)}))

(defn basic-query [data]
  (let [named-searches (named-searches/current-named-searches data)
        signed-in      (signed-in data)]
    {:signed-in signed-in
     :user      {:email (get-in data keypaths/user-email)}
     :store     (-> (get-in data keypaths/store)
                    (set/rename-keys {:store_slug        :store-slug
                                      :store_nickname    :store-nickname
                                      :instagram_account :instagram-account
                                      :styleseat_account :styleseat-account})
                    (assoc :gallery? (stylists/gallery? data)))
     :shopping  {:sections (cond-> [{:title "Shop hair"
                                     :items (filter named-searches/is-extension? named-searches)}
                                    {:title "Shop closures & frontals"
                                     :items (filter named-searches/is-closure-or-frontal? named-searches)}]
                             (-> signed-in ::as (= ::stylist))
                             (conj {:title "Stylist exclusives"
                                    :items (filter named-searches/is-stylist-product? named-searches)}))}}))

(defn query [data]
  (-> (basic-query data)
      (assoc-in [:user :store-credit] (get-in data keypaths/user-total-available-store-credit))
      (assoc-in [:promo-data] (promotion-banner/query data))))

(defn built-component [data opts]
  (component/build component (query data) nil))
