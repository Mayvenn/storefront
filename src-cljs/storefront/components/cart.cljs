(ns storefront.components.cart
  (:require [storefront.components.utils :as utils]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.navigation :as navigation]
            [clojure.string :as string]
            [storefront.components.order-summary :as order-summary :refer [display-cart-summary display-line-items]]
            [storefront.request-keys :as request-keys]
            [storefront.keypaths :as keypaths]
            [storefront.hooks.experiments :as experiments]
            [storefront.components.ui :as ui]
            [storefront.utils.query :as query]))

(defn shopping-link-attrs [data]
  (apply utils/route-to (navigation/shop-now-navigation-message data)))

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
          {:on-submit (utils/send-event-callback events/control-cart-update-coupon)}
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
               :on-click (utils/send-event-callback events/control-cart-update-coupon)})
            "Apply Promo Code"]]]
         [:div
          [:div.order-summary-cart
           (display-cart-summary data cart)
           [:a.button.checkout.primary.full-link#checkout-link
            {:on-click (when-not (cart-update-pending? data)
                         (utils/send-event-callback events/control-checkout-cart-submit))}
            "Check Out"]
           [:div.or-divider [:span "OR"]]
           (let [redirecting (get-in data keypaths/cart-paypal-redirect)]
             [:a {:href "#"
                  :data-test "paypal-checkout"
                  :on-click (utils/send-event-callback events/control-checkout-cart-paypal-setup)}
              [:.paypal-checkout {:class (when redirecting "redirecting")}]])]]]]]]]))

(defn display-empty-cart [data]
  [:div
   [:p.empty-cart-message "OH NO!"]
   [:figure.empty-bag]
   [:p
    [:a.button.primary.continue.empty-cart
     (shopping-link-attrs data)
     "Shop Now"]]])

(defn old-cart-component [data owner]
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

(defn new-cart-component [{:keys [products
                                  order
                                  coupon-code
                                  updating?
                                  shipping-methods]} owner]
  (let [item-count (count (orders/product-items order))]
    (om/component
     (html
      [:div.col-10.m-auto
       [:.h2.py3.center.gray (str "You have "
                                  item-count
                                  (if (>= 1 item-count)
                                    " item"
                                    " items")
                                  " in your shopping bag.")]
       [:.h2.py1 "Review your order"]
       (order-summary/redesigned-display-line-items products order)
       [:div.flex.items-center
        [:.col-8.pr1
         (ui/text-field "Promo code" keypaths/cart-coupon-code coupon-code {})]
        [:.col-4.pl1.mb2.inline-block (ui/button "Apply"
                                                 events/control-cart-update-coupon
                                                 {:show-spinner? updating?})]]
       (order-summary/redesigned-display-order-summary shipping-methods order)
       [:div.border-top.border-light-gray.py2]
       [:form
        {:on-submit (utils/send-event-callback events/control-checkout-cart-submit)}
        (ui/submit-button "Check Out" false updating?)]
       [:div.gray.center.py3 "OR"]
       [:div (ui/button
              [:.flex
               [:.flex-auto "Check out with"]
               [:div.img-paypal.bg-no-repeat.bg-contain {:style {:height "18px" :width "100%"}}]]
                        events/control-checkout-cart-paypal-setup
                        {:show-spinner? false
                         :color "btn-paypal-yellow-gradient"})]
       ]))))

(defn query [data]
  {:products         (get-in data keypaths/products)
   :order            (get-in data keypaths/order)
   :updating?        (cart-update-pending? data)
   :applying-coupon? (query/get {:request-key request-keys/add-promotion-code}
                                 (get-in data keypaths/api-requests))
   :shipping-methods (get-in data keypaths/shipping-methods)})

(defn cart-component [data owner]
  (om/component
   (html
    (if (experiments/three-steps-redesign? data)
      (om/build new-cart-component (query data))
      (om/build old-cart-component data)))))
