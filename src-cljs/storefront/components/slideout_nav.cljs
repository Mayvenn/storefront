(ns storefront.components.slideout-nav
  (:require [storefront.components.utils :as utils]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.routes :as routes]
            [storefront.messages :as messages]
            [storefront.accessors.taxons :refer [new-taxon? slug->name is-closure? is-extension? is-stylist-product?]]
            [storefront.accessors.stylists :refer [own-store?]]
            [storefront.accessors.navigation :as navigation]
            [storefront.components.formatters :refer [as-money]]
            [storefront.hooks.experiments :as experiments]))

(def section-inner :.ml3.py2)
(def section-outer :.border-bottom.border-light-silver.bg-pure-white.black)
(def section-outer-darker :.border-bottom.border-light-silver)

(defn row
  ([right] (row nil right))
  ([left right]
   [:.clearfix.pyp1
    [:.col.col-2 [:.px1 (or left ui/nbsp)]]
    [:.col.col-10.line-height-3 right]]))

(def menu-x
  (html
   [:.absolute {:style {:width "60px"}}
    [:.relative.rotate-45.p2 {:style {:height "60px"}}
     [:.absolute.border-right.border-dark-gray {:style {:width "18px" :height "36px"}}]
     [:.absolute.border-bottom.border-dark-gray {:style {:width "36px" :height "18px"}}]]]))

(def logo
  (html
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
    (when (utils/current-page? current-navigation-message event-name event-args)
      {:class "border-navy border-bottom border-width-2 bold pyp1"})
    content]))

(defn store-credit-flag [credit]
  [:.right.border-bottom.border-left.border-light-silver.bg-white
   {:style {:border-bottom-left-radius "8px"}
    :class (when (zero? credit) :invisible)}
   [:.h5.px2.py1.line-height-1
    [:span.gray "Credit: "] [:span.navy (as-money credit)]]])

(defn customer-section [selectable? user-email]
  [:div
   (row [:.truncate user-email])
   [:a.green.block (utils/route-to events/navigate-account-manage)
    (row (selectable? events/navigate-account-manage
                      [:span {:data-test "account-settings"} "Account Settings"]))]
   [:a.green.block (utils/route-to events/navigate-account-referrals)
    (row (selectable? events/navigate-account-referrals "Refer a Friend"))]])

(defn store-section [selectable? store]
  (let [{store-photo :profile_picture_url nickname :store_nickname} store]
    [:div
     (row
      (when store-photo
        [:.mxn1.pyp3 (ui/circle-picture {:width "32px"} store-photo)])
      [:div nickname])
     [:div
      [:a.green.block (utils/route-to events/navigate-stylist-dashboard-commissions)
       (row (selectable? events/navigate-stylist-dashboard
                         [:span {:data-test "dashboard"} "Dashboard"]))]
      [:a.green.block (utils/route-to events/navigate-stylist-manage-account)
       (row (selectable? events/navigate-stylist-manage-account
                         [:span {:data-test "account-settings"} "Account Settings"]))]
      [:a.green.block (utils/navigate-community) (row "Community")]]]))

(defn products-section [selectable? title taxons]
  [:div
   (row [:.border-bottom.border-light-silver.nav.navy title])
   [:.my1
    (for [{:keys [name slug]} taxons]
      [:a
       (merge {:key slug} (utils/route-to events/navigate-category {:taxon-slug slug}))
       (row
        (when (new-taxon? slug) ui/new-flag)
        [:.green.titleize
         (selectable? events/navigate-category {:taxon-slug slug} (get slug->name slug name))])])]])

(defn extensions-section [selectable? taxons]
  (products-section selectable? "Extensions" (filter is-extension? taxons)))

(defn closures-section [selectable? taxons]
  (products-section selectable? "Closures" (filter is-closure? taxons)))

(defn stylist-products-section [selectable? taxons]
  (products-section selectable? "Stylist Products" (filter is-stylist-product? taxons)))

(defn customer-shop-section [selectable? taxons]
  [section-outer
   [section-inner
    [:.sans-serif.medium "Shop"]
    (extensions-section selectable? taxons)
    (closures-section selectable? taxons)]])

(defn stylist-shop-section [selectable? taxons]
  [section-outer
   [section-inner
    [:.sans-serif.medium "Shop"]
    (extensions-section selectable? taxons)
    (closures-section selectable? taxons)
    (stylist-products-section selectable? taxons)]])

(defn help-section [selectable?]
  (html
   [section-outer-darker
    [section-inner
     [:a.green {:href "https://blog.mayvenn.com"} (row "Blog")]
     [:a.green (utils/route-to events/navigate-guarantee)
      (row (selectable? events/navigate-guarantee "Our Guarantee"))]
     [:a.green (utils/route-to events/navigate-help)
      (row (selectable? events/navigate-help "Contact Us"))]]]))

(def sign-in-section
  (html
   [section-outer-darker
    [section-inner
     [:.clearfix
      [:.col.col-6.p1
       [:a.btn.btn-outline.navy.col-12
        (merge {:data-test "sign-in"}
               (utils/route-to events/navigate-sign-in))
        "Sign In"]]
      [:.col.col-6.p1.center.h5.line-height-2
       [:.gray "No account?"]
       [:a.green
        (merge {:data-test "sign-up"}
               (utils/route-to events/navigate-sign-up))
        "Sign Up"]]]]]))

(def sign-out-section
  (html
   [:a.block.navy.center.col-12.p3.bg-white
    (merge {:data-test "sign-out"}
           (utils/fake-href events/control-sign-out))
    "Logout"]))

(defn guest-content [selectable? {:keys [taxons]}]
  [:div
   (customer-shop-section selectable? taxons)
   (help-section selectable?)
   sign-in-section])

(defn customer-content [selectable? {:keys [available-store-credit user-email taxons]}]
  [:div
   [section-outer
    (store-credit-flag available-store-credit)
    [section-inner (customer-section selectable? user-email)]]
   (customer-shop-section selectable? taxons)
   (help-section selectable?)
   sign-out-section])

(defn stylist-content [selectable? {:keys [available-store-credit store taxons]}]
  [:div
   [section-outer
    (store-credit-flag available-store-credit)
    [section-inner (store-section selectable? store)]]
   (stylist-shop-section selectable? taxons)
   (help-section selectable?)
   sign-out-section])

(defn component [{:keys [slid-out? stylist? user-email current-navigation-message] :as data} owner]
  (om/component
   (html
    (when slid-out?
      (let [selectable? (partial selectable current-navigation-message)]
        [:.h3.lg-up-hide
         ;; Clicks on links in the slideout nav close the slideout nav and follow the link
         {:on-click #(messages/handle-message events/control-menu-collapse-all)}
         [:.fixed.overlay.bg-darken-4.z3
          ;; Clicks on the overlay close the slideout nav, without letting the click through to underlying links
          {:on-click (utils/send-event-callback events/control-menu-collapse-all)}]
         [:.fixed.overflow-auto.top-0.left-0.col-10.z3.lit.bg-white.rounded-bottom-right-2
          {:style {:max-height "100%"}}
          [section-outer-darker menu-x [:.p2 logo]]
          (cond
            stylist?   (stylist-content selectable? data)
            user-email (customer-content selectable? data)
            :else      (guest-content selectable? data))]])))))


(defn query [data]
  {:slid-out?                  (get-in data keypaths/menu-expanded)
   :stylist?                   (own-store? data)
   :store                      (get-in data keypaths/store)
   :user-email                 (get-in data keypaths/user-email)
   :available-store-credit     (get-in data keypaths/user-total-available-store-credit)
   :current-navigation-message (get-in data keypaths/navigation-message)
   :taxons                     (get-in data keypaths/taxons)})

(defn built-component [data]
  (om/build component (query data)))
