(ns storefront.components.slideout-nav
  (:require [storefront.components.utils :as utils]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.state :as state]))

(defn slideout-nav-component [data owner]
  (om/component
   (html
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
         [:li [:a {:href "FIXME: link to 30 day guarantee"} "30 Day Guarantee"]]
         [:li [:a {:href "FIXME: link to customer service"} "Customer Service"]]]
        (when false ;; FIXME: if current user is stylist
          [:ul.ship-menu-expanded.closed
           [:li [:a {:href "FIXME: path to shop hair extensions"} "Hair Extensions"]]
           [:li [:a {:href "FIXME: path to stylist only products"} "Stylist Only Products"]]])]]))))
