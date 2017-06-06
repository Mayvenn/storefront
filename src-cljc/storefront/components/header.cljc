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
            [storefront.routes :as routes]))

(def sans-stylist? #{"store" "shop"})

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

(defn shopping-bag [cart-quantity]
  [:a.relative.pointer.block (assoc (utils/route-to events/navigate-cart)
                                    :style {:height "70px" :width "70px"}
                                    :data-test "cart")
   (svg/bag {:class (str "absolute overlay m-auto "
                         (if (pos? cart-quantity) "fill-navy" "fill-black"))})
   (when (pos? cart-quantity)
     [:div.absolute.overlay.m-auto {:style {:height "9px"}}
      [:div.center.navy.h6.line-height-1 {:data-test "populated-cart"} cart-quantity]])])

(defn component [data _ _]
  (component/create
   [:div.border-bottom.border-gray.flex.items-center
    hamburger
    [:div.flex-auto.py3 (logo "40px")]
    (shopping-bag (:cart-quantity (:cart data)))]))

(defn minimal-component [store _ _]
  (component/create
   [:div.border-bottom.border-gray.flex.items-center
    [:div.flex-auto.py3 (logo "40px")]]))

(defn store-query [data]
  {:expanded? (get-in data keypaths/store-info-expanded)
   :gallery?  (stylists/gallery? data)
   :store     (get-in data keypaths/store)})

(defn auth-query [data]
  {:expanded?  (get-in data keypaths/account-menu-expanded)
   :user-email (get-in data keypaths/user-email)})

(defn cart-query [data]
  {:cart-quantity (orders/product-quantity (get-in data keypaths/order))})

(defn shop-query [data]
  {:expanded?      (get-in data keypaths/shop-menu-expanded)
   :named-searches (named-searches/current-named-searches data)})

(defn context-query [data]
  {:stylist?    (stylists/own-store? data)
   :nav-message (get-in data keypaths/navigation-message)})

(defn query [data]
  {:store   (store-query data)
   :auth    (auth-query data)
   :cart    (cart-query data)
   :shop    (shop-query data)
   :context (context-query data)})

(defn built-component [data opts]
  (if (nav/minimal-events (get-in data keypaths/navigation-event))
    (component/build minimal-component (store-query data) nil)
    (component/build component (query data) nil)))
