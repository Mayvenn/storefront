(ns storefront.components.slideout-nav
  (:require [storefront.components.utils :as utils]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.routes :as routes]
            [storefront.accessors.taxons :refer [default-stylist-taxon-path]]
            [storefront.accessors.stylists :refer [own-store?]]
            [storefront.accessors.navigation :as navigation]
            [storefront.components.formatters :refer [as-money]]))

(defn show-store-credit? [app-state]
  (when-let [credit (get-in app-state keypaths/user-total-available-store-credit)]
    (pos? credit)))

(defn fake-href [event & [args]]
  {:href "#"
   :on-click (utils/send-event-callback event args)})

(defn logged-in? [data]
  (boolean (get-in data keypaths/user-email)))

(defn navigate-hair [data]
  (apply utils/route-to (navigation/shop-now-navigation-message data)))

(defn navigate-kits [data]
  (when-let [path (default-stylist-taxon-path data)]
    (utils/route-to events/navigate-category
                    {:taxon-path path})))

(defn navigate-community [data]
  {:href (or (get-in data keypaths/community-url) "#")
   :on-click (utils/send-event-callback events/external-redirect-community)})

(defn drop-down [data expanded-keypath [link-tag & link-contents] menu]
  [:.relative.z1
   (into [link-tag
          (fake-href events/control-menu-expand {:keypath expanded-keypath})]
         link-contents)
   (when (get-in data expanded-keypath)
     [:div
      {:on-click (utils/send-event-callback events/control-menu-collapse {:keypath expanded-keypath})}
      [:.fixed.overlay]
      menu])])

(defn account-store-credit [data]
  (when (show-store-credit? data)
    [:span.stylist-user-label
     "Store credit:"
     [:span.store-credit-amount (as-money (get-in data keypaths/user-total-available-store-credit))]]))

(def guest-account-menu
  [:span
   [:a (utils/route-to events/navigate-sign-in) "Sign In"]
   " | "
   [:a (utils/route-to events/navigate-sign-up) "Sign Up"]])

(defn stylist-account-menu [data]
  (drop-down
   data keypaths/account-menu-expanded
   [:a.account-menu-link
    (account-store-credit data)
    [:span.account-detail-name [:span.stylist-user-label "Stylist:"] (get-in data keypaths/user-email)]
    [:figure.down-arrow]]
   [:ul.account-detail-expanded
    [:li [:a (utils/route-to events/navigate-stylist-dashboard-commissions) "Dashboard"]]
    [:li [:a (utils/route-to events/navigate-stylist-manage-account) "Manage Account"]]
    [:li [:a (navigate-community data) "Stylist Community"]]
    [:li [:a (fake-href events/control-sign-out) "Logout"]]]))

(defn customer-account-menu [data]
  (drop-down
   data keypaths/account-menu-expanded
   [:a.account-menu-link
    (account-store-credit data)
    [:span.account-detail-name (get-in data keypaths/user-email)]
    [:figure.down-arrow]]
   [:ul.account-detail-expanded
    [:li [:a (utils/route-to events/navigate-account-referrals) "Refer A Friend"]]
    [:li [:a (utils/route-to events/navigate-account-manage) "Manage Account"]]
    [:li [:a (fake-href events/control-sign-out) "Logout"]]]))

(defn invasive-top-nav-component [data owner]
  ;; WAT 1: This is within the slideout nav, but is invisible on small screens
  ;; WAT 2: It is part of the secondary nav, but overlays primary nav through positioning hacks
  (om/component
   (html
    [:div.account-detail
     (cond
       (own-store? data) (stylist-account-menu data)
       (logged-in? data) (customer-account-menu data)
       :else             guest-account-menu)])))

(defn stylist-shop-link [data]
  (drop-down
   data keypaths/shop-menu-expanded
   [:a "Shop " [:figure.down-arrow]]
   [:ul.shop-menu-expanded.top-0
    [:li [:a (navigate-hair data) "Hair Extensions"]]
    [:li [:a (navigate-kits data) "Stylist Only Products"]]]))

(defn non-stylist-shop-link [data]
  [:a (navigate-hair data) "Shop"])

(defn middle-nav-component [data owner]
  (om/component
   (html
    [:div
     [:h2.horizontal-nav-title (:store_name (get-in data keypaths/store))]
     [:ul.horizontal-nav-menu
      [:li (if (own-store? data) (stylist-shop-link data) (non-stylist-shop-link data))]
      [:li [:a (utils/route-to events/navigate-guarantee) "30 Day Guarantee"]]
      [:li [:a (utils/route-to events/navigate-help) "Customer Service"]]]])))

(defn nav-box
  ([icon-class label behavior] (nav-box icon-class label behavior {:full-width? false}))
  ([icon-class label behavior {:keys [full-width?]}]
   [:a.slideout-nav-link
    (merge behavior
           {:class (if full-width? "full-width" "half-width")})
    [:div.slideout-nav-link-inner
     [:div.slideout-nav-link-icon {:class (str "icon-" icon-class)}]
     label]]))

(defn nav-hair-box [data]
  (nav-box "hair-extensions" "Hair Extensions" (navigate-hair data) {:full-width? true}))

(defn logout-box []
  (nav-box "logout" "Logout" (fake-href events/control-sign-out)))

(defn slideout-stylist-nav [data]
  (list
   [:li.slideout-nav-section.stylist
    [:h3.slideout-nav-section-header.highlight "Manage Store"]
    (nav-box "stylist-dashboard" "Dashboard" (utils/route-to events/navigate-stylist-dashboard-commissions))
    (nav-box "edit-profile" "Edit Profile" (utils/route-to events/navigate-stylist-manage-account))
    (nav-box "community" "Stylist Community" (navigate-community data) {:full-width? true})]
   [:li.slideout-nav-section
    [:h3.slideout-nav-section-header "Shop"]
    (nav-hair-box data)
    (nav-box "stylist-products" "Stylist Products" (navigate-kits data) {:full-width? true})]
   [:li.slideout-nav-section
    [:h3.slideout-nav-section-header "My Account"]
    (nav-box "manage-account" "Manage Account" (utils/route-to events/navigate-stylist-manage-account))
    (logout-box)]))

(defn slideout-customer-nav [data]
  (list
   [:li.slideout-nav-section
    [:h3.slideout-nav-section-header "Shop"]
    (nav-hair-box data)]
   [:li.slideout-nav-section
    [:h3.slideout-nav-section-header "My Account"]
    (nav-box "refer-friend" "Refer A Friend" (utils/route-to events/navigate-account-referrals) {:full-width? true})
    (nav-box "manage-account" "Manage Account" (utils/route-to events/navigate-account-manage))
    (logout-box)]))

(defn slideout-guest-nav [data]
  (list
   [:li.slideout-nav-section
    [:h3.slideout-nav-section-header "Shop"]
    (nav-hair-box data)]
   [:li.slideout-nav-section
    [:h3.slideout-nav-section-header "My Account"]
    (nav-box "sign-in" "Sign In" (utils/route-to events/navigate-sign-in))
    (nav-box "join" "Join" (utils/route-to events/navigate-sign-up))]))

(defn slideout-nav-component-really [data owner]
  (om/component
   (html
    [:ul.slideout-nav-list
     (when (show-store-credit? data)
       [:li.slideout-nav-section
        [:h4.store-credit
         [:span.label "Available store credit:"]
         [:span.value (as-money (get-in data keypaths/user-total-available-store-credit))]]])
     (cond
       (own-store? data) (slideout-stylist-nav data)
       (logged-in? data) (slideout-customer-nav data)
       :else             (slideout-guest-nav data))
     [:li.slideout-nav-section
      [:h3.slideout-nav-section-header "Help"]
      (nav-box "customer-service" "Customer Service" (utils/route-to events/navigate-help))
      (nav-box "30-day-guarantee" "30 Day Guarantee" (utils/route-to events/navigate-guarantee))]])))

(defn slideout-nav-component [data owner]
  (om/component
   (html
    (let [store (get-in data keypaths/store)
          store-photo (:profile_picture_url store)
          slid-out? (get-in data keypaths/menu-expanded)]
      [:div.slideout-nav-wrapper
       {:class (when slid-out? "slideout-nav-open")}
       [:nav.slideout-nav (when-not store-photo {:class "no-picture"})
        ;; Everything in .slideout-nav-header is only visible on small screens
        ;; WAT: except non-missing .slideout-nav-portrait, which is visible on
        ;; primary/secondary nav too. Why not render it there too?
        [:div.slideout-nav-header
         [:div.slideout-nav-img-container
          (if store-photo
            [:img.slideout-nav-portrait {:src store-photo}]
            [:div.slideout-nav-portrait.missing-picture])]
         [:h2.slideout-nav-title (store :store_name)]]
        ;; WAT: This is invisible on small screens
        [:div.horizontal-nav-list
         (om/build invasive-top-nav-component data)
         (om/build middle-nav-component data)]
        (when slid-out?
          [:div
           {:on-click (utils/send-event-callback events/control-menu-collapse
                                                 {:keypath keypaths/menu-expanded})}
           [:.fixed.overlay]
           (om/build slideout-nav-component-really data)])]]))))
