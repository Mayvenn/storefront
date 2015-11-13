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
           [:h4 (if (experiments/simplify-funnel? data)
                  "Have a Promo Code?"
                  "Have a Coupon Code?")]
           [:div.coupon-container
            [:label (if (experiments/simplify-funnel? data)
                      "Enter a promo code:"
                      "Enter a coupon code:")]
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
               :class [(when spinning "saving")
                       (when (experiments/simplify-funnel? data) "bright")]
               :disabled spinning
               :on-click (utils/send-event-callback data events/control-cart-update-coupon)})
            (if (experiments/simplify-funnel? data)
              "Apply Promo Code"
              "Apply Coupon Code")]]]
         [:div
          [:div.order-summary-cart
           (display-order-summary data cart)
           [:a.button.checkout.primary#checkout-link
            {:class ["full-link" (when (experiments/simplify-funnel? data) "bright")]
             :on-click (when-not (cart-update-pending? data)
                         (utils/send-event-callback data events/control-checkout-cart-submit))}
            (if (experiments/simplify-funnel? data)
                      "Check Out"
                      "Checkout")]
           (if (experiments/paypal? data)
             (list
              [:div.or-divider [:span "OR"]]
              [:img.paypal-checkout {:src "https://www.paypalobjects.com/webstatic/en_US/i/buttons/checkout-logo-large.png"
                                     :alt "Check out with PayPal"}])
             [:a.cart-continue
              (merge
               (shopping-link-attrs data)
               {:class (if (experiments/simplify-funnel? data)
                         "full-link old-school-link extra-spacing"
                         "continue button gray")})
              "Continue shopping"])]]]]]]]))

(defn display-empty-cart [data]
  [:div
   [:p.empty-cart-message "OH NO!"]
   [:figure.empty-bag]
   [:p
    [:a.button.primary.continue.empty-cart
     (merge
      (shopping-link-attrs data)
      (when (experiments/simplify-funnel? data) {:class "bright"}))
     (if (experiments/simplify-funnel? data)
       "Shop Now"
       "Let's Fix That")]]])

(defn cart-component [data owner]
  (om/component
   (html
    [:div
     (when (experiments/simplify-funnel? data)
       [:div.page-heading "My Cart"])
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
