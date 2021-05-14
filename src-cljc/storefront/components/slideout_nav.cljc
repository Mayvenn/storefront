(ns storefront.components.slideout-nav
  (:require [catalog.menu :as menu]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.orders :as orders]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.header :as header]
            [storefront.components.money-formatters :refer [as-money]]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [ui.promo-banner :as promo-banner]
            [storefront.components.svg :as svg]
            [storefront.accessors.experiments :as experiments]
            [mayvenn.live-help.core :as live-help]))

(defn burger-header [cart]
  (component/html
   (header/mobile-nav-header
    {:class "border-bottom border-gray bg-white black"
     :style {:height "70px"}}
    ;; HACKY(jeff): b/c of relative+absolute position of big-x, padding-left also increases y-offset, so we use negative margin to correct it
    (component/html
     [:div.mtn2.pl4
      {:style {:width  "100%"
               :height "100%"}}
      (ui/big-x {:data-test "close-slideout"
                 :attrs     {:on-click #(messages/handle-message events/control-menu-collapse-all)}})])
    (ui/clickable-logo {:event     events/navigate-home
                        :data-test "header-logo"
                        :height    "29px"})
    (ui/shopping-bag {:style     {:height "70px" :width "80px"}
                      :data-test "mobile-cart"}
                     cart))))

(defn ^:private marquee-col [content]
  (component/html
   [:div.flex-auto
    {:style {:flex-basis 0}}
    content]))

(defn ^:private marquee-row [left-content right-content]
  (component/html
   [:div.flex.my3
    (marquee-col left-content)
    [:div.pr3]
    (marquee-col right-content)]))

(defn ^:private stylist-portrait [{:keys [stylist-portrait]}]
  (component/html
   (let [header-image-size 40
         portrait-status   (:status stylist-portrait)]
     (if (#{"approved" "pending"} portrait-status)
       (ui/circle-picture {:class "mr2 flex items-center"
                           :width (str header-image-size "px")}
                          (ui/square-image stylist-portrait header-image-size))
       [:a.mr2.flex.items-center (utils/route-to events/navigate-stylist-account-profile)
        (ui/ucare-img {:width           header-image-size
                       :picture-classes "flex"}
                      "81bd063f-56ba-4e9c-9aef-19a1207fd422")]))))

(defn ^:private account-info-marquee [signed-in {:keys [email store-credit]}]
  (component/html
   (when (-> signed-in ::auth/at-all)
     [:div.my3.flex.flex-wrap
      (when (pos? store-credit)
        [:div.mr4.mb2
         [:div.title-3.proxima.shout "Credit"]
         [:div.content-2.proxima (as-money store-credit)]])
      [:div
       [:div.title-3.proxima.shout "Signed in with"]
       [:a.inherit-color.content-2.proxima
        (merge
         {:data-test "signed-in-as"}
         (utils/route-to (if (-> signed-in ::auth/as (= :stylist))
                           events/navigate-stylist-account-profile
                           events/navigate-account-manage)))
        email]]])))

(defn ^:private stylist-actions
  [vouchers?]
  (component/html
   [:div
    (when vouchers?
      (ui/button-medium-primary (assoc (utils/route-to events/navigate-voucher-redeem)
                                       :data-test    "redeem-voucher"
                                       :class "mb2")
                                "Redeem Client Voucher"))
    [:div.flex.flex-wrap
     (ui/button-small-secondary (assoc (utils/route-to events/navigate-stylist-account-profile)
                                       :data-test "account-settings"
                                       :class "mr2 mt2")
                                "Settings")
     (ui/button-small-secondary (assoc (utils/route-to events/navigate-v2-stylist-dashboard-orders)
                                       :data-test "dashboard"
                                       :class "mr2 mt2")
                                "Dashboard")]]))

(def ^:private user-actions
  (component/html
   (ui/button-large-secondary (assoc (utils/route-to events/navigate-account-manage)
                                     :data-test "account-settings")
                              "Account")))

(def ^:private guest-actions
  (component/html
   [:div.flex.items-center.justify-between
    [:div.col-3
     (ui/button-small-secondary (assoc (utils/route-to events/navigate-sign-in)
                                       :data-test "sign-in")
                                "Sign in")]
    [:div.col-8
     (ui/button-small-underline-primary
      (assoc (utils/route-to events/navigate-sign-up)
             :data-test "sign-up")
      "Or sign up now, get offers!")]]))

(defn ^:private actions-marquee
  [signed-in vouchers?]
  (case (-> signed-in ::auth/as)
    :stylist (stylist-actions vouchers?)
    :user    user-actions
    :guest   guest-actions))

(defn ^:private caretize-content
  [content]
  (component/html
   [:div.col-8.flex.justify-between.items-center
    [:span.medium.flex-auto content]
    ^:inline (ui/forward-caret {:width  16
                                :height 16})]))

(defn ^:private menu-row
  [{:slide-out-nav-menu-item/keys [target nested? id new-primary primary]}]
  (component/html
   [:li {:key id}
    [:div.py3
     [:a.block.inherit-color.flex.items-center.content-1.proxima
      (merge {:data-test id}
             (if nested?
               (apply utils/fake-href target)
               (apply utils/route-to target)))
      [:span.col-2.title-3.proxima.center (when new-primary
                                            new-primary)]
      (if nested?
        (caretize-content primary)
        [:span.medium.flex-auto primary])]]]))

(defn ^:private content-row
  [{:slide-out-nav-content-item/keys [id primary target]}]
  (component/html
   [:li {:key id}
    [:div.py3
     [:a.block.inherit-color.flex.items-center.content-2.proxima
      (merge
       {:data-test id}
       (if (map? target)
         target
         (apply utils/route-to target)))
      [:span.col-2]
      primary]]]))

(defn ^:private menu-area
  [{:slide-out-nav/keys [content-items menu-items]}]
  (component/html
   [:ul.list-reset.mb3.mt5
    (for [item menu-items]
      (menu-row item))
    [:div.mt5
     (for [[i row] (map-indexed vector content-items)]
       [:div
        {:key (str i)}
        (when-not (zero? i)
          [:div.border-bottom.border-cool-gray.col-8.m-auto])
        (content-row row)])]]))

(def ^:private sign-out-area
  (component/html
   (marquee-row
    (ui/button-large-secondary (assoc (utils/fake-href events/control-sign-out)
                                      :data-test "sign-out")
                               "Sign out")
    [:div])))

(defn ^:private gallery-link
  [stylist-experience past-appointments?]
  (component/html
   [:div
    (ui/button-small-underline-primary
     (if (and past-appointments? (= stylist-experience "aladdin"))
       (utils/route-to events/navigate-gallery-appointments)
       (utils/route-to events/navigate-gallery-edit))
     "Edit Gallery")]))

(defcomponent ^:private root-menu
  [{:keys [user signed-in vouchers? stylist-experience past-appointments?] :as data} owner opts]
  [:div
   [:div.bg-cool-gray.p4
    (when (auth/stylist? signed-in)
      [:div.flex.items-center (stylist-portrait user) (gallery-link stylist-experience past-appointments?)])
    (account-info-marquee signed-in user)
    [:div.my3
     (actions-marquee signed-in vouchers?)]]
   [:div.px3
    (menu-area data)]
   (when (-> signed-in ::auth/at-all)
     [:div.px6.border-top.border-gray
      sign-out-area])])

(def live-help-banner<
  {:live-help-banner/primary                "Need help?"
   :live-help-banner/id                     "hamburger-need-help"
   :live-help-button/cta-label              "Chat with us"
   :live-help-button/cta-target             [events/flow|live-help|opened {:location "hamburger-menu"}]
   :live-help-button/id                     "hamburger-chat-with-us"
   :live-help-button/label-and-border-color "#FFF"
   :live-help-button/icon                   [:svg/chat-bubble-diamonds-p-color {:class "fill-white mr1"
                                                                                :style {:height "14px"
                                                                                        :width  "13px"}}]})

(component/defcomponent live-help-banner-component
  [{:live-help-banner/keys [primary id] :as query} _ _]
  (when id
    [:div.bg-p-color.white.flex.justify-between.px3.py1.shout.proxima.title-3.mb3
     {:style {:font-size "14px"}}
     primary
     (component/build live-help/button-component query)]))

(defcomponent component
  [{:keys [cart on-taxon? menu-data promo-banner live-help-banner] :as data}
   _
   _]
  [:div
   (promo-banner/static-organism promo-banner nil nil)
   (component/build live-help-banner-component live-help-banner)

   [:div.top-0.sticky.z4
    (burger-header cart)]
   (if on-taxon?
     (component/build menu/component menu-data)
     (component/build root-menu data))])

(defn query [data]
  (-> (header/basic-query data)
      (cond->
          (live-help/kustomer-started? data)
        (->
         (assoc :live-help-banner live-help-banner<)
         (dissoc :promo-banner)))
      (assoc-in [:user :store-credit] (get-in data keypaths/user-total-available-store-credit))
      (assoc-in [:cart :quantity] (orders/displayed-cart-count (get-in data keypaths/order)))
      (assoc-in [:menu-data] (case (get-in data keypaths/current-traverse-nav-menu-type)
                               :category         (menu/category-query data)
                               :shop-looks       (menu/shop-looks-query data)
                               :shop-bundle-sets (menu/shop-bundle-sets-query data)
                               nil))))

(defn built-component [data opts]
  (component/build component (query data) nil))
