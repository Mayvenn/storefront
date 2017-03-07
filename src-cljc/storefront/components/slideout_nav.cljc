(ns storefront.components.slideout-nav
  (:require [storefront.platform.component-utils :as utils]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.events :as events]
            [storefront.accessors.experiments :as experiments]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.platform.messages :as messages]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.accessors.stylists :refer [own-store? community-url]]
            [storefront.components.money-formatters :refer [as-money]]
            [storefront.routes :as routes]
            [clojure.string :as str]))

(def section-inner :div.ml3.py2)
(def section-top-inner :div.ml3.pb2)
(def section-outer :div.border-bottom.border-gray.bg-white.dark-gray)
(def section-outer-darker :div.border-bottom.border-gray)

(defn row
  ([right] (row nil right))
  ([left right]
   [:div.flex.items-center.pyp1
    [:div.col-2 [:div.px1 (or left ui/nbsp)]]
    [:div.col-10 right]]))

(def menu-x
  (component/html
   [:div.absolute {:style {:width "60px"}}
    [:div.relative.rotate-45.p2 {:style {:height "60px"}
                                 :on-click #(messages/handle-message events/control-menu-collapse-all)}
     [:div.absolute.border-right.border-dark-gray {:style {:width "18px" :height "36px"}}]
     [:div.absolute.border-bottom.border-dark-gray {:style {:width "36px" :height "18px"}}]]]))

(def logo
  (component/html
   [:a.block.img-logo.bg-no-repeat.bg-contain.bg-center.teal.pp3
    (merge {:style {:height "30px"}
            :title "Mayvenn"
            :data-test "slideout-logo"}
           (utils/route-to events/navigate-home))]))

(defn selectable
  ([current-navigation-message event-name content]
   (selectable current-navigation-message event-name {} content))
  ([current-navigation-message event-name event-args content]
   [:span
    (when (routes/sub-page? current-navigation-message [event-name event-args])
      {:class "border-navy border-bottom border-width-2 medium pyp1"})
    content]))

(defn store-credit-flag [credit]
  [:div.clearfix
   [:div.right.border-bottom.border-left.border-gray.bg-white
    {:style {:border-bottom-left-radius "8px"}
     :class (when (zero? credit) :invisible)}
    [:div.h5.px2.py1.line-height-1
     [:span.dark-gray "Credit: "] [:span.navy (as-money credit)]]]])

(defn customer-section [selectable? user-email]
  [:nav {:aria-label "Mayvenn Account"}
   (row [:div.truncate user-email])
   [:ul.list-reset
    [:li [:a.teal.block (utils/route-to events/navigate-account-manage)
          (row (selectable? events/navigate-account-manage
                            [:span {:data-test "account-settings"} "Account Settings"]))]]
    [:li [:a.teal.block (utils/route-to events/navigate-account-referrals)
          (row (selectable? events/navigate-account-referrals "Refer a Friend"))]]]])

(defn store-section [selectable? store]
  (let [store-photo (or (-> store :portrait :resizable_url)
                        (:profile_picture_url store))]
    [:nav {:aria-label "Mayvenn Account"}
     (row
      [:div.mxn1.pyp3 (ui/circle-picture {:width "32px"} store-photo)]
      [:div (:store_nickname store)])
     [:ul.list-reset
      [:li [:a.teal.block (utils/route-to events/navigate-stylist-dashboard-commissions)
            (row (selectable? events/navigate-stylist-dashboard
                              [:span {:data-test "dashboard"} "Dashboard"]))]]
      [:li [:a.teal.block (utils/route-to events/navigate-stylist-account-profile)
            (row (selectable? events/navigate-stylist-account
                              [:span {:data-test "account-settings"} "Account Settings"]))]]
      [:li [:a.teal.block community-url (row "Community")]]]]))

(defn products-section [selectable? title named-searches]
  [:nav {:aria-label (str "Shop " title)}
   (row [:div.border-bottom.border-gray.navy title])
   [:ul.my1.list-reset
    (for [{:keys [name slug]} named-searches]
      [:li {:key slug}
       [:a
        (merge {:data-test (str "menu-" slug)}
               (utils/route-to events/navigate-category {:named-search-slug slug}))
        (row
         (when (named-searches/new-named-search? slug) ui/new-flag)
         [:div.teal.titleize.h5
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
  [section-outer-darker
   [section-inner
    [:nav {:aria-label "Help"}
     [:ul.list-reset
      [:li [:a.teal (utils/route-to events/navigate-shop-by-look)
            (row (selectable? events/navigate-shop-by-look "Shop By Look"))]]
      [:li [:a.teal {:href "https://blog.mayvenn.com"} (row "Blog")]]
      [:li [:a.teal (assoc (utils/route-to events/navigate-content-guarantee)
                           :data-test "content-guarantee")
            (row (selectable? events/navigate-content-guarantee "Our Guarantee"))]]
      [:li [:a.teal (assoc (utils/route-to events/navigate-content-about-us)
                           :data-test "content-about-us")
            (row (selectable? events/navigate-content-about-us "About Us"))]]
      [:li [:a.teal (assoc (utils/route-to events/navigate-content-help)
                           :data-test "content-help")
            (row (selectable? events/navigate-content-help "Contact Us"))]]]]]])

(def sign-in-section
  (component/html
   [section-outer-darker
    [section-inner
     [:div.flex.items-center
      {:style {"padding-bottom" "75px"}}
      [:div.col-6.p1
       [:a.btn.btn-outline.navy.col-12
        (merge {:data-test "sign-in"}
               (utils/route-to events/navigate-sign-in))
        "Sign In"]]
      [:div.col-6.p1.center.h5
       [:div.dark-gray "No account?"]
       [:a.teal
        (merge {:data-test "sign-up"}
               (utils/route-to events/navigate-sign-up))
        "Sign Up"]]]]]))

(def sign-out-section
  (component/html
   [:div.bg-light-gray
    {:style {"padding-bottom" "75px"}}
    [:a.block.navy.center.col-12.p3
     (merge {:data-test "sign-out"}
            (utils/fake-href events/control-sign-out))
     "Logout"]]))

(defn guest-content [selectable? {:keys [named-searches]}]
  [:div
   (customer-shop-section selectable? named-searches)
   (help-section selectable?)
   sign-in-section])

(defn customer-content [selectable? {:keys [available-store-credit user-email named-searches]}]
  [:div
   [section-outer
    (store-credit-flag available-store-credit)
    [section-top-inner (customer-section selectable? user-email)]]
   (customer-shop-section selectable? named-searches)
   (help-section selectable?)
   sign-out-section])

(defn stylist-content [selectable? {:keys [available-store-credit store named-searches]}]
  [:div
   [section-outer
    (store-credit-flag available-store-credit)
    [section-top-inner (store-section selectable? store)]]
   (stylist-shop-section selectable? named-searches)
   (help-section selectable?)
   sign-out-section])

(defn component [{:keys [slid-out? stylist? user-email current-navigation-message] :as data} owner opts]
  (component/create
   (let [selectable? (partial selectable current-navigation-message)]
     [:div.h4.hide-on-tb-dt
      {:class (when-not slid-out? "hide")}
      [:div.fixed.overlay.bg-darken-4.z4
       ;; Clicks on the overlay close the slideout nav, without letting the click through to underlying links
       {:on-click (utils/send-event-callback events/control-menu-collapse-all)}]
      [:div.fixed.overflow-auto.top-0.left-0.col-10.z4.lit.bg-white.rounded-bottom-right-1
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
