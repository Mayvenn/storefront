(ns checkout.header
  (:require [storefront.accessors.orders :as orders]
            [storefront.component :as component]
            [storefront.components.header :as storefront-header]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(defn desktop-header [{:keys [store user cart shopping signed-in the-ville? vouchers?]}]
  [:div.hide-on-mb.relative
   [:div.relative.border-bottom.border-light-gray {:style {:height "125px"}}
    [:div.max-960.mx-auto
     [:div.left (storefront-header/store-info signed-in store)]

     [:div.right
      [:div.h6.my2.flex.items-center
       (storefront-header/account-info signed-in user the-ville? vouchers?)
       [:div.pl2 (ui/shopping-bag {:style {
                                           :height (str ui/header-image-size "px") :width "28px"}
                                   :data-test "desktop-cart"}
                                  cart)]]]
     [:div.absolute.bottom-0.left-0.right-0
      [:div.mb4 (ui/clickable-logo {:event events/navigate-home
                                    :data-test "desktop-header-logo"
                                    :height "60px"})]]]]])

(defn component [{:keys [desktop-header-data item-count back]} _ _]
  (component/create
   (let [close-cart-route (utils/route-back-or-to back events/navigate-home)]
     [:div
      (desktop-header desktop-header-data)

      [:div.max-960.mx-auto.border-bottom.border-light-gray.flex.items-center

       [:div.col-2.hide-on-mb-tb
        [:a.h5.black.pointer.flex.justify-start.items-center close-cart-route
         (svg/left-caret {:class "stroke-dark-gray"
                          :height "1em" :width "1em"}) "Back to Shopping"]]

       [:div.col-1.hide-on-dt]

       [:div.flex-auto.py3.center.dark-gray
        {:data-test "mobile-cart"}
        "Shopping Bag - " (ui/pluralize-with-amount item-count "item")]

       [:div.col-2.hide-on-mb-tb]

       [:div.col-1.hide-on-dt
        [:a.h3.pointer.flex.items-center (merge close-cart-route
                                                {:data-test "cart-close" :title "Close"})
         (svg/close-x {:class "stroke-dark-gray fill-white"})]]]])))

(defn query [data]
  {:item-count          (orders/product-quantity (get-in data keypaths/order))
   :back                (first (get-in data keypaths/navigation-undo-stack))
   :desktop-header-data (storefront-header/query data)})

(defn built-component [data opts]
  (component/build component (query data) nil))
