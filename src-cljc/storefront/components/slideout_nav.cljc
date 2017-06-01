(ns storefront.components.slideout-nav
  (:require [storefront.platform.component-utils :as utils]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.events :as events]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.platform.messages :as messages]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.accessors.stylists :refer [own-store? community-url]]
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

(def visitor-menu
  [:div.flex.justify-between.mx-auto.col-10.py3
   [:div.col-6.mr3
    (ui/ghost-button
     (utils/fake-href events/navigate-sign-in)
     "Sign in")]
   [:div.col-6.ml3.center.h6
    [:div.dark-gray "No account?"]
    [:a.dark-gray.underline
     (utils/route-to events/navigate-sign-up)
     "Sign up now, get offers!"]]])

(defn user-menu [user-email]
  [:div
   [:div.mx-auto.col-10
    [:div.h7.bold.dark-gray "Signed in with:"]
    [:a.teal.h5
     (utils/route-to events/navigate-account-manage)
     user-email]]
   [:div.flex.justify-between.mx-auto.col-10.py2
    [:div.col-6.mr3.flex
     (ui/ghost-button
      (utils/fake-href events/navigate-sign-in)
      "Manage account")]
    [:div.col-6.ml3.flex
     (ui/ghost-button
      (utils/fake-href events/navigate-friend-referrals)
      "Refer a friend")]]])

(defn shop-marquee-account-info [{store-slug :store_slug}]
  [:div.mx-auto.col-10.flex
   [:div
    [:div.h7.bold.dark-gray (str "Welcome to " store-slug "'s shop.")]
    [:span
     [:a.dark-gray.h6
      (utils/route-to events/navigate-gallery)
      [:span.underline "See my gallery"] " | "]]
    [:a.dark-gray.h6
     (utils/route-to events/navigate-stylist-account-social)
     [:span.underline "follow me"] " | "]
    [:a.dark-gray.h6.underline
     (utils/route-to events/navigate-stylist-account-social)
     "book me"]]])

(defn component [{:keys [user-id user-email store stylist] :as data} owner opts]
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
       (cond
         (= store-slug "shop") [:div]
         :else (shop-marquee-account-info store))
       (cond
         user-id (user-menu user-email)
         :else   visitor-menu)]])))

(defn query [data]
  (merge
   (promotion-banner/query data)
   {:user-email (get-in data keypaths/user-email)
    :user-id    (get-in data keypaths/user-id)
    :store      (get-in data keypaths/store)
    :stylist?   (own-store? data)}))

(defn built-component [data opts]
  (component/build component (query data) nil))
