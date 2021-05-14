(ns checkout.header
  (:require [storefront.accessors.orders :as orders]
            [storefront.accessors.sites :as sites]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.header :as storefront-header]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(defn desktop-header [{:keys [store user cart signed-in vouchers?]}]
  (component/html
   [:div.hide-on-mb.relative
    [:div.relative.border-bottom.border-gray
     [:div.flex.justify-between.px8
      [:div {:key "store-info"} ^:inline (storefront-header/store-info signed-in store)]
      [:div {:key "account-info"}
       [:div.my2
        ^:inline (storefront-header/account-info signed-in user vouchers? store)]]]
     [:div.flex.justify-between.px8.mb4
      [:div {:style {:width "33px"}}]
      [:div.flex-grow-1 {:key "logo"}
       (ui/clickable-logo {:event events/navigate-home
                           :data-test "desktop-header-logo"
                           :height "44px"})]
      ^:inline (ui/shopping-bag {:style     {:height "44px"
                                             :width  "33px"}
                                 :data-test "desktop-cart"}
                                cart)]]]))

(defcomponent component
  [{:keys [desktop-header-data
           item-count
           back-to-shopping-link?
           back]} _ _]
  (let [close-cart-route (utils/route-back-or-to back
                                                 events/navigate-home)]
    [:div
     (desktop-header desktop-header-data)
     [:div.hide-on-tb-dt
      (storefront-header/mobile-nav-header
       {:class "border-bottom border-gray border-width-1 m-auto col-7-on-dt"}

       (when back-to-shopping-link?
         (component/html
          [:a.pointer.inherit-color.flex.items-center.ml1.content-3.proxima
           close-cart-route
           [:div (ui/back-caret {:width 16 :height 16})]
           [:div "Back to Shopping"]]))

       (component/html
        [:div.content-1.proxima.center
         {:data-test "mobile-cart"}
         "Shopping Bag ( " [:span.bold item-count] " )"])

       (component/html
        [:a.flex.pointer
         (merge close-cart-route
                {:data-test "cart-close"
                 :title     "Close"})
         (svg/x-sharp {:style {:width  "18px"
                               :height "18px"}})]))]]))

(defn query
  [data]
  (cond-> {:item-count          (orders/displayed-cart-count (get-in data keypaths/order))
           :back                (first (get-in data keypaths/navigation-undo-stack))
           :desktop-header-data (storefront-header/query data)}

    (= :classic (sites/determine-site data))
    (merge {:back-to-shopping-link? true})))

(defn built-component [data opts]
  (component/build component (query data) nil))
