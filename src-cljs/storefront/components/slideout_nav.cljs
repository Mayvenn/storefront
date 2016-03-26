(ns storefront.components.slideout-nav
  (:require [storefront.components.utils :as utils]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.routes :as routes]
            [storefront.accessors.taxons :refer [taxon-path-for default-stylist-taxon-path]]
            [storefront.accessors.stylists :refer [own-store?]]
            [storefront.accessors.navigation :as navigation]
            [storefront.messages :refer [send]]
            [storefront.components.formatters :refer [as-money]]))

(defn show-store-credit? [app-state]
  (when-let [credit (get-in app-state keypaths/user-total-available-store-credit)]
    (pos? credit)))

(defn close-all-menus [app-state]
  (send app-state
        events/control-menu-collapse
        {:keypath keypaths/menu-expanded})
  (send app-state
        events/control-menu-collapse
        {:keypath keypaths/account-menu-expanded})
  (send app-state
        events/control-menu-collapse
        {:keypath keypaths/shop-menu-expanded}))

(defn close-and-route [app-state event & [args]]
  {:href
   (routes/path-for @app-state event args)
   :on-click
   (fn [e]
     (.preventDefault e)
     (close-all-menus app-state)
     (routes/enqueue-navigate @app-state event args))})

(defn close-and-enqueue [app-state event & [args]]
  {:href "#"
   :on-click
   (fn [e]
     (.preventDefault e)
     (close-all-menus app-state)
     (send app-state event args))})

(defn logged-in? [data]
  (boolean (get-in data keypaths/user-email)))

(defn shop-now-attrs [data]
  (apply close-and-route data (navigation/shop-now-navigation-message data)))

(defn invasive-primary-nav-component [data owner]
  ;; WAT 1: This is within the slideout nav, but is invisible on small screens
  ;; WAT 2: It is part of the secondary nav, and through positioning hacks overlays primary nav
  (om/component
   (html
    [:div.account-detail
     (if-not (logged-in? data)
       [:span
        [:a (close-and-route data events/navigate-sign-in) "Sign In"]
        " | "
        [:a (close-and-route data events/navigate-sign-up) "Sign Up"]]
       [:.relative.z1
        [:a.account-menu-link
         {:href "#"
          :on-click
          (utils/send-event-callback data
                                     (if (get-in data keypaths/account-menu-expanded)
                                       events/control-menu-collapse
                                       events/control-menu-expand)
                                     {:keypath keypaths/account-menu-expanded})}
         (when (show-store-credit? data)
           [:span.stylist-user-label
            "Store credit:"
            [:span.store-credit-amount (as-money (get-in data keypaths/user-total-available-store-credit))]])
         [:span.account-detail-name
          (when (own-store? data) [:span.stylist-user-label "Stylist:"])
          (get-in data keypaths/user-email)]
         [:figure.down-arrow]]
        (when (get-in data keypaths/account-menu-expanded)
          [:ul.account-detail-expanded
           (if (own-store? data)
             (list
              [:li
               [:a (close-and-route data events/navigate-stylist-dashboard-commissions) "Dashboard"]]
              [:li
               [:a (close-and-route data events/navigate-stylist-manage-account) "Manage Account"]]
              [:li
               [:a {:href (get-in data keypaths/community-url)
                    :on-click (utils/send-event-callback data events/external-redirect-community)}
                "Stylist Community"]])
             (list
              [:li
               [:a (close-and-route data events/navigate-account-referrals) "Refer A Friend"]]
              [:li
               [:a (close-and-route data events/navigate-account-manage) "Manage Account"]]))
           [:li
            [:a (close-and-enqueue data events/control-sign-out)
             "Logout"]]])])])))

(defn secondary-and-invasive-primary-nav-component [data owner]
  ;; WAT: This is invisible on small screens
  (om/component
   (html
    [:div.horizontal-nav-list
     (om/build invasive-primary-nav-component data)
     [:h2.horizontal-nav-title (:store_name (get-in data keypaths/store))]
     [:ul.horizontal-nav-menu
      [:li.relative.z1
       (if (own-store? data)
         [:a
          (close-and-enqueue data events/control-menu-expand {:keypath keypaths/shop-menu-expanded})
          "Shop " [:figure.down-arrow]]
         [:a
          (shop-now-attrs data)
          "Shop"])
       (when (get-in data keypaths/shop-menu-expanded)
         [:ul.shop-menu-expanded.top-0
          [:li
           [:a (shop-now-attrs data) "Hair Extensions"]]
          [:li
           [:a
            (when-let [path (default-stylist-taxon-path data)]
              (close-and-route data events/navigate-category
                               {:taxon-path path}))
            "Stylist Only Products"]]])]
      [:li [:a (close-and-route data events/navigate-guarantee) "30 Day Guarantee"]]
      [:li [:a (close-and-route data events/navigate-help) "Customer Service"]]]])))

(defn slideout-nav-link [behavior {:keys [icon-class label full-width?]}]
  [:a.slideout-nav-link
   (merge behavior
          {:class (if full-width? "full-width" "half-width")})
   [:div.slideout-nav-link-inner
    [:div.slideout-nav-link-icon {:class (str "icon-" icon-class)}]
    label]])

(defn shop-hair-link [data]
  (slideout-nav-link
   (shop-now-attrs data)
   {:icon-class "hair-extensions"
    :label "Hair Extensions"
    :full-width? true}))

(defn logout-link [data]
  (slideout-nav-link
   (close-and-enqueue data events/control-sign-out)
   {:icon-class "logout"
    :label "Logout"
    :full-width? false}))

(defn slideout-stylist-nav [data]
  (list
   [:li.slideout-nav-section.stylist
    [:h3.slideout-nav-section-header.highlight "Manage Store"]
    (slideout-nav-link
     (close-and-route data events/navigate-stylist-dashboard-commissions)
     {:icon-class "stylist-dashboard"
      :label "Dashboard"
      :full-width? false})
    (slideout-nav-link
     (close-and-route data events/navigate-stylist-manage-account)
     {:icon-class "edit-profile"
      :label "Edit Profile"
      :full-width? false})
    (slideout-nav-link
     {:href (get-in data keypaths/community-url)
      :on-click (utils/send-event-callback data events/external-redirect-community)}
     {:icon-class "community"
      :label "Stylist Community"
      :full-width? true})]
   [:li.slideout-nav-section
    [:h3.slideout-nav-section-header "Shop"]
    (shop-hair-link data)
    (slideout-nav-link
     (when-let [path (default-stylist-taxon-path data)]
       (close-and-route data events/navigate-category
                        {:taxon-path path}))
     {:icon-class "stylist-products"
      :label "Stylist Products"
      :full-width? true})]
   [:li.slideout-nav-section
    [:h3.slideout-nav-section-header "My Account"]
    (slideout-nav-link
     (close-and-route data events/navigate-stylist-manage-account)
     {:icon-class "manage-account"
      :label "Manage Account"
      :full-width? false})
    (logout-link data)]))

(defn slideout-customer-nav [data]
  (list
   [:li.slideout-nav-section
    [:h3.slideout-nav-section-header "Shop"]
    (shop-hair-link data)]
   [:li.slideout-nav-section
    [:h3.slideout-nav-section-header "My Account"]
    (slideout-nav-link
     (close-and-route data events/navigate-account-referrals)
     {:icon-class "refer-friend"
      :label "Refer A Friend"
      :full-width? true})
    (slideout-nav-link
     (close-and-route data events/navigate-account-manage)
     {:icon-class "manage-account"
      :label "Manage Account"
      :full-width? false})
    (logout-link data)]))

(defn slideout-guest-nav [data]
  (list
   [:li.slideout-nav-section
    [:h3.slideout-nav-section-header "Shop"]
    (shop-hair-link data)]
   [:li.slideout-nav-section
    [:h3.slideout-nav-section-header "My Account"]
    (slideout-nav-link (close-and-route data events/navigate-sign-in)
     {:icon-class "sign-in"
      :label "Sign In"
      :full-width? false})
    (slideout-nav-link (close-and-route data events/navigate-sign-up)
     {:icon-class "join"
      :label "Join"
      :full-width? false})]))

(defn slideout-nav-component-really [data owner]
  (om/component
   (html
    [:ul.slideout-nav-list
     (when (show-store-credit? data)
       [:li.slideout-nav-section
        [:h4.store-credit
         [:span.label "Available store credit:"]
         [:span.value
          (as-money (get-in data keypaths/user-total-available-store-credit))]]])
     (cond
       (own-store? data) (slideout-stylist-nav data)
       (logged-in? data) (slideout-customer-nav data)
       :else             (slideout-guest-nav data))
     [:li.slideout-nav-section
      [:h3.slideout-nav-section-header "Help"]
      (slideout-nav-link
       (close-and-route data events/navigate-help)
       {:icon-class "customer-service"
        :label "Customer Service"
        :full-width? false})
      (slideout-nav-link
       (close-and-route data events/navigate-guarantee)
       {:icon-class "30-day-guarantee"
        :label "30 Day Guarantee"
        :full-width? false})]])))

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
        (om/build secondary-and-invasive-primary-nav-component data)
        (when slid-out?
          (om/build slideout-nav-component-really data))]]))))
