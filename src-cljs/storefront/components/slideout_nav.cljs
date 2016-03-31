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
            [storefront.components.formatters :refer [as-money]]
            [storefront.hooks.fastpass :as fastpass]))

(defn fake-href [event & [args]]
  {:href "#"
   :on-click (utils/send-event-callback event args)})

(defn navigate-hair [shop-now-navigation-message]
  (apply utils/route-to shop-now-navigation-message))

(defn navigate-kits [kits-path]
  (when kits-path
    (utils/route-to events/navigate-category
                    {:taxon-path kits-path})))

(defn navigate-community
  "Can't be a def because (fastpass/community-url) is impure."
  []
  {:href (or (fastpass/community-url) "#")
   :on-click (utils/send-event-callback events/external-redirect-community)})

(defn drop-down [expanded? menu-keypath [link-tag & link-contents] menu]
  [:.relative.z1
   (into [link-tag
          (fake-href events/control-menu-expand {:keypath menu-keypath})]
         link-contents)
   (when expanded?
     [:div
      {:on-click (utils/send-event-callback events/control-menu-collapse {:keypath menu-keypath})}
      [:.fixed.overlay]
      menu])])

(defn account-store-credit [available-store-credit]
  (when (pos? available-store-credit)
    [:span.stylist-user-label
     "Store credit:"
     [:span.store-credit-amount (as-money available-store-credit)]]))

(def guest-account-menu
  [:span
   [:a (utils/route-to events/navigate-sign-in) "Sign In"]
   " | "
   [:a (utils/route-to events/navigate-sign-up) "Sign Up"]])

(defn stylist-account-menu [{:keys [expanded? available-store-credit user-email]}]
  (drop-down
   expanded?
   keypaths/account-menu-expanded
   [:a.account-menu-link
    (account-store-credit available-store-credit)
    [:span.account-detail-name [:span.stylist-user-label "Stylist:"] user-email]
    [:figure.down-arrow]]
   [:ul.account-detail-expanded
    [:li [:a (utils/route-to events/navigate-stylist-dashboard-commissions) "Dashboard"]]
    [:li [:a (utils/route-to events/navigate-stylist-manage-account) "Manage Account"]]
    [:li [:a (navigate-community) "Stylist Community"]]
    [:li [:a (fake-href events/control-sign-out) "Logout"]]]))

(defn customer-account-menu [{:keys [expanded? available-store-credit user-email]}]
  (drop-down
   expanded?
   keypaths/account-menu-expanded
   [:a.account-menu-link
    (account-store-credit available-store-credit)
    [:span.account-detail-name user-email]
    [:figure.down-arrow]]
   [:ul.account-detail-expanded
    [:li [:a (utils/route-to events/navigate-account-referrals) "Refer A Friend"]]
    [:li [:a (utils/route-to events/navigate-account-manage) "Manage Account"]]
    [:li [:a (fake-href events/control-sign-out) "Logout"]]]))

(defn invasive-top-nav-component [{:keys [own-store? user-email] :as cursors} _]
  ;; WAT 1: This is within the slideout nav, but is invisible on small screens
  ;; WAT 2: It is part of the secondary nav, but overlays primary nav through positioning hacks
  (om/component
   (html
    [:div.account-detail
     (cond
       own-store?           (stylist-account-menu cursors)
       (boolean user-email) (customer-account-menu cursors)
       :else                guest-account-menu)])))

(defn stylist-shop-link [{:keys [expanded? navigate-hair-message stylist-kits-path]}]
  (drop-down
   expanded?
   keypaths/shop-menu-expanded
   [:a "Shop " [:figure.down-arrow]]
   [:ul.shop-menu-expanded.top-0
    [:li [:a (navigate-hair navigate-hair-message) "Hair Extensions"]]
    [:li [:a (navigate-kits stylist-kits-path) "Stylist Only Products"]]]))

(defn non-stylist-shop-link [{:keys [navigate-hair-message]}]
  [:a (navigate-hair navigate-hair-message) "Shop"])

(defn middle-nav-component [{:keys [navigate-hair-message expanded? stylist-kits-path own-store? store-name] :as cursors} _]
  (om/component
   (html
    [:div
     [:h2.horizontal-nav-title store-name]
     [:ul.horizontal-nav-menu
      [:li (if own-store?
             (stylist-shop-link {:navigate-hair-message navigate-hair-message
                                 :expanded?             expanded?
                                 :stylist-kits-path     stylist-kits-path})
             (non-stylist-shop-link {:navigate-hair-message navigate-hair-message}))]
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

(defn nav-hair-box [navigate-hair-message]
  (nav-box "hair-extensions" "Hair Extensions" (navigate-hair navigate-hair-message) {:full-width? true}))

(defn logout-box []
  (nav-box "logout" "Logout" (fake-href events/control-sign-out)))

(defn slideout-store-credit [available-store-credit]
  (when (pos? available-store-credit)
    [:li.slideout-nav-section
     [:h4.store-credit
      [:span.label "Available store credit:"]
      [:span.value (as-money available-store-credit)]]]))

(def slideout-help
  [:li.slideout-nav-section
   [:h3.slideout-nav-section-header "Help"]
   (nav-box "customer-service" "Customer Service" (utils/route-to events/navigate-help))
   (nav-box "30-day-guarantee" "30 Day Guarantee" (utils/route-to events/navigate-guarantee))])

(defn slideout-stylist-nav [{:keys [navigate-hair-message stylist-kits-path available-store-credit]}]
  [:ul.slideout-nav-list
   (slideout-store-credit available-store-credit)
   [:li.slideout-nav-section.stylist
    [:h3.slideout-nav-section-header.highlight "Manage Store"]
    (nav-box "stylist-dashboard" "Dashboard" (utils/route-to events/navigate-stylist-dashboard-commissions))
    (nav-box "edit-profile" "Edit Profile" (utils/route-to events/navigate-stylist-manage-account))
    (nav-box "community" "Stylist Community" (navigate-community) {:full-width? true})]
   [:li.slideout-nav-section
    [:h3.slideout-nav-section-header "Shop"]
    (nav-hair-box navigate-hair-message)
    (nav-box "stylist-products" "Stylist Products" (navigate-kits stylist-kits-path) {:full-width? true})]
   [:li.slideout-nav-section
    [:h3.slideout-nav-section-header "My Account"]
    (nav-box "manage-account" "Manage Account" (utils/route-to events/navigate-stylist-manage-account))
    (logout-box)]
   slideout-help])

(defn slideout-customer-nav [{:keys [navigate-hair-message available-store-credit]}]
  [:ul.slideout-nav-list
   (slideout-store-credit available-store-credit)
   [:li.slideout-nav-section
    [:h3.slideout-nav-section-header "Shop"]
    (nav-hair-box navigate-hair-message)]
   [:li.slideout-nav-section
    [:h3.slideout-nav-section-header "My Account"]
    (nav-box "refer-friend" "Refer A Friend" (utils/route-to events/navigate-account-referrals) {:full-width? true})
    (nav-box "manage-account" "Manage Account" (utils/route-to events/navigate-account-manage))
    (logout-box)]
   slideout-help])

(defn slideout-guest-nav [{:keys [navigate-hair-message]}]
  [:ul.slideout-nav-list
   [:li.slideout-nav-section
    [:h3.slideout-nav-section-header "Shop"]
    (nav-hair-box navigate-hair-message)]
   [:li.slideout-nav-section
    [:h3.slideout-nav-section-header "My Account"]
    (nav-box "sign-in" "Sign In" (utils/route-to events/navigate-sign-in))
    (nav-box "join" "Join" (utils/route-to events/navigate-sign-up))]
   slideout-help])

(defn slideout-nav-component [{:keys [store
                                      slid-out?
                                      account-menu-expanded?
                                      shop-menu-expanded?
                                      available-store-credit
                                      user-email
                                      own-store?
                                      navigate-hair-message
                                      stylist-kits-path]} _]
  (let [{store-photo :profile_picture_url
         store-name  :store_name} store]
    (om/component
     (html
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
         [:h2.slideout-nav-title store-name]]
        ;; WAT: This is invisible on small screens
        [:div.horizontal-nav-list
         (om/build invasive-top-nav-component
                   {:own-store?             own-store?
                    :expanded?              account-menu-expanded?
                    :available-store-credit available-store-credit
                    :user-email             user-email})
         (om/build middle-nav-component
                   {:navigate-hair-message navigate-hair-message
                    :expanded?             shop-menu-expanded?
                    :stylist-kits-path     stylist-kits-path
                    :own-store?            own-store?
                    :store-name            store-name})]
        (when slid-out?
          (let [slideout-query {:available-store-credit available-store-credit
                                :navigate-hair-message  navigate-hair-message
                                :stylist-kits-path      stylist-kits-path}]
            [:div
             {:on-click (utils/send-event-callback events/control-menu-collapse
                                                   {:keypath keypaths/menu-expanded})}
             [:.fixed.overlay]
             (cond
               own-store?           (slideout-stylist-nav slideout-query)
               (boolean user-email) (slideout-customer-nav slideout-query)
               :else                (slideout-guest-nav slideout-query))]))]]))))

(defn slideout-nav-query [data]
  {:store                  (get-in data keypaths/store)
   :slid-out?              (get-in data keypaths/menu-expanded)
   :account-menu-expanded? (get-in data keypaths/account-menu-expanded)
   :shop-menu-expanded?    (get-in data keypaths/shop-menu-expanded)
   :available-store-credit (get-in data keypaths/user-total-available-store-credit)
   :user-email             (get-in data keypaths/user-email)
   :own-store?             (own-store? data)
   :navigate-hair-message  (navigation/shop-now-navigation-message data)
   :stylist-kits-path      (default-stylist-taxon-path data)})
