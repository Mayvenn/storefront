(ns storefront.components.slideout-nav
  (:require [storefront.platform.component-utils :as utils]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.events :as events]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.platform.messages :as messages]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.accessors.stylists :refer [own-store?]]
            [storefront.components.money-formatters :refer [as-money]]
            [storefront.app-routes :as app-routes]
            [clojure.string :as str]))

(def section-inner :div.ml3.py2)
(def section-outer :div.border-bottom.border-light-silver.bg-pure-white.black)
(def section-outer-darker :div.border-bottom.border-light-silver)

(defn row
  ([right] (row nil right))
  ([left right]
   [:div.clearfix.pyp1
    [:div.col.col-2 [:div.px1 (or left ui/nbsp)]]
    [:div.col.col-10.line-height-3 right]]))

(def menu-x
  (component/html
   [:div.absolute {:style {:width "60px"}}
    [:div.relative.rotate-45.p2 {:style {:height "60px"}
                              :on-click #(messages/handle-message events/control-menu-collapse-all)}
     [:div.absolute.border-right.border-dark-gray {:style {:width "18px" :height "36px"}}]
     [:div.absolute.border-bottom.border-dark-gray {:style {:width "36px" :height "18px"}}]]]))

(def logo
  (component/html
   [:a.block.img-logo.bg-no-repeat.bg-contain.bg-center.green.pp3
    (merge {:style {:height "30px"}
            :title "Mayvenn"
            :data-test "slideout-logo"}
           (utils/route-to events/navigate-home))]))

(defn selectable
  ([current-navigation-message event-name content]
   (selectable current-navigation-message event-name {} content))
  ([current-navigation-message event-name event-args content]
   [:span
    (when (app-routes/current-page? current-navigation-message event-name event-args)
      {:class "border-navy border-bottom border-width-2 bold pyp1"})
    content]))

(defn store-credit-flag [credit]
  [:div.right.border-bottom.border-left.border-light-silver.bg-white
   {:style {:border-bottom-left-radius "8px"}
    :class (when (zero? credit) :invisible)}
   [:div.h5.px2.py1.line-height-1
    [:span.gray "Credit: "] [:span.navy (as-money credit)]]])

(defn customer-section [selectable? user-email]
  [:nav {:role "navigation" :aria-label "Mayvenn Account"}
   (row [:div.truncate user-email])
   [:ul.list-reset
    [:li [:a.green.block (utils/route-to events/navigate-account-manage)
          (row (selectable? events/navigate-account-manage
                            [:span {:data-test "account-settings"} "Account Settings"]))]]
    [:li [:a.green.block (utils/route-to events/navigate-account-referrals)
          (row (selectable? events/navigate-account-referrals "Refer a Friend"))]]]])

(defn store-section [selectable? store]
  (let [{store-photo :profile_picture_url nickname :store_nickname} store]
    [:nav {:role "navigation" :aria-label "Mayvenn Account"}
     (row
      [:div.mxn1.pyp3 (ui/circle-picture {:width "32px"} store-photo)]
      [:div nickname])
     [:ul.list-reset
      [:li [:a.green.block (utils/route-to events/navigate-stylist-dashboard-commissions)
            (row (selectable? events/navigate-stylist-dashboard
                              [:span {:data-test "dashboard"} "Dashboard"]))]]
      [:li [:a.green.block (utils/route-to events/navigate-stylist-account-profile)
            (row (selectable? events/navigate-stylist-account
                              [:span {:data-test "account-settings"} "Account Settings"]))]]
      [:li [:a.green.block (utils/navigate-community) (row "Community")]]]]))

(defn products-section [selectable? title named-searches]
  [:nav {:role "navigation" :aria-label (str "Shop " title)}
   (row [:div.border-bottom.border-light-silver.navy title])
   [:ul.my1.list-reset
    (for [{:keys [name slug]} named-searches]
      [:li {:key slug}
       [:a
        (merge {:data-test (str "menu-" slug)}
               (utils/route-to events/navigate-category {:named-search-slug slug}))
        (row
         (when (named-searches/new-named-search? slug) ui/new-flag)
         [:div.green.titleize
          (selectable? events/navigate-category {:named-search-slug slug} name)])]])]])

(defn extensions-section [selectable? named-searches]
  (products-section selectable? "Extensions" (filter named-searches/is-extension? named-searches)))

(defn closures-section [selectable? named-searches]
  (products-section selectable? "Closures" (filter named-searches/is-closure-or-frontal? named-searches)))

(defn stylist-products-section [selectable? named-searches]
  (products-section selectable? "Stylist Products" (filter named-searches/is-stylist-product? named-searches)))

(defn customer-shop-section [selectable? named-searches]
  [section-outer
   [section-inner
    [:div.medium "Shop"]
    (extensions-section selectable? named-searches)
    (closures-section selectable? named-searches)]])

(defn stylist-shop-section [selectable? named-searches]
  [section-outer
   [section-inner
    [:div.medium "Shop"]
    (extensions-section selectable? named-searches)
    (closures-section selectable? named-searches)
    (stylist-products-section selectable? named-searches)]])

(defn help-section [selectable?]
  (component/html
   [section-outer-darker
    [section-inner
     [:nav {:role "navigation" :aria-label "Help"}
      [:ul.list-reset
       [:li [:a.green (utils/route-to events/navigate-shop-by-look)
             (row (selectable? events/navigate-shop-by-look "Shop By Look"))]]
       [:li [:a.green {:href "https://blog.mayvenn.com"} (row "Blog")]]
       [:li [:a.green (assoc (utils/route-to events/navigate-guarantee)
                             :data-test "navigate-guarantee")
             (row (selectable? events/navigate-guarantee "Our Guarantee"))]]
       [:li [:a.green (assoc (utils/route-to events/navigate-content-help)
                             :data-test "navigate-content-help")
             (row (selectable? events/navigate-content-help "Contact Us"))]]]]]]))

(def sign-in-section
  (component/html
   [section-outer-darker
    [section-inner
     [:div.clearfix
      [:div.col.col-6.p1
       [:a.btn.btn-outline.navy.col-12
        (merge {:data-test "sign-in"}
               (utils/route-to events/navigate-sign-in))
        "Sign In"]]
      [:div.col.col-6.p1.center.h5.line-height-2
       [:div.gray "No account?"]
       [:a.green
        (merge {:data-test "sign-up"}
               (utils/route-to events/navigate-sign-up))
        "Sign Up"]]]]]))

(def sign-out-section
  (component/html
   [:a.block.navy.center.col-12.p3.bg-white
    (merge {:data-test "sign-out"}
           (utils/fake-href events/control-sign-out))
    "Logout"]))

(defn guest-content [selectable? {:keys [named-searches]}]
  [:div
   (customer-shop-section selectable? named-searches)
   (help-section selectable?)
   sign-in-section])

(defn customer-content [selectable? {:keys [available-store-credit user-email named-searches]}]
  [:div
   [section-outer
    (store-credit-flag available-store-credit)
    [section-inner (customer-section selectable? user-email)]]
   (customer-shop-section selectable? named-searches)
   (help-section selectable?)
   sign-out-section])

(defn stylist-content [selectable? {:keys [available-store-credit store named-searches]}]
  [:div
   [section-outer
    (store-credit-flag available-store-credit)
    [section-inner (store-section selectable? store)]]
   (stylist-shop-section selectable? named-searches)
   (help-section selectable?)
   sign-out-section])

(defn component [{:keys [slid-out? stylist? user-email current-navigation-message] :as data} owner opts]
  (component/create
   (let [selectable? (partial selectable current-navigation-message)]
     [:div.h3.lg-up-hide
      {:class (when-not slid-out? "hide")}
      [:div.fixed.overlay.bg-darken-4.z3
       ;; Clicks on the overlay close the slideout nav, without letting the click through to underlying links
       {:on-click (utils/send-event-callback events/control-menu-collapse-all)}]
      [:div.fixed.overflow-auto.top-0.left-0.col-10.z3.lit.bg-white.rounded-bottom-right-1
       {:style {:max-height "100vh"}}
       [section-outer-darker menu-x [:div.p2 logo]]
       (cond
         stylist?   (stylist-content selectable? data)
         user-email (customer-content selectable? data)
         :else      (guest-content selectable? data))]])))


(defn query [data]
  {:slid-out?                  (get-in data keypaths/menu-expanded)
   :stylist?                   (own-store? data)
   :store                      (get-in data keypaths/store)
   :user-email                 (get-in data keypaths/user-email)
   :available-store-credit     (get-in data keypaths/user-total-available-store-credit)
   :current-navigation-message (get-in data keypaths/navigation-message)
   :named-searches             (named-searches/current-named-searches data)})

(defn built-component [data opts]
  (component/build component (query data) nil))
