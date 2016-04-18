(ns storefront.components.slideout-nav
  (:require [storefront.components.utils :as utils]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.routes :as routes]
            [storefront.messages :as messages]
            [storefront.accessors.taxons :refer [default-stylist-taxon-slug]]
            [storefront.accessors.stylists :refer [own-store?]]
            [storefront.accessors.navigation :as navigation]
            [storefront.components.formatters :refer [as-money]]
            [storefront.hooks.experiments :as experiments]
            [storefront.hooks.fastpass :as fastpass]))

(defn navigate-hair [shop-now-navigation-message]
  (apply utils/route-to shop-now-navigation-message))

(defn navigate-kits [kits-path]
  (when kits-path
    (utils/route-to events/navigate-category
                    {:taxon-slug kits-path})))

(defn navigate-community
  "Can't be a def because (fastpass/community-url) is impure."
  []
  {:href (or (fastpass/community-url) "#")
   :on-click (utils/send-event-callback events/external-redirect-community)})

(defn account-store-credit [available-store-credit]
  (when (pos? available-store-credit)
    [:span.stylist-user-label
     "Store credit:"
     [:span.store-credit-amount (as-money available-store-credit)]]))

(def guest-account-menu
  (html
   [:span
    [:a (utils/route-to events/navigate-sign-in) "Sign In"]
    " | "
    [:a (utils/route-to events/navigate-sign-up) "Sign Up"]]))

(defn stylist-account-menu [{:keys [expanded? available-store-credit user-email]} _]
  (om/component
   (html
    (utils/drop-down
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
      [:li [:a (utils/fake-href events/control-sign-out) "Logout"]]]))))

(defn customer-account-menu [{:keys [expanded? available-store-credit user-email]} _]
  (om/component
   (html
    (utils/drop-down
     expanded?
     keypaths/account-menu-expanded
     [:a.account-menu-link
      (account-store-credit available-store-credit)
      [:span.account-detail-name user-email]
      [:figure.down-arrow]]
     [:ul.account-detail-expanded
      [:li [:a (utils/route-to events/navigate-account-referrals) "Refer A Friend"]]
      [:li [:a (utils/route-to events/navigate-account-manage) "Manage Account"]]
      [:li [:a (utils/fake-href events/control-sign-out) "Logout"]]]))))

(defn stylist-shop-link [{:keys [expanded? navigate-hair-message stylist-kits-path]}]
  (utils/drop-down
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
  (nav-box "logout" "Logout" (utils/fake-href events/control-sign-out)))

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
         ;; WAT: This is part of the secondary nav bar, but overlays primary nav through positioning hacks
         [:div.account-detail
          (let [account-menu-query {:expanded?              account-menu-expanded?
                                    :available-store-credit available-store-credit
                                    :user-email             user-email}]
            (cond
              own-store?           (om/build stylist-account-menu account-menu-query)
              (boolean user-email) (om/build customer-account-menu account-menu-query)
              :else                guest-account-menu))]
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
             {:on-click (utils/send-event-callback events/control-menu-collapse-all)}
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
   :stylist-kits-path      (default-stylist-taxon-slug data)})

(def logo
  (html
   [:a.block.img-logo.bg-no-repeat.bg-contain.teal.pp3
    (merge {:style {:height "30px"}
            :title "Mayvenn"}
           (utils/route-to events/navigate-home))]))

(def menu-x
  (html
   [:div {:style {:width "60px"}}
    [:div.relative.rotate-45.p2 {:style {:height "60px"}}
     [:div.absolute.border-right.border-gray {:style {:width "18px" :height "36px"}}]
     [:div.absolute.border-bottom.border-gray {:style {:width "36px" :height "18px"}}]]]))

(def new-taxon? #{"closures" "frontals"})

(defn products-section [title taxons]
  [:div
   [:.clearfix
    [:.col.col-2 utils/nbsp]
    [:.col.col-10
     [:.py1.border-bottom.border-light-gray
      title]]]
   [:ul.list-reset.my1
    (for [{:keys [name slug]} taxons]
      [:li
       {:key slug}
       [:a
        (utils/route-to events/navigate-category {:taxon-slug slug})
        [:.clearfix
         [:.col.col-2.px1.pyp1
          (if (new-taxon? slug)
            [:.h6.inline-block.border.border-gray.gray.pp2
             [:div {:style {:margin-bottom "-2px"}} "NEW"]]
            utils/nbsp)]
         [:.col.col-10.line-height-3
          [:.teal.titleize
           (case slug "stylist-products" "kits" name)]]]]])]])

(def is-closure? (comp #{"frontals" "closures"} :slug))
(def is-stylist-product? (comp #{"stylist-products"} :slug))
(defn is-extension? [taxon]
  (not (or (is-closure? taxon)
           (is-stylist-product? taxon))))

(defn extensions-section [taxons]
  (products-section "Extensions" (filter is-extension? taxons)))

(defn closures-section [taxons]
  (products-section "Closures" (filter is-closure? taxons)))

(defn stylist-products-section [taxons]
  (products-section "Stylist Products" (filter is-stylist-product? taxons)))

(def help-section
  (html
   [:div.border-bottom.border-light-gray
    [:.ml3
     [:ul.list-reset.my0.py2
      [:li.line-height-3
       [:a
        (utils/route-to events/navigate-guarantee)
        [:.clearfix
         [:.col.col-2.px1 utils/nbsp]
         [:.col.col-10.teal.block
          "Our Guarantee"]]]]
      [:li.line-height-3
       [:a
        (utils/route-to events/navigate-help)
        [:.clearfix
         [:.col.col-2.px1 utils/nbsp]
         [:a.col.col-10.teal.block
          "Contact Us"]]]]]]]))

(def sign-in-section
  (html
   [:.ml3
    [:.clearfix.py2
     [:.col.col-6.p1
      [:a.btn.btn-outline.teal.col-12
       (utils/route-to events/navigate-sign-in)
       "Sign In"]]
     [:div.col.col-6.p1.center.h5.line-height-2
      [:.gray "No account?"]
      [:a.teal (utils/route-to events/navigate-sign-up) "Sign Up"]]]]))

(def sign-out-section
  (html
   [:.center.col-12.p2
    {:on-click
     (utils/send-event-callback events/control-sign-out)}
    [:.btn.teal "Log out"]]))

(defn customer-shop-section [taxons]
  [:.bg-pure-white.border-bottom.border-light-gray
   [:.ml3
    [:.py2
     [:.sans-serif.medium "Shop"]
     (extensions-section taxons)
     (closures-section taxons)]]])

(defn stylist-shop-section [taxons]
  [:.bg-pure-white.border-bottom.border-light-gray
   [:.ml3
    [:.py2
     [:.sans-serif.medium "Shop"]
     (extensions-section taxons)
     (closures-section taxons)
     (stylist-products-section taxons)]]])

(defn new-component [{:keys [slid-out? taxons stylist? user-email]} owner]
  (om/component
   (html
    (when slid-out?
      [:div.h3
       {:on-click #(messages/handle-message events/control-menu-collapse-all)}
       [:.fixed.overlay.bg-darken-2.z3
        {:on-click (utils/send-event-callback events/control-menu-collapse-all)}]
       [:.fixed.overflow-auto.top-0.left-0.col-10.z3.lit.bg-silver
        {:style {:max-height "100%"}}
        [:div.border-bottom.border-light-gray.flex.items-center.bg-pure-white
         menu-x
         [:.flex-auto.p2 logo]]
        [:div
         (if stylist?
           (stylist-shop-section taxons)
           (customer-shop-section taxons))
         help-section
         (if user-email
           sign-out-section
           sign-in-section)]]]))))

(defn query [data]
  {:slid-out?  (get-in data keypaths/menu-expanded)
   :stylist?   (own-store? data)
   :user-email (get-in data keypaths/user-email)
   :taxons     (cond->> (get-in data keypaths/taxons)
                 (not (experiments/frontals? data)) (remove (comp #{"frontals"} :slug)))})

(defn built-new-component [data]
  (om/build new-component (query data)))
