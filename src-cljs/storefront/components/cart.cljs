(ns storefront.components.cart
  (:require [storefront.components.utils :as utils]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.navigation :as navigation]
            [clojure.string :as string]
            [storefront.components.order-summary :refer [display-order-summary display-line-items]]
            [storefront.request-keys :as request-keys]
            [storefront.keypaths :as keypaths]
            [storefront.hooks.experiments :as experiments]
            [storefront.utils.query :as query]))

(defn shopping-link-attrs [data]
  (apply utils/route-to data (navigation/shop-now-navigation-message data)))

(defn cart-update-pending? [data]
  (let [request-key-prefix (comp vector first :request-key)]
    (some #(query/get % (get-in data keypaths/api-requests))
          [{:request-key request-keys/checkout-cart}
           {:request-key request-keys/add-promotion-code}
           {request-key-prefix request-keys/update-line-item}
           {request-key-prefix request-keys/delete-line-item}])))

(defn display-full-cart [data owner]
  (let [cart (get-in data keypaths/order)]
    [:div
     [:div#update-cart
      [:div.inside-cart-form
       [:div.cart-items
        [:div.cart-line-items
         (display-line-items data cart true)]
        [:div.cart-bottom
         [:form
          {:on-submit (utils/send-event-callback data events/control-cart-update-coupon)}
          [:div.coupon-cart
           [:h4 "Have a Promo Code?"]
           [:div.coupon-container
            [:label "Enter a promo code:"]
            [:input.coupon-code-input
             (merge
              (utils/change-text data owner keypaths/cart-coupon-code)
              {:type "text"
               :name "coupon-code"})]]
           [:div.primary.button#update-button
            (let [spinning (query/get {:request-key request-keys/add-promotion-code}
                                      (get-in data keypaths/api-requests))]
              {:type "submit"
               :name "update"
               :class (when spinning "saving")
               :disabled spinning
               :on-click (utils/send-event-callback data events/control-cart-update-coupon)})
            "Apply Promo Code"]]]
         [:div
          [:div.order-summary-cart
           (display-order-summary data cart)
           [:a.button.checkout.primary.full-link#checkout-link
            {:on-click (when-not (cart-update-pending? data)
                         (utils/send-event-callback data events/control-checkout-cart-submit))}
            "Check Out"]
           (if (experiments/paypal? data)
             (list
              [:div.or-divider [:span "OR"]]
              [:a {:href "#"
                   :data-test "paypal-checkout"
                   :on-click (utils/send-event-callback data events/control-checkout-cart-paypal-setup)}
               [:.paypal-checkout]])
             [:a.cart-continue.full-link.old-school-link.extra-spacing
              (shopping-link-attrs data)
              "Continue shopping"])]]]]]]]))

(defn display-empty-cart [data]
  [:div
   [:p.empty-cart-message "OH NO!"]
   [:figure.empty-bag]
   [:p
    [:a.button.primary.continue.empty-cart
     (shopping-link-attrs data)
     "Shop Now"]]])

(defn cart-component [data owner]
  (om/component
   (html
    [:div
     [:div.page-heading "My Cart"]
     [:div.cart-container
      (let [cart (get-in data keypaths/order)]
        (if (and (:state cart)
                 (:number cart)
                 (-> cart orders/product-items count (> 0)))
          (display-full-cart data owner)
          (display-empty-cart data)))]
     [:div.home-actions-top
      [:div.guarantee]
      [:div.free-shipping-action]
      [:div.keep-shopping
       [:a.full-link (shopping-link-attrs data)]]]])))
