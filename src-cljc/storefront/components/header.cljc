(ns storefront.components.header
  (:require [storefront.accessors.named-searches :as named-searches]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.stylists :as stylists]
            [storefront.accessors.nav :as nav]
            [storefront.accessors.experiments :as experiments]
            [storefront.assets :as assets]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.routes :as routes]
            [clojure.set :as set]))

(def sans-stylist? #{"store" "shop"})

(def signed-in? #{::signed-in-as-user ::signed-in-as-stylist})

(defn fake-href-menu-expand [keypath]
  (utils/fake-href events/control-menu-expand {:keypath keypath}))

(def hamburger
  (component/html
   [:a.block.px3.py4 (assoc (fake-href-menu-expand keypaths/menu-expanded)
                            :style {:width "70px"}
                            :data-test "hamburger")
    [:div.border-top.border-bottom.border-dark-gray {:style {:height "15px"}} [:span.hide "MENU"]]
    [:div.border-bottom.border-dark-gray {:style {:height "15px"}}]]))

(defn logo [height]
  (component/html
   [:a.block.img-logo.bg-no-repeat.bg-center.bg-contain.teal
    (assoc (utils/route-to events/navigate-home)
           :style {:height height}
           :title "Mayvenn"
           :item-prop "logo"
           :data-test "header-logo"
           :content (str "https:" (assets/path "/images/header_logo.svg")))]))

(defn shopping-bag [opts cart-quantity]
  [:a.relative.pointer.block (merge (utils/route-to events/navigate-cart)
                                    {:data-test "cart"}
                                    opts)
   (svg/bag {:class (str "absolute overlay m-auto "
                         (if (pos? cart-quantity) "fill-navy" "fill-black"))})
   (when (pos? cart-quantity)
     [:div.absolute.overlay.m-auto {:style {:height "9px"}}
      [:div.center.navy.h6.line-height-1 {:data-test "populated-cart"} cart-quantity]])])

(def header-image-size 36)

(defn ^:private stylist-portrait [portrait]
  (ui/circle-picture {:class "mx-auto"
                      :width (str header-image-size "px")}
                     (ui/square-image portrait header-image-size)))

(defn store-info [{:keys [store-slug store-nickname portrait] :as store}]
  (when-not (sans-stylist? store-slug)
    [:div.h6.my2.flex.items-center
     (when portrait
       [:div.left.pr2
        (stylist-portrait portrait)])
     [:div.dark-gray "Welcome to " [:span.black.medium store-nickname "'s"] " shop."]]))

(defn account-info [{:keys [email signed-in-state]}]
  [:div.h6
   (when (signed-in? signed-in-state)
     [:div
      "Signed in with: "
      [:a.teal (utils/route-to events/navigate-account-manage) email]])])

(def menu-link :a.h5.medium.inherit-color.py2.px3)

(def menu
  (component/html
   [:div.center
    [menu-link (utils/route-to events/navigate-shop-by-look)
     "Shop looks"]
    [menu-link (assoc (utils/route-to events/navigate-categories)
                      :on-mouse-enter (utils/expand-menu-callback keypaths/shop-menu-expanded)
                      :on-click       (utils/expand-menu-callback keypaths/shop-menu-expanded))
     "Shop hair"]
    [menu-link (utils/route-to events/navigate-content-guarantee)
     "Our Guarantee"]
    [menu-link {:on-mouse-enter (utils/collapse-menus-callback keypaths/header-menus)
                :href           "https://blog.mayvenn.com"}
     "Real Beauty"]]))

(defn component [{:keys [store user cart]} _ _]
  (component/create
   [:div.border-bottom.border-gray
    [:div.hide-on-mb.relative {:style {:height "150px"}}
     [:div.container
      [:div.absolute.bottom-0.left-0.right-0
       [:div.mb4 (logo "60px")]
       [:div.mb1 menu]]
      [:div.left (store-info store)]
      [:div.right
       [:div.h6.my2.flex.items-center
        (account-info user)
        [:div.pl2 (shopping-bag {:style {:height (str header-image-size "px") :width "28px"}}
                                (:cart-quantity cart))]]]]]
    [:div.hide-on-tb-dt.flex.items-center
     hamburger
     [:div.flex-auto.py3 (logo "40px")]
     (shopping-bag {:style {:height "70px" :width "70px"}}
                   (:cart-quantity cart))]]))

(defn minimal-component [_ _ _]
  (component/html
   [:div.border-bottom.border-gray.flex.items-center
    [:div.flex-auto.py3 (logo "40px")]]))

(defn signed-in-state [data]
  (if (stylists/own-store? data)
    ::signed-in-as-stylist
    (if (get-in data keypaths/user-email)
      ::signed-in-as-user
      ::signed-out)))

(defn query [data]
  ;; TODO: these are very similar to slideout nav queries... unify them somehow?
  (let [named-searches  (named-searches/current-named-searches data)
        signed-in-state (signed-in-state data)]
    {:store (-> (get-in data keypaths/store)
                (set/rename-keys {:store_slug        :store-slug
                                  :store_nickname    :store-nickname
                                  :instagram_account :instagram-account
                                  :styleseat_account :styleseat-account})
                (assoc :gallery? (stylists/gallery? data)
                       :expanded? (get-in data keypaths/store-info-expanded)))
     :user  {:expanded?       (get-in data keypaths/account-menu-expanded)
             :email           (get-in data keypaths/user-email)
             :signed-in-state signed-in-state}
     :cart  {:cart-quantity (orders/product-quantity (get-in data keypaths/order))}
     :shop  {:expanded? (get-in data keypaths/shop-menu-expanded)
             :sections  (cond-> [{:title "Shop hair"
                                  :shop-items (filter named-searches/is-extension? named-searches)}
                                 {:title "Shop closures & frontals"
                                  :shop-items (filter named-searches/is-closure-or-frontal? named-searches)}]
                          (= ::signed-in-as-stylist signed-in-state)
                          (conj {:title      "Stylist exclusives"
                                 :shop-items (filter named-searches/is-stylist-product? named-searches)}))}}))

(defn built-component [data opts]
  (if (nav/minimal-events (get-in data keypaths/navigation-event))
    (component/build minimal-component {} nil)
    (component/build component (query data) nil)))
