(ns storefront.components.slideout-nav
  (:require [storefront.components.utils :as utils]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.state :as state]))

(defn slideout-nav-link [data {:keys [href on-click icon-class image label full-width?]}]
  [:a.slideout-nav-link
   {:href href :on-click on-click :class (if full-width? "full-width" "half-width")}
   [:div.slideout-nav-link-inner
    [:img.slideout-nav-link-icon {:class (str "icon-" icon-class) :src image}]
    label]])

(defn slideout-nav-component [data owner]
  (om/component
   (html
    [:div.slideout-nav-wrapper {:class (when (get-in data state/menu-expanded-path)
                                         "slideout-nav-open")}
     (let [store (get-in data state/store-path)]
       [:nav.slideout-nav (when-not (store :profile_picture_url)
                            {:class "no-picture"})
        [:div.slideout-nav-header
         [:div.slideout-nav-img-container
          [:img.slideout-nav-portrait {:src (store :profile_picture_url)}]]
         [:h2.slideout-nav-title (store :store_name)]]
        [:div.horizontal-nav-list
         [:div.account-detail
          (if false ;; FIXME: current_user
            [:a.account-menu-link {:href "FIXME: account menu link"}
             [:span.account-detail-name
              (when false ;; FIXME: is own store
                [:span.stylist-user-label "Stylist:"])
              "FIXME: current user email"]
             [:figure.down-arrow]]
            [:span
             [:a {:href "FIXME: login path"} "Sign In"]
             " | "
             [:a {:href "FIXME: sign in path"} "Sign Up"]])]
         (when false ;; FIXME: current_user
           [:ul.account-detail-expanded.closed
            (when false ;; FIXME: is own store
              [:li
               [:a {:href "FIXME: orders & commissions"} "Orders & Commissions"]]
              [:li
               [:a {:href "FIXME: bonus credit"} "Bonus Credit"]]
              [:li
               [:a {:href "FIXME: referrals"} "Referrals"]])
            [:li
             [:a {:href "FIXME: my orders"} "My Orders"]]
            [:li
             [:a {:href "FIXME: manage account depending on stylist or not"} "Manage Account"]]
            [:li
             [:a {:href "FIXME: logout path"} "Logout"]]
            ])
         [:h2.horizontal-nav-title
          (store :store_name)
          [:ul.header-social-icons
           (when-let [instagram-account (store :instagram_account)]
             [:li.instagram-icon
              [:a.full-link {:href (str "http://instagram.com/" instagram-account) :target "_blank"}]])
           ]]
         [:ul.horizontal-nav-menu
          [:li
           (if false ;; FIXME: is own store
             [:a {:href "FIXME: link to shop dropdown"}
              "Shop "
              [:figure.down-arrow]]
             [:a {:href "FIXME: link to shop"} "Shop"])]
          [:li [:a (utils/route-to data events/navigate-guarantee) "30 Day Guarantee"]]
          [:li [:a (utils/route-to data events/navigate-help) "Customer Service"]]]
         (when false ;; FIXME: if current user is stylist
           [:ul.ship-menu-expanded.closed
            [:li [:a {:href "FIXME: path to shop hair extensions"} "Hair Extensions"]]
            [:li [:a {:href "FIXME: path to stylist only products"} "Stylist Only Products"]]])]
        [:ul.slideout-nav-list
         (when false ;; FIXME: own store
           [:li.slideout-nav-section.stylist
            [:h3.slideout-nav-section-header.highlight "Manage Store"]
            (slideout-nav-link
             data
             {:href "FIXME path"
              :icon-class "commissions-and-payouts"
              :image "/images/slideout_nav/commissions_and_payouts.png"
              :label "Commissions & Payouts"
              :full-width? false})
            (slideout-nav-link
             data
             {:href "FIXME path"
              :icon-class "sales-bonuses"
              :image "/images/slideout_nav/sales_bonuses.png"
              :label "Stylist Bonuses"
              :full-width? false})
            (slideout-nav-link
             data
             {:href "FIXME path"
              :icon-class "stylist-referrals"
              :image "/images/slideout_nav/stylist_referrals.png"
              :label "Stylist Referrals"
              :full-width? false})
            (slideout-nav-link
             data
             {:href "FIXME path"
              :icon-class "edit-profile"
              :image "/images/slideout_nav/edit_profile.png"
              :label "Edit Profile"
              :full-width? false})
            ])
         [:li.slideout-nav-section
          [:h3.slideout-nav-section-header "Shop"]
          (slideout-nav-link
           data
           {:href "FIXME path"
            :icon-class "hair-extensions"
            :image "/images/slideout_nav/hair_extensions.png"
            :label "Hair Extensions"
            :full-width? true})
          (when false ;; FIXME is own store
            (slideout-nav-link
             data
             {:href "FIXME path"
              :icon-class "stylist-products"
              :image "/images/slideout_nav/stylist-products.png"
              :label "Stylist Products"
              :full-width? true}))]
         [:li.slideout-nav-section
          [:h3.slideout-nav-section-header "My Account"]
          (if false ;; FIXME logged in user
            [:div
             (slideout-nav-link
              data
              {:href "FIXME path"
               :icon-class "my-orders"
               :image "/images/slideout_nav/my_orders.png"
               :label "My Orders"
               :full-width? true})
             (slideout-nav-link
              data
              {:href "FIXME path"
               :icon-class "manage-account"
               :image "/images/slideout_nav/manage-account.png"
               :label "Manage Account"
               :full-width? false})
             (slideout-nav-link
              data
              {:href "FIXME path"
               :icon-class "logout"
               :image "/images/slideout_nav/logout.png"
               :label "Logout"
               :full-width? false})]
            [:div
             (slideout-nav-link
              data
              {:href "FIXME path"
               :icon-class "sign-in"
               :image "/images/slideout_nav/sign_in.png"
               :label "Sign In"
               :full-width? false})
             (slideout-nav-link
              data
              {:href "FIXME path"
               :icon-class "join"
               :image "/images/slideout_nav/join.png"
               :label "Join"
               :full-width? false})]
            )]
         [:li.slideout-nav-section
          [:h3.slideout-nav-section-header "Help"]
          (slideout-nav-link
           data
           (merge (utils/route-to data events/navigate-help)
                  {:icon-class "customer-service"
                   :image "/images/slideout_nav/customer_service.png"
                   :label "Customer Service"
                   :full-width? false}))
          (slideout-nav-link
           data
           (merge (utils/route-to data events/navigate-guarantee)
                  {:icon-class "30-day-guarantee"
                   :image "/images/slideout_nav/30_day_guarantee.png"
                   :label "30 Day Guarantee"
                   :full-width? false}))]]])])))
