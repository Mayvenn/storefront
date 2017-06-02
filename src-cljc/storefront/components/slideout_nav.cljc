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
            [storefront.components.ui :as ui]))

(def menu-x
  (component/html
   [:div.absolute {:style {:width "60px"}}
    [:div.relative.rotate-45.p2 {:style {:height "60px"}
                                 :on-click #(messages/handle-message events/control-menu-collapse-all)}
     [:div.absolute.border-right.border-dark-gray {:style {:width "18px" :height "36px"}}]
     [:div.absolute.border-bottom.border-dark-gray {:style {:width "36px" :height "18px"}}]]]))

(defn logo [height]
  (component/html
   [:a.block.img-logo.bg-no-repeat.bg-center.bg-contain.teal.pp3
    (merge {:style {:height height}
            :title "Mayvenn"
            :item-prop "logo"
            :data-test "header-logo"
            :content (str "https:" (assets/path "/images/header_logo.svg"))}
           (utils/route-to events/navigate-home))]))

(defn marquee-button [event text]
  (ui/ghost-button
   (utils/fake-href event)
   text))

(defn ^:private marquee-col [content]
  [:div.flex.flex-auto
   {:style {:flex-basis 0}}
   content])

(defn marquee-row [left-content right-content]
  [:div.flex.justify-between.px4.mb3.mt2
   (marquee-col left-content)
   [:div.pr3]
   (marquee-col right-content)])

(def visitor-marquee-panel
  (marquee-row
   (marquee-button events/navigate-sign-in "Sign in")
   [:div.h6.col-12.center
    [:div.dark-gray "No account?"]
    [:a.dark-gray.underline
     (utils/route-to events/navigate-sign-up)
     "Sign up now, get offers!"]]))


(defn ^:private user-marquee-panel [user-email]
  ;; TODO add store credit
  [:div
   [:div.px4
    [:div.h7.bold.dark-gray "Signed in with:"]
    [:a.teal.h5
     (utils/route-to events/navigate-account-manage)
     user-email]]
   (marquee-row
    (marquee-button events/navigate-sign-in "Manage account")
    (marquee-button events/navigate-friend-referrals "Refer a friend"))])

(defn stylist-marquee-panel [user-email]
  [:div
   [:div.px4
    [:div.h7.bold.dark-gray "Signed in with:"]
    [:a.teal.h5
     (utils/route-to events/navigate-account-manage)
     user-email]]
   (marquee-row
    (marquee-button events/navigate-sign-in "Manage account")
    (marquee-button events/navigate-friend-referrals "Refer a stylist"))
   (marquee-row
    (marquee-button events/navigate-sign-in "Dashboard")
    (marquee-button events/navigate-friend-referrals "Community"))])

(defn ^:private gallery-link [text]
  [:span
   [:a.dark-gray.h6.underline
    (utils/route-to events/navigate-gallery)
    text]])

(defn ^:private instagram-link [text]
  [:a.dark-gray.h6.underline
   (utils/route-to events/navigate-stylist-account-social)
   text])

(defn ^:private styleseat-link [text]
  [:a.dark-gray.h6.underline
   (utils/route-to events/navigate-stylist-account-social)
   text])

(defn ^:private stylist-portrait [portrait size]
  (ui/circle-picture {:class "mx-auto"
                      :width (str size "px")}
                     (ui/square-image portrait size)))

(defn store-info-stylist-marquee-panel [{store-slug        :store_slug
                                         instagram-account :instagram_account
                                         styleseat-account :styleseat_account
                                         portrait          :portrait} gallery?]
  [:div.mx-auto.col-10.flex
   (when portrait
     [:div.left.self-center.pr2
      (stylist-portrait portrait 36)])
   [:div.dark-gray.left
    [:div.h7.bold (str "Hi, " store-slug ". Welcome to your shop.")]
    (interpose " | " (cond-> []
                       gallery?          (conj (gallery-link "See your gallery"))
                       instagram-account (conj (instagram-link "Follow me"))
                       styleseat-account (conj (styleseat-link "Book me"))))]])

(defn store-info-marquee-panel [{store-slug        :store_slug
                                 instagram-account :instagram_account
                                 styleseat-account :styleseat_account
                                 portrait          :portrait}
                                gallery?]
  [:div.mx-auto.col-10.flex
   (when portrait
     [:div.left.self-center.pr2
      (stylist-portrait portrait 36)])
   [:div.dark-gray.left
    [:div.h7.bold (str "Welcome to " store-slug "'s shop.")]
    (interpose " | " (cond-> []
                       gallery?          (conj (gallery-link "See my gallery"))
                       instagram-account (conj (instagram-link "Follow me"))
                       styleseat-account (conj (styleseat-link "Book me"))))]])

(defn component [{:keys [user-id user-email store gallery? stylist?] :as data} owner opts]
  (component/create
   (let [store-slug (:store_slug store)]
     [:div
      [:div.fixed.top-0.left-0.right-0.z4.bg-white
       (promotion-banner/built-component data opts)
       [:div.border-bottom.border-gray.mx-auto
        {:style {:max-width "1440px"}}
        menu-x
        [:div.center.col-12.px3.py2 {:style {:min-width "251px"}}
         (logo "40px")]]]
      [:div.py3 {:style {:margin-top "60px"}}
       (when-not (#{"shop" "store"} store-slug)
         (if stylist?
           (store-info-stylist-marquee-panel store gallery?)
           (store-info-marquee-panel store gallery?)))
       (cond
         stylist? (stylist-marquee-panel user-email)
         user-id  (user-marquee-panel user-email)
         :else    visitor-marquee-panel)]])))

(defn query [data]
  (merge
   (promotion-banner/query data)
   {:user-email (get-in data keypaths/user-email)
    :user-id    (get-in data keypaths/user-id)
    :store      (get-in data keypaths/store)
    :gallery?   (stylists/gallery? data)
    :stylist?   (stylists/own-store? data)}))

(defn built-component [data opts]
  (component/build component (query data) nil))
