(ns storefront.components.slideout-nav
  (:require [storefront.components.utils :as utils]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.routes :as routes]
            [storefront.taxons :refer [taxon-path-for default-taxon-path]]
            [storefront.messages :refer [enqueue-message]]))

(defn close-and-route [app-state event & [args]]
  {:href
   (routes/path-for @app-state event args)
   :on-click
   (fn [e]
     (.preventDefault e)
     (enqueue-message (get-in @app-state keypaths/event-ch) [events/control-menu-collapse])
     (enqueue-message (get-in @app-state keypaths/event-ch) [events/control-account-menu-collapse])
     (routes/enqueue-navigate @app-state event args))})

(defn close-and-enqueue [app-state event]
  {:href "#"
   :on-click
   (fn [e]
     (.preventDefault e)
     (enqueue-message (get-in @app-state keypaths/event-ch) [events/control-menu-collapse])
     (enqueue-message (get-in @app-state keypaths/event-ch) [event]))})

(defn slideout-nav-link [data {:keys [href on-click icon-class image label full-width?]}]
  [:a.slideout-nav-link
   {:href href :on-click on-click :class (if full-width? "full-width" "half-width")}
   [:div.slideout-nav-link-inner
    [:img.slideout-nav-link-icon {:class (str "icon-" icon-class) :src image}]
    label]])

(defn logged-in? [data]
  (boolean (get-in data keypaths/user-email)))

(defn own-store? [data]
  (= (get-in data keypaths/user-store-slug)
     (get-in data keypaths/store-slug)))

(defn slideout-nav-component [data owner]
  (om/component
   (html
    [:div.slideout-nav-wrapper {:class (when (get-in data keypaths/menu-expanded)
                                         "slideout-nav-open")}
     (let [store (get-in data keypaths/store)]
       [:nav.slideout-nav (when-not (store :profile_picture_url)
                            {:class "no-picture"})
         [:div.slideout-nav-header
         [:div.slideout-nav-img-container
          [:img.slideout-nav-portrait {:src (store :profile_picture_url)}]]
         [:h2.slideout-nav-title (store :store_name)]]
        [:div.horizontal-nav-list
         [:div.account-detail
          (if (logged-in? data)
            [:a.account-menu-link
             {:href "#"
              :on-click
              (if (get-in data keypaths/account-menu-expanded)
                (utils/enqueue-event data events/control-account-menu-collapse)
                (utils/enqueue-event data events/control-account-menu-expand))}
             [:span.account-detail-name
              (when (own-store? data)
                [:span.stylist-user-label "Stylist:"])
              (get-in data keypaths/user-email)]
             [:figure.down-arrow]]
            [:span
             [:a (close-and-route data events/navigate-sign-in) "Sign In"]
             " | "
             [:a (close-and-route data events/navigate-sign-up) "Sign Up"]])]
         (when (logged-in? data)
           [:ul.account-detail-expanded
            {:class
             (if (get-in data keypaths/account-menu-expanded)
               "open"
               "closed")}
            (when (own-store? data)
              [:div
               [:li
                [:a (close-and-route data events/navigate-stylist-commissions) "Orders & Commissions"]]
               [:li
                [:a (close-and-route data events/navigate-stylist-bonus-credit) "Bonus Credit"]]
               [:li
                [:a (close-and-route data events/navigate-stylist-referrals) "Referrals"]]])
            [:li
             [:a (close-and-route data events/navigate-my-orders) "My Orders"]]
            [:li
             [:a
              (if (own-store? data)
                (close-and-route data events/navigate-stylist-manage-account)
                (close-and-route data events/navigate-manage-account))
              "Manage Account"]]
            [:li
             [:a (close-and-enqueue data events/control-sign-out)
              "Logout"]]])
         [:h2.horizontal-nav-title
          (store :store_name)
          [:ul.header-social-icons
           (when-let [instagram-account (store :instagram_account)]
             [:li.instagram-icon
              [:a.full-link {:href (str "http://instagram.com/" instagram-account) :target "_blank"}]])]]
         [:ul.horizontal-nav-menu
          [:li
           [:a
            (when-let [path (default-taxon-path data)]
              (close-and-route data events/navigate-category
                               {:taxon-path path}))
            "Shop"]]
          [:li [:a (close-and-route data events/navigate-guarantee) "30 Day Guarantee"]]
          [:li [:a (close-and-route data events/navigate-help) "Customer Service"]]]]
        [:ul.slideout-nav-list
         (when (own-store? data)
           [:li.slideout-nav-section.stylist
            [:h3.slideout-nav-section-header.highlight "Manage Store"]
            (slideout-nav-link
             data
             (merge (close-and-route data events/navigate-stylist-commissions)
                    {:icon-class "commissions-and-payouts"
                     :image "/images/slideout_nav/comissions_and_payouts.png"
                     :label "Commissions & Payouts"
                     :full-width? false}))
            (slideout-nav-link
             data
             (merge (close-and-route data events/navigate-stylist-bonus-credit)
                    {:icon-class "sales-bonuses"
                     :image "/images/slideout_nav/sales_bonuses.png"
                     :label "Stylist Bonuses"
                     :full-width? false}))
            (slideout-nav-link
             data
             (merge (close-and-route data events/navigate-stylist-referrals)
                    {:icon-class "stylist-referrals"
                     :image "/images/slideout_nav/stylist_referrals.png"
                     :label "Stylist Referrals"
                     :full-width? false}))
            (slideout-nav-link
             data
             (merge (close-and-route data events/navigate-stylist-manage-account)
                    {:icon-class "edit-profile"
                     :image "/images/slideout_nav/edit_profile.png"
                     :label "Edit Profile"
                     :full-width? false}))
            ])
         [:li.slideout-nav-section
          [:h3.slideout-nav-section-header "Shop"]
          (slideout-nav-link
           data
           (merge
            (if-let [path (default-taxon-path data)]
              (close-and-route data events/navigate-category
                               {:taxon-path path})
              {})
            {:icon-class "hair-extensions"
             :image "/images/slideout_nav/hair_extensions.png"
             :label "Hair Extensions"
             :full-width? true}))]
         [:li.slideout-nav-section
          [:h3.slideout-nav-section-header "My Account"]
          (if (logged-in? data)
            [:div
             (slideout-nav-link
              data
              (merge
               (close-and-route data events/navigate-my-orders)
               {:icon-class "my-orders"
                :image "/images/slideout_nav/my_orders.png"
                :label "My Orders"
                :full-width? true}))
             (slideout-nav-link
              data
              (merge (if (own-store? data)
                       (close-and-route data events/navigate-stylist-manage-account)
                       (close-and-route data events/navigate-manage-account))
                     {:icon-class "manage-account"
                      :image "/images/slideout_nav/manage-account.png"
                      :label "Manage Account"
                      :full-width? false}))
             (slideout-nav-link
              data
              (merge (close-and-enqueue data events/control-sign-out)
                     {:icon-class "logout"
                      :image "/images/slideout_nav/logout.png"
                      :label "Logout"
                      :full-width? false}))]
            [:div
             (slideout-nav-link
              data
              (merge (close-and-route data events/navigate-sign-in)
                     {:icon-class "sign-in"
                      :image "/images/slideout_nav/sign_in.png"
                      :label "Sign In"
                      :full-width? false}))
             (slideout-nav-link
              data
              (merge (close-and-route data events/navigate-sign-up)
                     {:icon-class "join"
                      :image "/images/slideout_nav/join.png"
                      :label "Join"
                      :full-width? false}))]
            )]
         [:li.slideout-nav-section
          [:h3.slideout-nav-section-header "Help"]
          (slideout-nav-link
           data
           (merge (close-and-route data events/navigate-help)
                  {:icon-class "customer-service"
                   :image "/images/slideout_nav/customer_service.png"
                   :label "Customer Service"
                   :full-width? false}))
          (slideout-nav-link
           data
           (merge (close-and-route data events/navigate-guarantee)
                  {:icon-class "30-day-guarantee"
                   :image "/images/slideout_nav/30_day_guarantee.png"
                   :label "30 Day Guarantee"
                   :full-width? false}))]]])])))
