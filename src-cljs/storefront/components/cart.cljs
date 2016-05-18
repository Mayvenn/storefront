(ns storefront.components.cart
  (:require [storefront.components.utils :as utils]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.products :as products]
            [storefront.components.formatters :refer [as-money as-money-or-free]]
            [storefront.components.svg :as svg]
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

(defn new-display-line-items [products order cart-quantities update-line-item-requests]
  (for [{product-id :product-id variant-id :id :as line-item} (orders/product-items order)]
    [:.clearfix.mb1.border-bottom.border-light-silver.py2 {:key variant-id}
     [:a.left.mr1
      [:img.border.border-light-silver.rounded-1 {:src (products/thumbnail-url products product-id)
                                                :alt (:product-name line-item)
                                                :style {:width "7.33em"
                                                        :height "7.33em"}}]]
     [:.overflow-hidden.h4.black.p1
      [:a.black.medium.titleize (products/summary line-item)]
      [:.mt1.line-height-2
       (when-let [length (-> line-item :variant-attrs :length)]
         [:.h5 "Length: " length])
       [:.h5 "Price: " (as-money (:unit-price line-item))]]
      [:.pt2.flex.items-center
       [:div.flex-auto
        [:a.gray
         {:href "#"
          :on-click (if false
                      utils/noop-callback
                      (utils/send-event-callback events/control-cart-remove variant-id))}
         "Remove"]]
       [:div
        (ui/counter (get cart-quantities variant-id)
                    (get update-line-item-requests variant-id)
                    (utils/send-event-callback events/control-cart-line-item-dec
                                               {:variant-id variant-id})
                    (utils/send-event-callback events/control-cart-line-item-inc
                                               {:variant-id variant-id}))]]]]))

(defn new-cart-component [{:keys [products
                                  order
                                  item-count
                                  coupon-code
                                  applying-coupon?
                                  updating?
                                  redirecting-to-paypal?
                                  shipping-methods
                                  cart-quantities
                                  update-line-item-requests]} owner]
  (om/component
   (html
    (ui/container
     [:.h2.center.py3.silver (str "You have " item-count
                         (if (>= 1 item-count) " item" " items")
                         " in your shopping bag.")]
     [:.align-left.col-12
      [:.h2.py1 "Review your order"]
      (new-display-line-items products order cart-quantities update-line-item-requests)
      [:form.flex.items-center.pt2
       {:on-submit (utils/send-event-callback events/control-cart-update-coupon)}
       [:.col-8.pr1
        (ui/text-field "Promo code" keypaths/cart-coupon-code coupon-code {})]
       [:.col-4.pl1.mb2.inline-block (ui/button "Apply"
                                                events/control-cart-update-coupon
                                                {:disabled? updating?
                                                 :show-spinner? applying-coupon?})]]
      [:div.mtn1
       (order-summary/redesigned-display-order-summary shipping-methods order)]
      [:div.border-top.border-light-silver.py2]
      [:form
       {:on-submit (utils/send-event-callback events/control-checkout-cart-submit)}
       (ui/submit-button "Check Out" {:spinning? false :disabled? updating?})]
      [:div.h4.gray.center.py2 "OR"]
      [:div.pb4 (ui/button
                 [:.col-12.flex.items-center.justify-center
                  [:.right-align.mr1 "Check out with"]
                  [:.h2.medium.sans-serif.italic "PayPal™"]]
                 events/control-checkout-cart-paypal-setup
                 {:show-spinner? redirecting-to-paypal?
                  :disabled? updating?
                  :color "bg-paypal-blue"})]]))))

(defn query [data]
  (let [cart-quantities (get-in data keypaths/cart-quantities)
        order           (get-in data keypaths/order)]
    {:order                     order
     :cart-quantities           cart-quantities
     :item-count                (orders/product-quantity order)
     :products                  (get-in data keypaths/products)
     :coupon-code               (get-in data keypaths/cart-coupon-code)
     :updating?                 (cart-update-pending? data)
     :applying-coupon?          (query/get {:request-key request-keys/add-promotion-code}
                                           (get-in data keypaths/api-requests))
     :redirecting-to-paypal?    (get-in data keypaths/cart-paypal-redirect)
     :shipping-methods          (get-in data keypaths/shipping-methods)
     :nav-message               (navigation/shop-now-navigation-message data)
     :update-line-item-requests (->> cart-quantities
                                     keys
                                     (map (juxt identity
                                                #(query/get
                                                  {:request-key
                                                   (conj request-keys/update-line-item %)}
                                                  (get-in data keypaths/api-requests))))
                                     (into {}))}))
(defn new-empty-cart-component [{:keys [nav-message]} owner]
  (om/component
   (html
    (ui/container
     [:.col-10.center.m-auto.py3
      (svg/bag {:height "70px" :width "70px"} 1)]

     [:p.h1.center "Oh No!"]

     [:.py3.line-height-3.gray.center "Your Shopping Bag is Empty."]

     (ui/button "Shop Now" [] (apply utils/route-to nav-message))))))


(defn cart-component [data owner]
  (om/component
   (html
    (if (experiments/three-steps-redesign? data)
      (let [component-data (query data)]
        (if (> (:item-count component-data) 0)
          (om/build new-cart-component component-data)
          (om/build new-empty-cart-component component-data)))
      (om/build old-cart-component data)))))
