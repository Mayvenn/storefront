(ns storefront.components.slideout-nav
  (:require [storefront.components.utils :as utils]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.routes :as routes]
            [storefront.taxons :refer [taxon-path-for default-taxon-path]]
            [storefront.messages :refer [send]]))

(defn close-and-route [app-state event & [args]]
  {:href
   (routes/path-for @app-state event args)
   :on-click
   (fn [e]
     (.preventDefault e)
     (send app-state events/control-menu-collapse)
     (send app-state events/control-account-menu-collapse)
     (routes/enqueue-navigate @app-state event args))})

(defn close-and-enqueue [app-state event]
  {:href "#"
   :on-click
   (fn [e]
     (.preventDefault e)
     (send app-state events/control-menu-collapse)
     (send app-state event))})

(defn slideout-nav-link [data {:keys [href on-click icon-class label full-width?]}]
  [:a.slideout-nav-link
   {:href href :on-click on-click :class (if full-width? "full-width" "half-width")}
   [:div.slideout-nav-link-inner
    [:div.slideout-nav-link-icon {:class (str "icon-" icon-class)}]
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
           (if-let [profile-picture-url (store :profile_picture_url)]
             [:img.slideout-nav-portrait {:src profile-picture-url}]
             [:div.slideout-nav-portrait.missing-picture])]
         [:h2.slideout-nav-title (store :store_name)]]
        [:div.horizontal-nav-list
         [:div.account-detail
          (if (logged-in? data)
            [:a.account-menu-link
             {:href "#"
              :on-click
              (if (get-in data keypaths/account-menu-expanded)
                (utils/send-event-callback data events/control-account-menu-collapse)
                (utils/send-event-callback data events/control-account-menu-expand))}
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
                     :label "Commissions & Payouts"
                     :full-width? false}))
            (slideout-nav-link
             data
             (merge (close-and-route data events/navigate-stylist-bonus-credit)
                    {:icon-class "sales-bonuses"
                     :label "Stylist Bonuses"
                     :full-width? false}))
            (slideout-nav-link
             data
             (merge (close-and-route data events/navigate-stylist-referrals)
                    {:icon-class "stylist-referrals"
                     :label "Stylist Referrals"
                     :full-width? false}))
            (slideout-nav-link
             data
             (merge (close-and-route data events/navigate-stylist-manage-account)
                    {:icon-class "edit-profile"
                     :label "Edit Profile"
                     :full-width? false}))])
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
                :label "My Orders"
                :full-width? true}))
             (slideout-nav-link
              data
              (merge (if (own-store? data)
                       (close-and-route data events/navigate-stylist-manage-account)
                       (close-and-route data events/navigate-manage-account))
                     {:icon-class "manage-account"
                      :label "Manage Account"
                      :full-width? false}))
             (slideout-nav-link
              data
              (merge (close-and-enqueue data events/control-sign-out)
                     {:icon-class "logout"
                      :label "Logout"
                      :full-width? false}))]
            [:div
             (slideout-nav-link
              data
              (merge (close-and-route data events/navigate-sign-in)
                     {:icon-class "sign-in"
                      :label "Sign In"
                      :full-width? false}))
             (slideout-nav-link
              data
              (merge (close-and-route data events/navigate-sign-up)
                     {:icon-class "join"
                      :label "Join"
                      :full-width? false}))])]
         [:li.slideout-nav-section
          [:h3.slideout-nav-section-header "Help"]
          (slideout-nav-link
           data
           (merge (close-and-route data events/navigate-help)
                  {:icon-class "customer-service"
                   :label "Customer Service"
                   :full-width? false}))
          (slideout-nav-link
           data
           (merge (close-and-route data events/navigate-guarantee)
                  {:icon-class "30-day-guarantee"
                   :label "30 Day Guarantee"
                   :full-width? false}))]]])])))
