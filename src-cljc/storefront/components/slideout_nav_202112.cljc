(ns storefront.components.slideout-nav-202112
  (:require #?(:cljs [storefront.api :as api])
            [catalog.menu :as menu]
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
            [storefront.transitions :as transitions]
            [ui.promo-banner :as promo-banner]
            [storefront.components.svg :as svg]
            [storefront.accessors.experiments :as experiments]))

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

(defn ^:private caretize-content
  [content]
  (component/html
   [:div.col-8.flex.justify-between.items-center
    [:span.medium.flex-auto content]
    ^:inline (ui/forward-caret {:width  16
                                :height 16})]))

(defn ^:private menu-row
  [{:slide-out-nav-menu-item/keys [target nested? id label-icon new-primary primary]}]
  (component/html
   [:li {:key id}
    [:div.py3
     [:a.block.inherit-color.flex.items-center.content-1.proxima
      (merge {:data-test id}
             (cond
               (map? target) target
               nested?       (apply utils/fake-href target)
               :else         (apply utils/route-to target)))
      [:div.col-2.px2.flex.items-center.justify-end
       (when new-primary [:div.title-3.proxima.center new-primary])
       (when label-icon (svg/symbolic->html [label-icon {:style {:width "1em" :height "1em"}}]))]
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

(defn ^:private account-area
  [{:account-tab/keys [content-items menu-items]}]
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

(defn ^:private gallery-link
  [stylist-experience past-appointments?]
  (component/html
   [:div
    (ui/button-small-underline-primary
     (merge
      {:data-test "edit-gallery"}
      (if (and past-appointments? (= stylist-experience "aladdin"))
        (utils/route-to events/navigate-gallery-appointments)
        (utils/route-to events/navigate-gallery-edit)))
     "Edit Gallery")]))

(def ^:private tabs
  [{:id                 :menu
    :title              "Menu"
    :not-selected-class "border-right"}
   {:id                 :account
    :title              "Account"
    :not-selected-class "border-left"}])

(defn tabs-component [{:slideout-tabs/keys [tabs selected-tab]}]
  [:div.flex.flex-wrap.content-2
   (for [{:keys [id title navigate not-selected-class]} tabs]
     [:a.col-6.black.center.p2
      (merge (utils/fake-href events/slideout-nav-tab-selected {:tab     id
                                                                :keypath selected-tab})
             {:key       (str "nav-tabs-" id)
              :data-test (str "nav-" (name id))
              :class     (if (= id selected-tab)
                           "bg-white border-top border-p-color border-width-4"
                           (str "bg-cool-gray border-width-1 " not-selected-class))})
      title])] )

(defcomponent ^:private root-menu
  [{:keys [user signed-in vouchers? stylist-experience past-appointments? order-details?] :as data} owner opts]
  [:div
   (tabs-component data)
   (case (:slideout-tabs/selected-tab data)
     :menu
     [:div.px3
      (menu-area data)]

     :account
     [:div.bg-white.p4
      (account-info-marquee signed-in user)
      (when (auth/stylist? signed-in)
        [:div.flex.items-center (stylist-portrait user) (gallery-link stylist-experience past-appointments?)])
      (account-area data)])])

(defcomponent component
  [{:keys [cart on-taxon? menu-data promo-banner] :as data} _ _]
  [:div
   (promo-banner/static-organism promo-banner nil nil)

   [:div.top-0.sticky.z4
    (burger-header cart)]
   (if on-taxon?
     (component/build menu/component menu-data)
     (component/build root-menu data))])

(defn query [data]
  (let [selected-tab (get-in data keypaths/slideout-nav-selected-tab)
        auth         (auth/signed-in data)
        user-type    (-> auth ::auth/as)
        signed-in?   (-> auth ::auth/at-all)
        vouchers?    (experiments/dashboard-with-vouchers? data)]
    (merge
     (-> (header/basic-query data)
         (assoc-in [:user :store-credit] (get-in data keypaths/user-total-available-store-credit))
         (assoc-in [:user :store-credit] (get-in data keypaths/user-total-available-store-credit))
         (assoc-in [:menu-data] (case (get-in data keypaths/current-traverse-nav-menu-type)
                                  :category         (menu/category-query data)
                                  :shop-looks       (menu/shop-looks-query data)
                                  :shop-bundle-sets (menu/shop-bundle-sets-query data)
                                  nil)))
     {:slideout-tabs/tabs         tabs
      :slideout-tabs/selected-tab selected-tab}
     {:account-tab/menu-items (concat (when (= user-type :guest)
                                        [{:slide-out-nav-menu-item/target  [events/navigate-sign-in]
                                          :slide-out-nav-menu-item/id      "sign-in"
                                          :slide-out-nav-menu-item/primary "Sign In"}
                                         {:slide-out-nav-menu-item/target  [events/navigate-sign-up]
                                          :slide-out-nav-menu-item/id      "sign-up"
                                          :slide-out-nav-menu-item/primary "Sign Up for an Account"}])
                                      (when (= user-type :user)
                                        [{:slide-out-nav-menu-item/target  [events/navigate-account-manage]
                                          :slide-out-nav-menu-item/id      "account-settings"
                                          :slide-out-nav-menu-item/primary "Account"}
                                         {:slide-out-nav-menu-item/target  [events/navigate-yourlooks-order-details]
                                          :slide-out-nav-menu-item/id      "my-next-look"
                                          :slide-out-nav-menu-item/primary "My Next Look"}])
                                      (when (= user-type :stylist)
                                        (when vouchers? [{:slide-out-nav-menu-item/target  [events/navigate-voucher-redeem]
                                                          :slide-out-nav-menu-item/id      "redeem-voucher"
                                                          :slide-out-nav-menu-item/primary "Redeem Client Voucher"}])
                                        [{:slide-out-nav-menu-item/target  [events/navigate-yourlooks-order-details]
                                          :slide-out-nav-menu-item/id      "my-next-look"
                                          :slide-out-nav-menu-item/primary "My Next Look"}
                                         {:slide-out-nav-menu-item/target  [events/navigate-stylist-account-profile]
                                          :slide-out-nav-menu-item/id      "account-settings"
                                          :slide-out-nav-menu-item/primary "Settings"}
                                         {:slide-out-nav-menu-item/target  [events/navigate-v2-stylist-dashboard-orders]
                                          :slide-out-nav-menu-item/id      "dashboard"
                                          :slide-out-nav-menu-item/primary "Dashboard"}])
                                      (when signed-in? [{:slide-out-nav-menu-item/target  [events/control-sign-out]
                                                         :slide-out-nav-menu-item/id      "sign-out"
                                                         :slide-out-nav-menu-item/primary "Log Out"}]))})))

(defmethod transitions/transition-state events/slideout-nav-tab-selected [_ _ {:keys [tab]} app-state]
  (let [selected-tab (get-in app-state keypaths/slideout-nav-selected-tab)]
    (when-not (= tab selected-tab)
      (assoc-in app-state keypaths/slideout-nav-selected-tab tab))))
