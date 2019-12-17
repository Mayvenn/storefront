(ns checkout.header
  (:require [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.header :as storefront-header]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(defn desktop-header [{:keys [store user cart signed-in vouchers?]}]
  [:div.hide-on-mb.relative
   [:div.relative.border-bottom.border-cool-gray {:style {:height "125px"}}
    [:div.max-960.mx-auto
     [:div.left (storefront-header/store-info signed-in store)]
     [:div.right
      [:div.h6.my2.flex.items-center
       (storefront-header/account-info signed-in user vouchers?)
       [:div.pl2 (ui/shopping-bag {:style {:height (str ui/header-image-size "px") :width "28px"}
                                   :data-test "desktop-cart"}
                                  cart)]]]
     [:div.absolute.bottom-0.left-0.right-0
      [:div.mb4 (ui/clickable-logo {:event events/navigate-home
                                    :data-test "desktop-header-logo"
                                    :height "60px"})]]]]])

(defcomponent component
  [{:keys [desktop-header-data
           item-count
           hide-back-to-shopping-link?
           back]} _ _]
  (let [close-cart-route (utils/route-back-or-to back events/navigate-home)]
    [:div
     (desktop-header desktop-header-data)
     (storefront-header/mobile-nav-header
      {:class "border-bottom border-gray border-width-2"}

      (when-not hide-back-to-shopping-link?
        [:a.pointer.inherit-color.flex.justify-start.items-center close-cart-route
         [:div (ui/back-caret {:width 16 :height 16})]
         [:div "Back to Shopping"]])

      [:div.content-1.proxima.center
       {:data-test "mobile-cart"}
       "Shopping Cart ( " [:span.bold item-count] " )"]

      [:a.flex.pointer (merge close-cart-route
                         {:data-test "cart-close" :title "Close"})
       (svg/x-sharp {:style {:width  "18px"
                             :height "18px"}})])]))

(defn query [data]
  (let [shop? (= "shop" (get-in data keypaths/store-slug))]
    {:item-count                  (orders/product-quantity (get-in data keypaths/order))
     :back                        (first (get-in data keypaths/navigation-undo-stack))
     :desktop-header-data         (storefront-header/query data)
     :hide-back-to-shopping-link? shop?}))

(defn built-component [data opts]
  (component/build component (query data) nil))
